package com.bitheads.cursorparty;

import com.bitheads.braincloud.client.BrainCloudWrapper;
import com.bitheads.braincloud.services.RelayService;
import com.bitheads.braincloud.client.IRelayCallback;
import com.bitheads.braincloud.client.IRelayConnectCallback;
import com.bitheads.braincloud.client.IRelaySystemCallback;
import com.bitheads.braincloud.client.IRTTCallback;
import com.bitheads.braincloud.client.IRTTConnectCallback;
import com.bitheads.braincloud.client.IServerCallback;
import com.bitheads.braincloud.client.ReasonCodes;
import com.bitheads.braincloud.client.RelayConnectionType;
import com.bitheads.braincloud.client.ServiceName;
import com.bitheads.braincloud.client.ServiceOperation;

import java.nio.charset.StandardCharsets;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.json.JSONObject;
import org.json.JSONArray;

public class App implements IRelayCallback, IRelaySystemCallback {
    static final int MATCH_DURATION_SEC = 90;
    static final int COUNTDOWN_FROM_SEC = 80;

    static App _instance = null;

    static public App getInstance() {
        return _instance;
    }

    public State state = new State();
    public JFrame frame;

    BrainCloudWrapper _bcWrapper;
    String clientVersion;
    JLabel _serverVersionLabel = null;
    boolean _isConnectingRTT = false;
    boolean _disconnecting = false;
    RelayConnectionType _connectionType = RelayConnectionType.WEBSOCKET;
    long _lastMoveSendTime = System.currentTimeMillis();
    boolean _pendingMoveSend = false;
    private java.util.Timer _autoEndTimer = null;
    private long _lastPingBroadcastTime = 0;
    private JPanel _versionOverlay = null;

    public static void main(String args[]) {
        System.setProperty("apple.awt.application.name", "Cursor Party");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Cursor Party");
        _instance = new App();
    }

    public App() {
        File dir1 = new File(".");
        System.out.println("current directory: " + dir1.getAbsolutePath());

        _bcWrapper = new BrainCloudWrapper();
        _bcWrapper.initialize(ids.appId, ids.appSecret, ids.version, ids.url);
        _bcWrapper.getClient().enableLogging(true);

        clientVersion = _bcWrapper.getClient().getBrainCloudVersion();

        startCallbackLoop();

        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }

    void startCallbackLoop() {
        App that = this;

        Thread bcThread = new Thread(() -> {
            synchronized (that) {
                try {
                    while (true) {
                        _bcWrapper.runCallbacks();
                        wait(1);

                        if (_pendingMoveSend) {
                            long nowMs = System.currentTimeMillis();
                            if (nowMs - _lastMoveSendTime >= 1000 / 60) {
                                sendPlayerMove();
                            }
                        }

                        // Broadcast relay RTT to all players every 2 seconds while in game
                        if (state != null && state.screen instanceof GameScreen) {
                            long nowMs = System.currentTimeMillis();
                            if (nowMs - _lastPingBroadcastTime >= 2000) {
                                _lastPingBroadcastTime = nowMs;
                                broadcastRelayPing();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        bcThread.start();
    }

    void createAndShowGUI() {
        frame = new JFrame("Cursor Party");
        frame.getContentPane().setPreferredSize(new Dimension(1024, 768));
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                if (_bcWrapper.getClient().isAuthenticated()) {
                    _bcWrapper.logout(false, new IServerCallback() {
                        @Override
                        public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation,
                                JSONObject jsonData) {
                            System.out.println("Log Out Success");
                            System.exit(0);
                        }

                        @Override
                        public void serverError(ServiceName serviceName, ServiceOperation serviceOperation,
                                int statusCode, int reasonCode, String jsonError) {
                            System.out.println("Log Out Failed");
                            System.exit(0);
                        }
                    });
                } else {
                    System.exit(0);
                }
            }
        });
        frame.pack();
        frame.setLocationRelativeTo(null);

        java.net.URL iconURL = App.class.getResource("/resources/icon.png");
        if (iconURL != null) {
            java.awt.Image icon = new javax.swing.ImageIcon(iconURL).getImage();
            frame.setIconImage(icon);
            try {
                Class<?> taskbarClass = Class.forName("java.awt.Taskbar");
                Object taskbar = taskbarClass.getMethod("getTaskbar").invoke(null);
                taskbarClass.getMethod("setIconImage", java.awt.Image.class).invoke(taskbar, icon);
            } catch (Exception e) {
                try {
                    Class<?> cls = Class.forName("com.apple.eawt.Application");
                    Object app = cls.getMethod("getApplication").invoke(null);
                    cls.getMethod("setDockIconImage", java.awt.Image.class).invoke(app, icon);
                } catch (Exception ignored) {}
            }
        }

        frame.setVisible(true);
        frame.validate();
        frame.repaint();

        // Persistent stacked version overlay — lower left corner, matches JS/CPP style
        _versionOverlay = new JPanel();
        _versionOverlay.setLayout(new BoxLayout(_versionOverlay, BoxLayout.Y_AXIS));
        _versionOverlay.setBackground(new Color(0, 0, 0, 140));
        _versionOverlay.setOpaque(true);
        _versionOverlay.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        Font overlayFont = new Font("SansSerif", Font.PLAIN, 10);
        Color overlayText = new Color(190, 190, 190);

        for (String line : new String[] {
                "App:    " + ids.version,
                "Client: " + clientVersion,
                "Server: ..." }) {
            JLabel lbl = new JLabel(line);
            lbl.setFont(overlayFont);
            lbl.setForeground(overlayText);
            _versionOverlay.add(lbl);
            if (line.startsWith("Server:"))
                _serverVersionLabel = lbl;
        }

        frame.getRootPane().getLayeredPane().add(_versionOverlay, JLayeredPane.POPUP_LAYER);
        int contentH = frame.getContentPane().getPreferredSize().height;
        _versionOverlay.setBounds(6, contentH - 58, 130, 52);

        if (_bcWrapper.canReconnect()) {
            _bcWrapper.reconnect(new IServerCallback() {
                @Override
                public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation,
                        JSONObject result) {
                    JSONObject data = result.getJSONObject("data");
                    String playerName = data.getString("playerName");
                    System.out.println("Player name: " + playerName);
                    _bcWrapper.getPlayerStateService().updateUserName(playerName, null);
                    state.user = new User("", playerName, 7, false);
                    state.user.allowSendTo = false;
                    fetchAppProperties();
                    fetchServerVersion();
                    goToMainMenuScreen();
                }

                @Override
                public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode,
                        int reasonCode, String jsonError) {
                    System.out.println("Reconnect failed. Going to login screen");
                    goToLoginScreen();
                }
            });
        } else {
            goToLoginScreen();
        }
    }

    void fetchServerVersion() {
        _bcWrapper.getClient().getAuthenticationService().getServerVersion(new IServerCallback() {
            @Override
            public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject result) {
                try {
                    String sv = result.getJSONObject("data").getString("serverVersion");
                    if (_serverVersionLabel != null) {
                        SwingUtilities.invokeLater(() -> {
                            _serverVersionLabel.setText("Server: " + sv);
                            _versionOverlay.revalidate();
                            _versionOverlay.repaint();
                        });
                    }
                } catch (Exception e) {
                    System.out.println("Failed to get server version: " + e.getMessage());
                }
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation,
                    int statusCode, int reasonCode, String jsonError) {
                System.out.println("getServerVersion failed: " + jsonError);
            }
        });
    }

    void fetchAppProperties() {
        _bcWrapper.getGlobalAppService().readProperties(new IServerCallback() {
            @Override
            public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject result) {
                try {
                    JSONObject data = result.getJSONObject("data");
                    if (data.has("AllLobbyTypes")) {
                        String rawValue = data.getJSONObject("AllLobbyTypes").getString("value");
                        JSONObject lobbyTypesJson = new JSONObject(rawValue);
                        ArrayList<String> lobbies = new ArrayList<>();
                        for (String key : lobbyTypesJson.keySet()) {
                            JSONObject entry = lobbyTypesJson.getJSONObject(key);
                            if (entry.has("lobby")) {
                                lobbies.add(entry.getString("lobby"));
                            }
                        }
                        if (!lobbies.isEmpty())
                            state.appLobbies = lobbies;
                    }

                    if (data.has("SplotchDuration")) {
                        state.splotchDurationSec = Integer.parseInt(
                                data.getJSONObject("SplotchDuration").getString("value"));
                    }

                    if (data.has("Colors")) {
                        try {
                            JSONArray colorsJson = new JSONArray(
                                    data.getJSONObject("Colors").getString("value"));
                            Color[] newColors = new Color[colorsJson.length()];
                            for (int i = 0; i < colorsJson.length(); i++) {
                                newColors[i] = Color.decode(colorsJson.getString(i));
                            }
                            Colors.COLORS = newColors;
                        } catch (Exception e) {
                            System.out.println("Failed to parse Colors property: " + e.getMessage());
                        }
                    }

                    onStateChanged();
                } catch (Exception e) {
                    System.out.println("Failed to parse app properties: " + e.getMessage());
                }
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation,
                    int statusCode, int reasonCode, String jsonError) {
                System.out.println("readProperties failed: " + jsonError);
            }
        });
    }

    void changeScreen(Screen screen) {
        state.screen = screen;
        frame.getContentPane().removeAll();
        frame.getContentPane().add(state.screen.panel);
        onStateChanged();
    }

    public void onStateChanged() {
        state.screen.onStateChanged(state);
        frame.pack();
        frame.validate();
        frame.repaint();
    }

    public void goToLoginScreen() {
        changeScreen(new LoginScreen());
    }

    public void goToLoadingScreen(String text) {
        changeScreen(new LoadingScreen(text));
    }

    public void goToMainMenuScreen() {
        changeScreen(new MainMenuScreen());
    }

    public void goToLobbyScreen() {
        changeScreen(new LobbyScreen());
    }

    public void goToGameScreen() {
        state.shockwaves.clear();
        _pendingMoveSend = false;
        _lastMoveSendTime = System.currentTimeMillis();

        // Host sends game_start to all players and starts the match timer
        if (state.lobby != null && state.lobby.ownerCxId.equals(state.user.cxId)) {
            long startTime = System.currentTimeMillis();
            state.gameStartTime = startTime; // Host sets own time directly (no relay echo)

            ++state.roundNumber;
            JSONObject gameStartMsg = new JSONObject();
            gameStartMsg.put("op", "game_start");
            gameStartMsg.put("data", new JSONObject()
                    .put("startTime", startTime)
                    .put("round", state.roundNumber));
            _bcWrapper.getRelayService().sendToAll(
                    gameStartMsg.toString().getBytes(StandardCharsets.US_ASCII),
                    true, true, RelayService.CHANNEL_HIGH_PRIORITY_2);

            // Schedule auto-end after MATCH_DURATION_SEC
            _autoEndTimer = new java.util.Timer();
            _autoEndTimer.schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    synchronized (App.this) {
                        if (_bcWrapper.getClient().isAuthenticated()) {
                            _bcWrapper.getRelayService().endMatch(new JSONObject());
                        }
                    }
                }
            }, MATCH_DURATION_SEC * 1000L);
        }

        changeScreen(new GameScreen());
    }

    public void dieWithMessage(String message) {
        _bcWrapper.getRelayService().disconnect();
        _bcWrapper.getRelayService().deregisterSystemCallback();
        _bcWrapper.getRelayService().deregisterRelayCallback();
        _bcWrapper.getRTTService().deregisterAllCallbacks();
        _bcWrapper.getClient().resetCommunication();

        JOptionPane.showMessageDialog(frame, message, "ERROR", JOptionPane.ERROR_MESSAGE);

        goToLoginScreen();
    }

    public void brainCloudConnect(String username, String password) {
        goToLoadingScreen("Connecting...");

        synchronized (this) {
            _bcWrapper.authenticateUniversal(username, password, true, new IServerCallback() {
                @Override
                public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation,
                        JSONObject result) {
                    _bcWrapper.getPlayerStateService().updateUserName(username, null);
                    state.user = new User("", username, 7, false);
                    state.user.allowSendTo = false;
                    fetchAppProperties();
                    fetchServerVersion();
                    goToMainMenuScreen();
                }

                @Override
                public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode,
                        int reasonCode, String jsonError) {
                    dieWithMessage("Failed to authenticate.");
                }
            });
        }
    }

    @Override
    public void relayCallback(int netId, byte[] bytes) {
        try {
            String jsonString = new String(bytes, StandardCharsets.US_ASCII);
            JSONObject json = new JSONObject(jsonString);
            String op = json.getString("op");

            // Game-level ops (no sender lookup needed)
            if (op.equals("game_start")) {
                JSONObject d = json.getJSONObject("data");
                state.gameStartTime = d.getLong("startTime");
                state.roundNumber = d.optInt("round", state.roundNumber);
                return;
            }
            // END_MATCH is handled via relaySystemCallback (relay.endMatch() path)
            if (op.equals("END_MATCH")) {
                return;
            }
            if (op.equals("clear_splotches")) {
                state.splotches.clear();
                return;
            }

            // Find sender
            String cxId = _bcWrapper.getRelayService().getCxIdForNetId(netId);
            User user = null;
            for (int i = 0; i < state.lobby.members.size(); ++i) {
                User member = state.lobby.members.get(i);
                if (member.cxId.equals(cxId)) {
                    user = member;
                    break;
                }
            }
            if (user == null)
                return;

            switch (op) {
                case "relay_ping": {
                    user.activePing = json.getJSONObject("data").getInt("ping");
                    break;
                }
                case "move": {
                    JSONObject posJson = json.getJSONObject("data");
                    user.pos = new Point2D.Float(posJson.getFloat("x"), posJson.getFloat("y"));
                    break;
                }
                case "shockwave": {
                    JSONObject posJson = json.getJSONObject("data");
                    float rx = posJson.getFloat("x");
                    float ry = posJson.getFloat("y");
                    // Use the sender's synced rotation (default random if an older client omits it)
                    double rAngle = posJson.has("angle") ? posJson.getDouble("angle") : Math.random() * Math.PI * 2.0;
                    state.shockwaves.add(new Shockwave(
                            new Point2D.Float(rx, ry),
                            Colors.COLORS[user.colorIndex % Colors.NUM_COLORS]));
                    createSplotch(rx, ry, user.colorIndex, rAngle);
                    break;
                }
                case "splotch_sync": {
                    JSONObject data = json.getJSONObject("data");
                    if (data.getBoolean("first"))
                        state.splotches.clear();
                    JSONArray arr = data.getJSONArray("splotches");
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject e = arr.getJSONObject(i);
                        float sx = (float) e.getDouble("x");
                        float sy = (float) e.getDouble("y");
                        int ci = e.getInt("c");
                        // "a" = synced rotation (default random if absent); "t" = original timestamp
                        double sa = e.has("a") ? e.getDouble("a") : Math.random() * Math.PI * 2.0;
                        long t = e.getLong("t");
                        Splotch s = new Splotch(new Point2D.Float(sx, sy), ci, sa);
                        s.startTimeMs = t;
                        state.splotches.add(s);
                    }
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            dieWithMessage("Bad packet.");
        }
    }

    @Override
    public void relaySystemCallback(JSONObject jsonData) {
        String sysOp = jsonData.getString("op");
        if (sysOp.equals("DISCONNECT")) {
            for (int i = 0; i < state.lobby.members.size(); ++i) {
                User member = state.lobby.members.get(i);
                if (member.cxId.equals(jsonData.getString("cxId"))) {
                    member.pos = null;
                }
            }
        } else if (sysOp.equals("END_MATCH")) {
            SwingUtilities.invokeLater(() -> onGameScreenToLobby());
        } else if (sysOp.equals("CONNECT")) {
            // If we are the host, sync game start time and current splotches to the new
            // player
            if (state.lobby != null && state.lobby.ownerCxId.equals(state.user.cxId)) {
                String newCxId = jsonData.getString("cxId");
                int newNetId = _bcWrapper.getRelayService().getNetIdForCxId(newCxId);
                long playerMask = 1L << (long) newNetId;

                // Re-send game_start so the JIP player gets the authoritative start time
                JSONObject gameStartMsg = new JSONObject();
                gameStartMsg.put("op", "game_start");
                gameStartMsg.put("data", new JSONObject()
                        .put("startTime", state.gameStartTime)
                        .put("round", state.roundNumber));
                _bcWrapper.getRelayService().sendToPlayers(
                        gameStartMsg.toString().getBytes(StandardCharsets.US_ASCII),
                        playerMask, true, false, RelayService.CHANNEL_HIGH_PRIORITY_2);

                // Send current splotch canvas in size-bounded chunks
                sendSplotchSyncToMask(playerMask);
            }
        }
    }

    private static final int MAX_SPLOTCH_SYNC_BYTES = 900;

    private void sendSplotchSyncToMask(long mask) {
        if (state.splotches.isEmpty() || mask == 0)
            return;
        boolean isFirst = true;
        JSONArray batch = new JSONArray();
        int currentSize = 65; // envelope overhead

        for (int i = 0; i <= state.splotches.size(); i++) {
            String entryStr = null;
            JSONObject entry = null;
            if (i < state.splotches.size()) {
                Splotch s = state.splotches.get(i);
                entry = new JSONObject()
                        .put("x", s.pos.getX())
                        .put("y", s.pos.getY())
                        .put("c", s.colorIndex)
                        .put("a", s.angle)
                        .put("t", s.startTimeMs);
                entryStr = entry.toString();
            }

            boolean flush = (i == state.splotches.size()) || (entry != null
                    && currentSize + entryStr.length() + 1 > MAX_SPLOTCH_SYNC_BYTES && batch.length() > 0);
            if (flush && batch.length() > 0) {
                JSONObject syncMsg = new JSONObject();
                syncMsg.put("op", "splotch_sync");
                syncMsg.put("data", new JSONObject()
                        .put("first", isFirst)
                        .put("splotches", batch));
                _bcWrapper.getRelayService().sendToPlayers(
                        syncMsg.toString().getBytes(StandardCharsets.US_ASCII),
                        mask, true, false, RelayService.CHANNEL_HIGH_PRIORITY_2);
                isFirst = false;
                batch = new JSONArray();
                currentSize = 65;
            }
            if (entry != null) {
                batch.put(entry);
                currentSize += entryStr.length() + 1;
            }
        }
    }

    void onLobbyEvent(JSONObject result) {
        JSONObject jsonData = result.getJSONObject("data");

        if (jsonData.has("lobby")) {
            state.lobby = new Lobby(jsonData.getJSONObject("lobby"), jsonData.getString("lobbyId"));
            onStateChanged();

            if (state.screen instanceof LoadingScreen) {
                goToLobbyScreen();
            }
        }

        String operation = result.getString("operation");

        if (operation.equals("DISBANDED")) {
            if (jsonData.getJSONObject("reason").getInt("code") != ReasonCodes.RTT_ROOM_READY) {
                onGameScreenClose();
            }
        } else if (operation.equals("STARTING")) {
            // Reset timer here — this event arrives on ALL clients simultaneously
            state.lobbyStatusText = "Starting...";
            state.lobbySubStatus = "Provisioning server...";
            state.lobbyStatusStartTime = System.currentTimeMillis();
            SwingUtilities.invokeLater(() -> onStateChanged());
        } else if (operation.equals("ROOM_PROGRESS")) {
            int curStep = jsonData.optInt("curStep", 0);
            int ofStep = jsonData.optInt("ofStep", 0);
            String msg = jsonData.optString("msg", "");
            state.lobbySubStatus = (curStep > 0)
                    ? curStep + "/" + ofStep + ": " + msg
                    : (msg.isEmpty() ? "Starting server..." : msg);
            SwingUtilities.invokeLater(() -> onStateChanged());
        } else if (operation.equals("ROOM_ASSIGNED")) {
            state.lobbySubStatus = "Server assigned...";
            SwingUtilities.invokeLater(() -> onStateChanged());
        } else if (operation.equals("ROOM_READY")) {
            // Update text only — timer continues from when STARTING fired (matches JS/C++)
            state.lobbySubStatus = "Connecting...";
            SwingUtilities.invokeLater(() -> onStateChanged());
            _bcWrapper.getRelayService().registerRelayCallback(this);
            _bcWrapper.getRelayService().registerSystemCallback(this);

            JSONObject connectData = jsonData.getJSONObject("connectData");
            JSONObject ports = connectData.getJSONObject("ports");
            String host = connectData.getString("address");

            // GameLift and i3D servers only expose a single WebSocket port.
            // If either is present, force WEBSOCKET regardless of the user's selection.
            RelayConnectionType connectType = _connectionType;
            int port;

            if (ports.has("gamelift") && !ports.isNull("gamelift")) {
                port = ports.getInt("gamelift");
                connectType = RelayConnectionType.WEBSOCKET;
                System.out.println("ROOM_READY: GameLift server, forcing WS on port " + port);
            } else if (ports.has("i3d") && !ports.isNull("i3d")) {
                port = ports.getInt("i3d");
                connectType = RelayConnectionType.WEBSOCKET;
                System.out.println("ROOM_READY: i3D server, forcing WS on port " + port);
            } else if (connectType == RelayConnectionType.WEBSOCKET)
                port = ports.getInt("ws");
            else if (connectType == RelayConnectionType.TCP)
                port = ports.getInt("tcp");
            else
                port = ports.getInt("udp");

            JSONObject options = new JSONObject();
            options.put("ssl", false);
            options.put("host", host);
            options.put("port", port);
            options.put("passcode", jsonData.getString("passcode"));
            options.put("lobbyId", jsonData.getString("lobbyId"));

            final RelayConnectionType finalConnectType = connectType;
            _bcWrapper.getRelayService().connect(finalConnectType, options, new IRelayConnectCallback() {
                @Override
                public void relayConnectSuccess(JSONObject jsonData) {
                    goToGameScreen();
                }

                @Override
                public void relayConnectFailure(String errorMessage) {
                    if (!_disconnecting) {
                        dieWithMessage("Failed to connect to server, msg: " + errorMessage);
                    }
                }
            });
        }
    }

    public void onPlayClicked(String protocolStr, String lobbyType, boolean usePingData) {
        state.usePingData = usePingData;
        state.lobbySearchStartTime = System.currentTimeMillis();
        goToLoadingScreen("Joining...");

        switch (protocolStr) {
            case "WEBSOCKET":
                _connectionType = RelayConnectionType.WEBSOCKET;
                break;
            case "TCP":
                _connectionType = RelayConnectionType.TCP;
                break;
            case "UDP":
                _connectionType = RelayConnectionType.UDP;
                break;
        }

        final String lobbyAlgo = "{\"strategy\":\"ranged-absolute\",\"alignment\":\"center\",\"ranges\":[1000]}";

        synchronized (this) {
            _bcWrapper.getRTTService().registerRTTLobbyCallback(new IRTTCallback() {
                @Override
                public void rttCallback(JSONObject eventJson) {
                    onLobbyEvent(eventJson);
                }
            });

            _isConnectingRTT = true;
            _disconnecting = false;
            _bcWrapper.getRTTService().enableRTT(new IRTTConnectCallback() {
                @Override
                public void rttConnectSuccess() {
                    state.user.cxId = _bcWrapper.getClient().getRttConnectionId();
                    _isConnectingRTT = false;

                    if (usePingData) {
                        // Ping regions then use ping-aware matchmaking
                        _bcWrapper.getLobbyService().getRegionsForLobbies(
                                new String[] { lobbyType },
                                new IServerCallback() {
                                    @Override
                                    public void serverCallback(ServiceName sn, ServiceOperation so, JSONObject result) {
                                        _bcWrapper.getLobbyService().pingRegions(new IServerCallback() {
                                            @Override
                                            public void serverCallback(ServiceName sn, ServiceOperation so,
                                                    JSONObject r) {
                                                // Store ping data
                                                JSONObject pd = _bcWrapper.getLobbyService().getPingData();
                                                state.pingData.clear();
                                                if (pd != null) {
                                                    for (String region : pd.keySet()) {
                                                        state.pingData.put(region, pd.getInt(region));
                                                    }
                                                }
                                                _bcWrapper.getLobbyService().findOrCreateLobbyWithPingData(
                                                        lobbyType, 0, 1, lobbyAlgo, "{}", null, "{}",
                                                        false, buildExtraJson(state.user.colorIndex), "all",
                                                        new IServerCallback() {
                                                            @Override
                                                            public void serverCallback(ServiceName sn,
                                                                    ServiceOperation so, JSONObject r) {
                                                            }

                                                            @Override
                                                            public void serverError(ServiceName sn, ServiceOperation so,
                                                                    int sc, int rc, String je) {
                                                                dieWithMessage("Failed to find lobby.\n" + rc);
                                                            }
                                                        });
                                            }

                                            @Override
                                            public void serverError(ServiceName sn, ServiceOperation so, int sc, int rc,
                                                    String je) {
                                                // Fall back to standard lobby on ping failure
                                                fallbackFindLobby(lobbyType, lobbyAlgo);
                                            }
                                        });
                                    }

                                    @Override
                                    public void serverError(ServiceName sn, ServiceOperation so, int sc, int rc,
                                            String je) {
                                        fallbackFindLobby(lobbyType, lobbyAlgo);
                                    }
                                });
                    } else {
                        fallbackFindLobby(lobbyType, lobbyAlgo);
                    }
                }

                @Override
                public void rttConnectFailure(String errorMessage) {
                    if (_isConnectingRTT) {
                        dieWithMessage("Failed to enable RTT");
                    } else if (!_disconnecting) {
                        dieWithMessage("RTT Disconnected");
                    }
                }
            });
        }
    }

    private void fallbackFindLobby(String lobbyType, String lobbyAlgo) {
        _bcWrapper.getLobbyService().findOrCreateLobby(lobbyType, 0, 1,
                lobbyAlgo, "{}", null, "{}", false,
                buildExtraJson(state.user.colorIndex), "all",
                new IServerCallback() {
                    @Override
                    public void serverCallback(ServiceName sn, ServiceOperation so, JSONObject r) {
                    }

                    @Override
                    public void serverError(ServiceName sn, ServiceOperation so, int sc, int rc, String je) {
                        dieWithMessage("Failed to find lobby.\n" + rc);
                    }
                });
    }

    public void onLogoutClicked() {
        goToLoadingScreen("Logging out...");

        _bcWrapper.logout(true, new IServerCallback() {
            @Override
            public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation,
                    JSONObject jsonData) {
                state.appLobbies.clear();
                goToLoginScreen();
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode,
                    int reasonCode, String jsonError) {
                System.out.println("Log out failed: " + jsonError);
                if (_bcWrapper.getClient().isAuthenticated()) {
                    goToMainMenuScreen();
                } else {
                    dieWithMessage(jsonError);
                }
            }
        });

        goToLoginScreen();
    }

    // Host: end the match for all players and return to lobby for the next round
    public void onEndMatch() {
        if (_autoEndTimer != null) {
            _autoEndTimer.cancel();
            _autoEndTimer = null;
        }
        synchronized (this) {
            if (_bcWrapper.getClient().isAuthenticated()) {
                _bcWrapper.getRelayService().endMatch(new JSONObject());
            }
        }
        // onGameScreenToLobby() will be triggered for all players (including host)
        // via the END_MATCH system callback from the server
    }

    // Return to the lobby after a match — relay disconnects but RTT/lobby stay
    // alive
    public void onGameScreenToLobby() {
        if (_autoEndTimer != null) {
            _autoEndTimer.cancel();
            _autoEndTimer = null;
        }
        state.gameStartTime = 0;
        state.lobbyStatusText = "";
        state.lobbySubStatus = "";
        state.lobbyStatusStartTime = 0;
        state.splotches.clear();
        _pendingMoveSend = false;
        _lastMoveSendTime = System.currentTimeMillis();
        _disconnecting = false;

        _bcWrapper.getRelayService().deregisterRelayCallback();
        _bcWrapper.getRelayService().deregisterSystemCallback();
        _bcWrapper.getRelayService().disconnect();

        // RTT stays enabled — deregister all callbacks then re-register lobby
        // so the next STARTING / ROOM_READY events are received
        _bcWrapper.getRTTService().deregisterAllCallbacks();

        state.user.isReady = false;
        if (state.lobby != null) {
            _bcWrapper.getLobbyService().updateReady(
                    state.lobby.lobbyId, false,
                    buildExtraJson(state.user.colorIndex), null);
        }

        synchronized (this) {
            _bcWrapper.getRTTService().registerRTTLobbyCallback(new IRTTCallback() {
                @Override
                public void rttCallback(JSONObject eventJson) {
                    onLobbyEvent(eventJson);
                }
            });
        }

        goToLobbyScreen();
    }

    // Host: clear all splotches for every player mid-game
    public void onClearSplotches() {
        state.splotches.clear();
        synchronized (this) {
            if (_bcWrapper.getClient().isAuthenticated()) {
                JSONObject clearMsg = new JSONObject();
                clearMsg.put("op", "clear_splotches");
                _bcWrapper.getRelayService().sendToAll(
                        clearMsg.toString().getBytes(StandardCharsets.US_ASCII),
                        true, true, RelayService.CHANNEL_HIGH_PRIORITY_2);
            }
        }
    }

    public void createSplotch(float x, float y, int colorIndex, double angle) {
        state.splotches.add(new Splotch(
                new java.awt.geom.Point2D.Float(x, y),
                colorIndex % Colors.NUM_COLORS,
                angle));
    }

    public void onGameScreenClose() {
        // Cancel auto-end timer if host
        if (_autoEndTimer != null) {
            _autoEndTimer.cancel();
            _autoEndTimer = null;
        }
        state.gameStartTime = 0;
        state.splotches.clear();

        _disconnecting = true;

        _bcWrapper.getRelayService().deregisterRelayCallback();
        _bcWrapper.getRelayService().deregisterSystemCallback();
        _bcWrapper.getRelayService().disconnect();
        _bcWrapper.getRTTService().deregisterAllCallbacks();
        _bcWrapper.getRTTService().disableRTT();

        state.lobby = null;
        state.user.isReady = false;
        state.lobbyStatusText = "";
        state.lobbySubStatus = "";
        state.lobbyStatusStartTime = 0;
        _pendingMoveSend = false;
        _lastMoveSendTime = System.currentTimeMillis();
        goToMainMenuScreen();
    }

    // Build the extra JSON for lobby join/updateReady calls.
    // Always includes colorIndex; includes per-region pings when available.
    private String buildExtraJson(int colorIndex) {
        JSONObject extra = new JSONObject();
        extra.put("colorIndex", colorIndex);
        if (!state.pingData.isEmpty()) {
            JSONObject pingsJson = new JSONObject();
            for (java.util.Map.Entry<String, Integer> entry : state.pingData.entrySet()) {
                pingsJson.put(entry.getKey(), entry.getValue());
            }
            extra.put("pings", pingsJson);
        }
        return extra.toString();
    }

    // Broadcast our current relay RTT to all players. Called every 2 seconds while
    // in game.
    private void broadcastRelayPing() {
        if (!_bcWrapper.getClient().isAuthenticated())
            return;
        synchronized (this) {
            int ping = _bcWrapper.getRelayService().getPing();
            // Update own entry immediately
            for (int i = 0; i < state.lobby.members.size(); ++i) {
                if (state.lobby.members.get(i).cxId.equals(state.user.cxId)) {
                    state.lobby.members.get(i).activePing = ping;
                    break;
                }
            }
            JSONObject msg = new JSONObject();
            msg.put("op", "relay_ping");
            msg.put("data", new JSONObject().put("ping", ping));
            _bcWrapper.getRelayService().sendToAll(
                    msg.toString().getBytes(StandardCharsets.US_ASCII),
                    false, false, RelayService.CHANNEL_HIGH_PRIORITY_1);
        }
    }

    public void onColorChanged(int colorIndex) {
        state.user.colorIndex = colorIndex;
        _bcWrapper.getLobbyService().updateReady(state.lobby.lobbyId, state.user.isReady,
                buildExtraJson(colorIndex), null);
        onStateChanged();
    }

    public void onGameStart() {
        state.user.isReady = true;
        // Show status immediately on the host; STARTING event will refresh it on all
        // clients
        state.lobbyStatusText = "Starting...";
        state.lobbySubStatus = "";
        state.lobbyStatusStartTime = System.currentTimeMillis();
        _bcWrapper.getLobbyService().updateReady(state.lobby.lobbyId, state.user.isReady,
                buildExtraJson(state.user.colorIndex), null);
        onStateChanged();
    }

    public void sendPlayerMove() {
        JSONObject data = new JSONObject();
        data.put("op", "move");
        JSONObject posJson = new JSONObject();
        posJson.put("x", state.user.pos.getX());
        posJson.put("y", state.user.pos.getY());
        data.put("data", posJson);

        _bcWrapper.getRelayService().sendToAll(
                data.toString().getBytes(StandardCharsets.US_ASCII),
                state.reliable,
                state.ordered,
                RelayService.CHANNEL_HIGH_PRIORITY_1);

        _lastMoveSendTime = System.currentTimeMillis();
    }

    public void onPlayerMove(float x, float y) {
        state.user.pos = new Point2D.Float(x, y);

        User myUser = null;
        for (int i = 0; i < state.lobby.members.size(); ++i) {
            User member = state.lobby.members.get(i);
            if (member.cxId.equals(state.user.cxId)) {
                myUser = member;
                break;
            }
        }
        if (myUser != null) {
            myUser.pos = new Point2D.Float(x, y);
        }

        _pendingMoveSend = true;
    }

    public void onPlayerShockwave(float x, float y) {
        // Pick a rotation once and send it so every client renders this splotch the same.
        double angle = Math.random() * Math.PI * 2.0;

        state.shockwaves.add(new Shockwave(
                new Point2D.Float(x, y),
                Colors.COLORS[state.user.colorIndex % Colors.NUM_COLORS]));
        createSplotch(x, y, state.user.colorIndex, angle);

        JSONObject data = new JSONObject();
        data.put("op", "shockwave");
        JSONObject posJson = new JSONObject();
        posJson.put("x", x);
        posJson.put("y", y);
        posJson.put("teamCode", 0);
        posJson.put("angle", angle);
        data.put("data", posJson);

        long playerMask = 0;
        for (int i = 0; i < state.lobby.members.size(); ++i) {
            User user = state.lobby.members.get(i);
            if (!user.allowSendTo)
                continue;
            int netId = _bcWrapper.getRelayService().getNetIdForCxId(user.cxId);
            playerMask |= (1L << (long) netId);
        }

        _bcWrapper.getRelayService().sendToPlayers(
                data.toString().getBytes(StandardCharsets.US_ASCII),
                playerMask,
                true,
                false,
                RelayService.CHANNEL_HIGH_PRIORITY_2);
    }
}
