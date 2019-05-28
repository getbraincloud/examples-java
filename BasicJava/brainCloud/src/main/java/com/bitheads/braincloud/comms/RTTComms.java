package com.bitheads.braincloud.comms;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.ByteBuffer;
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
import com.bitheads.braincloud.client.IRTTCallback;
import com.bitheads.braincloud.client.IRTTConnectCallback;
import com.bitheads.braincloud.client.IServerCallback;
import com.bitheads.braincloud.client.ServiceName;
import com.bitheads.braincloud.client.ServiceOperation;
import com.bitheads.braincloud.services.AuthenticationService;

public class RTTComms implements IServerCallback {

    enum RTTCallbackType {
        ConnectSuccess,
        ConnectFailure,
        Event
    }

    private class RTTCallback {
        public RTTCallbackType _type;
        public String _message;
        public JSONObject _json;

        RTTCallback(RTTCallbackType type) {
            _type = type;
        }

        RTTCallback(RTTCallbackType type, String message) {
            _type = type;
            _message = message;
        }

        RTTCallback(RTTCallbackType type, JSONObject json) {
            _type = type;
            _json = json;
        }
    }

    private class WSClient extends WebSocketClient {
        public WSClient(String ip) throws Exception {
            super(new URI(ip));
        }

        @Override
        public void onMessage(String message) {
            try {
                onRecv(message);
            } catch (Exception e) {
                e.printStackTrace();
                disconnect();
                return;
            }
        }
        
        @Override
        public void onMessage(ByteBuffer bytes) {
            String message = new String(bytes.array());
            try {
                onRecv(message);
            } catch (Exception e) {
                e.printStackTrace();
                disconnect();
                return;
            }
        }

        @Override
        public void onOpen(ServerHandshake handshake) {
            System.out.println("RTT WS Connected");

            try {
                onWSConnected();
            } catch (Exception e) {
                e.printStackTrace();
                failedToConnect();
                return;
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            System.out.println("RTT WS onClose: " + reason + ", code: " + Integer.toString(code) + ", remote: " + Boolean.toString(remote));
            synchronized(_callbackEventQueue) {
                _callbackEventQueue.add(new RTTCallback(RTTCallbackType.ConnectFailure, "webSocket onClose: " + reason));
            }
        }

        @Override
        public void onError(Exception e) {
            e.printStackTrace();
            synchronized(_callbackEventQueue) {
                _callbackEventQueue.add(new RTTCallback(RTTCallbackType.ConnectFailure, "webSocket onError"));
            }
        }
    }

    private BrainCloudClient _client;
    private boolean _loggingEnabled = false;
    private IRTTConnectCallback _connectCallback = null;
    private HashMap<String, IRTTCallback> _callbacks = new HashMap<String, IRTTCallback>();

    private String _appId;
    private Map<String, String> _secretMap;
    private String _secretKey;

    private String _sessionId;
    private String _profileId;
    private String _connectionId;

    private JSONObject _auth;
    private JSONObject _endpoint;
    
    private Socket _socket = null;
    private boolean _isConnected = false;
    
    private boolean _useWebSocket = false;
    private WSClient _webSocketClient;

    private int _heartbeatSeconds = 30;
    private long _lastHeartbeatTime = 0;

    private ArrayList<RTTCallback> _callbackEventQueue = new ArrayList<RTTCallback>();

    public RTTComms(BrainCloudClient client) {
        _client = client;
    }

    public void enableRTT(IRTTConnectCallback callback, boolean useWebSocket) {
        _connectCallback = callback;
        _useWebSocket = useWebSocket;

        BrainCloudRestClient restClient = _client.getRestClient();
        _appId = restClient.getAppId();
        _sessionId = restClient.getSessionId();

        AuthenticationService authenticationService = _client.getAuthenticationService();
        _profileId = authenticationService.getProfileId();

        _client.getRTTService().requestClientConnection(this);
    }

    public void disableRTT() {
        if (_webSocketClient != null) {
            _webSocketClient.close();
            _webSocketClient = null;
        }
    }

    /**
     * Returns true if RTT is enabled
     */
    public boolean isRTTEnabled()
    {
        return _isConnected;
    }

    public boolean getLoggingEnabled() {
        return _loggingEnabled;
    }

    public String getConnectionId() {
        return _connectionId;
    }

    public void enableLogging(boolean isEnabled) {
        _loggingEnabled = isEnabled;
    }

    public void runCallbacks() {
        synchronized(_callbackEventQueue) {
            while (!_callbackEventQueue.isEmpty()) {
                RTTCallback rttCallback = _callbackEventQueue.remove(0);
                switch (rttCallback._type) {
                    case ConnectSuccess: {
                        _connectCallback.rttConnectSuccess();
                        break;
                    }
                    case ConnectFailure: {
                        _connectCallback.rttConnectFailure(rttCallback._message);
                        break;
                    }
                    case Event: {
                        try {
                            String serviceName = rttCallback._json.getString("service");
                            if (_callbacks.containsKey(serviceName)) {
                                _callbacks.get(serviceName).rttCallback(rttCallback._json);
                            }
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                }
            }
        }

        // Heartbeat
        if (_isConnected) {
            if (System.currentTimeMillis() - _lastHeartbeatTime >= _heartbeatSeconds * 1000) {
                _lastHeartbeatTime = System.currentTimeMillis();
                try {
                    JSONObject json = new JSONObject();
                    json.put("operation", "HEARTBEAT");
                    json.put("service", "rtt");
                    sendWS(json);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void failedToConnect() {
        synchronized(_callbackEventQueue) {
            String host = "";
            int port = 0;
            try {
                if (_endpoint != null) {
                    host = _endpoint.getString("host");
                    port = _endpoint.getInt("port");
                }
            } catch (JSONException e) {
                // We tried
            }
            _callbackEventQueue.add(new RTTCallback(RTTCallbackType.ConnectFailure, 
                    "Failed to connect to RTT Event server: " + host + ":" + 
                    Integer.toString(port)));
        }
    }

    private JSONObject buildConnectionRequest(String protocol) throws Exception {
        // Send connection request
        JSONObject json = new JSONObject();
        json.put("operation", "CONNECT");
        json.put("service", "rtt");

        JSONObject system = new JSONObject();
        system.put("protocol", protocol);
        system.put("platform", "JAVA");

        JSONObject jsonData = new JSONObject();
        jsonData.put("appId", _appId);
        jsonData.put("profileId", _profileId);
        jsonData.put("sessionId", _sessionId);
        jsonData.put("auth", _auth);
        jsonData.put("system", system);
        json.put("data", jsonData);

        return json;
    }

    private void onSocketConnected(DataInputStream in) throws Exception {
        // Start receiving thread
        startReceiving(in);

        if (!send(buildConnectionRequest("tcp"))) {
            failedToConnect();
        }
    }

    private void onWSConnected() throws Exception {
        sendWS(buildConnectionRequest("ws"));
    }

//    private byte[] buildLenEncodedMessage(String message) {
//        byte[] msgBytes = message.getBytes(StandardCharsets.UTF_8);
//        int len = msgBytes.length;
//        byte[] buffer = new byte[len + 4];
//        buffer[0] = (byte)((len >> 24) & 0xFF);
//        buffer[1] = (byte)((len >> 16) & 0xFF);
//        buffer[2] = (byte)((len >> 8) & 0xFF);
//        buffer[3] = (byte)((len) & 0xFF);
//        for (int i = 0; i < len; ++i)
//        {
//            buffer[i + 4] = msgBytes[i];
//        }
//        return buffer;
//    }

    private void connect() {
        Thread connectionThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (_loggingEnabled) {
                        System.out.println("RTT TCP: Connecting...");
                    }

                    // Create socket
                    InetAddress serverIP = InetAddress.getByName(_endpoint.getString("host"));
                    int port = _endpoint.getInt("port");
                    _socket = new Socket(serverIP, port);
                    _isConnected = true;
                    _lastHeartbeatTime = System.currentTimeMillis();

                    if (_loggingEnabled) {
                        System.out.println("RTT TCP: connected");
                    }

                    onSocketConnected(null);
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    failedToConnect();
                    return;
                }
            }
        });

        connectionThread.start();
    }

    private void connectWebSocket() throws JSONException {

        boolean sslEnabled = _endpoint.getBoolean("ssl");

        String scheme = sslEnabled ? "wss://" : "ws://";
        StringBuilder uri = new StringBuilder(scheme)
                .append(_endpoint.getString("host"))
                .append(':')
                .append(_endpoint.getInt("port"));

        if (_auth != null) {
            char separator = '?';

            Iterator<String> it = _auth.keys();
            while(it.hasNext()) {
                String key = (String)it.next();
                
                uri.append(separator)
                        .append(key)
                        .append('=')
                        .append(_auth.getString(key));
                if (separator == '?') separator = '&';
            }
        }

        if (_loggingEnabled) {
            System.out.println("RTT WS: Connecting " + uri);
        }
        
        try {
            _webSocketClient = new WSClient(uri.toString());

            if (sslEnabled) {
                setupSSL();
            }
            
            _webSocketClient.connect();
        } 
        catch (Exception e) {
            e.printStackTrace();
            failedToConnect();
            return;
        }

        /*
        // Create socket
        InetAddress serverIP = InetAddress.getByName(_externalIp);
        _socket = new Socket(serverIP, _externalHTTPPort);

        String message =
            "GET / HTTP/1.1\r\n" +
            "Upgrade: websocket\r\n" +
            "Sec-WebSocket-Version: 13\r\n" +
            "Accept-Encoding: gzip, deflate, sdch\r\n" +
            "Accept-Language: zh-CN,zh;q=0.8,en-US;q=0.6,en;q=0.4\r\n" +
            "Sec-WebSocket-Key: bKdPyn3u98cTfZJSh4TNeQ==\r\n" +
            "Sec-WebSocket-Extensions: permessage-deflate; client_max_window_bits\r\n" +
            "User-Agent: Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.116 Safari/537.36\r\n" +
            "Connection: Upgrade\r\n\r\n";
        System.out.println("RTT WS SEND: " + message);
        DataOutputStream out = new DataOutputStream(_socket.getOutputStream());
        out.writeBytes(message);

        _isConnected = true;

        // Receive HTTP response
        DataInputStream in;
        in = new DataInputStream(_socket.getInputStream());
        byte[] header = new byte[1400];

        String msg = "";

        int index = 0;
        while (true) {
            byte B = in.readByte();
            header[index++] = B;
            if (B == (byte)'\n')
            {
                if (index >= 4)
                {
                    if (header[index - 4] == (byte)'\r' &&
                        header[index - 3] == (byte)'\n' &&
                        header[index - 2] == (byte)'\r')
                    {
                        header[index] = '\0';
                        msg = new String(header);
                        break;
                    }
                }
            }
        }

        System.out.println("RTT WS RECV: " + msg);

        if (_loggingEnabled) {
            System.out.println("RTT: connected");
        }

        onSocketConnected(in);*/
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

    private void disconnect() {
        _isConnected = false;

        try {
            if (_socket != null) {
                synchronized(_socket) {
                    if (_socket != null) {
                        _socket.close();
                        _socket = null;
                    }
                }
            }
            if (_webSocketClient != null) {
                _webSocketClient = null;
            }
        } catch (Exception e) {
        }
    }

    private boolean send(JSONObject jsonData) {
        try {
            String message = jsonData.toString();

            if (_loggingEnabled) {
                System.out.println("RTT SEND: " + message);
            }

            DataOutputStream out = new DataOutputStream(_socket.getOutputStream());
            out.writeInt(message.length());
            out.writeBytes(message);

            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean sendWS(JSONObject jsonData) {
        try {
            String message = jsonData.toString();

            if (_loggingEnabled) {
                System.out.println("RTT SEND: " + message);
            }

            _webSocketClient.send(/*buildLenEncodedMessage*/(message));

            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void onRecv(String message) throws Exception {
        if (_loggingEnabled) {
            System.out.println("RTT RECV: " + message);
        }

        try {
            JSONObject jsonData = new JSONObject(message);
            String service = jsonData.getString("service");

            switch (service) {
                case "evs":
                case "rtt":
                    processRttMessage(jsonData);
                    break;
                default: {
                    synchronized(_callbackEventQueue) {
                        _callbackEventQueue.add(new RTTCallback(RTTCallbackType.Event, jsonData));
                    }
                    break;
                }
            }
        }
        catch (Exception e) {
            synchronized(_callbackEventQueue) {
                _callbackEventQueue.add(new RTTCallback(RTTCallbackType.ConnectFailure, "Bad message: " + message));
            }
            throw(e);
        }
    }

    private void processRttMessage(JSONObject json) throws JSONException {
        String operation = json.getString("operation");
        switch (operation) {
            case "CONNECT": {
                _isConnected = true;
                _heartbeatSeconds = json.getJSONObject("data").getInt("heartbeatSeconds");
                _connectionId = json.getJSONObject("data").getString("cxId");
                if (json.getString("service").equals("rtt")) {
                    synchronized(_callbackEventQueue) {
                        _callbackEventQueue.add(new RTTCallback(RTTCallbackType.ConnectSuccess));
                    }
                } else {
                    synchronized(_callbackEventQueue) {
                        _callbackEventQueue.add(new RTTCallback(RTTCallbackType.ConnectFailure));
                    }
                }
                break;
            }
        }
    }

    private void startReceiving(DataInputStream in_in) {
        final DataInputStream capture_in_in = in_in;
        
        Thread receiveThread = new Thread(new Runnable() {
            @Override
            public void run() {
                DataInputStream in;
                try {
                    if (capture_in_in != null) {
                        in = capture_in_in;
                    } else {
                        in = new DataInputStream(_socket.getInputStream());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    _isConnected = false;
                    return;
                }
                while (_isConnected) {
                    try {
                        int len = in.readInt();

                        byte[] bytes = new byte[len];
                        in.readFully(bytes);
                        String message = new String(bytes);

                        onRecv(message);
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                }
                disconnect();
            }
        });

        receiveThread.start();
    }

    @Override
    public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {
        switch (serviceName) {
        case rttRegistration:
            processRTTMessage(serviceOperation, jsonData);
            break;
        default:
            break;
        }
    }

    private void processRTTMessage(ServiceOperation serviceOperation, JSONObject jsonData) {
        switch (serviceOperation) {
            case REQUEST_CLIENT_CONNECTION: {
                try {
                    JSONObject data = jsonData.getJSONObject("data");
                    _endpoint = getEndpointToUse(data.getJSONArray("endpoints"));
                    if (_endpoint == null) {
                        _connectCallback.rttConnectFailure("No endpoint available");
                        return;
                    }

                    _auth = data.getJSONObject("auth");

                    if (_useWebSocket) {
                        connectWebSocket();
                    }
                    else {
                        connect();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    _connectCallback.rttConnectFailure("Failed to establish connection");
                    return;
                }
            }
            break;
        default:
            break;
        }
    }

    private JSONObject getEndpointToUse(JSONArray endpoints) throws JSONException {
        
        if (_useWebSocket) {
            //   1st choice: websocket + ssl
            //   2nd: websocket
            JSONObject endpoint = getEndpointForType(endpoints, "ws", true);
            if (endpoint != null) {
                return endpoint;
            }
            return getEndpointForType(endpoints, "ws", false);
        }
        else {
            //   1st choice: tcp
            //   2nd: tcp + ssl (not implemented yet)
            JSONObject endpoint = getEndpointForType(endpoints, "tcp", false);
            if (endpoint != null) {
                return endpoint;
            }
            return getEndpointForType(endpoints, "tcp", true);
        }
    }

    private JSONObject getEndpointForType(JSONArray endpoints, String type, boolean wantSsl) throws JSONException {

        for (int i = 0; i < endpoints.length(); ++i) {
            JSONObject endpoint = endpoints.getJSONObject(i);
            String protocol = endpoint.getString("protocol");
            if (protocol.equals(type)) {
                if (wantSsl) {
                    if (endpoint.getBoolean("ssl")) {
                        return endpoint;
                    }
                }
                else {
                    return endpoint;
                }
            }
        }
        return null;
    }

    @Override
    public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
        _connectCallback.rttConnectFailure(jsonError);
    }

    public void registerRTTCallback(String serviceName, IRTTCallback callback) {
        _callbacks.put(serviceName, callback);
    }

    public void deregisterRTTCallback(String serviceName) {
        _callbacks.remove(serviceName);
    }

    public void deregisterAllCallbacks() {
        _callbacks.clear();
    }
}
