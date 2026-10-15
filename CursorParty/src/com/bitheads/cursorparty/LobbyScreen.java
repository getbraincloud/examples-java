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

    private static final int MEMBER_ROW_H = 54;
    private static final int AVATAR_SIZE  = 18;

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
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
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

    private boolean _autoStarted = false;

    @Override
    public void onStateChanged(State state)
    {
        refreshUI();
        // BC_AUTOSTART=1 (unattended multi-instance runs): the host starts once someone else is in.
        if (App.autoStart && !_autoStarted && state.lobby != null && state.user != null
                && state.lobby.ownerCxId.equals(state.user.cxId) && state.lobby.members.size() >= 2) {
            _autoStarted = true;
            javax.swing.SwingUtilities.invokeLater(() -> App.getInstance().onGameStart());
        }
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
        JPanel grid = new JPanel(new GridLayout(ROWS, COLS, 4, 4));
        grid.setBackground(Theme.WINDOW);
        grid.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        int currentColor = App.getInstance().state.user.colorIndex;
        for (int i = 0; i < Colors.NUM_COLORS; i++)
        {
            final int idx = i;
            JButton btn = new Theme.FlatButton("", Colors.COLORS[i], null);
            btn.setPreferredSize(new Dimension(BTN_W, BTN_H));
            if (idx == currentColor)
            {
                btn.setBorderPainted(true);
                btn.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
            }
            btn.addActionListener(e -> {
                App.getInstance().onColorChanged(idx);
                popup.setVisible(false);
            });
            grid.add(btn);
        }
        popup.add(grid);
        return popup;
    }

    private static <T extends java.awt.Component> T at(T c, int x, int y, int w, int h)
    {
        c.setBounds(x, y, w, h);
        return c;
    }

    private static String fmtPing(Integer ms)
    {
        if (ms == null || ms < 0) return "-";
        return ms >= 999 ? "T/O" : ms + "ms";
    }

    // One cpp member row: swatch, name, HOST/YOU badges, worldwide rank right, ready state below.
    private JPanel buildMemberRow(User member, int width, boolean isMe, boolean isHostMember, List<String> regions)
    {
        State state = App.getInstance().state;
        boolean showPings = !regions.isEmpty();
        JPanel row = new JPanel(null);
        row.setOpaque(false);
        row.setSize(width, showPings ? MEMBER_ROW_H + 16 : MEMBER_ROW_H);

        int colorIndex = isMe ? state.user.colorIndex : member.colorIndex;
        ColorSwatch swatch = new ColorSwatch(Colors.COLORS[colorIndex % Colors.NUM_COLORS], AVATAR_SIZE);
        row.add(at(swatch, 8, 8, AVATAR_SIZE, AVATAR_SIZE));
        if (isMe)
        {
            swatch.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            swatch.setToolTipText("Change colour");
            swatch.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseClicked(MouseEvent e)
                {
                    buildColorPickerPopup().show(swatch, 0, swatch.getHeight());
                }
            });
        }

        JLabel lblName = Theme.label(member.name, Theme.font(13), isMe ? Theme.GOOD : Theme.TEXT);
        int nameW = lblName.getPreferredSize().width;
        row.add(at(lblName, 36, 8, nameW, 18));
        int badgeX = 36 + nameW + 8;
        if (isHostMember)
        {
            JLabel badge = Theme.badge("HOST", new Color(0x5A, 0x48, 0x10), Theme.WARN);
            row.add(at(badge, badgeX, 8, badge.getWidth(), 18));
            badgeX += badge.getWidth() + 6;
        }
        if (isMe)
        {
            JLabel badge = Theme.badge("YOU", Theme.FRAME_HOVER, Theme.TEXT);
            row.add(at(badge, badgeX, 8, badge.getWidth(), 18));
        }

        String rankStr = member.worldwideRank >= 0 ? "#" + member.worldwideRank : "Unranked";
        JLabel lblRank = Theme.label(rankStr, Theme.font(12), member.worldwideRank >= 0 ? Theme.rankColor(member.worldwideRank) : Theme.DIM);
        lblRank.setHorizontalAlignment(SwingConstants.RIGHT);
        row.add(at(lblRank, width - 120, 8, 112, 18));

        boolean ready = isMe ? state.user.isReady : member.isReady;
        row.add(at(Theme.label(ready ? "Ready" : "Not ready", Theme.font(12), Theme.DIM), 8, 32, 200, 16));

        if (showPings)
        {
            // Per-member region pings from lobby extra; own row falls back to the local capture.
            HashMap<String, Integer> pings = member.pings.isEmpty() && isMe ? state.pingData : member.pings;
            StringBuilder sb = new StringBuilder();
            for (String r : regions) sb.append(r).append(' ').append(fmtPing(pings.get(r))).append("   ");
            row.add(at(Theme.label(sb.toString().trim(), new Font("Monospaced", Font.PLAIN, 11), Theme.DIM), 8, 50, width - 16, 14));
        }

        row.add(Theme.separator(0, row.getHeight() - 1, width));
        return row;
    }

    void refreshUI()
    {
        panel.removeAll();
        State state = App.getInstance().state;
        Dimension screenRes = App.getInstance().contentSize();
        panel.setBackground(Theme.BG);

        int top = 100;
        int boxH = screenRes.height - top - 90;
        int leftX = 24, leftW = 440;
        int rightX = leftX + leftW + 32, rightW = screenRes.width - rightX - 24;
        boolean isHost = state.lobby.ownerCxId.equals(state.user.cxId);

        // ── Status toast (top centre) while the round is starting ─────────────
        _lblStatus = null;
        _lblStatusTimer = null;
        if (!state.lobbyStatusText.isEmpty())
        {
            String text = state.lobbyStatusText + (state.lobbySubStatus.isEmpty() ? "" : " " + state.lobbySubStatus);
            _lblStatus = Theme.label(text, Theme.font(12), Theme.WARN);
            _lblStatusTimer = Theme.label("0:00", Theme.font(12), Theme.DIM);
            int tw = _lblStatus.getPreferredSize().width + 70;
            Theme.Box toast = Theme.box();
            toast.setBounds((screenRes.width - tw) / 2, 14, tw, 34);
            toast.add(at(_lblStatus, 16, 8, tw - 70, 18));
            toast.add(at(_lblStatusTimer, tw - 48, 8, 40, 18));
            panel.add(toast);
            updateStatusTimer();
        }

        // ── Members window (left) ─────────────────────────────────────────────
        {
            Theme.Box box = Theme.box();
            box.setBounds(leftX, top, leftW, boxH);
            panel.add(box);
            box.add(at(Theme.label("Lobby", Theme.font(12), Theme.TEXT), 6, 4, 200, 18));
            box.add(at(Theme.label("Lobby  " + state.lobby.lobbyId, Theme.font(12), Theme.TEXT), 14, 32, leftW - 28, 18));
            box.add(Theme.separator(8, 56, leftW - 16));
            box.add(at(Theme.label("Members", Theme.font(11), Theme.DIM), 14, 64, 200, 16));
            JLabel lblRankHdr = Theme.label("WORLDWIDE RANK", Theme.font(11), Theme.DIM);
            lblRankHdr.setHorizontalAlignment(SwingConstants.RIGHT);
            box.add(at(lblRankHdr, leftW - 200, 64, 186, 16));

            TreeSet<String> regionSet = new TreeSet<>();
            if (state.usePingData)
            {
                regionSet.addAll(state.pingData.keySet());
                for (User m : state.lobby.members) regionSet.addAll(m.pings.keySet());
            }
            List<String> regions = new ArrayList<>(regionSet);

            int listY = 86, listH = boxH - listY - 50;
            Theme.Box list = Theme.box(Theme.WINDOW);
            list.setBounds(14, listY, leftW - 28, listH);
            box.add(list);
            int rowY = 4;
            for (User member : state.lobby.members)
            {
                boolean isMe = member.cxId.equals(state.user.cxId);
                JPanel row = buildMemberRow(member, leftW - 44, isMe, member.cxId.equals(state.lobby.ownerCxId), regions);
                row.setLocation(8, rowY);
                list.add(row);
                rowY += row.getHeight() + 4;
            }

            Theme.FlatButton btnLeave = Theme.button("Leave");
            box.add(at(btnLeave, 14, boxH - 40, 56, 22));
            btnLeave.addActionListener(e -> App.getInstance().onGameScreenClose());

            // Host: Start is always available (cpp has no readiness gate). Guests toggle ready.
            Theme.FlatButton btnReady = Theme.button(isHost ? "Start" : (state.user.isReady ? "Not Ready" : "Ready"));
            box.add(at(btnReady, 78, boxH - 40, btnReady.getPreferredSize().width + 4, 22));
            btnReady.addActionListener(e -> {
                if (isHost) App.getInstance().onGameStart();
                else App.getInstance().onToggleReady();
            });
        }

        // ── Chat / Leaderboards / Info window (right) ─────────────────────────
        {
            int tabY = top - 44;
            Theme.FlatButton btnChatTab = Theme.tab("CHAT");
            Theme.FlatButton btnLeaderboardTab = Theme.tab("LEADERBOARDS");
            Theme.FlatButton btnInfoTab = Theme.tab("INFO");
            panel.add(at(btnChatTab, rightX + 16, tabY + 14, 90, 28));
            panel.add(at(btnLeaderboardTab, rightX + 114, tabY + 14, 150, 28));
            panel.add(at(btnInfoTab, rightX + 272, tabY + 14, 90, 28));
            Theme.Box strip = Theme.box();
            strip.setBounds(rightX, tabY, 378, 50);
            panel.add(strip);

            Theme.Box box = Theme.box();
            box.setBounds(rightX, top, rightW, boxH);
            panel.add(box);

            int innerW = rightW - 28, innerH = boxH - 28;
            JPanel tabCards = new JPanel(new CardLayout());
            tabCards.setOpaque(false);
            tabCards.setBounds(14, 14, innerW, innerH);
            box.add(tabCards);

            JPanel chatTab = new JPanel(null);
            chatTab.setOpaque(false);
            Theme.FlatButton btnThisLobby = Theme.pill("THIS LOBBY");
            Theme.FlatButton btnGlobal = Theme.pill("GLOBAL");
            chatTab.add(at(btnThisLobby, 0, 0, btnThisLobby.getPreferredSize().width, 22));
            chatTab.add(at(btnGlobal, btnThisLobby.getPreferredSize().width + 8, 0, btnGlobal.getPreferredSize().width, 22));
            chatTab.add(Theme.separator(0, 30, innerW));

            JPanel chatSubCards = new JPanel(new CardLayout());
            chatSubCards.setOpaque(false);
            chatSubCards.setBounds(0, 38, innerW, innerH - 38);
            chatTab.add(chatSubCards);

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
                }, innerW, innerH - 38);
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
                }, innerW, innerH - 38);
            }
            chatSubCards.add(_lobbyChatPanel, CHAT_SUBCARD_LOBBY);
            chatSubCards.add(_globalChatPanel, CHAT_SUBCARD_GLOBAL);
            CardLayout chatSubLayout = (CardLayout) chatSubCards.getLayout();
            Runnable showSub = () -> {
                chatSubLayout.show(chatSubCards, _chatSub);
                btnThisLobby.setSelectedPill(_chatSub.equals(CHAT_SUBCARD_LOBBY));
                btnGlobal.setSelectedPill(_chatSub.equals(CHAT_SUBCARD_GLOBAL));
            };
            btnThisLobby.addActionListener(e -> { _chatSub = CHAT_SUBCARD_LOBBY; showSub.run(); });
            btnGlobal.addActionListener(e -> { _chatSub = CHAT_SUBCARD_GLOBAL; showSub.run(); });
            showSub.run();

            if (_leaderboardPanel == null) {
                _leaderboardPanel = new LeaderboardPanel(innerW, innerH);
            }

            // Info tab: lobby id / region / players / time in lobby / status
            JPanel infoTab = new JPanel(null);
            infoTab.setOpaque(false);
            {
                String lobbyIdStr = state.lobby.lobbyId;
                int colonIdx = lobbyIdStr.indexOf(':');
                String region = colonIdx > 0 ? lobbyIdStr.substring(0, colonIdx) : "-";
                Font infoFont = Theme.font(12);
                int y = 0;
                for (String line : new String[] { "Lobby: " + lobbyIdStr, "Region: " + region,
                        "Players: " + state.lobby.members.size() }) {
                    infoTab.add(at(Theme.label(line, infoFont, Theme.TEXT), 0, y, innerW, 18));
                    y += 22;
                }
                _lblInfoTimeInLobby = at(Theme.label("Time in lobby: 00:00", infoFont, Theme.TEXT), 0, y, innerW, 18);
                infoTab.add(_lblInfoTimeInLobby);
                y += 26;
                String statusText = !state.lobbyStatusText.isEmpty() ? state.lobbyStatusText
                        : (isHost ? "Press Start when ready." : "Waiting for host to start...");
                infoTab.add(at(Theme.label(statusText, infoFont, Theme.GOOD), 0, y, innerW, 18));
                updateInfoTimeInLobby();
            }

            tabCards.add(chatTab, CARD_CHAT);
            tabCards.add(_leaderboardPanel, CARD_LEADERBOARD);
            tabCards.add(infoTab, CARD_INFO);
            CardLayout tabLayout = (CardLayout) tabCards.getLayout();
            Runnable showTab = () -> {
                tabLayout.show(tabCards, _tab);
                btnChatTab.setSelectedPill(_tab.equals(CARD_CHAT));
                btnLeaderboardTab.setSelectedPill(_tab.equals(CARD_LEADERBOARD));
                btnInfoTab.setSelectedPill(_tab.equals(CARD_INFO));
            };
            btnChatTab.addActionListener(e -> { _tab = CARD_CHAT; showTab.run(); });
            btnLeaderboardTab.addActionListener(e -> { _tab = CARD_LEADERBOARD; showTab.run(); });
            btnInfoTab.addActionListener(e -> { _tab = CARD_INFO; showTab.run(); });
            showTab.run();
        }
        panel.revalidate();
        panel.repaint();
    }

    // Tab choices survive refreshUI() rebuilds.
    private String _tab = CARD_CHAT;
    private String _chatSub = CHAT_SUBCARD_LOBBY;
}
