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
import java.util.List;

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

    static App _instance = null;

    static public App getInstance() {
        return _instance;
    }

    public State state = new State();
    public JFrame frame;

    BrainCloudWrapper _bcWrapper;
    String clientVersion;
    JLabel _serverVersionLabel = null;
    boolean _disconnecting = false;
    RelayConnectionType _connectionType = RelayConnectionType.WEBSOCKET;
    long _lastMoveSendTime = System.currentTimeMillis();
    boolean _pendingMoveSend = false;
    private java.util.Timer _autoEndTimer = null;
    private long _lastPingBroadcastTime = 0;
    private JPanel _versionOverlay = null;

    // Shared RTT-enable mechanism. brainCloud's enableRTT() silently no-ops (its
    // callback never fires) when RTT is already connected or already connecting,
    // so anything that needs RTT (chat bootstrap from the Main Menu, matchmaking
    // from the Play button) must funnel through here rather than each calling
    // enableRTT() directly — otherwise whichever caller comes second gets stuck
    // waiting on a callback that will never arrive.
    private boolean _rttConnecting = false;
    private ArrayList<Runnable> _rttEnableWaiters = new ArrayList<>();

    // Global chat
    private boolean _chatRTTRegistered = false;
    private String _chatChannelId = null;
    private boolean _chatChannelResolving = false;
    private long _chatChannelRetryAtMs = 0;
    private static final long CHAT_CHANNEL_RETRY_MS = 5000;

    // Match summary / leaderboard posting
    static final int RESULT_GRACE_SEC = 3; // package-visible: GameScreen's timer needs the true end-of-match time
    private static final int MAX_RELAY_BYTES = 900;
    private long _lastResultsPollMs = 0;
    private static final long RESULTS_POLL_INTERVAL_MS = 1000;
    private JSONArray _pendingMatchResult = new JSONArray();

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

                    if (data.has("PointsLeaderboardId")) {
                        String v = data.getJSONObject("PointsLeaderboardId").getString("value");
                        if (!v.isEmpty()) state.pointsLeaderboardId = v;
                    }
                    if (data.has("PointsLeaderboardIdQuarterly")) {
                        String v = data.getJSONObject("PointsLeaderboardIdQuarterly").getString("value");
                        if (!v.isEmpty()) state.pointsLeaderboardIdQuarterly = v;
                    }
                    if (data.has("CoverageLeaderboardId")) {
                        String v = data.getJSONObject("CoverageLeaderboardId").getString("value");
                        if (!v.isEmpty()) state.coverageLeaderboardId = v;
                    }
                    if (data.has("CoverageLeaderboardIdQuarterly")) {
                        String v = data.getJSONObject("CoverageLeaderboardIdQuarterly").getString("value");
                        if (!v.isEmpty()) state.coverageLeaderboardIdQuarterly = v;
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
        enableChatRTT();
    }

    public void goToLobbyScreen() {
        changeScreen(new LobbyScreen());
    }

    public void goToMatchSummaryScreen() {
        changeScreen(new MatchSummaryScreen());
    }

    public void goToGameScreen() {
        state.shockwaves.clear();
        state.matchResult = new MatchResult();
        state.coverage.clear();
        state.coverageComputedGen = -1;
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

            // Schedule the results broadcast + cloud-code leaderboard post at
            // MATCH_DURATION_SEC, then the actual endMatch() RESULT_GRACE_SEC later —
            // mirrors the cpp reference client's Running -> ResultsBroadcast -> Ended
            // timeline so every client gets match_result before the relay tears down.
            _autoEndTimer = new java.util.Timer();
            _autoEndTimer.schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    synchronized (App.this) {
                        if (_bcWrapper.getClient().isAuthenticated()) {
                            broadcastMatchResults();
                        }
                    }
                }
            }, MATCH_DURATION_SEC * 1000L);
            _autoEndTimer.schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    synchronized (App.this) {
                        if (_bcWrapper.getClient().isAuthenticated()) {
                            _bcWrapper.getRelayService().endMatch(new JSONObject());
                        }
                    }
                }
            }, (MATCH_DURATION_SEC + RESULT_GRACE_SEC) * 1000L);
        }

        changeScreen(new GameScreen());
    }

    public void dieWithMessage(String message) {
        _bcWrapper.getRelayService().disconnect();
        _bcWrapper.getRelayService().deregisterSystemCallback();
        _bcWrapper.getRelayService().deregisterRelayCallback();
        _bcWrapper.getRTTService().deregisterAllCallbacks();
        _bcWrapper.getClient().resetCommunication();

        // resetCommunication() tears down the whole session — reset the RTT/chat
        // bootstrap flags too so a fresh login doesn't trust stale state.
        _rttConnecting = false;
        _rttEnableWaiters.clear();
        _chatRTTRegistered = false;
        _chatChannelId = null;
        _chatChannelResolving = false;
        _chatChannelRetryAtMs = 0;

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
                state.splotchGeneration++;
                return;
            }
            if (op.equals("match_result")) {
                JSONObject d = json.getJSONObject("data");
                int round = d.getInt("round");
                if (d.getBoolean("first"))
                    _pendingMatchResult = new JSONArray();
                JSONArray entries = d.getJSONArray("e");
                for (int i = 0; i < entries.length(); i++)
                    _pendingMatchResult.put(entries.getJSONObject(i));
                if (d.getBoolean("last"))
                    applyMatchResult(round, _pendingMatchResult);
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
                    state.splotchGeneration++;
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
            SwingUtilities.invokeLater(() -> onMatchEnded());
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

    // ── Match end / summary / leaderboard posting ───────────────────────────────

    // Called on every client when the relay server's END_MATCH system event
    // arrives (either the natural post-timer end, or a host-initiated early end).
    private void onMatchEnded() {
        if (_autoEndTimer != null) {
            _autoEndTimer.cancel();
            _autoEndTimer = null;
        }

        // Fallback: if the host's match_result broadcast never arrived (dropped, or
        // the host disconnected mid-broadcast), compute a local snapshot so the
        // summary screen has something to show. No cloud posting from this
        // fallback path — only the host posts to the leaderboards.
        if ((!state.matchResult.valid || state.matchResult.round != state.roundNumber) && state.lobby != null) {
            List<Coverage.CoverageEntry> coverage = Coverage.computeCoverage(state.splotches, state.lobby.members);
            state.matchResult = buildMatchResult(state.roundNumber, coverage);
        }

        state.gameStartTime = 0;
        state.lobbyStatusText = "";
        state.lobbySubStatus = "";
        state.lobbyStatusStartTime = 0;
        state.shockwaves.clear();
        state.splotches.clear();
        state.splotchGeneration++;
        state.coverage.clear();
        state.user.isReady = false;
        _pendingMoveSend = false;
        _lastMoveSendTime = System.currentTimeMillis();
        _disconnecting = false;

        state.matchSummaryArrivalTime = System.currentTimeMillis();
        _lastResultsPollMs = 0;

        _bcWrapper.getRelayService().deregisterRelayCallback();
        _bcWrapper.getRelayService().deregisterSystemCallback();
        _bcWrapper.getRelayService().disconnect();

        // RTT stays enabled — deregister all callbacks then re-register lobby (and
        // chat, since deregisterAllCallbacks() cleared that registration too even
        // though the underlying RTT connection and chat channel subscription stay up).
        _bcWrapper.getRTTService().deregisterAllCallbacks();
        _chatRTTRegistered = false;
        enableChatRTT();

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

        goToMatchSummaryScreen();
    }

    // Called when the local player continues past Match Summary — either by clicking
    // the button or via the 45s auto-timeout. The host's Start button in the Lobby has
    // no readiness gate at all (always available as an early-start option), so the
    // host never auto-readies here — they just land back in the Lobby where Start is
    // waiting for them. Everyone else marks themselves ready (so the ready count the
    // host sees builds up) and lands in the same place.
    public void onContinueFromSummary() {
        boolean isHost = state.lobby != null && state.lobby.ownerCxId.equals(state.user.cxId);
        if (!isHost) {
            state.user.isReady = true;
            if (state.lobby != null) {
                _bcWrapper.getLobbyService().updateReady(state.lobby.lobbyId, true,
                        buildExtraJson(state.user.colorIndex), null);
            }
        }
        goToLobbyScreen();
    }

    private void applyMatchResult(int round, JSONArray entries) {
        if (state.matchResult.valid && state.matchResult.round == round)
            return; // idempotent — guards against a duplicate broadcast (e.g. a migrated host)
        MatchResult result = new MatchResult();
        result.round = round;
        result.valid = true;
        for (int i = 0; i < entries.length(); i++) {
            JSONObject e = entries.getJSONObject(i);
            MatchResult.Entry entry = new MatchResult.Entry();
            entry.cxId = e.getString("cx");
            entry.rank = e.getInt("r");
            entry.coveragePct = e.getInt("c") / 100.0f; // basis points -> percent
            entry.beaten = e.getInt("b");
            result.entries.add(entry);
        }
        state.matchResult = result;
        SwingUtilities.invokeLater(this::onStateChanged);
    }

    private MatchResult buildMatchResult(int round, List<Coverage.CoverageEntry> coverage) {
        MatchResult result = new MatchResult();
        result.round = round;
        result.valid = true;
        for (Coverage.CoverageEntry c : coverage) {
            MatchResult.Entry e = new MatchResult.Entry();
            e.cxId = c.cxId;
            e.rank = c.rank;
            e.coveragePct = c.coveragePct;
            e.beaten = c.beaten;
            result.entries.add(e);
        }
        return result;
    }

    // Host-only: computes the final coverage snapshot, broadcasts it to everyone
    // (relay op match_result) and posts it to the leaderboards via cloud code.
    // Guarded per-round since the points leaderboard is cumulative — a duplicate
    // post would silently and permanently inflate a lifetime total.
    private void broadcastMatchResults() {
        if (state.lobby == null) return;
        if (state.leaderboardPostedRound == state.roundNumber) return;
        state.leaderboardPostedRound = state.roundNumber;

        List<Coverage.CoverageEntry> coverage = Coverage.computeCoverage(state.splotches, state.lobby.members);
        MatchResult result = buildMatchResult(state.roundNumber, coverage);
        state.matchResult = result;
        SwingUtilities.invokeLater(this::onStateChanged);

        sendMatchResultToAll(result);
        hostPostMatchResultsToCloud(result);
    }

    private void sendMatchResultToAll(MatchResult result) {
        if (result.entries.isEmpty()) return;
        boolean isFirst = true;
        JSONArray batch = new JSONArray();
        int currentSize = 80; // envelope overhead estimate

        for (int i = 0; i <= result.entries.size(); i++) {
            String entryStr = null;
            JSONObject entry = null;
            if (i < result.entries.size()) {
                MatchResult.Entry e = result.entries.get(i);
                entry = new JSONObject()
                        .put("cx", e.cxId)
                        .put("r", e.rank)
                        .put("c", Math.round(e.coveragePct * 100.0f))
                        .put("b", e.beaten);
                entryStr = entry.toString();
            }

            boolean isLastIteration = (i == result.entries.size());
            boolean flush = isLastIteration || (entry != null
                    && currentSize + entryStr.length() + 1 > MAX_RELAY_BYTES && batch.length() > 0);
            if (flush && batch.length() > 0) {
                JSONObject msg = new JSONObject();
                msg.put("op", "match_result");
                msg.put("data", new JSONObject()
                        .put("round", result.round)
                        .put("first", isFirst)
                        .put("last", isLastIteration)
                        .put("e", batch));
                // Reliable AND ordered (unlike splotch_sync's reliable/unordered) — a chunk-
                // reassembly race is cosmetic for splotches but would corrupt a posted score here.
                _bcWrapper.getRelayService().sendToAll(
                        msg.toString().getBytes(StandardCharsets.US_ASCII),
                        true, true, RelayService.CHANNEL_HIGH_PRIORITY_1);
                isFirst = false;
                batch = new JSONArray();
                currentSize = 80;
            }
            if (entry != null) {
                batch.put(entry);
                currentSize += entryStr.length() + 1;
            }
        }
    }

    // The PostMatchResults cloud-code script itself is server-side (already
    // deployed against this brainCloud app — the cpp/react/godot clients already
    // call it) and posts on our behalf via postScoreToLeaderboardOnBehalfOf, since
    // individual clients can no longer post directly (closes an "any client can
    // post any score for itself" integrity hole).
    private void hostPostMatchResultsToCloud(MatchResult result) {
        JSONObject payload = new JSONObject();
        payload.put("round", result.round);
        payload.put("lobbyId", state.lobby.lobbyId);
        payload.put("pointsLeaderboardId", state.pointsLeaderboardId);
        payload.put("pointsLeaderboardIdQuarterly", state.pointsLeaderboardIdQuarterly);
        payload.put("coverageLeaderboardId", state.coverageLeaderboardId);
        payload.put("coverageLeaderboardIdQuarterly", state.coverageLeaderboardIdQuarterly);

        JSONArray entries = new JSONArray();
        for (MatchResult.Entry e : result.entries) {
            User member = memberByCxId(e.cxId);
            if (member == null || member.profileId.isEmpty())
                continue; // can't post server-side without a profileId
            entries.put(new JSONObject()
                    .put("profileId", member.profileId)
                    .put("name", member.name)
                    .put("points", e.beaten + 1)
                    .put("coverageBasisPoints", Math.round(e.coveragePct * 100.0f)));
        }
        payload.put("entries", entries);

        final int round = result.round;
        _bcWrapper.getScriptService().runScript("PostMatchResults", payload.toString(), new IServerCallback() {
            @Override
            public void serverCallback(ServiceName sn, ServiceOperation so, JSONObject scriptResult) {
                try {
                    // The script's return value sits at data.response (a sibling of
                    // runTimeData/success), not directly at data.results.
                    JSONArray results = scriptResult.getJSONObject("data").getJSONObject("response").getJSONArray("results");
                    applyLeaderboardResultsFromCloud(round, results);
                } catch (Exception ex) {
                    System.out.println("Failed to parse PostMatchResults response: " + ex.getMessage());
                }
            }

            @Override
            public void serverError(ServiceName sn, ServiceOperation so, int statusCode, int reasonCode, String jsonError) {
                System.out.println("PostMatchResults failed: " + jsonError);
            }
        });
    }

    // Non-host clients don't get the leaderboard delta via relay (no such op
    // exists) — they poll a GlobalEntity the cloud script writes, indexed by
    // "<lobbyId>:<round>", since a host that disconnects right after posting
    // would otherwise leave everyone else waiting forever even though the post
    // itself already succeeded.
    public void tickMatchResultsPoll() {
        if (!state.matchResult.valid || state.lobby == null) return;
        if (state.lobby.ownerCxId.equals(state.user.cxId)) return; // host posts directly, no need to poll

        for (MatchResult.Entry e : state.matchResult.entries)
            if (e.lbDelta.ready) return; // already applied this round

        long now = System.currentTimeMillis();
        if (now - _lastResultsPollMs < RESULTS_POLL_INTERVAL_MS) return;
        _lastResultsPollMs = now;

        String indexedId = state.lobby.lobbyId + ":" + state.matchResult.round;
        final int round = state.matchResult.round;
        _bcWrapper.getGlobalEntityService().getListByIndexedId(indexedId, 1, new IServerCallback() {
            @Override
            public void serverCallback(ServiceName sn, ServiceOperation so, JSONObject result) {
                try {
                    JSONArray entityList = result.getJSONObject("data").getJSONArray("entityList");
                    if (entityList.length() == 0) return;
                    JSONArray results = entityList.getJSONObject(0).getJSONObject("data").getJSONArray("results");
                    applyLeaderboardResultsFromCloud(round, results);
                } catch (Exception ex) {
                    System.out.println("Failed to parse match-results GlobalEntity: " + ex.getMessage());
                }
            }

            @Override
            public void serverError(ServiceName sn, ServiceOperation so, int statusCode, int reasonCode, String jsonError) {
                // Silently retry next tick — MatchSummaryScreen shows "Leaderboard
                // unavailable" once LEADERBOARD_TIMEOUT_MS has elapsed.
            }
        });
    }

    // Shared by both the host's direct script response and the GlobalEntity poll.
    private void applyLeaderboardResultsFromCloud(int round, JSONArray results) {
        if (!state.matchResult.valid || state.matchResult.round != round) return;
        for (int i = 0; i < results.length(); i++) {
            JSONObject r = results.getJSONObject(i);
            User member = memberByProfileId(r.optString("profileId", ""));
            if (member == null) continue;
            for (MatchResult.Entry e : state.matchResult.entries) {
                if (!e.cxId.equals(member.cxId)) continue;
                e.lbDelta.ready = true;
                parsePeriodDelta(e.lbDelta.pointsLifetime, r.optJSONObject("pointsLifetime"));
                parsePeriodDelta(e.lbDelta.pointsQuarterly, r.optJSONObject("pointsQuarterly"));
                parsePeriodDelta(e.lbDelta.coverageLifetime, r.optJSONObject("coverageLifetime"));
                parsePeriodDelta(e.lbDelta.coverageQuarterly, r.optJSONObject("coverageQuarterly"));
                break;
            }
        }
        SwingUtilities.invokeLater(this::onStateChanged);
    }

    private void parsePeriodDelta(MatchResult.PeriodDelta delta, JSONObject period) {
        if (period == null) return;
        delta.improved = period.optBoolean("improved", false);
        delta.rankBefore = period.optInt("before", -1);
        delta.rankAfter = period.optInt("after", -1);
    }

    private User memberByCxId(String cxId) {
        if (state.lobby == null) return null;
        for (User m : state.lobby.members)
            if (m.cxId.equals(cxId)) return m;
        return null;
    }

    private User memberByProfileId(String profileId) {
        if (state.lobby == null || profileId.isEmpty()) return null;
        for (User m : state.lobby.members)
            if (m.profileId.equals(profileId)) return m;
        return null;
    }

    // ── Shared RTT enable (chat + matchmaking both fund through this) ──────────

    private void ensureRTTEnabled(Runnable onReady) {
        if (_bcWrapper.getRTTService().getRTTEnabled()) {
            onReady.run();
            return;
        }
        _rttEnableWaiters.add(onReady);
        if (_rttConnecting) return;
        _rttConnecting = true;

        _bcWrapper.getRTTService().enableRTT(new IRTTConnectCallback() {
            @Override
            public void rttConnectSuccess() {
                _rttConnecting = false;
                state.user.cxId = _bcWrapper.getClient().getRttConnectionId();
                ArrayList<Runnable> waiters = new ArrayList<>(_rttEnableWaiters);
                _rttEnableWaiters.clear();
                for (Runnable r : waiters) r.run();
            }

            @Override
            public void rttConnectFailure(String errorMessage) {
                _rttConnecting = false;
                _rttEnableWaiters.clear();
                if (!_disconnecting) {
                    dieWithMessage("Failed to enable RTT: " + errorMessage);
                }
            }
        });
    }

    // ── Global chat ─────────────────────────────────────────────────────────────

    private void enableChatRTT() {
        if (!_chatRTTRegistered) {
            _chatRTTRegistered = true;
            _bcWrapper.getRTTService().registerRTTChatCallback(new IRTTCallback() {
                @Override
                public void rttCallback(JSONObject eventJson) {
                    onChatRTTEvent(eventJson);
                }
            });
        }
        ensureRTTEnabled(this::ensureChatChannel);
    }

    private void ensureChatChannel() {
        if (_chatChannelId != null || _chatChannelResolving) return;
        if (System.currentTimeMillis() < _chatChannelRetryAtMs) return;
        _chatChannelResolving = true;

        _bcWrapper.getChatService().getChannelId("gl", "gl", new IServerCallback() {
            @Override
            public void serverCallback(ServiceName sn, ServiceOperation so, JSONObject result) {
                String channelId;
                try {
                    channelId = result.getJSONObject("data").getString("channelId");
                } catch (Exception ex) {
                    serverError(sn, so, 0, 0, ex.getMessage());
                    return;
                }

                _bcWrapper.getChatService().channelConnect(channelId, 30, new IServerCallback() {
                    @Override
                    public void serverCallback(ServiceName sn2, ServiceOperation so2, JSONObject connectResult) {
                        _chatChannelId = channelId;
                        _chatChannelResolving = false;
                        try {
                            JSONArray messages = connectResult.getJSONObject("data").getJSONArray("messages");
                            state.chatMessages.clear();
                            for (int i = 0; i < messages.length(); i++)
                                state.chatMessages.add(parseChatMessage(messages.getJSONObject(i)));
                        } catch (Exception ex) {
                            System.out.println("Failed to parse chat history: " + ex.getMessage());
                        }
                        SwingUtilities.invokeLater(App.this::onStateChanged);
                    }

                    @Override
                    public void serverError(ServiceName sn2, ServiceOperation so2, int statusCode, int reasonCode, String jsonError) {
                        _chatChannelResolving = false;
                        _chatChannelRetryAtMs = System.currentTimeMillis() + CHAT_CHANNEL_RETRY_MS;
                    }
                });
            }

            @Override
            public void serverError(ServiceName sn, ServiceOperation so, int statusCode, int reasonCode, String jsonError) {
                _chatChannelResolving = false;
                _chatChannelRetryAtMs = System.currentTimeMillis() + CHAT_CHANNEL_RETRY_MS;
            }
        });
    }

    private ChatMessage parseChatMessage(JSONObject m) {
        ChatMessage msg = new ChatMessage();
        msg.msgId = m.optString("msgId", "");
        JSONObject from = m.optJSONObject("from");
        String fromName = from != null ? from.optString("name", "") : "";
        msg.fromName = fromName.isEmpty() ? "Player" : fromName;
        JSONObject content = m.optJSONObject("content");
        msg.text = content != null ? content.optString("text", "") : "";
        return msg;
    }

    // operation: INCOMING (new message) / UPDATE (edited) / DELETE (removed), all
    // keyed by msgId, exactly mirroring the reference cpp client.
    private void onChatRTTEvent(JSONObject eventJson) {
        try {
            if (!"chat".equals(eventJson.optString("service"))) return;
            String operation = eventJson.getString("operation"); // sibling of "data", not nested inside it
            JSONObject data = eventJson.getJSONObject("data");

            if (operation.equals("DELETE")) {
                String msgId = data.getString("msgId");
                state.chatMessages.removeIf(m -> m.msgId.equals(msgId));
            } else {
                ChatMessage msg = parseChatMessage(data);
                boolean replaced = false;
                for (int i = 0; i < state.chatMessages.size(); i++) {
                    if (state.chatMessages.get(i).msgId.equals(msg.msgId)) {
                        state.chatMessages.set(i, msg);
                        replaced = true;
                        break;
                    }
                }
                if (!replaced) state.chatMessages.add(msg);
            }
            SwingUtilities.invokeLater(this::onStateChanged);
        } catch (Exception e) {
            System.out.println("Failed to parse chat RTT event: " + e.getMessage());
        }
    }

    public void sendGlobalChatMessage(String text) {
        if (_chatChannelId == null || text.isEmpty()) return;
        _bcWrapper.getChatService().postChatMessageSimple(_chatChannelId, text, true, null);
    }

    // Sends a chat message to everyone currently in this lobby, via the Lobby
    // service's SendSignal (not the Chat service — rides the RTT connection the
    // lobby already has, no separate channel/registration needed). Appends
    // locally right away; the receive handler (onLobbyEvent, "SIGNAL" operation)
    // skips the echo of our own signal that the server sends back to us too.
    public void sendLobbySignalChat(String text) {
        if (text.isEmpty() || state.lobby == null) return;

        JSONObject signal = new JSONObject();
        signal.put("text", text);
        _bcWrapper.getLobbyService().sendSignal(state.lobby.lobbyId, signal.toString(), null);

        ChatMessage msg = new ChatMessage();
        msg.fromName = state.user.name;
        msg.text = text;
        state.lobby.chatMessages.add(msg);
        onStateChanged();
    }

    void onLobbyEvent(JSONObject result) {
        JSONObject jsonData = result.getJSONObject("data");

        if (jsonData.has("lobby")) {
            ArrayList<ChatMessage> carryForwardChat = state.lobby != null ? state.lobby.chatMessages : null;
            state.lobby = new Lobby(jsonData.getJSONObject("lobby"), jsonData.getString("lobbyId"));
            if (carryForwardChat != null) state.lobby.chatMessages = carryForwardChat;
            if (state.lobbyJoinedAtMs == 0) state.lobbyJoinedAtMs = System.currentTimeMillis();
            onStateChanged();

            if (state.screen instanceof LoadingScreen) {
                goToLobbyScreen();
            }
        }

        String operation = result.getString("operation");

        if (operation.equals("SIGNAL")) {
            // This-lobby chat, per the design direction: implemented via SendSignal
            // (Lobby service), not the Chat service. Real wire shape: data: {
            // lobbyId, from: {id,name,pic,cxId}, signalData: <our own payload> }.
            // "from" is the server's authoritative sender info.
            JSONObject fromJson = jsonData.optJSONObject("from");
            String fromCxId = fromJson != null ? fromJson.optString("cxId", "") : "";
            String fromName = fromJson != null ? fromJson.optString("name", "") : "";
            JSONObject signalData = jsonData.optJSONObject("signalData");
            String text = signalData != null ? signalData.optString("text", "") : "";

            // Skip echoes of our own signal — sendLobbySignalChat already appended
            // it locally on send. Compared by cxId (not name) since two players
            // could share a display name.
            if (!text.isEmpty() && !fromCxId.equals(state.user.cxId) && state.lobby != null) {
                ChatMessage msg = new ChatMessage();
                msg.fromName = fromName.isEmpty() ? "Player" : fromName;
                msg.text = text;
                state.lobby.chatMessages.add(msg);
                onStateChanged();
            }
        } else if (operation.equals("DISBANDED")) {
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
            case "WS": // MainMenuScreen shows the abbreviated label
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

            _disconnecting = false;
            ensureRTTEnabled(() -> {
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

    // Host: end the match for all players early. Note this skips the
    // ResultsBroadcast phase entirely (no match_result, no leaderboard post) —
    // only a full-duration match gets scored; an early/manual end forfeits it,
    // matching the cpp reference client's behaviour. onMatchEnded() (triggered
    // for all players, including the host, via the END_MATCH system callback)
    // still shows a Match Summary with a local coverage snapshot.
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
    }

    // Host: clear all splotches for every player mid-game
    public void createSplotch(float x, float y, int colorIndex, double angle) {
        state.splotches.add(new Splotch(
                new java.awt.geom.Point2D.Float(x, y),
                colorIndex % Colors.NUM_COLORS,
                angle));
        state.splotchGeneration++;
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
        // disableRTT() drops the connection entirely, taking the chat channel
        // subscription with it — reset both so goToMainMenuScreen() below
        // re-resolves and re-subscribes fresh instead of trusting a stale channel id.
        _chatRTTRegistered = false;
        _chatChannelId = null;
        _chatChannelResolving = false;
        _chatChannelRetryAtMs = 0;

        state.lobby = null;
        state.user.isReady = false;
        state.lobbyStatusText = "";
        state.lobbySubStatus = "";
        state.lobbyStatusStartTime = 0;
        state.matchResult = new MatchResult();
        state.coverage.clear();
        state.lobbyJoinedAtMs = 0;
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

    // Non-host ready toggle — mirrors cpp's app_toggleReady(). Starting the round is
    // still host-only (via onGameStart/the Start button), but every other member
    // needs a way to signal readiness; unlike onGameStart this only flips the local
    // flag, it never forces true and never starts anything itself.
    public void onToggleReady() {
        state.user.isReady = !state.user.isReady;
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
