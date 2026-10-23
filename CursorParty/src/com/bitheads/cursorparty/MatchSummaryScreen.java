package com.bitheads.cursorparty;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;

import javax.swing.JPanel;

// Shown between a match ending and the next round. Port of cpp's matchSummary.cpp: winner
// banner, one card per player (coverage, points, leaderboard movement), rematch countdown.
class MatchSummaryScreen extends Screen
{
    // cpp MATCH_SUMMARY_REMATCH_MS / LEADERBOARD_RESULT_TIMEOUT_MS
    static final long REMATCH_MS = 45000L;
    static final long LEADERBOARD_TIMEOUT_MS = 8000L;

    private static final int PANEL_W = 940;
    private static final int PANEL_H = 640;
    private static final int LINE_H = 22;

    private static final Color COLOR_ME = new Color(0x59, 0xFF, 0x73);
    private static final Color COLOR_GOOD = new Color(0x73, 0xF2, 0x8C);
    private static final Color COLOR_BEST = new Color(0xFF, 0xD6, 0x4D);

    private final Theme.FlatButton _btnRematch;
    private final Theme.FlatButton _btnMenu;
    private final javax.swing.Timer _tickTimer;
    private int _px, _py, _pw, _ph;

    public MatchSummaryScreen()
    {
        panel = new SummaryPanel();
        panel.setLayout(null);
        panel.setBackground(Theme.BG);

        // Host: Start has no readiness gate, so this just returns to the Lobby.
        // Everyone else readies up (so the host sees the count build) and returns too.
        _btnRematch = Theme.button("");
        _btnRematch.addActionListener(e -> App.getInstance().onContinueFromSummary());
        panel.add(_btnRematch);

        _btnMenu = Theme.button("Main Menu");
        _btnMenu.addActionListener(e -> App.getInstance().onGameScreenClose());
        panel.add(_btnMenu);

        refreshUI();

        _tickTimer = new javax.swing.Timer(1000, null);
        _tickTimer.addActionListener(e -> {
            if (App.getInstance().state.screen != MatchSummaryScreen.this)
            {
                _tickTimer.stop();
                return;
            }
            tick();
        });
        _tickTimer.start();
    }

    @Override
    public void onStateChanged(State state)
    {
        refreshUI();
    }

    private void tick()
    {
        State state = App.getInstance().state;
        App.getInstance().tickMatchResultsPoll();
        if (System.currentTimeMillis() - state.matchSummaryArrivalTime >= REMATCH_MS)
        {
            App.getInstance().onContinueFromSummary();
            return; // screen just changed to Lobby
        }
        refreshUI();
    }

    void refreshUI()
    {
        State state = App.getInstance().state;
        Dimension screenRes = App.getInstance().contentSize();
        _pw = Math.min(PANEL_W, screenRes.width - 48);
        _ph = Math.min(PANEL_H, screenRes.height - 48);
        _px = (screenRes.width - _pw) / 2;
        _py = (screenRes.height - _ph) / 2;

        boolean isHost = state.lobby != null && state.lobby.ownerCxId.equals(state.user.cxId);
        int ready = 0, total = 0;
        if (state.lobby != null)
        {
            total = state.lobby.members.size();
            for (User m : state.lobby.members)
                if (m.cxId.equals(state.user.cxId) ? state.user.isReady : m.isReady) ready++;
        }
        _btnRematch.setText(isHost ? "Return to Lobby" : "Queue for Rematch  " + ready + "/" + total);

        int cw = _pw - 28, bw = (int) (_pw * 0.6);
        int fy = _py + _ph - 82;
        _btnRematch.setBounds(_px + 14, fy + 30, bw, 36);
        _btnMenu.setBounds(_px + 14 + bw + 8, fy + 30, cw - bw - 8, 36);
        panel.repaint();
    }

    private String nameForCxId(String cxId)
    {
        State state = App.getInstance().state;
        if (state.lobby != null)
            for (User m : state.lobby.members)
                if (m.cxId.equals(cxId)) return m.name;
        return "?";
    }

    private int colorIndexForCxId(String cxId)
    {
        State state = App.getInstance().state;
        if (cxId.equals(state.user.cxId)) return state.user.colorIndex;
        if (state.lobby != null)
            for (User m : state.lobby.members)
                if (m.cxId.equals(cxId)) return m.colorIndex;
        return 0;
    }

    // Only the periods that actually improved, e.g. just "Quarterly #52->#46".
    private static String periodsText(MatchResult.PeriodDelta lifetime, MatchResult.PeriodDelta quarterly)
    {
        ArrayList<String> parts = new ArrayList<>();
        if (lifetime.improved) parts.add("Lifetime #" + lifetime.rankBefore + "->#" + lifetime.rankAfter);
        if (quarterly.improved) parts.add("Quarterly #" + quarterly.rankBefore + "->#" + quarterly.rankAfter);
        return String.join("  |  ", parts);
    }

    class SummaryPanel extends JPanel
    {
        private Graphics2D g2;

        private int pill(String text, int x, int y, Color bg, Color fg)
        {
            g2.setFont(Theme.font(12));
            FontMetrics fm = g2.getFontMetrics();
            int w = fm.stringWidth(text) + 16;
            g2.setColor(bg);
            g2.fillRoundRect(x, y, w, fm.getHeight() + 6, 8, 8);
            g2.setColor(fg);
            g2.drawString(text, x + 8, y + 3 + fm.getAscent());
            return w;
        }

        private void text(String s, int x, int y, Color c, int size)
        {
            g2.setFont(Theme.font(size));
            g2.setColor(c);
            g2.drawString(s, x, y + g2.getFontMetrics().getAscent());
        }

        private void textRight(String s, int right, int y, Color c, int size)
        {
            g2.setFont(Theme.font(size));
            text(s, right - g2.getFontMetrics().stringWidth(s), y, c, size);
        }

        private int cardHeight(MatchResult.Entry e)
        {
            int lines = 1;
            if (!e.lbDelta.ready) return LINE_H * 3 + LINE_H + 16;
            boolean pointsUp = e.lbDelta.pointsLifetime.improved || e.lbDelta.pointsQuarterly.improved;
            boolean coverageBest = e.lbDelta.coverageLifetime.improved || e.lbDelta.coverageQuarterly.improved;
            lines += (pointsUp ? 1 : 0) + (coverageBest ? 1 : 0) + (!pointsUp && !coverageBest ? 1 : 0);
            return LINE_H * (2 + lines) + 16;
        }

        private int drawCard(MatchResult.Entry e, int x, int y, int w)
        {
            State state = App.getInstance().state;
            boolean isMe = e.cxId.equals(state.user.cxId);
            int h = cardHeight(e);
            g2.setColor(new Color(0x1F, 0x21, 0x2E));
            g2.fillRoundRect(x, y, w, h, 10, 10);
            g2.setColor(isMe ? COLOR_ME : Theme.BORDER);
            g2.setStroke(new BasicStroke(isMe ? 1.5f : 1f));
            g2.drawRoundRect(x, y, w - 1, h - 1, 10, 10);
            g2.setStroke(new BasicStroke(1f));

            int px = x + 14, py = y + 10;
            text("#" + e.rank, px, py, Theme.rankColor(e.rank), 12);
            g2.setColor(Colors.COLORS[colorIndexForCxId(e.cxId) % Colors.NUM_COLORS]);
            g2.fillOval(px + 26, py + 4, 8, 8);
            String name = nameForCxId(e.cxId);
            text(name, px + 40, py, isMe ? COLOR_ME : Theme.TEXT, 12);
            if (isMe)
            {
                g2.setFont(Theme.font(12));
                text("(YOU)", px + 46 + g2.getFontMetrics().stringWidth(name), py, Theme.DIM, 12);
            }
            textRight(String.format("%.0f%%", e.coveragePct), x + w - 20, py, isMe ? COLOR_ME : Theme.TEXT, 12);
            textRight("COVERAGE", x + w - 20, py + LINE_H - 4, Theme.DIM, 12);

            py += LINE_H * 2;
            int pw = pill("+" + (e.beaten + 1) + " pts", px, py, new Color(0x33, 0x3D, 0x57), new Color(0xBF, 0xD1, 0xFF));
            text(e.beaten + " beaten + 1 for playing", px + pw + 6, py + 3, Theme.DIM, 12);
            py += LINE_H;

            if (!e.lbDelta.ready)
            {
                long since = System.currentTimeMillis() - state.matchSummaryArrivalTime;
                text(since >= LEADERBOARD_TIMEOUT_MS ? "Leaderboard unavailable" : "Updating leaderboards...", px, py + 3, Theme.DIM, 12);
                return h;
            }
            MatchResult.LeaderboardDelta lb = e.lbDelta;
            boolean pointsUp = lb.pointsLifetime.improved || lb.pointsQuarterly.improved;
            boolean coverageBest = lb.coverageLifetime.improved || lb.coverageQuarterly.improved;
            if (pointsUp)
            {
                pill("^ Rank up - Opponents Beaten  " + periodsText(lb.pointsLifetime, lb.pointsQuarterly), px, py, new Color(0x29, 0x52, 0x33), COLOR_GOOD);
                py += LINE_H;
            }
            if (coverageBest)
            {
                pill("* Personal best - Coverage %  " + periodsText(lb.coverageLifetime, lb.coverageQuarterly), px, py, new Color(0x57, 0x47, 0x1A), COLOR_BEST);
                py += LINE_H;
            }
            if (!pointsUp && !coverageBest)
                pill("No leaderboard rank change this round", px, py, new Color(0x33, 0x33, 0x3D), Theme.DIM);
            return h;
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            State state = App.getInstance().state;

            g2.setColor(Theme.WINDOW);
            g2.fillRoundRect(_px, _py, _pw, _ph, 10, 10);
            g2.setColor(Theme.BORDER);
            g2.drawRoundRect(_px, _py, _pw - 1, _ph - 1, 10, 10);

            int cx = _px + 14, cy = _py + 14, cw = _pw - 28;
            text("Match Summary", cx, cy, Theme.TEXT, 18);
            int players = state.lobby != null ? state.lobby.members.size() : 0;
            text("Lobby " + (state.lobby != null ? state.lobby.lobbyId : "") + " - " + players + " Players", cx, cy + 26, Theme.DIM, 12);
            cy += 50;

            MatchResult result = state.matchResult;
            if (result.valid && !result.entries.isEmpty())
            {
                MatchResult.Entry winner = result.entries.get(0);
                g2.setColor(new Color(0x4D, 0x3D, 0x14));
                g2.fillRoundRect(cx, cy, cw, 40, 10, 10);
                g2.setColor(COLOR_BEST);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(cx, cy, cw - 1, 39, 10, 10);
                g2.setStroke(new BasicStroke(1f));
                text(String.format("%s wins the round, covering %.0f%% of the board.", nameForCxId(winner.cxId), winner.coveragePct),
                        cx + 14, cy + 12, COLOR_BEST, 12);
                cy += 52;
            }
            else
            {
                text("Waiting for results...", cx, cy, Theme.DIM, 12);
                cy += 24;
            }

            text("RANK / PLAYER", cx, cy, Theme.DIM, 12);
            text("LEADERBOARD RESULT", _px + _pw - 200, cy, Theme.DIM, 12);
            cy += 18;
            g2.setColor(Theme.BORDER);
            g2.drawLine(cx, cy, cx + cw, cy);
            cy += 8;

            int listBottom = _py + _ph - 90;
            g2.setClip(cx, cy, cw, Math.max(0, listBottom - cy));
            for (MatchResult.Entry e : result.entries)
                cy += drawCard(e, cx, cy, cw - 4) + 8;
            g2.setClip(null);

            // footer: next-round countdown above the rematch / main menu buttons
            int fy = _py + _ph - 82;
            g2.setColor(Theme.BORDER);
            g2.drawLine(cx, fy, cx + cw, fy);
            long left = Math.max(0, (REMATCH_MS - (System.currentTimeMillis() - state.matchSummaryArrivalTime) + 999) / 1000);
            text(String.format("Next Round: %d:%02d", left / 60, left % 60), cx, fy + 8, Theme.DIM, 12);
            g2.dispose();
        }
    }
}
