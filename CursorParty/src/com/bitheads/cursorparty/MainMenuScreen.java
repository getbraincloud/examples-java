package com.bitheads.cursorparty;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

class MainMenuScreen extends Screen
{
    private JComboBox<String> _cboProtocol;
    private JComboBox<String> _cboLobbyType;
    private JCheckBox _chkUsePingData;
    private ChatPanel _globalChatPanel;
    private LeaderboardPanel _leaderboardPanel;

    private static final String CARD_CHAT = "chat";
    private static final String CARD_LEADERBOARD = "leaderboard";

    public MainMenuScreen()
    {
        JFrame frame = App.getInstance().frame;
        Dimension screenRes = frame.getPreferredSize();

        panel = new JPanel();
        panel.setLayout(null);
        panel.setBackground(Colors.BG_COLOR);

        int x = screenRes.width  / 2;
        int y = screenRes.height / 2 - 120;

        JLabel lblTitle = new JLabel("Main Menu", SwingConstants.CENTER);
        lblTitle.setSize(screenRes.width, 40);
        lblTitle.setLocation(0, 40);
        lblTitle.setFont(new Font(lblTitle.getFont().getName(), Font.PLAIN, 32));
        lblTitle.setForeground(Colors.TEXT_COLOR);
        panel.add(lblTitle);

        JLabel lblProtocol = new JLabel("Protocol", SwingConstants.CENTER);
        lblProtocol.setSize(200, 20);
        lblProtocol.setLocation(x - 100, y + 8);
        lblProtocol.setForeground(Colors.TEXT_COLOR);
        panel.add(lblProtocol);

        _cboProtocol = new JComboBox<>(new String[]{"WEBSOCKET", "TCP", "UDP"});
        _cboProtocol.setSize(200, 30);
        _cboProtocol.setLocation(x - 100, y + 30);
        panel.add(_cboProtocol);

        JLabel lblLobby = new JLabel("Lobby Type", SwingConstants.CENTER);
        lblLobby.setSize(200, 20);
        lblLobby.setLocation(x - 100, y + 66);
        lblLobby.setForeground(Colors.TEXT_COLOR);
        panel.add(lblLobby);

        _cboLobbyType = new JComboBox<>();
        _cboLobbyType.setSize(200, 30);
        _cboLobbyType.setLocation(x - 100, y + 88);
        panel.add(_cboLobbyType);
        refreshLobbyList();

        _chkUsePingData = new JCheckBox("With Ping Region Data");
        _chkUsePingData.setSize(200, 24);
        _chkUsePingData.setLocation(x - 100, y + 126);
        _chkUsePingData.setBackground(Colors.BG_COLOR);
        _chkUsePingData.setForeground(Colors.TEXT_COLOR);
        panel.add(_chkUsePingData);

        JButton btnPlay = new JButton("Play");
        btnPlay.setSize(200, 30);
        btnPlay.setLocation(x - 100, y + 158);
        panel.add(btnPlay);

        JButton btnLogout = new JButton("Log Out");
        btnLogout.setSize(200, 30);
        btnLogout.setLocation(x - 100, y + 198);
        panel.add(btnLogout);

        btnPlay.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                App.getInstance().onPlayClicked(
                    _cboProtocol.getSelectedItem().toString(),
                    _cboLobbyType.getSelectedItem().toString(),
                    _chkUsePingData.isSelected());
            }
        });

        btnLogout.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                App.getInstance().onLogoutClicked();
            }
        });

        // ── Chat / Leaderboard side panel (right side; the setup card above is
        // centered in a 200px column, leaving this space clear) ────────────────
        {
            int panelW = 300;
            int panelX = screenRes.width - panelW - 24;
            int panelY = 100;
            int panelH = screenRes.height - panelY - 60;

            JButton btnChatTab = new JButton("CHAT");
            btnChatTab.setBounds(panelX, panelY, panelW / 2 - 2, 26);
            panel.add(btnChatTab);

            JButton btnLeaderboardTab = new JButton("LEADERBOARD");
            btnLeaderboardTab.setBounds(panelX + panelW / 2 + 2, panelY, panelW / 2 - 2, 26);
            panel.add(btnLeaderboardTab);

            JPanel cards = new JPanel(new CardLayout());
            cards.setBounds(panelX, panelY + 30, panelW, panelH - 30);
            panel.add(cards);

            _globalChatPanel = new ChatPanel(new ChatPanel.Source() {
                @Override
                public java.util.ArrayList<ChatMessage> getMessages() {
                    return App.getInstance().state.chatMessages;
                }

                @Override
                public void sendMessage(String text) {
                    App.getInstance().sendGlobalChatMessage(text);
                }
            }, panelW, panelH - 30);
            cards.add(_globalChatPanel, CARD_CHAT);

            _leaderboardPanel = new LeaderboardPanel(panelW, panelH - 30);
            cards.add(_leaderboardPanel, CARD_LEADERBOARD);

            CardLayout cardLayout = (CardLayout) cards.getLayout();
            btnChatTab.addActionListener(e -> cardLayout.show(cards, CARD_CHAT));
            btnLeaderboardTab.addActionListener(e -> cardLayout.show(cards, CARD_LEADERBOARD));
        }
    }

    @Override
    public void onStateChanged(State state)
    {
        refreshLobbyList();
        if (_globalChatPanel != null) _globalChatPanel.refresh();
    }

    private void refreshLobbyList()
    {
        State state = App.getInstance().state;
        String current = _cboLobbyType.getSelectedItem() != null
                ? _cboLobbyType.getSelectedItem().toString() : null;

        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        if (state.appLobbies.isEmpty())
        {
            model.addElement("CursorPartyV2");
            model.addElement("CursorPartyV2Backfill");
        }
        else
        {
            for (String lobby : state.appLobbies) model.addElement(lobby);
        }
        _cboLobbyType.setModel(model);

        if (current != null) _cboLobbyType.setSelectedItem(current);
    }
}
