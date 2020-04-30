package com.bitheads.braincloud.comms;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bitheads.braincloud.client.BrainCloudClient;
import com.bitheads.braincloud.client.IRelayCallback;
import com.bitheads.braincloud.client.IRelayConnectCallback;
import com.bitheads.braincloud.client.IRelaySystemCallback;
import com.bitheads.braincloud.client.IServerCallback;
import com.bitheads.braincloud.client.RelayConnectionType;
import com.bitheads.braincloud.client.RelayConnectionType;
import com.bitheads.braincloud.client.ServiceName;
import com.bitheads.braincloud.client.ServiceOperation;
import com.bitheads.braincloud.services.AuthenticationService;

public class RelayComms {

    enum RelayCallbackType {
        ConnectSuccess,
        ConnectFailure,
        Relay,
        System
    }

    private final int CONTROL_BYTES_SIZE    = 1;
    private final int CHANNEL_COUNT         = 4;
    private final int MAX_PACKET_ID_HISTORY = 128;

    private final int MAX_PLAYERS       = 128;
    private final int INVALID_NET_ID    = MAX_PLAYERS;

    // Messages sent from Client to Relay-Server
    private final int CL2RS_CONNECTION          = 129;
    private final int CL2RS_DISCONNECT          = 130;
    private final int CL2RS_RELAY               = 131;
    private final int CL2RS_PING                = 133;
    private final int CL2RS_RSMG_ACKNOWLEDGE    = 134;
    private final int CL2RS_ACKNOWLEDGE         = 135;

    // Messages sent from Relay-Server to Client
    private final int RS2CL_RSMG         = 129;
    private final int RS2CL_PONG         = CL2RS_PING;
    private final int RS2CL_ACKNOWLEDGE  = CL2RS_ACKNOWLEDGE;

    private final int RELIABLE_BIT = 0x8000;
    private final int ORDERED_BIT  = 0x4000;

    private final long CONNECT_RESEND_INTERVAL_MS = 500;

    private final int MAX_PACKET_ID = 0xFFF;
    private final int PACKET_LOWER_THRESHOLD = MAX_PACKET_ID * 25 / 100;
    private final int PACKET_HIGHER_THRESHOLD = MAX_PACKET_ID * 75 / 100;

    private class RelayCallback {
        public RelayCallbackType _type;
        public String _message;
        public JSONObject _json;
        public int _netId;
        public byte[] _data;

        RelayCallback(RelayCallbackType type) {
            _type = type;
        }

        RelayCallback(RelayCallbackType type, String message) {
            _type = type;
            _message = message;
        }

        RelayCallback(RelayCallbackType type, JSONObject json) {
            _type = type;
            _json = json;
        }

        RelayCallback(RelayCallbackType type, int netId, byte[] data) {
            _type = type;
            _netId = netId;
            _data = data;
        }
    }

    private class ConnectInfo {
        public String _passcode;
        public String _lobbyId;

        ConnectInfo(String passcode, String lobbyId) {
            _passcode = passcode;
            _lobbyId = lobbyId;
        }
    }

    private class WSClient extends WebSocketClient {
        public WSClient(String ip) throws Exception {
            super(new URI(ip));
        }
        
        @Override
        public void onMessage(String message) {
            onRecv(ByteBuffer.wrap(message.getBytes()));
        }
        
        @Override
        public void onMessage(ByteBuffer bytes) {
            onRecv(bytes);
        }

        @Override
        public void onOpen(ServerHandshake handshake) {
            System.out.println("Relay WS Connected");
            onWSConnected();
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            System.out.println("Relay WS onClose: " + reason + ", code: " + Integer.toString(code) + ", remote: " + Boolean.toString(remote));
            disconnect();
            synchronized(_callbackEventQueue) {
                _callbackEventQueue.add(new RelayCallback(RelayCallbackType.ConnectFailure, "webSocket onClose: " + reason));
            }
        }

        @Override
        public void onError(Exception e) {
            e.printStackTrace();
            disconnect();
            synchronized(_callbackEventQueue) {
                _callbackEventQueue.add(new RelayCallback(RelayCallbackType.ConnectFailure, "webSocket onError"));
            }
        }
    }

    public class UdpRsmgPacket {
        public int id;
        public JSONObject json;
    }

    class RelayPacket {
        public int packetId;
        public int netId;
        public byte[] data;

        public RelayPacket(int _packetId, int _netId, byte[] _data) {
            packetId = _packetId;
            netId = _netId;
            data = _data;
        }
    }

    class Reliable {
        public ByteBuffer buffer;
        public int packetId;
        public int channel;
        public long sendTimeMs;
        public long resendTimeMs;
        public long waitTimeMs;

        public Reliable(ByteBuffer _buffer, int _channel, int _packetId) {
            buffer = _buffer;
            channel = _channel;
            packetId = _packetId;
            sendTimeMs = System.currentTimeMillis();
            resendTimeMs = sendTimeMs;
            waitTimeMs = channel <= 1 ? 50 : channel == 2 ? 150 : 250;
        }
    }

    private ArrayList<Reliable> _reliables = new ArrayList<Reliable>();
    private ArrayList<UdpRsmgPacket> _udpRsmgPackets = new ArrayList<UdpRsmgPacket>();
    private int _nextExpectedUdpRsmgPacketId = 0;

    private BrainCloudClient _client;
    private boolean _loggingEnabled = false;
    private IRelayConnectCallback _connectCallback = null;
    private ArrayList<RelayCallback> _callbackEventQueue = new ArrayList<RelayCallback>();

    private boolean _isConnected = false;
    private boolean _isConnecting = false;
    private long _lastConnectTryTime = 0;
    private int _netId = -1;
    private String _ownerId = null;
    private ConnectInfo _connectInfo = null;

    private HashMap<Integer, String> _netIdToProfileId = new HashMap<Integer, String>();
    private HashMap<String, Integer> _profileIdToNetId = new HashMap<String, Integer>();

    private int _sendPacketId[] = new int[CHANNEL_COUNT * 2]; // *2 here is for reliable vs unreliable
    private ArrayList<ArrayList<Integer>> _packetIdHistory = new ArrayList<ArrayList<Integer>>();
    private int _recvPacketId[] = new int[CHANNEL_COUNT * 2];
    private ArrayList<ArrayList<RelayPacket>> _orderedReliablePackets = new ArrayList<ArrayList<RelayPacket>>();
    
    private int _ping = 999;
    private boolean _pingInFlight = false;
    private int _pingIntervalMS = 1000;
    private long _lastPingTime = 0;
    
    private RelayConnectionType _connectionType = RelayConnectionType.WEBSOCKET;
    private WSClient _webSocketClient;
    private Socket _tcpSocket;
    private DatagramSocket _udpSocket;
    private InetAddress _udpAddr;
    private int _udpPort;
    private Object _lock = new Object();

    private IRelayCallback _relayCallback = null;
    private IRelaySystemCallback _relaySystemCallback = null;
    
    public RelayComms(BrainCloudClient client) {
        _client = client;
        for (int i = 0; i < CHANNEL_COUNT * 2; ++i) {
            _sendPacketId[i] = 0;
            _recvPacketId[i] = -1;
            _packetIdHistory.add(new ArrayList<Integer>());
        }
        for (int i = 0; i < CHANNEL_COUNT; ++i) {
            _orderedReliablePackets.add(new ArrayList<RelayPacket>());
        }
    }

    public boolean getLoggingEnabled() {
        return _loggingEnabled;
    }

    public void enableLogging(boolean isEnabled) {
        _loggingEnabled = isEnabled;
    }

    public void connect(RelayConnectionType connectionType, JSONObject options, IRelayConnectCallback callback) {
        if (_isConnected) {
            disconnect();
        }

        _connectionType = connectionType;
        _isConnected = false;
        _connectCallback = callback;
        _ping = 999;
        _pingInFlight = false;
        _netIdToProfileId.clear();
        _profileIdToNetId.clear();
        _netId = -1;
        _ownerId = null;

        if (options == null) {
            _callbackEventQueue.add(new RelayCallback(RelayCallbackType.ConnectFailure, "Invalid arguments"));
            return;
        }

        final boolean ssl;
        final String host;
        final int port;
        final String passcode;
        final String lobbyId;

        try {
            ssl = options.has("ssl") ? options.getBoolean("ssl") : false;
            host = options.getString("host");
            port = options.getInt("port");
            passcode = options.getString("passcode");
            lobbyId = options.getString("lobbyId");
        } catch (JSONException e) {
            e.printStackTrace();
            _callbackEventQueue.add(new RelayCallback(RelayCallbackType.ConnectFailure, "Invalid arguments"));
            return;
        }

        _connectInfo = new ConnectInfo(passcode, lobbyId);

        // connect...
        switch (_connectionType) {
            case WEBSOCKET: {
                try {
                    String uri = (ssl ? "wss://" : "ws://") + host + ":" + port;

                    _webSocketClient = new WSClient(uri);
        
                    if (ssl) {
                        setupSSL();
                    }
                    
                    _webSocketClient.connect();
                } catch (Exception e) {
                    e.printStackTrace();
                    _callbackEventQueue.add(new RelayCallback(RelayCallbackType.ConnectFailure, "Failed to connect"));
                    disconnect();
                    return;
                }
                break;
            }
            case TCP: {
                Thread connectionThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            _tcpSocket = new Socket(InetAddress.getByName(host), port);
                            _tcpSocket.setTcpNoDelay(true);
                            if (_loggingEnabled) {
                                System.out.println("RELAY TCP: Connected");
                            }
                            onTCPConnected();
                        } catch (Exception e) {
                            e.printStackTrace();
                            _callbackEventQueue.add(new RelayCallback(RelayCallbackType.ConnectFailure, "Failed to connect"));
                            disconnect();
                            return;
                        }
                    }
                });
                connectionThread.start();
                break;
            }
            case UDP: {
                try {
                    _udpAddr = InetAddress.getByName(host);
                    _udpSocket = new DatagramSocket();
                    _udpPort = port;
                    if (_loggingEnabled) {
                        System.out.println("RELAY UDP: Socket Open");
                    }
                    onUDPConnected();
                } catch (Exception e) {
                    e.printStackTrace();
                    _callbackEventQueue.add(new RelayCallback(RelayCallbackType.ConnectFailure, "Failed to connect"));
                    disconnect();
                    return;
                }
                break;
            }
            default: {
                _callbackEventQueue.add(new RelayCallback(RelayCallbackType.ConnectFailure, "Protocol Unimplemented"));
            }
        }
    }

    public void disconnect() {
        if (_isConnected && _connectionType == RelayConnectionType.UDP) {
            ByteBuffer buffer = ByteBuffer.allocate(3);
            buffer.order(ByteOrder.BIG_ENDIAN);
            buffer.putShort((short)3);
            buffer.put((byte)CL2RS_DISCONNECT);
            send(buffer);
        }

        synchronized(_lock) {
            _isConnected = false;
            _isConnecting = false;

            if (_webSocketClient != null) {
                _webSocketClient.close();
                _webSocketClient = null;
            }

            try {
                if (_tcpSocket != null) {
                    _tcpSocket.close();
                    _tcpSocket = null;
                }
            } catch (Exception e) {
                _tcpSocket = null;
            }

            try {
                if (_udpSocket != null) {
                    _udpSocket.close();
                    _udpSocket = null;
                }
            } catch (Exception e) {
                _udpSocket = null;
            }

            for (int i = 0; i < CHANNEL_COUNT * 2; ++i) {
                _sendPacketId[i] = 0;
                _recvPacketId[i] = -1;
                _packetIdHistory.get(i).clear();
            }
            for (int i = 0; i < CHANNEL_COUNT; ++i) {
                _orderedReliablePackets.get(i).clear();
            }
            _connectInfo = null;

            _udpRsmgPackets.clear();
            _nextExpectedUdpRsmgPacketId = 0;
        }
    }

    public boolean isConnected() {
        boolean ret;
        synchronized(_lock) {
            ret = _isConnected;
        }
        return ret;
    }

    public int getPing() {
        return _ping;
    }

    public void setPingInterval(int intervalMS) {
        _pingIntervalMS = intervalMS;
    }

    public String getOwnerProfileId() {
        return _ownerId;
    }

    public String getProfileIdForNetId(int netId) {
        return _netIdToProfileId.get(netId);
    }

    public int getNetIdForProfileId(String profileId) {
        if (_profileIdToNetId.containsKey(profileId))
        {
            return _profileIdToNetId.get(profileId);
        }
        return INVALID_NET_ID;
    }
    
    public void registerRelayCallback(IRelayCallback callback) {
        _relayCallback = callback;
    }
    public void deregisterRelayCallback() {
        _relayCallback = null;
    }
    
    public void registerSystemCallback(IRelaySystemCallback callback) {
        _relaySystemCallback = callback;
    }
    public void deregisterSystemCallback() {
        _relaySystemCallback = null;
    }

    private void setupSSL() throws Exception {

        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[]{};
            }

            public void checkClientTrusted(X509Certificate[] chain, String authType) 
                    throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) 
                    throws CertificateException {
            }
        }};

        // Install the all-trusting trust manager
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new SecureRandom());
        SSLSocketFactory factory = sc.getSocketFactory();

        _webSocketClient.setSocket(factory.createSocket());
    }

    private void onWSConnected() {
        try {
            send(CL2RS_CONNECTION, buildConnectionRequest());
        } catch(Exception e) {
            e.printStackTrace();
            disconnect();
            synchronized(_callbackEventQueue) {
                _callbackEventQueue.add(new RelayCallback(RelayCallbackType.ConnectFailure, "Failed to build Connection Request"));
            }
        }
    }

    private void onTCPConnected() {
        try {
            startTCPReceivingThread();
            send(CL2RS_CONNECTION, buildConnectionRequest());
        } catch(Exception e) {
            e.printStackTrace();
            disconnect();
            synchronized(_callbackEventQueue) {
                _callbackEventQueue.add(new RelayCallback(RelayCallbackType.ConnectFailure, "Failed to build Connection Request"));
            }
        }
    }

    private void onUDPConnected() {
        try {
            startUDPReceivingThread();
            _isConnecting = true;
            _lastConnectTryTime = System.currentTimeMillis();
            send(CL2RS_CONNECTION, buildConnectionRequest());
        } catch(Exception e) {
            e.printStackTrace();
            disconnect();
            synchronized(_callbackEventQueue) {
                _callbackEventQueue.add(new RelayCallback(RelayCallbackType.ConnectFailure, "Failed to build Connection Request"));
            }
        }
    }

    private JSONObject buildConnectionRequest() throws Exception {
        JSONObject json = new JSONObject();

        json.put("lobbyId", _connectInfo._lobbyId);
        json.put("profileId", _client.getAuthenticationService().getProfileId());
        json.put("passcode", _connectInfo._passcode);

        return json;
    }

    private void send(int netId, JSONObject json) {
        send(netId, json.toString());
    }

    private void send(int netId, String text) {
        if (_loggingEnabled) {
            System.out.println("RELAY SEND: " + text);
        }

        byte[] textBytes = text.getBytes(StandardCharsets.US_ASCII);
        int bufferSize = textBytes.length + 3;
        
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short)bufferSize);
        buffer.put((byte)netId);
        buffer.put(textBytes, 0, textBytes.length);
        buffer.rewind();

        send(buffer);
    }

    private void send(ByteBuffer buffer) {
        buffer.rewind();

        try {
            synchronized(_lock) {
                switch (_connectionType) {
                    case WEBSOCKET: {
                        if (_webSocketClient != null) {
                            _webSocketClient.send(buffer);
                        }
                        break;
                    }
                    case TCP: {
                        if (_tcpSocket != null) {
                            byte[] bytes = buffer.array();
                            _tcpSocket.getOutputStream().write(bytes, 0, bytes.length);
                            _tcpSocket.getOutputStream().flush();
                        }
                        break;
                    }
                    case UDP: {
                        if (_udpSocket != null) {
                            byte[] bytes = buffer.array();
                            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, _udpAddr, _udpPort);
                            _udpSocket.send(packet);
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            disconnect();
            synchronized(_callbackEventQueue) {
                _callbackEventQueue.add(new RelayCallback(RelayCallbackType.ConnectFailure, "RELAY Send Failed"));
            }
        }
    }

    public void sendRelay(byte[] data, int toNetId, boolean reliable, boolean ordered, int channel) {
        if (!isConnected()) return;

        if (!((toNetId >= 0 && toNetId < MAX_PLAYERS) || toNetId == CL2RS_RELAY))
        {
            synchronized(_callbackEventQueue) {
                _callbackEventQueue.add(new RelayCallback(RelayCallbackType.ConnectFailure, "Invalid Net Id: " + toNetId));
            }
            return;
        }
        if (data.length > 1024)
        {
            synchronized(_callbackEventQueue) {
                _callbackEventQueue.add(new RelayCallback(RelayCallbackType.ConnectFailure, "Packet too big " + data.length + " > max 1024"));
            }
            return;
        }

        int bufferSize = data.length + 5;

        // Relay Header
        int rh = 0;
        if (reliable) rh |= RELIABLE_BIT;
        if (ordered) rh |= ORDERED_BIT;
        rh |= channel << 12;
        int channelIdx = channel + (reliable ? 0 : CHANNEL_COUNT);
        int packetId = _sendPacketId[channelIdx];
        rh |= packetId;
        _sendPacketId[channelIdx] = (packetId + 1) & 0xFFF;
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short)bufferSize);
        buffer.put((byte)toNetId);
        buffer.putShort((short)rh);
        buffer.put(data, 0, data.length);
        send(buffer);

        if (reliable && _connectionType == RelayConnectionType.UDP) {
            synchronized(_lock) {
                _reliables.add(new Reliable(buffer, channel, packetId));
            }
        }
    }

    private void startTCPReceivingThread() {
        Thread receiveThread = new Thread(new Runnable() {
            @Override
            public void run() {
                DataInputStream in;
                try {
                    synchronized(_lock) {
                        in = new DataInputStream(_tcpSocket.getInputStream());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    disconnect();
                    synchronized(_callbackEventQueue) {
                        _callbackEventQueue.add(new RelayCallback(RelayCallbackType.ConnectFailure, "TCP Connect Failed"));
                    }
                    return;
                }
                while (true) {
                    try {
                        int len = in.readShort() & 0xFFFF;
                        byte[] bytes = new byte[len - 2];
                        in.readFully(bytes);

                        ByteBuffer buffer = ByteBuffer.allocate(len);
                        buffer.order(ByteOrder.BIG_ENDIAN);
                        buffer.putShort((short)len);
                        buffer.put(bytes, 0, bytes.length);
                        buffer.rewind();
                        
                        onRecv(buffer);
                    } catch (Exception e) {
                        e.printStackTrace();
                        disconnect();
                        synchronized(_callbackEventQueue) {
                            _callbackEventQueue.add(new RelayCallback(RelayCallbackType.ConnectFailure, "TCP Connect Failed"));
                        }
                        return;
                    }
                }
            }
        });

        receiveThread.start();
    }

    private void startUDPReceivingThread() {
        Thread receivingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] receiveData = new byte[1400];
                DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);

                try {
                    while (true) {
                        DatagramSocket socket = _udpSocket;
                        if (socket == null) {
                            break;
                        }
                        try {
                            socket.receive(packet);
                        } catch (SocketTimeoutException e) {
                            continue;
                        } catch (SocketException e) {
                            if (e.getMessage().equals("socket closed")) {
                                break; // Leave peacefully
                            }
                            throw e;
                        }

                        ByteBuffer buffer = ByteBuffer.allocate(packet.getLength());
                        buffer.order(ByteOrder.BIG_ENDIAN);
                        buffer.put(packet.getData(), 0, packet.getLength());
                        buffer.rewind();
                        
                        onRecv(buffer);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    disconnect();
                    synchronized(_callbackEventQueue) {
                        _callbackEventQueue.add(new RelayCallback(RelayCallbackType.ConnectFailure, "UDP Receive Failed"));
                    }
                    return;
                }
            }
        });

        receivingThread.start();
    }

    private void sendPing() {
        if (_pingInFlight) return;
        if (_loggingEnabled) {
            System.out.println("RELAY SEND: PING");
        }

        ByteBuffer buffer = ByteBuffer.allocate(5);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short)5);
        buffer.put((byte)CL2RS_PING);
        buffer.putShort((short)_ping);
        send(buffer);
        
        _lastPingTime = System.currentTimeMillis();
        _pingInFlight = true;
    }

    private void onRecv(ByteBuffer buffer) {
        int len = buffer.limit();
        if (len < 3) {
            disconnect();
            synchronized(_callbackEventQueue) {
                _callbackEventQueue.add(new RelayCallback(RelayCallbackType.ConnectFailure, "Relay Recv Error: packet cannot be smaller than 3 bytes"));
            }
            return;
        }

        buffer.rewind();
        buffer.order(ByteOrder.BIG_ENDIAN);

        int size = buffer.getShort() & 0xFFFF;
        int netId = buffer.get() & 0xFF;

        if (len < size) {
            disconnect();
            synchronized(_callbackEventQueue) {
                _callbackEventQueue.add(new RelayCallback(RelayCallbackType.ConnectFailure, "Relay Recv Error: Packet is smaller than header's size"));
            }
            return;
        }

        if (netId == RS2CL_RSMG) {
            onRSMG(buffer, size - 3);
        }
        else if (netId == RS2CL_PONG) {
            onPONG();
        }
        else if (netId == RS2CL_ACKNOWLEDGE) {
            if (size < 5) {
                disconnect();
                synchronized(_callbackEventQueue) {
                    _callbackEventQueue.add(new RelayCallback(RelayCallbackType.ConnectFailure, "Relay Recv Error: ack packet cannot be smaller than 5 bytes"));
                }
                return;
            }
            if (_connectionType == RelayConnectionType.UDP) {
                onACK(buffer, size - 3);
            }
        }
        else if (netId < MAX_PLAYERS) {
            // if (_loggingEnabled) { // This is overkill
            //     System.out.println("RELAY MSG From: " + netId);
            // }
            if (size < 5) {
                disconnect();
                synchronized(_callbackEventQueue) {
                    _callbackEventQueue.add(new RelayCallback(RelayCallbackType.ConnectFailure, "Relay Recv Error: relay packet cannot be smaller than 5 bytes"));
                }
                return;
            }
            onRelay(buffer, netId, size - 3);
        }
        else {
            disconnect();
            synchronized(_callbackEventQueue) {
                _callbackEventQueue.add(new RelayCallback(RelayCallbackType.ConnectFailure, "Relay Recv Error: Unknown netId: " + netId));
            }
            return;
        }
    }

    private void onACK(ByteBuffer buffer, int size) {
        int rh = buffer.getShort() & 0xFFFF;
        int channel = (rh >> 12) & 0x3;
        int packetId = rh & 0xFFF;

        synchronized(_lock) {
            for (int i = 0; i < _reliables.size(); ++i) {
                Reliable reliable = _reliables.get(i);
                if (reliable.channel == channel && reliable.packetId == packetId) {
                    _reliables.remove(i);
                    if (_loggingEnabled) {
                        System.out.println("RELAY ACK: " + packetId + ", " + channel);
                    }
                    break;
                }
            }
        }
    }

    private boolean packetLE(int a, int b) {
        if (a > PACKET_HIGHER_THRESHOLD && b <= PACKET_LOWER_THRESHOLD) {
            return true;
        }
        return a <= b;
    }
    
    private void onRelay(ByteBuffer buffer, int netId, int size) {
        int rh = buffer.getShort() & 0xFFFF;
        boolean reliable = (rh & RELIABLE_BIT) == 0 ? false : true;
        boolean ordered = (rh & ORDERED_BIT) == 0  ? false : true;
        int channel = (rh >> 12) & 0x3;
        int packetId = rh & 0xFFF;

        int channelIdx = channel + (reliable ? 0 : CHANNEL_COUNT);

        // Create the packet data
        byte[] eventBuffer = new byte[size - 2];
        buffer.position(5);
        buffer.get(eventBuffer, 0, size - 2);

        // Ack reliables, always. An ack might have been previously dropped.
        if (_connectionType == RelayConnectionType.UDP) {
            if (reliable) {
                // Ack
                ByteBuffer ack = ByteBuffer.allocate(6);
                ack.order(ByteOrder.BIG_ENDIAN);
                ack.putShort((short)6);
                ack.put((byte)CL2RS_ACKNOWLEDGE);
                ack.putShort((short)rh);
                ack.put((byte)netId);
                send(ack);
            }

            synchronized(_lock) {
                // Is it duplicate?
                ArrayList<Integer> packetHistory = _packetIdHistory.get(channelIdx);
                for (int i = 0; i < packetHistory.size(); ++i) {
                    if (packetHistory.get(i) == packetId) {
                        return; // Just ignore it
                    }
                }

                // Record in history
                packetHistory.add(packetId);
                while (packetHistory.size() > MAX_PACKET_ID_HISTORY) {
                    packetHistory.remove(0);
                }

                if (ordered) {
                    if (reliable) {
                        ArrayList<RelayPacket> orderedReliablePackets = _orderedReliablePackets.get(channel);
                        if (packetId != _recvPacketId[channelIdx] + 1) {
                            int insertIdx = 0;
                            for (; insertIdx < orderedReliablePackets.size(); ++insertIdx) {
                                RelayPacket packet = orderedReliablePackets.get(insertIdx);
                                if (packet.packetId <= packetId) break;
                            }
                            orderedReliablePackets.add(insertIdx, new RelayPacket(packetId, netId, eventBuffer));
                            return;
                        }
                        _recvPacketId[channelIdx] = packetId;
                        synchronized(_callbackEventQueue) {
                            _callbackEventQueue.add(new RelayCallback(RelayCallbackType.Relay, netId, eventBuffer));
                        }
                        while (orderedReliablePackets.size() > 0) {
                            RelayPacket packet = orderedReliablePackets.get(0);
                            if (packet.packetId == _recvPacketId[channelIdx] + 1) {
                                synchronized(_callbackEventQueue) {
                                    _callbackEventQueue.add(new RelayCallback(RelayCallbackType.Relay, packet.netId, packet.data));
                                }
                                orderedReliablePackets.remove(0);
                                continue;
                            }
                            break; // Out of order
                        }
                    }
                    else {
                        // Just drop out of order packets for unreliables
                        if (packetLE(packetId, _recvPacketId[channelIdx])) {
                            return;
                        }
                        _recvPacketId[channelIdx] = packetId;
                    }
                }
            }
            // else - If not ordered, we don't care if it's out of order.
        }

        // Queue the packet callback
        synchronized(_callbackEventQueue) {
            _callbackEventQueue.add(new RelayCallback(RelayCallbackType.Relay, netId, eventBuffer));
        }
    }
    private void onPONG() {
        if (_pingInFlight) {
            _pingInFlight = false;
            _ping = (int)Math.min((long)999, System.currentTimeMillis() - _lastPingTime);
            if (_loggingEnabled) {
                System.out.println("RELAY PONG: " + _ping);
            }
        }
    }

    private void ackRSMG(int packetId) {
        if (_loggingEnabled) {
            System.out.println("RELAY RSMG ACK: " + packetId);
        }
        ByteBuffer buffer = ByteBuffer.allocate(5);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short)5);
        buffer.put((byte)CL2RS_RSMG_ACKNOWLEDGE);
        buffer.putShort((short)packetId);
        send(buffer);
    }

    private void onRSMG(ByteBuffer buffer, int size) {
        try {
            int rsmgPacketId = buffer.getShort() & 0xFFFF;

            if (_connectionType == RelayConnectionType.UDP) {
                ackRSMG(rsmgPacketId);
            }

            size -= 2;
            byte[] bytes = new byte[size];
            buffer.get(bytes, 0, size);
            String jsonString = new String(bytes, StandardCharsets.US_ASCII);
            JSONObject json = new JSONObject(jsonString);
            
            if (_loggingEnabled) {
                System.out.println("RELAY System Msg: " + jsonString);
            }

            switch (json.getString("op")) {
                case "CONNECT": {
                    int netId = json.getInt("netId");
                    String profileId = json.getString("profileId");
                    _netIdToProfileId.put(netId, profileId);
                    _profileIdToNetId.put(profileId, netId);
                    if (profileId.equals(_client.getAuthenticationService().getProfileId())) {
                        synchronized(_lock) {
                            if (!_isConnected) {
                                _isConnected = true;
                                _isConnecting = false;
                                _lastPingTime = System.currentTimeMillis();
                                _netId = netId;
                                _ownerId = json.getString("ownerId");

                                synchronized(_callbackEventQueue) {
                                    _callbackEventQueue.add(new RelayCallback(RelayCallbackType.ConnectSuccess, json));
                                }
                            }
                        }
                    }
                    break;
                }
                case "NET_ID": {
                    int netId = json.getInt("netId");
                    String profileId = json.getString("profileId");
                    _netIdToProfileId.put(netId, profileId);
                    _profileIdToNetId.put(profileId, netId);
                    break;
                }
                case "MIGRATE_OWNER": {
                    _ownerId = json.getString("profileId");
                    break;
                }
            }

            if (_connectionType == RelayConnectionType.UDP) {
                if (rsmgPacketId == _nextExpectedUdpRsmgPacketId) {
                    ++_nextExpectedUdpRsmgPacketId;
                    synchronized(_callbackEventQueue) {
                        _callbackEventQueue.add(new RelayCallback(RelayCallbackType.System, json));
                    }
                    for (int i = 0; i < _udpRsmgPackets.size(); ++i) {
                        UdpRsmgPacket packet = _udpRsmgPackets.get(i);
                        if (packet.id == _nextExpectedUdpRsmgPacketId) {
                            ++_nextExpectedUdpRsmgPacketId;
                            synchronized(_callbackEventQueue) {
                                _callbackEventQueue.add(new RelayCallback(RelayCallbackType.System, packet.json));
                            }
                            _udpRsmgPackets.remove(i);
                            --i;
                        }
                        else {
                            break;
                        }
                    }
                } else {
                    int insertId = 0;
                    for (; insertId < _udpRsmgPackets.size(); ++insertId) {
                        UdpRsmgPacket packet = _udpRsmgPackets.get(insertId);
                        if (packet.id == rsmgPacketId) return; // Already in queue, it's a duplicate, ignore it
                        if (packet.id > rsmgPacketId) break;
                    }
                    UdpRsmgPacket packet = new UdpRsmgPacket();
                    packet.id = insertId;
                    packet.json = json;
                    _udpRsmgPackets.add(insertId, packet);
                }
            } else {
                synchronized(_callbackEventQueue) {
                    _callbackEventQueue.add(new RelayCallback(RelayCallbackType.System, json));
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
            disconnect();
            synchronized(_callbackEventQueue) {
                _callbackEventQueue.add(new RelayCallback(RelayCallbackType.ConnectFailure, "Relay System Msg error"));
            }
        }
    }

    public void runCallbacks() {
        synchronized(_callbackEventQueue) {
            while (!_callbackEventQueue.isEmpty()) {
                RelayCallback relayCallback = _callbackEventQueue.remove(0);
                switch (relayCallback._type) {
                    case ConnectSuccess: {
                        if (_connectCallback != null) {
                            _connectCallback.relayConnectSuccess(relayCallback._json);
                        }
                        break;
                    }
                    case ConnectFailure: {
                        if (_connectCallback != null) {
                            _connectCallback.relayConnectFailure(relayCallback._message);
                        }
                        break;
                    }
                    case Relay: {
                        if (_relayCallback != null) {
                            _relayCallback.relayCallback(relayCallback._netId, relayCallback._data);
                        }
                        break;
                    }
                    case System: {
                        if (_relaySystemCallback != null) {
                            _relaySystemCallback.relaySystemCallback(relayCallback._json);
                        }
                        break;
                    }
                }
            }
        }

        if (_isConnecting) {
            long timeMs = System.currentTimeMillis();
            if (timeMs - _lastConnectTryTime > CONNECT_RESEND_INTERVAL_MS) {
                _lastConnectTryTime = timeMs;
                try {
                    send(CL2RS_CONNECTION, buildConnectionRequest());
                } catch(Exception e) {
                    e.printStackTrace();
                    disconnect();
                    synchronized(_callbackEventQueue) {
                        _callbackEventQueue.add(new RelayCallback(RelayCallbackType.ConnectFailure, "Relay System Fail to build connection request"));
                    }
                    return;
                }
            }
        }

        // Resend reliable
        if (_connectionType == RelayConnectionType.UDP) {
            long nowMs = System.currentTimeMillis();
            boolean resendTimedOut = false;
            synchronized(_lock) {
                for (int i = 0; i < _reliables.size(); ++i) {
                    Reliable reliable = _reliables.get(i);
                    long elapsedMs = nowMs - reliable.resendTimeMs;
                    if (nowMs >= reliable.waitTimeMs) {
                        // Did we timeout?
                        if (nowMs - reliable.sendTimeMs >= 10000) {
                            resendTimedOut = true;
                            break;
                        }

                        // Resend
                        reliable.waitTimeMs = Math.min(500, (reliable.waitTimeMs * 125) / 100);
                        reliable.resendTimeMs = nowMs;
                        send(reliable.buffer);

                        if (_loggingEnabled) {
                            System.out.println("RELAY RESEND: " + reliable.packetId + ", " + reliable.channel);
                        }
                    }
                }
            }
            if (resendTimedOut) {
                if (_loggingEnabled) {
                    System.out.println("RELAY UDP: Timed out. Too many packet drops");
                }
                disconnect();
                synchronized(_callbackEventQueue) {
                    _callbackEventQueue.add(new RelayCallback(RelayCallbackType.ConnectFailure, "Timed out. Too many packet drops."));
                }
            }
        }

        // Ping. Which also works as a heartbeat
        if (_isConnected) {
            if (System.currentTimeMillis() - _lastPingTime >= _pingIntervalMS) {
                sendPing();
            }
        }
    }
}
