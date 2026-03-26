package com.bitheads.cursorparty;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

class MainMenuScreen extends Screen
{
    private JComboBox<String> _cboProtocol;
    private JComboBox<String> _cboLobbyType;

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

        JButton btnPlay = new JButton("Play");
        btnPlay.setSize(200, 30);
        btnPlay.setLocation(x - 100, y + 130);
        panel.add(btnPlay);

        JButton btnLogout = new JButton("Log Out");
        btnLogout.setSize(200, 30);
        btnLogout.setLocation(x - 100, y + 170);
        panel.add(btnLogout);

        btnPlay.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                App.getInstance().onPlayClicked(
                    _cboProtocol.getSelectedItem().toString(),
                    _cboLobbyType.getSelectedItem().toString());
            }
        });

        btnLogout.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                App.getInstance().onLogoutClicked();
            }
        });
    }

    @Override
    public void onStateChanged(State state)
    {
        refreshLobbyList();
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
