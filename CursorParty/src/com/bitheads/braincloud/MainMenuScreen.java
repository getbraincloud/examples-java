package com.bitheads.braincloud;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

class MainMenuScreen extends Screen
{
    private JComboBox _cboProtocol;
    private JComboBox _cboLobbyType;

    public MainMenuScreen()
    {
        // Create the Swing stuff
        JFrame frame = App.getInstance().frame;
        Dimension screenRes = frame.getPreferredSize();

        panel = new JPanel();
        panel.setLayout(null);

        int x = screenRes.width / 2;
        int y = screenRes.height / 2 - (240) / 2;

        // Tittle
        JLabel lblTitle = new JLabel("Main Menu", SwingConstants.CENTER);
        lblTitle.setSize(screenRes.width, 40);
        lblTitle.setLocation(0, 40);
        Font font = lblTitle.getFont();
        lblTitle.setFont(new Font(font.getName(), Font.PLAIN, 32));
        panel.add(lblTitle);

        // Protocol
        _cboProtocol = new JComboBox(new String[]{"WEBSOCKET", "TCP", "UDP"});
        _cboProtocol.setSize(200, 30);
        _cboProtocol.setLocation(x - 100, y + 30);
        panel.add(_cboProtocol);

        // Lobby choices
        _cboLobbyType = new JComboBox(new String[]{"CursorPartyV2", "CursorPartyV2Backfill"});
        _cboLobbyType.setSize(200, 30);
        _cboLobbyType.setLocation(x - 100, y + 60);
        panel.add(_cboLobbyType);

        // Play
        JButton btnPlay = new JButton("Play");
        btnPlay.setSize(200, 30);
        btnPlay.setLocation(x - 100, y + 100);
        panel.add(btnPlay);

        // Log Out
        JButton btnLogout = new JButton("Log Out");
        btnLogout.setSize(200, 30);
        btnLogout.setLocation(x - 100, y + 150);
        panel.add(btnLogout);

        // Event listeners
        // OnPlayClicked
        btnPlay.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String protocolStr = _cboProtocol.getSelectedItem().toString();
                String lobbyTypeStr = _cboLobbyType.getSelectedItem().toString();
                App.getInstance().onPlayClicked(protocolStr, lobbyTypeStr);
            }
        });

        // OnLogoutClicked
        btnLogout.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                App.getInstance().onLogoutClicked();
            }
        });
    }
}
