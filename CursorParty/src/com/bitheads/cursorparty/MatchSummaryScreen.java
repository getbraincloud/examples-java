package com.bitheads.cursorparty;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
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

    private static final int CARD_W = 880;
    private static final int CARD_H = 620;
    private static final int PAD = 24;
    private static final int ROW_H = 90;

    private static final Color CARD_BG = new Color(0x1D, 0x20, 0x2C);
    private static final Color ROW_BG = new Color(0x24, 0x28, 0x33);
    private static final Color ROW_ME_BG = new Color(0x1D, 0x30, 0x25);
    private static final Color DIM = new Color(140, 150, 160);
    private static final Color GREEN = new Color(0x66, 0xEE, 0x88);
    private static final Color AMBER = new Color(0xEE, 0xAA, 0x44);
    private static final Color BLUE = new Color(0x88, 0xBB, 0xFF);

    private JLabel _lblCountdown;
    private javax.swing.Timer _tickTimer;

    // A rectangular colour-coded "chip" label — the closest Swing equivalent to the
    // mockup's rounded pill badges without introducing custom-painted rounded
    // components inconsistent with the rest of this app's flat-chip styling
    // (LobbyScreen's YOU/HOST badges use the exact same idiom).
    private static JLabel buildPill(String text, Color bg, Color fg, int x, int y)
    {
        JLabel lbl = new JLabel(text);
        lbl.setOpaque(true);
        lbl.setBackground(bg);
        lbl.setForeground(fg);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        lbl.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        Dimension pref = lbl.getPreferredSize();
        lbl.setBounds(x, y, pref.width, pref.height);
        return lbl;
    }

    private static class ColorDot extends JPanel
    {
        private final Color _color;

        ColorDot(Color color, int size)
        {
            _color = color;
            setPreferredSize(new Dimension(size, size));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(_color);
            g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
            g2.dispose();
        }
    }

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
        if (elapsed >= REMATCH_MS)
        {
            App.getInstance().onContinueFromSummary();
            return; // screen just changed to Lobby — nothing left here to tick
        }

        if (_lblCountdown != null)
        {
            long remaining = Math.max(0, REMATCH_MS - elapsed);
            _lblCountdown.setText(String.format("🕐 Next Round: %d:%02d", remaining / 1000 / 60, (remaining / 1000) % 60));
        }
        refreshUI();
    }

    void refreshUI()
    {
        panel.removeAll();
        State state = App.getInstance().state;
        JFrame frame = App.getInstance().frame;
        Dimension screenRes = frame.getPreferredSize();

        MatchResult result = state.matchResult;

        int cardX = (screenRes.width - CARD_W) / 2;
        int cardY = Math.max(20, (screenRes.height - CARD_H) / 2);

        JPanel card = new JPanel(null);
        card.setBounds(cardX, cardY, CARD_W, CARD_H);
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createLineBorder(new Color(0x33, 0x38, 0x45), 1));
        panel.add(card);

        JLabel lblTitle = new JLabel("Match Summary");
        lblTitle.setBounds(PAD, 16, CARD_W - PAD * 2, 30);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 22));
        lblTitle.setForeground(Colors.TEXT_COLOR);
        card.add(lblTitle);

        int playerCount = state.lobby != null ? state.lobby.members.size() : 0;
        String lobbyId = state.lobby != null ? state.lobby.lobbyId : "";
        JLabel lblSubtitle = new JLabel("Lobby " + lobbyId + " · " + playerCount + " Players");
        lblSubtitle.setBounds(PAD, 44, CARD_W - PAD * 2, 18);
        lblSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblSubtitle.setForeground(DIM);
        card.add(lblSubtitle);

        int y = 70;

        if (!result.valid || result.entries.isEmpty())
        {
            JLabel lblWaiting = new JLabel("Waiting for results...", SwingConstants.CENTER);
            lblWaiting.setBounds(PAD, y, CARD_W - PAD * 2, 24);
            lblWaiting.setFont(new Font("SansSerif", Font.PLAIN, 16));
            lblWaiting.setForeground(DIM);
            card.add(lblWaiting);
        }
        else
        {
            MatchResult.Entry winner = result.entries.get(0);
            JPanel banner = new JPanel(null);
            banner.setBounds(PAD, y, CARD_W - PAD * 2, 40);
            banner.setBackground(new Color(0x2A, 0x25, 0x18));
            banner.setBorder(BorderFactory.createLineBorder(new Color(0x66, 0x55, 0x33), 1));
            card.add(banner);

            JLabel lblWinner = new JLabel(String.format("🏆 %s wins the round, covering %.0f%% of the board.",
                    nameForCxId(winner.cxId), winner.coveragePct));
            lblWinner.setBounds(12, 0, CARD_W - PAD * 2 - 24, 40);
            lblWinner.setFont(new Font("SansSerif", Font.BOLD, 13));
            lblWinner.setForeground(AMBER);
            banner.add(lblWinner);
            y += 48;

            JLabel colRank = new JLabel("RANK / PLAYER");
            colRank.setBounds(PAD, y, 200, 16);
            colRank.setFont(new Font("SansSerif", Font.BOLD, 10));
            colRank.setForeground(DIM);
            card.add(colRank);

            JLabel colCoverage = new JLabel("COVERAGE", SwingConstants.CENTER);
            colCoverage.setBounds(PAD + 230, y, 90, 16);
            colCoverage.setFont(new Font("SansSerif", Font.BOLD, 10));
            colCoverage.setForeground(DIM);
            card.add(colCoverage);

            JLabel colResult = new JLabel("LEADERBOARD RESULT");
            colResult.setBounds(PAD + 340, y, 300, 16);
            colResult.setFont(new Font("SansSerif", Font.BOLD, 10));
            colResult.setForeground(DIM);
            card.add(colResult);
            y += 24;

            JPanel rowsPanel = new JPanel();
            rowsPanel.setLayout(new BoxLayout(rowsPanel, BoxLayout.Y_AXIS));
            rowsPanel.setBackground(CARD_BG);
            for (MatchResult.Entry e : result.entries)
            {
                rowsPanel.add(buildPlayerRow(e, CARD_W - PAD * 2 - 4));
            }

            JScrollPane scroll = new JScrollPane(rowsPanel,
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setBounds(PAD, y, CARD_W - PAD * 2, CARD_H - y - 90);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            scroll.getViewport().setBackground(CARD_BG);
            scroll.getVerticalScrollBar().setUnitIncrement(ROW_H / 3);
            card.add(scroll);
        }

        int bottomY = CARD_H - 62;

        _lblCountdown = new JLabel("🕐 Next Round: 0:00");
        _lblCountdown.setBounds(PAD, bottomY, 300, 18);
        _lblCountdown.setFont(new Font("SansSerif", Font.PLAIN, 12));
        _lblCountdown.setForeground(DIM);
        card.add(_lblCountdown);

        boolean isHost = state.lobby != null && state.lobby.ownerCxId.equals(state.user.cxId);
        int readyCount = 0;
        int totalCount = state.lobby != null ? state.lobby.members.size() : 0;
        if (state.lobby != null)
            for (User m : state.lobby.members) if (m.isReady) readyCount++;

        int btnY = bottomY + 24;
        int btnW = (CARD_W - PAD * 2 - 12);
        // Host: Start has no readiness gate at all, so this button never readies the
        // host up — it's just a shortcut back to the Lobby where Start is waiting.
        // Everyone else: readies up (so the host sees the count build) and returns too.
        JButton btnRematch = new JButton(isHost ? "Return to Lobby" : "Queue for Rematch  " + readyCount + "/" + totalCount);
        btnRematch.setBounds(PAD, btnY, (int) (btnW * 0.68), 34);
        btnRematch.setBackground(new Color(0x44, 0x66, 0xEE));
        btnRematch.setForeground(Color.WHITE);
        btnRematch.setFont(new Font("SansSerif", Font.BOLD, 13));
        card.add(btnRematch);
        btnRematch.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                App.getInstance().onContinueFromSummary();
            }
        });

        JButton btnMenu = new JButton("Main Menu");
        btnMenu.setBounds(PAD + (int) (btnW * 0.68) + 12, btnY, (int) (btnW * 0.32) - 12, 34);
        card.add(btnMenu);
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

    // Only the periods that actually improved are listed (e.g. just "Quarterly
    // #52->#46" if lifetime didn't move this round), joined with " · ".
    private String periodsText(MatchResult.PeriodDelta lifetime, MatchResult.PeriodDelta quarterly,
            String lifetimeLabel, String quarterlyLabel)
    {
        ArrayList<String> parts = new ArrayList<>();
        if (lifetime.improved) parts.add(lifetimeLabel + " #" + lifetime.rankBefore + "→#" + lifetime.rankAfter);
        if (quarterly.improved) parts.add(quarterlyLabel + " #" + quarterly.rankBefore + "→#" + quarterly.rankAfter);
        return String.join(" · ", parts);
    }

    private JPanel buildPlayerRow(MatchResult.Entry e, int width)
    {
        State state = App.getInstance().state;
        boolean isMe = e.cxId.equals(state.user.cxId);

        JPanel row = new JPanel(null);
        row.setPreferredSize(new Dimension(width, ROW_H));
        row.setMaximumSize(new Dimension(width, ROW_H));
        row.setBackground(isMe ? ROW_ME_BG : ROW_BG);
        row.setOpaque(true);
        row.setBorder(isMe ? BorderFactory.createLineBorder(new Color(0x3C, 0x8C, 0x53), 1)
                : BorderFactory.createEmptyBorder(1, 1, 1, 1));

        JLabel lblRank = new JLabel("#" + e.rank);
        lblRank.setBounds(12, 0, 30, ROW_H);
        lblRank.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblRank.setForeground(AMBER);
        row.add(lblRank);

        ColorDot dot = new ColorDot(Colors.COLORS[colorIndexForCxId(e.cxId) % Colors.NUM_COLORS], 14);
        dot.setBounds(46, ROW_H / 2 - 24, 14, 14);
        row.add(dot);

        JLabel lblName = new JLabel(nameForCxId(e.cxId));
        lblName.setBounds(68, ROW_H / 2 - 28, 130, 20);
        lblName.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblName.setForeground(Colors.TEXT_COLOR);
        row.add(lblName);

        if (isMe)
        {
            JLabel badge = new JLabel("YOU", SwingConstants.CENTER);
            badge.setBounds(198, ROW_H / 2 - 26, 36, 16);
            badge.setFont(new Font("SansSerif", Font.BOLD, 9));
            badge.setOpaque(true);
            badge.setBackground(new Color(0x33, 0x55, 0x88));
            badge.setForeground(Color.WHITE);
            row.add(badge);
        }

        JLabel lblPct = new JLabel(String.format("%.0f%%", e.coveragePct), SwingConstants.CENTER);
        lblPct.setBounds(230, ROW_H / 2 - 22, 90, 26);
        lblPct.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblPct.setForeground(Colors.TEXT_COLOR);
        row.add(lblPct);

        JLabel lblPctCaption = new JLabel("COVERAGE", SwingConstants.CENTER);
        lblPctCaption.setBounds(230, ROW_H / 2 + 4, 90, 14);
        lblPctCaption.setFont(new Font("SansSerif", Font.PLAIN, 9));
        lblPctCaption.setForeground(DIM);
        row.add(lblPctCaption);

        int pillX = 340;
        int pillY = 12;

        JLabel ptsPill = buildPill("⏱ +" + (e.beaten + 1) + " pts", new Color(0x24, 0x30, 0x45), BLUE, pillX, pillY);
        row.add(ptsPill);

        JLabel ptsCaption = new JLabel(e.beaten + " beaten + 1 for playing");
        ptsCaption.setBounds(pillX + ptsPill.getWidth() + 8, pillY, width - pillX - ptsPill.getWidth() - 16, 20);
        ptsCaption.setFont(new Font("SansSerif", Font.PLAIN, 11));
        ptsCaption.setForeground(DIM);
        row.add(ptsCaption);

        int lineY = pillY + 26;
        if (e.lbDelta.ready)
        {
            boolean pointsImproved = e.lbDelta.pointsLifetime.improved || e.lbDelta.pointsQuarterly.improved;
            boolean coverageImproved = e.lbDelta.coverageLifetime.improved || e.lbDelta.coverageQuarterly.improved;

            if (pointsImproved)
            {
                String txt = "↑ Rank up · Opponents Beaten " + periodsText(e.lbDelta.pointsLifetime, e.lbDelta.pointsQuarterly, "Lifetime", "Quarterly");
                row.add(buildPill(txt, new Color(0x1A, 0x33, 0x22), GREEN, pillX, lineY));
                lineY += 26;
            }
            if (coverageImproved)
            {
                String txt = "★ Personal best · Coverage % " + periodsText(e.lbDelta.coverageLifetime, e.lbDelta.coverageQuarterly, "Lifetime", "Quarterly");
                row.add(buildPill(txt, new Color(0x33, 0x2C, 0x18), AMBER, pillX, lineY));
                lineY += 26;
            }
            if (!pointsImproved && !coverageImproved)
            {
                row.add(buildPill("No leaderboard rank change this round", new Color(0x28, 0x2B, 0x35), DIM, pillX, lineY));
            }
        }
        else
        {
            long elapsedSinceArrival = System.currentTimeMillis() - state.matchSummaryArrivalTime;
            String txt = elapsedSinceArrival >= LEADERBOARD_TIMEOUT_MS ? "Leaderboard unavailable" : "Updating leaderboards...";
            row.add(buildPill(txt, new Color(0x28, 0x2B, 0x35), DIM, pillX, lineY));
        }

        return row;
    }
}
