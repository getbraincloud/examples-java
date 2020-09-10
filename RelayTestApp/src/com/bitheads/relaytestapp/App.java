package com.bitheads.relaytestapp;

import com.bitheads.braincloud.client.BrainCloudClient;
import com.bitheads.braincloud.client.BrainCloudWrapper;
import com.bitheads.braincloud.services.EventService;
import com.bitheads.braincloud.services.RelayService;
import com.bitheads.braincloud.client.IRelayCallback;
import com.bitheads.braincloud.client.IRelayConnectCallback;
import com.bitheads.braincloud.client.IRelaySystemCallback;
import com.bitheads.braincloud.client.IRTTCallback;
import com.bitheads.braincloud.client.IRTTCallback;
import com.bitheads.braincloud.client.IRTTConnectCallback;
import com.bitheads.braincloud.client.IServerCallback;
import com.bitheads.braincloud.client.ReasonCodes;
import com.bitheads.braincloud.client.RelayConnectionType;
import com.bitheads.braincloud.client.ServiceName;
import com.bitheads.braincloud.client.ServiceOperation;

import java.nio.charset.StandardCharsets;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.awt.Point;
import java.io.File;

import org.json.JSONArray;
import org.json.JSONObject;

public class App implements IRelayCallback, IRelaySystemCallback
{
    static App _instance = null;
    static public App getInstance()
    {
        return _instance;
    }

    public State state = new State(); // Publicly accessible state of the app
    public JFrame frame; // Main window frame

    BrainCloudWrapper _bcWrapper;
    boolean _isConnectingRTT = false;
    boolean _disconnecting = false;
    RelayConnectionType _connectionType = RelayConnectionType.WEBSOCKET;
    long _lastMoveSendTime = System.currentTimeMillis();
    boolean _pendingMoveSend = false;

    public static void main(String args[])
    {
        _instance = new App();
    }

    public App()
    {
        File dir1 = new File (".");
        System.out.println("current directory: " + dir1.getAbsolutePath());

        _bcWrapper = new BrainCloudWrapper();

        // Replace ids.appId, ids.appSecret, ids.url with your own.
        if (ids.url == null)
        {
            _bcWrapper.initialize(ids.appId, ids.appSecret, Version.version);
        }
        else
        {
            _bcWrapper.initialize(ids.appId, ids.appSecret, Version.version, ids.url);
        }

        _bcWrapper.getClient().enableLogging(true);

        startCallbackLoop();

        // Schedule a job for the event-dispatching thread:
        // creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                createAndShowGUI();
            }
        });
    }
    
    void startCallbackLoop()
    {
        App that = this;

        // Start a listening thread for braincloud events
        Thread bcThread = new Thread(() -> 
        {
            synchronized(that)
            {
                try
                {
                    while (true)
                    {
                        _bcWrapper.runCallbacks();
                        wait(1);

                        // Check if we don't have move send pendings
                        if (_pendingMoveSend) {
                            long nowMs = System.currentTimeMillis();
                            if (nowMs - _lastMoveSendTime >= 1000 / 60) { // Cap send at 60 fps
                                sendPlayerMove();
                            }
                        }
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });
        bcThread.start();
    }
    
    void createAndShowGUI()
    {
        // Main window frame
        frame = new JFrame("Relay Test App");
        frame.getContentPane().setPreferredSize(new Dimension(1024, 768));
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new java.awt.event.WindowAdapter()
        {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent)
            {
                System.exit(0);
            }
        });
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.validate();
        frame.repaint();

        goToLoginScreen();
    }

    void changeScreen(Screen screen)
    {
        state.screen = screen;
        frame.getContentPane().removeAll();
        frame.getContentPane().add(state.screen.panel);
        onStateChanged();
    }

    public void onStateChanged()
    {
        state.screen.onStateChanged(state);
        frame.pack();
        frame.validate();
        frame.repaint();
    }

    public void goToLoginScreen()
    {
        changeScreen(new LoginScreen());
    }

    public void goToLoadingScreen(String text)
    {
        changeScreen(new LoadingScreen(text));
    }

    public void goToMainMenuScreen()
    {
        changeScreen(new MainMenuScreen());
    }

    public void goToLobbyScreen()
    {
        changeScreen(new LobbyScreen());
    }

    public void goToGameScreen()
    {
        state.shockwaves.clear();
        _pendingMoveSend = false;
        _lastMoveSendTime = System.currentTimeMillis();
        changeScreen(new GameScreen());
    }

    public void dieWithMessage(String message)
    {
        _bcWrapper.getRelayService().disconnect();
        _bcWrapper.getRelayService().deregisterSystemCallback();
        _bcWrapper.getRelayService().deregisterRelayCallback();
        _bcWrapper.getRTTService().deregisterAllCallbacks();
        _bcWrapper.getClient().resetCommunication();

        JOptionPane.showMessageDialog(frame, message, "ERROR", JOptionPane.ERROR_MESSAGE);

        goToLoginScreen();
    }

    public void brainCloudConnect(String username, String password)
    {
        // Swap to the connecting screen.
        goToLoadingScreen("Connecting...");

        synchronized(this)
        {
            _bcWrapper.authenticateUniversal(username, password, true, new IServerCallback()
            {
                @Override
                public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject result)
                {
                    JSONObject jsonData = result.getJSONObject("data");
                    
                    // Update username stored in brainCloud.
                    // This is necessary because the login username is not necessary the app username (Player name)
                    _bcWrapper.getPlayerStateService().updateName(username, null);

                    // Set the state with our user information. 7 is default white color index.
                    state.user = new User(jsonData.getString("profileId"), username, 7, false);
                    state.user.allowSendTo = false; // We don't relay packet to ourself
                    goToMainMenuScreen();
                }

                @Override
                public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError)
                {
                    dieWithMessage("Failed to authenticate.");
                }
            });
        }
    }

    @Override
    public void relayCallback(int netId, byte[] bytes)
    {
        try
        {
            String jsonString = new String(bytes, StandardCharsets.US_ASCII);
            JSONObject json = new JSONObject(jsonString);

            String profileId = _bcWrapper.getRelayService().getProfileIdForNetId(netId);
            User user = null;
            for (int i = 0; i < state.lobby.members.size(); ++i)
            {
                User member = state.lobby.members.get(i);
                if (member.profileId.equals(profileId))
                {
                    user = member;
                    break;
                }
            }
            if (user == null)
            {
                return;
            }
    
            switch (json.getString("op"))
            {
                case "move":
                {
                    JSONObject posJson = json.getJSONObject("data");
                    user.pos = new Point(posJson.getInt("x"), posJson.getInt("y"));
                    break;
                }
                case "shockwave":
                {
                    JSONObject posJson = json.getJSONObject("data");
                    state.shockwaves.add(new Shockwave(new Point(posJson.getInt("x"), posJson.getInt("y")), Colors.COLORS[user.colorIndex]));
                    break;
                }
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
            dieWithMessage("Bad packet.");
        }
    }

    @Override
    public void relaySystemCallback(JSONObject jsonData)
    {
        if (jsonData.getString("op").equals("DISCONNECT")) // A member has disconnected from the game
        {
            for (int i = 0; i < state.lobby.members.size(); ++i)
            {
                User member = state.lobby.members.get(i);
                if (member.profileId.equals(jsonData.getString("profileId")))
                {
                    member.pos = null; // This will stop displaying this member
                }
            }
        }
    }

    void onLobbyEvent(JSONObject result)
    {
        JSONObject jsonData = result.getJSONObject("data");

        // If there is a lobby object present in the message, update our lobby
        // state with it.
        if (jsonData.has("lobby"))
        {
            state.lobby = new Lobby(jsonData.getJSONObject("lobby"), jsonData.getString("lobbyId"));
            onStateChanged();

            // If we were joining lobby, show the lobby screen. We have the information to
            // display now.
            if (state.screen instanceof LoadingScreen)
            {
                goToLobbyScreen();
            }
        }

        String operation = result.getString("operation");

        if (operation.equals("DISBANDED"))
        {
            if (jsonData.getJSONObject("reason").getInt("code") == ReasonCodes.RTT_ROOM_READY)
            {
                // Server has been created. Connect to it
                _bcWrapper.getRelayService().registerRelayCallback(this);
                _bcWrapper.getRelayService().registerSystemCallback(this);
                JSONObject options = new JSONObject();
                options.put("ssl", false);
                options.put("host", state.server.getJSONObject("connectData").getString("address"));
                if (_connectionType == RelayConnectionType.WEBSOCKET)
                    options.put("port", state.server.getJSONObject("connectData").getJSONObject("ports").getInt("ws"));
                else if (_connectionType == RelayConnectionType.TCP)
                    options.put("port", state.server.getJSONObject("connectData").getJSONObject("ports").getInt("tcp"));
                else if (_connectionType == RelayConnectionType.UDP)
                    options.put("port", state.server.getJSONObject("connectData").getJSONObject("ports").getInt("udp"));
                options.put("passcode", state.server.getString("passcode"));
                options.put("lobbyId", state.server.getString("lobbyId"));
                _bcWrapper.getRelayService().connect(_connectionType, options, new IRelayConnectCallback()
                {
                    @Override
                    public void relayConnectSuccess(JSONObject jsonData)
                    {
                        goToGameScreen();
                    }

                    @Override
                    public void relayConnectFailure(String errorMessage)
                    {
                        if (!_disconnecting)
                        {
                            dieWithMessage("Failed to connect to server, msg: " + errorMessage);
                        }
                    }
                });
            }
            else
            {
                // Disbanded for any other reason than ROOM_READY, means we failed to launch the game.
                onGameScreenClose();
            }
        }
        else if (operation.equals("STARTING"))
        {
            // Game is starting, show loading screen
            goToLoadingScreen("Connecting...");
        }
        else if (operation.equals("ROOM_READY"))
        {
            // Server has been created, save connection info.
            state.server = jsonData;
        }
    }

    public void onPlayClicked(String protocolStr)
    {
        goToLoadingScreen("Joining...");

        switch (protocolStr)
        {
            case "WEBSOCKET": _connectionType = RelayConnectionType.WEBSOCKET; break;
            case "TCP": _connectionType = RelayConnectionType.TCP; break;
            case "UDP": _connectionType = RelayConnectionType.UDP; break;
        }

        synchronized(this)
        {
            _bcWrapper.getRTTService().registerRTTLobbyCallback(new IRTTCallback()
            {
                @Override
                public void rttCallback(JSONObject eventJson)
                {
                    onLobbyEvent(eventJson);
                }
            });

            // Enable RTT service, then find a lobby
            _isConnectingRTT = true;
            _disconnecting = false;
            _bcWrapper.getRTTService().enableRTT(new IRTTConnectCallback()
            {
                @Override
                public void rttConnectSuccess()
                {
                    _isConnectingRTT = false;
                    _bcWrapper.getLobbyService().findOrCreateLobby("CursorPartyV2", 0, 1, "{\"strategy\":\"ranged-absolute\",\"alignment\":\"center\",\"ranges\":[1000]}", "{}", null, "{}", false, "{\"colorIndex\":" + state.user.colorIndex + "}", "all", new IServerCallback()
                    {
                        @Override
                        public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject result)
                        {
                            // Success of lobby found will be in the event onLobbyEvent
                        }
        
                        @Override
                        public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError)
                        {
                            dieWithMessage("Failed to find lobby.\n" + reasonCode);
                        }
                    });
                }
            
                @Override
                public void rttConnectFailure(String errorMessage)
                {
                    if (_isConnectingRTT)
                    {
                        dieWithMessage("Failed to enable RTT");
                    }
                    else if (!_disconnecting)
                    {
                        dieWithMessage("RTT Disconnected");
                    }
                }
            });
        }
    }

    public void onGameScreenClose()
    {
        _disconnecting = true;

        _bcWrapper.getRelayService().deregisterRelayCallback();
        _bcWrapper.getRelayService().deregisterSystemCallback();
        _bcWrapper.getRelayService().disconnect();
        _bcWrapper.getRTTService().deregisterAllCallbacks();
        _bcWrapper.getRTTService().disableRTT();

        state.lobby = null;
        state.server = null;
        state.user.isReady = false;
        _pendingMoveSend = false; //TODO put that in state object
        _lastMoveSendTime = System.currentTimeMillis();
        goToMainMenuScreen();
    }

    public void onColorChanged(int colorIndex)
    {
        state.user.colorIndex = colorIndex;
        _bcWrapper.getLobbyService().updateReady(state.lobby.lobbyId, state.user.isReady, "{\"colorIndex\":" + colorIndex + "}", null);
        onStateChanged();
    }

    public void onGameStart()
    {
        state.user.isReady = true;
        _bcWrapper.getLobbyService().updateReady(state.lobby.lobbyId, state.user.isReady, "{\"colorIndex\":" + state.user.colorIndex + "}", null);
        onStateChanged();
    }

    public void sendPlayerMove()
    {
        JSONObject data = new JSONObject();
        data.put("op", "move");
        JSONObject posJson = new JSONObject();
        posJson.put("x", state.user.pos.x);
        posJson.put("y", state.user.pos.y);
        data.put("data", posJson);

        _bcWrapper.getRelayService().sendToAll(
            data.toString().getBytes(StandardCharsets.US_ASCII),
            state.reliable,
            state.ordered,
            RelayService.CHANNEL_HIGH_PRIORITY_1);

        _lastMoveSendTime = System.currentTimeMillis();
    }

    public void onPlayerMove(int x, int y)
    {
        // Update our own position right away
        state.user.pos = new Point(x, y);

        User myUser = null;
        for (int i = 0; i < state.lobby.members.size(); ++i)
        {
            User member = state.lobby.members.get(i);
            if (member.profileId.equals(state.user.profileId))
            {
                myUser = member;
                break;
            }
        }
        if (myUser != null)
        {
            myUser.pos = new Point(x, y);
        }

        // Make sure we don't send faster than 60 fps. Java seems to trigger mouse move events
        // at a crazy rate!
        _pendingMoveSend = true;
    }

    public void onPlayerShockwave(int x, int y)
    {
        state.shockwaves.add(new Shockwave(new Point(x, y), Colors.COLORS[state.user.colorIndex]));

        JSONObject data = new JSONObject();
        data.put("op", "shockwave");
        JSONObject posJson = new JSONObject();
        posJson.put("x", x);
        posJson.put("y", y);
        data.put("data", posJson);

        long playerMask = 0;
        for (int i = 0; i < state.lobby.members.size(); ++i)
        {
            User user = state.lobby.members.get(i);
            if (!user.allowSendTo) continue;
            int netId = _bcWrapper.getRelayService().getNetIdForProfileId(user.profileId);
            playerMask |= (1L << (long)netId);
        }

        _bcWrapper.getRelayService().sendToPlayers(
            data.toString().getBytes(StandardCharsets.US_ASCII),
            playerMask, 
            false,
            true,
            RelayService.CHANNEL_HIGH_PRIORITY_1);
    }
}
