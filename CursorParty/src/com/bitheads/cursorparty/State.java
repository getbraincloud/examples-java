package com.bitheads.cursorparty;

import java.util.ArrayList;

import org.json.JSONObject;

public class State
{
    public Screen screen;
    public User user;
    public Lobby lobby;
    public JSONObject server;
    public ArrayList<Shockwave> shockwaves   = new ArrayList<Shockwave>();
    public ArrayList<Splotch>  splotches     = new ArrayList<Splotch>();
    public boolean reliable = false;
    public boolean ordered  = true;
    public long    gameStartTime       = 0;   // epoch ms, 0 = not started
    public int     roundNumber         = 0;
    public long    lobbySearchStartTime = 0;  // epoch ms for loading screen timer
    public int     splotchDurationSec  = -1;  // -1 = forever
    public ArrayList<String> appLobbies = new ArrayList<String>();
    public String  lobbyStatusText      = "";  // "" = idle; non-empty = show status + timer in LobbyScreen
    public long    lobbyStatusStartTime = 0;   // epoch ms when lobbyStatusText was last set
}
