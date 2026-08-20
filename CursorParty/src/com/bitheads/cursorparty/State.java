package com.bitheads.cursorparty;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONObject;

public class State
{
    public Screen screen;
    public User user;
    public Lobby lobby;
    public JSONObject server;
    public boolean usePingData = false;
    public HashMap<String, Integer> pingData = new HashMap<>();
    public ArrayList<Shockwave> shockwaves   = new ArrayList<Shockwave>();
    public ArrayList<Splotch>  splotches     = new ArrayList<Splotch>();
    public boolean reliable = false;
    public boolean ordered  = true;
    public long    gameStartTime       = 0;   // epoch ms, 0 = not started
    public int     roundNumber         = 0;
    public long    lobbySearchStartTime = 0;  // epoch ms for loading screen timer
    public int     splotchDurationSec  = -1;  // -1 = forever
    public ArrayList<String> appLobbies = new ArrayList<String>();
    public String  lobbyStatusText      = "";  // "" = idle; non-empty = show banner in LobbyScreen
    public String  lobbySubStatus       = "";  // secondary line (progresses through provisioning steps)
    public long    lobbyStatusStartTime = 0;   // epoch ms when lobbyStatusText was last set
    public long    lobbyJoinedAtMs      = 0;   // epoch ms when this lobby was first entered; drives the INFO tab's "time in lobby"

    // Global chat (this-lobby chat lives on Lobby.chatMessages instead, since it
    // must be carried forward across the wholesale Lobby rebuilds onLobbyEvent does)
    public ArrayList<ChatMessage> chatMessages = new ArrayList<ChatMessage>();

    // Leaderboard ids — defaults match the cpp reference client; overridable via
    // the same Global App Properties mechanism as Colors/SplotchDuration above.
    public String pointsLeaderboardId            = "CursorParty_Points";
    public String pointsLeaderboardIdQuarterly    = "CursorParty_Points_Quarterly";
    public String coverageLeaderboardId           = "CursorParty_HighestCoverage";
    public String coverageLeaderboardIdQuarterly  = "CursorParty_HighestCoverage_Quarterly";

    // Coverage tracking
    public ArrayList<Coverage.CoverageEntry> coverage = new ArrayList<Coverage.CoverageEntry>(); // live in-match rank board
    public long splotchGeneration    = 0;  // bumped on every splotch add/clear/sync; gates the debounced recompute below
    public long coverageComputedGen  = -1;
    public long coverageComputedAtMs = 0;

    // Match summary / post-match leaderboard posting
    public MatchResult matchResult        = new MatchResult(); // valid=false until the host's broadcast (or a local fallback) lands
    public long          matchSummaryArrivalTime = 0; // epoch ms, drives the 45s auto-queue and 8s "leaderboard unavailable" timeouts
    public int            leaderboardPostedRound  = -1; // idempotency guard — the points board is cumulative, a double-post can't be undone
}
