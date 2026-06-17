package com.bitheads.cursorparty;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

class LobbyScreen extends Screen
{
    private static final int COLS    = 10;
    private static final int ROWS    = Colors.NUM_COLORS / COLS; // 4
    private static final int BTN_W   = 40;
    private static final int BTN_H   = 28;
    private static final int BTN_GAP = 2;

    private JLabel _lblStatus;
    private JLabel _lblStatusTimer;
    private javax.swing.Timer _statusTimer;

    public LobbyScreen()
    {
        panel = new JPanel();
        panel.setLayout(null);
        refreshUI();

        // Swing timer updates the elapsed-time counter every second while status is active
        _statusTimer = new javax.swing.Timer(1000, null);
        _statusTimer.addActionListener(e -> {
            if (App.getInstance().state.screen != LobbyScreen.this) {
                _statusTimer.stop();
                return;
            }
            updateStatusTimer();
        });
        _statusTimer.start();
    }

    @Override
    public void onStateChanged(State state)
    {
        refreshUI();
    }

    private void updateStatusTimer()
    {
        State state = App.getInstance().state;
        if (_lblStatusTimer == null || state.lobbyStatusText.isEmpty()) return;
        long elapsedSec = Math.max(0, (System.currentTimeMillis() - state.lobbyStatusStartTime) / 1000);
        long min = elapsedSec / 60;
        long sec = elapsedSec % 60;
        _lblStatusTimer.setText(String.format("%d:%02d", min, sec));
    }

    void refreshUI()
    {
        panel.removeAll();
        State state = App.getInstance().state;

        JFrame frame = App.getInstance().frame;
        Dimension screenRes = frame.getPreferredSize();
        int cx = screenRes.width / 2;

        panel.setBackground(Colors.BG_COLOR);

        // Title
        {
            JLabel lblTitle = new JLabel("Lobby", SwingConstants.CENTER);
            lblTitle.setSize(screenRes.width, 40);
            lblTitle.setLocation(0, 40);
            lblTitle.setFont(new Font(lblTitle.getFont().getName(), Font.PLAIN, 32));
            lblTitle.setForeground(Colors.TEXT_COLOR);
            panel.add(lblTitle);

            // Lobby ID — persistent reference shown below the title
            String lobbyId = state.lobby != null ? state.lobby.lobbyId : "";
            JLabel lblLobbyId = new JLabel(lobbyId, SwingConstants.CENTER);
            lblLobbyId.setSize(screenRes.width, 20);
            lblLobbyId.setLocation(0, 84);
            lblLobbyId.setFont(new Font("Monospaced", Font.PLAIN, 11));
            lblLobbyId.setForeground(new Color(130, 140, 160));
            panel.add(lblLobbyId);
        }

        // Colour picker — 4 rows × 10 = 40 colours
        {
            int paletteW = COLS * (BTN_W + BTN_GAP) - BTN_GAP;
            int paletteX = cx - paletteW / 2;
            int paletteY = 100;

            for (int row = 0; row < ROWS; row++)
            {
                for (int col = 0; col < COLS; col++)
                {
                    int colorIndex = row * COLS + col;
                    int bx = paletteX + col * (BTN_W + BTN_GAP);
                    int by = paletteY + row * (BTN_H + BTN_GAP);

                    JButton btn = new JButton();
                    btn.setSize(BTN_W, BTN_H);
                    btn.setLocation(bx, by);
                    btn.setBackground(Colors.COLORS[colorIndex]);
                    btn.setOpaque(true);
                    btn.setBorderPainted(true);

                    if (colorIndex == state.user.colorIndex)
                        btn.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
                    else
                        btn.setBorder(BorderFactory.createLineBorder(new Color(60, 63, 65), 1));

                    panel.add(btn);
                    final int idx = colorIndex;
                    btn.addActionListener(new ActionListener()
                    {
                        @Override
                        public void actionPerformed(ActionEvent e)
                        {
                            App.getInstance().onColorChanged(idx);
                        }
                    });
                }
            }
        }

        // Members list
        {
            int membersY = 100 + ROWS * (BTN_H + BTN_GAP) + 20;
            Font memberFont = new Font("SansSerif", Font.PLAIN, 18);

            for (int i = 0; i < state.lobby.members.size(); ++i)
            {
                User member = state.lobby.members.get(i);
                int colorIdx = member.cxId.equals(state.user.cxId)
                        ? state.user.colorIndex : member.colorIndex;

                JLabel lbl = new JLabel(member.name, SwingConstants.CENTER);
                lbl.setSize(280, 26);
                lbl.setLocation(cx - 140, membersY + i * 28);
                lbl.setFont(memberFont);
                lbl.setForeground(Colors.COLORS[colorIdx % Colors.NUM_COLORS]);
                panel.add(lbl);
            }
        }

        // contentBottom tracks the lowest occupied Y so the status banner never overlaps
        int contentBottom = 100 + ROWS * (BTN_H + BTN_GAP) + 20
                          + state.lobby.members.size() * 28 + 16;

        // Ping data table — only shown when usePingData is enabled
        if (state.usePingData)
        {
            int rowY = contentBottom;

            // Region quality label
            String lobbyIdStr = state.lobby != null ? state.lobby.lobbyId : "";
            int colonIdx = lobbyIdStr.indexOf(':');
            String lobbyRegion = (colonIdx > 0 && !lobbyIdStr.substring(0, colonIdx).matches("\\d+"))
                ? lobbyIdStr.substring(0, colonIdx) : "";
            if (!lobbyRegion.isEmpty() && !state.pingData.isEmpty())
            {
                int bestPing = state.pingData.values().stream().mapToInt(Integer::intValue).min().orElse(0);
                Integer lobbyPing = state.pingData.get(lobbyRegion);
                boolean isGood = lobbyPing != null && (lobbyPing - bestPing) <= 30;
                JLabel lblRegion = new JLabel("Region: " + lobbyRegion, SwingConstants.CENTER);
                lblRegion.setSize(screenRes.width, 16);
                lblRegion.setLocation(0, rowY);
                lblRegion.setFont(new Font("Monospaced", Font.PLAIN, 11));
                lblRegion.setForeground(isGood ? new Color(0x44, 0xEE, 0x44) : new Color(0xEE, 0x44, 0x44));
                panel.add(lblRegion);
                rowY += 18;
            }

            // Collect all unique region names from our pingData and members' pings
            TreeSet<String> regionSet = new TreeSet<>(state.pingData.keySet());
            for (User member : state.lobby.members)
                regionSet.addAll(member.pings.keySet());

            if (!regionSet.isEmpty())
            {
                List<String> regions = new ArrayList<>(regionSet);
                Font monoFont = new Font("Monospaced", Font.PLAIN, 11);
                Color dimColor = new Color(140, 150, 160);
                int rowH = 16;

                JLabel lblTitle = new JLabel("Ping Data (ms)", SwingConstants.CENTER);
                lblTitle.setSize(screenRes.width, rowH);
                lblTitle.setLocation(0, rowY);
                lblTitle.setFont(new Font("SansSerif", Font.BOLD, 11));
                lblTitle.setForeground(dimColor);
                panel.add(lblTitle);
                rowY += rowH + 2;

                // Header row
                StringBuilder header = new StringBuilder(String.format("%-18s", ""));
                for (String r : regions) header.append(String.format("  %-14s", r));
                JLabel lblHeader = new JLabel(header.toString(), SwingConstants.CENTER);
                lblHeader.setSize(screenRes.width, rowH);
                lblHeader.setLocation(0, rowY);
                lblHeader.setFont(monoFont);
                lblHeader.setForeground(dimColor);
                panel.add(lblHeader);
                rowY += rowH;

                for (User member : state.lobby.members)
                {
                    HashMap<String, Integer> pings = member.pings.isEmpty()
                        && member.cxId.equals(state.user.cxId) && !state.pingData.isEmpty()
                        ? state.pingData : member.pings;
                    if (pings.isEmpty()) continue;

                    String nameCol = member.name;
                    if (member.cxId.equals(state.lobby.ownerCxId)) nameCol += " [H]";
                    StringBuilder row = new StringBuilder(String.format("%-18s", nameCol));
                    for (String r : regions)
                    {
                        Integer ms = pings.get(r);
                        if (ms == null) row.append(String.format("  %-14s", "-"));
                        else if (ms >= 999) row.append(String.format("  %-14s", "T/O"));
                        else row.append(String.format("  %-14d", ms));
                    }

                    JLabel lbl = new JLabel(row.toString(), SwingConstants.CENTER);
                    lbl.setSize(screenRes.width, rowH);
                    lbl.setLocation(0, rowY);
                    lbl.setFont(monoFont);
                    int colorIdx = member.cxId.equals(state.user.cxId)
                        ? state.user.colorIndex : member.colorIndex;
                    lbl.setForeground(member.cxId.equals(state.user.cxId)
                        ? Colors.COLORS[colorIdx % Colors.NUM_COLORS] : dimColor);
                    panel.add(lbl);
                    rowY += rowH;
                }
            }

            contentBottom = rowY + 8;
        }

        // Status banner — shown while STARTING / ROOM_READY provisioning is in progress.
        _lblStatus = null;
        _lblStatusTimer = null;

        // The button row is pinned to the bottom; compute it first so the banner can
        // be placed ABOVE it (with a gap) and never overlap the Leave/Start buttons.
        int btnY = screenRes.height - 80;

        if (!state.lobbyStatusText.isEmpty())
        {
            boolean hasSub = !state.lobbySubStatus.isEmpty();
            int bannerHeight = hasSub ? 64 : 44;

            // Below all content, but always at least an 8px gap above the buttons.
            int bannerY = Math.max(contentBottom + 8, btnY - 8 - bannerHeight);

            JPanel statusBanner = new JPanel(null);
            statusBanner.setSize(screenRes.width, bannerHeight);
            statusBanner.setLocation(0, bannerY);
            statusBanner.setBackground(new Color(0x0F, 0x24, 0x61));
            panel.add(statusBanner);

            _lblStatus = new JLabel(state.lobbyStatusText, SwingConstants.CENTER);
            _lblStatus.setSize(screenRes.width, 22);
            _lblStatus.setLocation(0, 4);
            _lblStatus.setFont(new Font("SansSerif", Font.BOLD, 14));
            _lblStatus.setForeground(Color.WHITE);
            statusBanner.add(_lblStatus);

            if (hasSub)
            {
                JLabel lblSub = new JLabel(state.lobbySubStatus, SwingConstants.CENTER);
                lblSub.setSize(screenRes.width, 18);
                lblSub.setLocation(0, 28);
                lblSub.setFont(new Font("SansSerif", Font.PLAIN, 12));
                lblSub.setForeground(new Color(170, 185, 210));
                statusBanner.add(lblSub);
            }

            _lblStatusTimer = new JLabel("0:00", SwingConstants.CENTER);
            _lblStatusTimer.setSize(screenRes.width, 16);
            _lblStatusTimer.setLocation(0, hasSub ? 46 : 26);
            _lblStatusTimer.setFont(new Font("Monospaced", Font.PLAIN, 12));
            _lblStatusTimer.setForeground(new Color(190, 200, 220));
            statusBanner.add(_lblStatusTimer);

            updateStatusTimer();
        }

        // Buttons (btnY computed above so the status banner can sit clear of them)
        JButton btnLeave = new JButton("Leave");
        btnLeave.setSize(160, 30);
        btnLeave.setLocation(cx - 200, btnY);
        panel.add(btnLeave);
        btnLeave.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                App.getInstance().onGameScreenClose();
            }
        });

        if (state.lobby.ownerCxId.equals(state.user.cxId) && !state.user.isReady)
        {
            JButton btnStart = new JButton("Start");
            btnStart.setSize(160, 30);
            btnStart.setLocation(cx + 40, btnY);
            panel.add(btnStart);
            btnStart.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    App.getInstance().onGameStart();
                }
            });
        }
    }
}
