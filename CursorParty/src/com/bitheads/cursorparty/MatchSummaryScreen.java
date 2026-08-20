package com.bitheads.cursorparty;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

// Shown between a match ending (relay system END_MATCH) and the next round
// starting. Mirrors cpp's matchSummary.cpp: winner banner, per-player cards
// (rank/coverage%/points pill/leaderboard-delta pills), a rematch countdown,
// and a "Queue for Rematch"/"Main Menu" button pair.
class MatchSummaryScreen extends Screen
{
    // Matches the cpp reference client's actual constant (a nearby comment there
    // says "15s auto-rematch" but the real MATCH_SUMMARY_REMATCH_MS is 45000ms).
    static final long REMATCH_MS = 45000L;
    static final long LEADERBOARD_TIMEOUT_MS = 8000L;

    private JLabel _lblCountdown;
    private javax.swing.Timer _tickTimer;

    public MatchSummaryScreen()
    {
        panel = new JPanel();
        panel.setLayout(null);
        panel.setBackground(Colors.BG_COLOR);
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

        long elapsed = System.currentTimeMillis() - state.matchSummaryArrivalTime;
        if (!state.user.isReady && elapsed >= REMATCH_MS)
        {
            App.getInstance().onSetRematchReady(true);
        }

        if (_lblCountdown != null)
        {
            long remaining = Math.max(0, REMATCH_MS - elapsed);
            _lblCountdown.setText(String.format("Next Round: %d:%02d", remaining / 1000 / 60, (remaining / 1000) % 60));
        }
        refreshUI();
    }

    void refreshUI()
    {
        panel.removeAll();
        State state = App.getInstance().state;
        JFrame frame = App.getInstance().frame;
        Dimension screenRes = frame.getPreferredSize();
        int cx = screenRes.width / 2;

        MatchResult result = state.matchResult;

        JLabel lblTitle = new JLabel("Match Summary", SwingConstants.CENTER);
        lblTitle.setSize(screenRes.width, 40);
        lblTitle.setLocation(0, 24);
        lblTitle.setFont(new Font(lblTitle.getFont().getName(), Font.PLAIN, 32));
        lblTitle.setForeground(Colors.TEXT_COLOR);
        panel.add(lblTitle);

        if (!result.valid || result.entries.isEmpty())
        {
            JLabel lblWaiting = new JLabel("Waiting for results...", SwingConstants.CENTER);
            lblWaiting.setSize(screenRes.width, 24);
            lblWaiting.setLocation(0, 90);
            lblWaiting.setFont(new Font("SansSerif", Font.PLAIN, 16));
            lblWaiting.setForeground(new Color(140, 150, 160));
            panel.add(lblWaiting);
        }
        else
        {
            MatchResult.Entry winner = result.entries.get(0);
            String winnerName = nameForCxId(winner.cxId);
            JLabel lblWinner = new JLabel(
                    String.format("%s wins the round, covering %.0f%% of the board.", winnerName, winner.coveragePct),
                    SwingConstants.CENTER);
            lblWinner.setSize(screenRes.width, 24);
            lblWinner.setLocation(0, 74);
            lblWinner.setFont(new Font("SansSerif", Font.BOLD, 16));
            lblWinner.setForeground(Colors.TEXT_COLOR);
            panel.add(lblWinner);

            int cardW = 620;
            int cardH = 62;
            int cardGap = 8;
            int y = 112;
            for (MatchResult.Entry e : result.entries)
            {
                drawPlayerCard(e, cx - cardW / 2, y, cardW, cardH);
                y += cardH + cardGap;
            }
        }

        int btnY = screenRes.height - 96;

        _lblCountdown = new JLabel("Next Round: 0:00", SwingConstants.CENTER);
        _lblCountdown.setSize(screenRes.width, 20);
        _lblCountdown.setLocation(0, btnY - 28);
        _lblCountdown.setFont(new Font("Monospaced", Font.PLAIN, 13));
        _lblCountdown.setForeground(new Color(170, 185, 210));
        panel.add(_lblCountdown);

        int readyCount = 0;
        int totalCount = state.lobby != null ? state.lobby.members.size() : 0;
        if (state.lobby != null)
            for (User m : state.lobby.members) if (m.isReady) readyCount++;

        JButton btnRematch = new JButton((state.user.isReady ? "Cancel Rematch " : "Queue for Rematch ") + readyCount + "/" + totalCount);
        btnRematch.setSize(220, 30);
        btnRematch.setLocation(cx - 230, btnY);
        panel.add(btnRematch);
        btnRematch.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                App.getInstance().onSetRematchReady(!state.user.isReady);
            }
        });

        JButton btnMenu = new JButton("Main Menu");
        btnMenu.setSize(160, 30);
        btnMenu.setLocation(cx + 30, btnY);
        panel.add(btnMenu);
        btnMenu.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                App.getInstance().onGameScreenClose();
            }
        });

        panel.revalidate();
        panel.repaint();
    }

    private String nameForCxId(String cxId)
    {
        State state = App.getInstance().state;
        if (state.lobby != null)
            for (User m : state.lobby.members)
                if (m.cxId.equals(cxId)) return m.name;
        return "Player";
    }

    private int colorIndexForCxId(String cxId)
    {
        State state = App.getInstance().state;
        if (state.lobby != null)
            for (User m : state.lobby.members)
                if (m.cxId.equals(cxId)) return m.colorIndex;
        return 0;
    }

    private void drawPlayerCard(MatchResult.Entry e, int x, int y, int w, int h)
    {
        State state = App.getInstance().state;
        boolean isMe = e.cxId.equals(state.user.cxId);

        JPanel card = new JPanel(null);
        card.setBounds(x, y, w, h);
        card.setBackground(new Color(0x2E, 0x33, 0x3D));
        panel.add(card);

        JLabel lblRank = new JLabel("#" + e.rank);
        lblRank.setBounds(10, 8, 40, 20);
        lblRank.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblRank.setForeground(Colors.TEXT_COLOR);
        card.add(lblRank);

        String name = nameForCxId(e.cxId) + (isMe ? " (you)" : "");
        JLabel lblName = new JLabel(name);
        lblName.setBounds(56, 8, 220, 20);
        lblName.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblName.setForeground(Colors.COLORS[colorIndexForCxId(e.cxId) % Colors.NUM_COLORS]);
        card.add(lblName);

        JLabel lblCoverage = new JLabel(String.format("%.1f%% covered", e.coveragePct));
        lblCoverage.setBounds(56, 30, 220, 18);
        lblCoverage.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblCoverage.setForeground(new Color(170, 185, 210));
        card.add(lblCoverage);

        JLabel lblPts = new JLabel("+" + (e.beaten + 1) + " pts");
        lblPts.setBounds(w - 300, 8, 90, 20);
        lblPts.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblPts.setForeground(new Color(0x66, 0xCC, 0xFF));
        card.add(lblPts);

        String lbText;
        Color lbColor;
        if (e.lbDelta.ready)
        {
            boolean pointsImproved = e.lbDelta.pointsLifetime.improved || e.lbDelta.pointsQuarterly.improved;
            boolean coverageImproved = e.lbDelta.coverageLifetime.improved || e.lbDelta.coverageQuarterly.improved;
            if (pointsImproved && coverageImproved)
            { lbText = "Rank up + Personal best!"; lbColor = new Color(0x66, 0xEE, 0x88); }
            else if (pointsImproved)
            { lbText = "Rank up - Opponents Beaten"; lbColor = new Color(0x66, 0xEE, 0x88); }
            else if (coverageImproved)
            { lbText = "Personal best - Coverage %"; lbColor = new Color(0x66, 0xEE, 0x88); }
            else
            { lbText = "No leaderboard rank change this round"; lbColor = new Color(140, 150, 160); }
        }
        else
        {
            long elapsedSinceArrival = System.currentTimeMillis() - state.matchSummaryArrivalTime;
            lbText = elapsedSinceArrival >= LEADERBOARD_TIMEOUT_MS ? "Leaderboard unavailable" : "Updating leaderboards...";
            lbColor = new Color(140, 150, 160);
        }
        JLabel lblDelta = new JLabel(lbText);
        lblDelta.setBounds(w - 220, 8, 210, 44);
        lblDelta.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblDelta.setForeground(lbColor);
        card.add(lblDelta);
    }
}
