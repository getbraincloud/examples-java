package com.bitheads.cursorparty;

import java.awt.geom.Point2D;
import java.util.HashMap;

public class User
{
    public String cxId = "";
    public String name = "";
    public String profileId = ""; /* brainCloud profileId — needed server-side for leaderboard posting (not the same as cxId) */
    public int colorIndex = 7;
    public boolean isReady = false;
    public Point2D pos = null;
    public boolean allowSendTo = true;
    public HashMap<String, Integer> pings = new HashMap<>(); /* Pre-game region latencies shared via lobby extra (ms) */
    public int activePing = -1; /* Live relay-server RTT broadcast during gameplay (ms); -1 = not yet received */

    public User(String in_cxId, String in_name, int in_colorIndex, boolean in_isReady)
    {
        cxId = in_cxId;
        name = in_name;
        colorIndex = in_colorIndex;
        isReady = in_isReady;
    }
}
