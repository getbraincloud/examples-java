package com.bitheads.cursorparty;

import java.util.ArrayList;

// Mirrors the cpp reference client's globals.h MatchResult/MatchResultEntry/
// LeaderboardDelta/LeaderboardPeriodDelta structs.
public class MatchResult
{
    public static class PeriodDelta
    {
        public boolean improved = false;
    }

    public static class LeaderboardDelta
    {
        public boolean ready = false;
        public PeriodDelta pointsLifetime = new PeriodDelta();
        public PeriodDelta pointsQuarterly = new PeriodDelta();
        public PeriodDelta coverageLifetime = new PeriodDelta();
        public PeriodDelta coverageQuarterly = new PeriodDelta();
    }

    public static class Entry
    {
        public String cxId = "";
        public int rank = 0;
        public float coveragePct = 0f;
        public int beaten = 0;
        public LeaderboardDelta lbDelta = new LeaderboardDelta();
    }

    public boolean valid = false;
    public int round = -1;
    public ArrayList<Entry> entries = new ArrayList<Entry>();
}
