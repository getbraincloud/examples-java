package com.bitheads.cursorparty;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
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

    private static final Color CARD_BG = new Color(0x1D, 0x20, 0x2C);
    private static final Color CARD_BORDER = new Color(0x33, 0x38, 0x45);
    private static final Color DIM = new Color(140, 150, 160);
    private static final Color GREEN = new Color(0x66, 0xEE, 0x88);

    public MainMenuScreen()
    {
        JFrame frame = App.getInstance().frame;
        Dimension screenRes = frame.getPreferredSize();

        panel = new JPanel();
        panel.setLayout(null);
        panel.setBackground(Colors.BG_COLOR);

        int setupW = 420;
        int sideW = 380;
        int gap = 24;
        int groupW = setupW + gap + sideW;
        int cardX = (screenRes.width - groupW) / 2;
        int cardY = Math.max(60, screenRes.height / 2 - 260);
        int cardH = 520;

        // ── Setup card (left) ────────────────────────────────────────────────
        JPanel setupCard = new JPanel(null);
        setupCard.setBounds(cardX, cardY, setupW, cardH);
        setupCard.setBackground(CARD_BG);
        setupCard.setBorder(BorderFactory.createLineBorder(CARD_BORDER, 1));
        panel.add(setupCard);

        int pad = 28;
        int y = 28;

        JLabel lblTitle = new JLabel("Cursor Party", SwingConstants.CENTER);
        lblTitle.setBounds(0, y, setupW, 40);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 28));
        lblTitle.setForeground(Colors.TEXT_COLOR);
        setupCard.add(lblTitle);
        y += 46;

        JLabel lblTagline = new JLabel("<html><div style='text-align:center;'>Paint more of the board than "
                + "anyone else &mdash; cover it,<br>and you win the party.</div></html>", SwingConstants.CENTER);
        lblTagline.setBounds(pad, y, setupW - pad * 2, 34);
        lblTagline.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblTagline.setForeground(GREEN);
        setupCard.add(lblTagline);
        y += 44;

        setupCard.add(buildSeparator(pad, y, setupW - pad * 2));
        y += 20;

        JLabel lblLobby = new JLabel("LOBBY TYPE");
        lblLobby.setBounds(pad, y, setupW - pad * 2, 16);
        lblLobby.setFont(new Font("SansSerif", Font.BOLD, 10));
        lblLobby.setForeground(DIM);
        setupCard.add(lblLobby);
        y += 20;

        _cboLobbyType = new JComboBox<>();
        _cboLobbyType.setBounds(pad, y, setupW - pad * 2, 30);
        setupCard.add(_cboLobbyType);
        refreshLobbyList();
        y += 42;

        JLabel lblProtocol = new JLabel("NETWORK PROTOCOL");
        lblProtocol.setBounds(pad, y, setupW - pad * 2, 16);
        lblProtocol.setFont(new Font("SansSerif", Font.BOLD, 10));
        lblProtocol.setForeground(DIM);
        setupCard.add(lblProtocol);
        y += 20;

        // Displays abbreviated protocol names (WS/TCP/UDP); App.onPlayClicked maps
        // "WS" back to RelayConnectionType.WEBSOCKET the same way it already
        // handles "TCP"/"UDP" — no wire-protocol change, just a shorter label.
        _cboProtocol = new JComboBox<>(new String[]{"WS", "TCP", "UDP"});
        _cboProtocol.setBounds(pad, y, setupW - pad * 2, 30);
        setupCard.add(_cboProtocol);
        y += 42;

        _chkUsePingData = new JCheckBox("With Ping Region Data");
        _chkUsePingData.setBounds(pad, y, setupW - pad * 2, 24);
        _chkUsePingData.setBackground(CARD_BG);
        _chkUsePingData.setForeground(Colors.TEXT_COLOR);
        setupCard.add(_chkUsePingData);
        y += 34;

        setupCard.add(buildSeparator(pad, y, setupW - pad * 2));
        y += 16;

        JLabel lblHelper = new JLabel("Not sure what any of this means? Just tap below.", SwingConstants.CENTER);
        lblHelper.setBounds(pad, y, setupW - pad * 2, 16);
        lblHelper.setFont(new Font("SansSerif", Font.ITALIC, 11));
        lblHelper.setForeground(DIM);
        setupCard.add(lblHelper);
        y += 26;

        JButton btnPlay = new JButton("Find / Create Lobby");
        btnPlay.setBounds(pad, y, setupW - pad * 2, 40);
        btnPlay.setBackground(new Color(0x44, 0x66, 0xEE));
        btnPlay.setForeground(Color.WHITE);
        btnPlay.setFont(new Font("SansSerif", Font.BOLD, 14));
        setupCard.add(btnPlay);
        y += 52;

        JButton btnLogout = new JButton("Log Out");
        btnLogout.setBounds(pad, y, setupW - pad * 2, 26);
        setupCard.add(btnLogout);

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

        // ── Chat / Leaderboard card (right) ─────────────────────────────────
        {
            int panelX = cardX + setupW + gap;
            int panelY = cardY;
            int panelW = sideW;
            int panelH = cardH;

            JPanel sideCard = new JPanel(null);
            sideCard.setBounds(panelX, panelY, panelW, panelH);
            sideCard.setBackground(CARD_BG);
            sideCard.setBorder(BorderFactory.createLineBorder(CARD_BORDER, 1));
            panel.add(sideCard);

            int tabH = 30;
            JButton btnLeaderboardTab = new JButton("LEADERBOARD");
            btnLeaderboardTab.setBounds(0, 0, panelW / 2 - 1, tabH);
            sideCard.add(btnLeaderboardTab);

            JButton btnChatTab = new JButton("CHAT");
            btnChatTab.setBounds(panelW / 2 + 1, 0, panelW / 2 - 1, tabH);
            sideCard.add(btnChatTab);

            JPanel cards = new JPanel(new CardLayout());
            cards.setBounds(0, tabH, panelW, panelH - tabH);
            sideCard.add(cards);

            _leaderboardPanel = new LeaderboardPanel(panelW, panelH - tabH);
            cards.add(_leaderboardPanel, CARD_LEADERBOARD);

            _globalChatPanel = new ChatPanel(new ChatPanel.Source() {
                @Override
                public java.util.ArrayList<ChatMessage> getMessages() {
                    return App.getInstance().state.chatMessages;
                }

                @Override
                public void sendMessage(String text) {
                    App.getInstance().sendGlobalChatMessage(text);
                }
            }, panelW, panelH - tabH);
            cards.add(_globalChatPanel, CARD_CHAT);

            CardLayout cardLayout = (CardLayout) cards.getLayout();
            cardLayout.show(cards, CARD_LEADERBOARD); // Leaderboard is the default tab
            btnLeaderboardTab.addActionListener(e -> cardLayout.show(cards, CARD_LEADERBOARD));
            btnChatTab.addActionListener(e -> cardLayout.show(cards, CARD_CHAT));
        }
    }

    private JPanel buildSeparator(int x, int y, int width)
    {
        JPanel sep = new JPanel();
        sep.setBounds(x, y, width, 1);
        sep.setBackground(CARD_BORDER);
        return sep;
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
