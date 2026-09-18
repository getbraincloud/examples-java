package com.bitheads.cursorparty;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;

class LobbyScreen extends Screen
{
    private static final int COLS    = 10;
    private static final int ROWS    = Colors.NUM_COLORS / COLS; // 4
    private static final int BTN_W   = 40;
    private static final int BTN_H   = 28;

    private static final int MEMBERS_X = 20;
    private static final int MEMBERS_Y = 120;
    private static final int MEMBER_ROW_H = 50;
    private static final int AVATAR_SIZE  = 36;

    private static final String CARD_CHAT = "chat";
    private static final String CARD_LEADERBOARD = "leaderboard";
    private static final String CARD_INFO = "info";
    private static final String CHAT_SUBCARD_LOBBY = "lobby";
    private static final String CHAT_SUBCARD_GLOBAL = "global";

    private JLabel _lblStatus;
    private JLabel _lblStatusTimer;
    private JLabel _lblInfoTimeInLobby;
    private javax.swing.Timer _statusTimer;

    // Created once and re-attached (not rebuilt) across refreshUI() calls — see
    // ChatPanel's own comment on why its widgets must never be recreated.
    private ChatPanel _lobbyChatPanel;
    private ChatPanel _globalChatPanel;
    private LeaderboardPanel _leaderboardPanel;

    // A small circular colour swatch. On the local player's row it's clickable and
    // opens a colour-picker popup — mirrors the cpp/react/Godot reference clients,
    // which all moved from an always-visible swatch grid to a popup anchored on
    // your own row (see App.onColorChanged for the actual persist call, unchanged).
    private static class ColorSwatch extends JPanel
    {
        private Color _color;

        ColorSwatch(Color color, int size)
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
            g2.setColor(new Color(255, 255, 255, 60));
            g2.drawOval(0, 0, getWidth() - 1, getHeight() - 1);
            g2.dispose();
        }
    }

    public LobbyScreen()
    {
        panel = new JPanel();
        panel.setLayout(null);
        refreshUI();

        // Swing timer updates the elapsed-time counters every second while this screen is up
        _statusTimer = new javax.swing.Timer(1000, null);
        _statusTimer.addActionListener(e -> {
            if (App.getInstance().state.screen != LobbyScreen.this) {
                _statusTimer.stop();
                return;
            }
            updateStatusTimer();
            updateInfoTimeInLobby();
        });
        _statusTimer.start();
    }

    @Override
    public void onStateChanged(State state)
    {
        refreshUI();
        if (_lobbyChatPanel != null) _lobbyChatPanel.refresh();
        if (_globalChatPanel != null) _globalChatPanel.refresh();
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

    private void updateInfoTimeInLobby()
    {
        State state = App.getInstance().state;
        if (_lblInfoTimeInLobby == null || state.lobbyJoinedAtMs == 0) return;
        long elapsedSec = Math.max(0, (System.currentTimeMillis() - state.lobbyJoinedAtMs) / 1000);
        _lblInfoTimeInLobby.setText(String.format("Time in lobby: %02d:%02d", elapsedSec / 60, elapsedSec % 60));
    }

    // Builds the popup shown when the local player clicks their own colour swatch —
    // same 40-colour palette the old always-visible grid used, just relocated.
    private JPopupMenu buildColorPickerPopup()
    {
        JPopupMenu popup = new JPopupMenu();
        JPanel grid = new JPanel(new GridLayout(ROWS, COLS, 2, 2));
        grid.setBackground(Colors.BG_COLOR);
        grid.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        int currentColor = App.getInstance().state.user.colorIndex;
        for (int i = 0; i < Colors.NUM_COLORS; i++)
        {
            final int idx = i;
            JButton btn = new JButton();
            btn.setPreferredSize(new Dimension(BTN_W, BTN_H));
            btn.setBackground(Colors.COLORS[i]);
            btn.setOpaque(true);
            btn.setBorderPainted(true);
            btn.setBorder(idx == currentColor
                    ? BorderFactory.createLineBorder(Color.WHITE, 2)
                    : BorderFactory.createLineBorder(new Color(60, 63, 65), 1));
            btn.addActionListener(e -> {
                App.getInstance().onColorChanged(idx);
                popup.setVisible(false);
            });
            grid.add(btn);
        }
        popup.add(grid);
        return popup;
    }

    private JPanel buildMemberRow(User member, int width, boolean isMe, boolean isHostMember)
    {
        JPanel row = new JPanel(null);
        row.setBounds(0, 0, width, MEMBER_ROW_H);
        row.setOpaque(false);

        int colorIndex = isMe ? App.getInstance().state.user.colorIndex : member.colorIndex;
        ColorSwatch swatch = new ColorSwatch(Colors.COLORS[colorIndex % Colors.NUM_COLORS], AVATAR_SIZE);
        swatch.setBounds(0, 7, AVATAR_SIZE, AVATAR_SIZE);
        row.add(swatch);

        if (isMe)
        {
            swatch.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            swatch.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseClicked(MouseEvent e)
                {
                    buildColorPickerPopup().show(swatch, 0, swatch.getHeight());
                }
            });
        }

        JLabel lblName = new JLabel(member.name);
        lblName.setBounds(AVATAR_SIZE + 12, 2, 170, 20);
        lblName.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblName.setForeground(Colors.TEXT_COLOR);
        row.add(lblName);

        int badgeX = AVATAR_SIZE + 12 + 175;
        if (isMe)
        {
            JLabel badge = new JLabel("YOU", SwingConstants.CENTER);
            badge.setBounds(badgeX, 4, 36, 16);
            badge.setFont(new Font("SansSerif", Font.BOLD, 9));
            badge.setOpaque(true);
            badge.setBackground(new Color(0x33, 0x55, 0x88));
            badge.setForeground(Color.WHITE);
            row.add(badge);
            badgeX += 40;
        }
        if (isHostMember)
        {
            JLabel badge = new JLabel("HOST", SwingConstants.CENTER);
            badge.setBounds(badgeX, 4, 40, 16);
            badge.setFont(new Font("SansSerif", Font.BOLD, 9));
            badge.setOpaque(true);
            badge.setBackground(new Color(0x88, 0x55, 0x22));
            badge.setForeground(Color.WHITE);
            row.add(badge);
        }

        JLabel lblStatus = new JLabel(member.isReady ? "Ready" : "Not ready");
        lblStatus.setBounds(AVATAR_SIZE + 12, 25, width - AVATAR_SIZE - 12, 16);
        lblStatus.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblStatus.setForeground(member.isReady ? new Color(0x66, 0xEE, 0x88) : new Color(140, 150, 160));
        row.add(lblStatus);

        return row;
    }

    void refreshUI()
    {
        panel.removeAll();
        State state = App.getInstance().state;

        JFrame frame = App.getInstance().frame;
        Dimension screenRes = frame.getPreferredSize();

        panel.setBackground(Colors.BG_COLOR);

        int sidePanelW = 300;
        int sidePanelX = screenRes.width - sidePanelW - 20;
        int membersColW = sidePanelX - MEMBERS_X - 20;

        // Title
        {
            JLabel lblTitle = new JLabel("Lobby", SwingConstants.CENTER);
            lblTitle.setSize(screenRes.width, 40);
            lblTitle.setLocation(0, 20);
            lblTitle.setFont(new Font(lblTitle.getFont().getName(), Font.PLAIN, 32));
            lblTitle.setForeground(Colors.TEXT_COLOR);
            panel.add(lblTitle);
        }

        // ── Members list (left column) ────────────────────────────────────────
        int contentBottom;
        {
            JLabel lblMembers = new JLabel("Members");
            lblMembers.setBounds(MEMBERS_X, 90, membersColW, 24);
            lblMembers.setFont(new Font("SansSerif", Font.BOLD, 16));
            lblMembers.setForeground(Colors.TEXT_COLOR);
            panel.add(lblMembers);

            int rowY = MEMBERS_Y;
            for (User member : state.lobby.members)
            {
                boolean isMe = member.cxId.equals(state.user.cxId);
                boolean isHostMember = member.cxId.equals(state.lobby.ownerCxId);
                JPanel row = buildMemberRow(member, membersColW, isMe, isHostMember);
                row.setBounds(MEMBERS_X, rowY, membersColW, MEMBER_ROW_H);
                panel.add(row);
                rowY += MEMBER_ROW_H;
            }
            contentBottom = rowY + 8;
        }

        // Ping data table — only shown when usePingData is enabled; confined to the
        // members column so it doesn't run under the chat/leaderboard/info panel.
        if (state.usePingData)
        {
            int rowY = contentBottom;

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
                lblRegion.setBounds(MEMBERS_X, rowY, membersColW, 16);
                lblRegion.setFont(new Font("Monospaced", Font.PLAIN, 11));
                lblRegion.setForeground(isGood ? new Color(0x44, 0xEE, 0x44) : new Color(0xEE, 0x44, 0x44));
                panel.add(lblRegion);
                rowY += 18;
            }

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
                lblTitle.setBounds(MEMBERS_X, rowY, membersColW, rowH);
                lblTitle.setFont(new Font("SansSerif", Font.BOLD, 11));
                lblTitle.setForeground(dimColor);
                panel.add(lblTitle);
                rowY += rowH + 2;

                StringBuilder header = new StringBuilder(String.format("%-18s", ""));
                for (String r : regions) header.append(String.format("  %-14s", r));
                JLabel lblHeader = new JLabel(header.toString(), SwingConstants.CENTER);
                lblHeader.setBounds(MEMBERS_X, rowY, membersColW, rowH);
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
                    lbl.setBounds(MEMBERS_X, rowY, membersColW, rowH);
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

            int bannerY = Math.max(contentBottom + 8, btnY - 8 - bannerHeight);

            JPanel statusBanner = new JPanel(null);
            statusBanner.setBounds(MEMBERS_X, bannerY, membersColW, bannerHeight);
            statusBanner.setBackground(new Color(0x0F, 0x24, 0x61));
            panel.add(statusBanner);

            _lblStatus = new JLabel(state.lobbyStatusText, SwingConstants.CENTER);
            _lblStatus.setBounds(0, 4, membersColW, 22);
            _lblStatus.setFont(new Font("SansSerif", Font.BOLD, 14));
            _lblStatus.setForeground(Color.WHITE);
            statusBanner.add(_lblStatus);

            if (hasSub)
            {
                JLabel lblSub = new JLabel(state.lobbySubStatus, SwingConstants.CENTER);
                lblSub.setBounds(0, 28, membersColW, 18);
                lblSub.setFont(new Font("SansSerif", Font.PLAIN, 12));
                lblSub.setForeground(new Color(170, 185, 210));
                statusBanner.add(lblSub);
            }

            _lblStatusTimer = new JLabel("0:00", SwingConstants.CENTER);
            _lblStatusTimer.setBounds(0, hasSub ? 46 : 26, membersColW, 16);
            _lblStatusTimer.setFont(new Font("Monospaced", Font.PLAIN, 12));
            _lblStatusTimer.setForeground(new Color(190, 200, 220));
            statusBanner.add(_lblStatusTimer);

            updateStatusTimer();
        }

        // Buttons (btnY computed above so the status banner can sit clear of them)
        int membersCx = MEMBERS_X + membersColW / 2;
        JButton btnLeave = new JButton("Leave");
        btnLeave.setSize(160, 30);
        btnLeave.setLocation(membersCx - 170, btnY);
        panel.add(btnLeave);
        btnLeave.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                App.getInstance().onGameScreenClose();
            }
        });

        if (state.lobby.ownerCxId.equals(state.user.cxId))
        {
            // Host: always available — an early-start option, not gated on everyone
            // (or even the host's own) readiness, matching cpp's Start button exactly.
            JButton btnStart = new JButton("Start");
            btnStart.setSize(160, 30);
            btnStart.setLocation(membersCx + 10, btnY);
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
        else
        {
            // Non-host: a Ready Up / Not Ready toggle — flips only the local player's
            // own state (App.onToggleReady), never starts the round itself.
            JButton btnReady = new JButton(state.user.isReady ? "Not Ready" : "Ready Up");
            btnReady.setSize(160, 30);
            btnReady.setLocation(membersCx + 10, btnY);
            panel.add(btnReady);
            btnReady.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    App.getInstance().onToggleReady();
                }
            });
        }

        // ── Chat / Leaderboards / Info side panel (right column) ────────────────
        {
            int panelW = sidePanelW;
            int panelX = sidePanelX;
            int panelY = 90;
            int panelH = screenRes.height - panelY - 96;

            int tabW = panelW / 3;
            JButton btnChatTab = new JButton("CHAT");
            btnChatTab.setBounds(panelX, panelY, tabW - 2, 26);
            panel.add(btnChatTab);

            JButton btnLeaderboardTab = new JButton("LEADERBOARDS");
            btnLeaderboardTab.setBounds(panelX + tabW, panelY, tabW - 2, 26);
            panel.add(btnLeaderboardTab);

            JButton btnInfoTab = new JButton("INFO");
            btnInfoTab.setBounds(panelX + tabW * 2, panelY, tabW - 2, 26);
            panel.add(btnInfoTab);

            JPanel tabCards = new JPanel(new CardLayout());
            tabCards.setBounds(panelX, panelY + 30, panelW, panelH - 30);
            panel.add(tabCards);

            JPanel chatTab = new JPanel(new java.awt.BorderLayout());
            chatTab.setBackground(Colors.BG_COLOR);

            JPanel chatSubTabRow = new JPanel(null);
            chatSubTabRow.setPreferredSize(new Dimension(panelW, 22));
            chatSubTabRow.setBackground(Colors.BG_COLOR);
            JButton btnThisLobby = new JButton("THIS LOBBY");
            btnThisLobby.setBounds(0, 0, panelW / 2 - 2, 22);
            chatSubTabRow.add(btnThisLobby);
            JButton btnGlobal = new JButton("GLOBAL");
            btnGlobal.setBounds(panelW / 2 + 2, 0, panelW / 2 - 2, 22);
            chatSubTabRow.add(btnGlobal);
            chatTab.add(chatSubTabRow, java.awt.BorderLayout.NORTH);

            JPanel chatSubCards = new JPanel(new CardLayout());
            chatTab.add(chatSubCards, java.awt.BorderLayout.CENTER);

            if (_lobbyChatPanel == null) {
                _lobbyChatPanel = new ChatPanel(new ChatPanel.Source() {
                    @Override
                    public ArrayList<ChatMessage> getMessages() {
                        Lobby lobby = App.getInstance().state.lobby;
                        return lobby != null ? lobby.chatMessages : new ArrayList<>();
                    }

                    @Override
                    public void sendMessage(String text) {
                        App.getInstance().sendLobbySignalChat(text);
                    }
                }, panelW, panelH - 30 - 22);
            }
            if (_globalChatPanel == null) {
                _globalChatPanel = new ChatPanel(new ChatPanel.Source() {
                    @Override
                    public ArrayList<ChatMessage> getMessages() {
                        return App.getInstance().state.chatMessages;
                    }

                    @Override
                    public void sendMessage(String text) {
                        App.getInstance().sendGlobalChatMessage(text);
                    }
                }, panelW, panelH - 30 - 22);
            }
            chatSubCards.add(_lobbyChatPanel, CHAT_SUBCARD_LOBBY);
            chatSubCards.add(_globalChatPanel, CHAT_SUBCARD_GLOBAL);
            CardLayout chatSubLayout = (CardLayout) chatSubCards.getLayout();
            btnThisLobby.addActionListener(e -> chatSubLayout.show(chatSubCards, CHAT_SUBCARD_LOBBY));
            btnGlobal.addActionListener(e -> chatSubLayout.show(chatSubCards, CHAT_SUBCARD_GLOBAL));

            if (_leaderboardPanel == null) {
                _leaderboardPanel = new LeaderboardPanel(panelW, panelH - 30);
            }

            // Info tab: lobby id / region / players / time in lobby / status
            JPanel infoTab = new JPanel(null);
            infoTab.setBackground(Colors.BG_COLOR);
            {
                String lobbyIdStr = state.lobby != null ? state.lobby.lobbyId : "";
                int colonIdx = lobbyIdStr.indexOf(':');
                String region = colonIdx > 0 ? lobbyIdStr.substring(0, colonIdx) : "-";

                int y = 8;
                Font infoFont = new Font("Monospaced", Font.PLAIN, 12);

                JLabel lblLobbyId = new JLabel("Lobby: " + lobbyIdStr);
                lblLobbyId.setBounds(8, y, panelW - 16, 18);
                lblLobbyId.setFont(infoFont);
                lblLobbyId.setForeground(Colors.TEXT_COLOR);
                infoTab.add(lblLobbyId);
                y += 22;

                JLabel lblRegion = new JLabel("Region: " + region);
                lblRegion.setBounds(8, y, panelW - 16, 18);
                lblRegion.setFont(infoFont);
                lblRegion.setForeground(Colors.TEXT_COLOR);
                infoTab.add(lblRegion);
                y += 22;

                JLabel lblPlayers = new JLabel("Players: " + state.lobby.members.size());
                lblPlayers.setBounds(8, y, panelW - 16, 18);
                lblPlayers.setFont(infoFont);
                lblPlayers.setForeground(Colors.TEXT_COLOR);
                infoTab.add(lblPlayers);
                y += 22;

                _lblInfoTimeInLobby = new JLabel("Time in lobby: 00:00");
                _lblInfoTimeInLobby.setBounds(8, y, panelW - 16, 18);
                _lblInfoTimeInLobby.setFont(infoFont);
                _lblInfoTimeInLobby.setForeground(Colors.TEXT_COLOR);
                infoTab.add(_lblInfoTimeInLobby);
                y += 26;

                boolean isHost = state.lobby.ownerCxId.equals(state.user.cxId);
                String statusText = !state.lobbyStatusText.isEmpty() ? state.lobbyStatusText
                        : (isHost && !state.user.isReady ? "Press Start when ready." : "Waiting for host to start...");
                JLabel lblInfoStatus = new JLabel(statusText);
                lblInfoStatus.setBounds(8, y, panelW - 16, 18);
                lblInfoStatus.setFont(infoFont);
                lblInfoStatus.setForeground(new Color(0x66, 0xEE, 0x88));
                infoTab.add(lblInfoStatus);

                updateInfoTimeInLobby();
            }

            tabCards.add(chatTab, CARD_CHAT);
            tabCards.add(_leaderboardPanel, CARD_LEADERBOARD);
            tabCards.add(infoTab, CARD_INFO);
            CardLayout tabLayout = (CardLayout) tabCards.getLayout();
            btnChatTab.addActionListener(e -> tabLayout.show(tabCards, CARD_CHAT));
            btnLeaderboardTab.addActionListener(e -> tabLayout.show(tabCards, CARD_LEADERBOARD));
            btnInfoTab.addActionListener(e -> tabLayout.show(tabCards, CARD_INFO));
        }
    }
}
