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


    public MainMenuScreen()
    {
        Dimension screenRes = App.getInstance().contentSize();

        panel = new JPanel();
        panel.setLayout(null);
        panel.setBackground(Theme.BG);

        // cpp: setup window left, leaderboard/chat window right, both anchored low-centre.
        int setupW = 400, sideW = 380, gap = 32;
        int cardX = (screenRes.width - setupW - gap - sideW) / 2;
        int cardY = 200;
        int cardH = screenRes.height - cardY;

        Theme.Box setupCard = Theme.box();
        setupCard.setBounds(cardX, cardY, setupW, cardH + 10);
        panel.add(setupCard);

        int pad = 14;
        int fieldW = setupW - pad * 2;
        setupCard.add(at(Theme.label("Cursor Party", Theme.font(12), Theme.TEXT), 6, 4, 200, 18));

        int y = 34;
        JLabel lblTagline = Theme.label("<html>Paint more of the board than anyone else -- cover it,<br>and you win the party.</html>",
                Theme.font(12), Theme.GOOD);
        setupCard.add(at(lblTagline, pad, y, fieldW, 32));
        y += 38;
        setupCard.add(Theme.separator(pad, y, fieldW));
        y += 8;

        setupCard.add(at(Theme.label("LOBBY TYPE", Theme.font(11), Theme.DIM), pad, y, fieldW, 16));
        y += 18;
        _cboLobbyType = new JComboBox<>();
        setupCard.add(at(_cboLobbyType, pad, y, 240, 20));
        refreshLobbyList();
        y += 28;

        setupCard.add(at(Theme.label("NETWORK PROTOCOL", Theme.font(11), Theme.DIM), pad, y, fieldW, 16));
        y += 18;
        // Abbreviated labels (WS/TCP/UDP); App.onPlayClicked maps "WS" back to WEBSOCKET.
        _cboProtocol = new JComboBox<>(new String[]{"WS", "TCP", "UDP"});
        setupCard.add(at(_cboProtocol, pad, y, 240, 20));
        y += 28;

        _chkUsePingData = new JCheckBox("With Ping Region Data");
        _chkUsePingData.setOpaque(false);
        setupCard.add(at(_chkUsePingData, pad - 4, y, fieldW, 22));
        y += 30;

        setupCard.add(Theme.separator(pad, y, fieldW));
        y += 8;
        setupCard.add(at(Theme.label("Not sure what any of this means? Just tap below.", Theme.font(12), Theme.DIM), pad, y, fieldW, 16));
        y += 22;

        Theme.FlatButton btnPlay = Theme.accentButton("Find / Create Lobby");
        setupCard.add(at(btnPlay, pad, y, fieldW, 40));
        y += 52;

        Theme.FlatButton btnLogout = Theme.button("Log Out");
        setupCard.add(at(btnLogout, pad, y, 80, 22));

        btnPlay.addActionListener(e -> App.getInstance().onPlayClicked(
                _cboProtocol.getSelectedItem().toString(),
                _cboLobbyType.getSelectedItem().toString(),
                _chkUsePingData.isSelected()));
        btnLogout.addActionListener(e -> App.getInstance().onLogoutClicked());

        // Leaderboard / chat window (right), tabs above the content like cpp.
        {
            int panelX = cardX + setupW + gap;
            int tabY = cardY - 44;

            Theme.FlatButton btnLeaderboardTab = Theme.tab("LEADERBOARD");
            Theme.FlatButton btnChatTab = Theme.tab("CHAT");
            panel.add(at(btnLeaderboardTab, panelX + 16, tabY + 14, 150, 28));
            panel.add(at(btnChatTab, panelX + 174, tabY + 14, 100, 28));
            Theme.Box tabStrip = Theme.box();
            tabStrip.setBounds(panelX, tabY, 290, 50);
            panel.add(tabStrip);

            Theme.Box sideCard = Theme.box();
            sideCard.setBounds(panelX, cardY, sideW, cardH + 10);
            panel.add(sideCard);
            panel.setComponentZOrder(sideCard, panel.getComponentCount() - 1);

            JPanel cards = new JPanel(new CardLayout());
            cards.setOpaque(false);
            cards.setBounds(pad, pad, sideW - pad * 2, cardH - pad * 2 - 20);
            sideCard.add(cards);

            _leaderboardPanel = new LeaderboardPanel(sideW - pad * 2, cardH - pad * 2 - 20);
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
            }, sideW - pad * 2, cardH - pad * 2 - 20);
            cards.add(_globalChatPanel, CARD_CHAT);

            CardLayout cardLayout = (CardLayout) cards.getLayout();
            cardLayout.show(cards, CARD_LEADERBOARD); // Leaderboard is the default tab
            btnLeaderboardTab.setSelectedPill(true);
            btnLeaderboardTab.addActionListener(e -> {
                cardLayout.show(cards, CARD_LEADERBOARD);
                btnLeaderboardTab.setSelectedPill(true);
                btnChatTab.setSelectedPill(false);
            });
            btnChatTab.addActionListener(e -> {
                cardLayout.show(cards, CARD_CHAT);
                btnLeaderboardTab.setSelectedPill(false);
                btnChatTab.setSelectedPill(true);
            });
        }
    }

    private static <T extends java.awt.Component> T at(T c, int x, int y, int w, int h)
    {
        c.setBounds(x, y, w, h);
        return c;
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
