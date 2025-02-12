package com.bitheads.braincloud;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

class LoginScreen extends Screen
{
    // Swing UI stuff
    JTextField _txtUsername;
    JPasswordField _txtPassword;
    // JCheckBox _chkUseWebSocket;

    public LoginScreen()
    {
        // Create the Swing stuff
        JFrame frame = App.getInstance().frame;
        Dimension screenRes = frame.getPreferredSize();

        panel = new JPanel();
        panel.setLayout(null);

        int x = screenRes.width / 2 - 100;
        int y = screenRes.height / 2 - (240) / 2;

        // Tittle
        JLabel lblTitle = new JLabel("brainCloud Relay Test App", SwingConstants.CENTER);
        lblTitle.setSize(screenRes.width, 40);
        lblTitle.setLocation(0, 40);
        Font font = lblTitle.getFont();
        lblTitle.setFont(new Font(font.getName(), Font.PLAIN, 32));
        panel.add(lblTitle);

        // User name (currently used as unique userId)
        JLabel lblUsername = new JLabel("Username");
        lblUsername.setSize(200, 20);
        lblUsername.setLocation(x, y);
        panel.add(lblUsername);

        _txtUsername = new JTextField();
        _txtUsername.setSize(200, 30);
        _txtUsername.setLocation(x, y + 20);
        panel.add(_txtUsername);
        
        lblUsername.setToolTipText("Usernames are unique");
        _txtUsername.setToolTipText("Usernames are unique");

        JLabel lblPassword = new JLabel("Password");
        lblPassword.setSize(200, 20);
        lblPassword.setLocation(x, y + 50);
        panel.add(lblPassword);

        _txtPassword = new JPasswordField();
        _txtPassword.setSize(200, 30);
        _txtPassword.setLocation(x, y + 70);
        panel.add(_txtPassword);

        lblPassword.setToolTipText("Will create a new user if doesn't exist");
        _txtPassword.setToolTipText("Will create a new user if doesn't exist");

        JButton btnConnect = new JButton("Connect");
        btnConnect.setSize(200, 30);
        btnConnect.setLocation(x, y + 140);
        panel.add(btnConnect);

        JLabel lblVersion = new JLabel("Version: " + App.getInstance().clientVersion);
        lblVersion.setSize(200, 20);
        lblVersion.setLocation(x, y + 200);
        panel.add(lblVersion);

        // _chkUseWebSocket = new JCheckBox("Use WebSocket", _prefs.get("useWebSocket", "false").equals("true"));
        // _chkUseWebSocket.setSize(200, 30);
        // _chkUseWebSocket.setLocation(x, y + 110);
        // panel.add(_chkUseWebSocket);

        btnConnect.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                connect();
            }
        });
    }

    void connect()
    {
        // Basic validation
        if (_txtUsername.getText().isEmpty())
        {
            System.out.println("Please specify a username");
            return;
        }
        if (_txtPassword.getText().isEmpty())
        {
            System.out.println("Please specify a password");
            return;
        }

        // boolean useWebSocket = _chkUseWebSocket.isSelected();

        // _prefs.put("username", _txtUsername.getText().toLowerCase());
        // _prefs.put("password", _txtPassword.getText());
        // _prefs.put("useWebSocket", useWebSocket ? "true" : "false");

        // App.getInstance().setUseWebSocket(useWebSocket);
        App.getInstance().brainCloudConnect(_txtUsername.getText().toLowerCase(), _txtPassword.getText());
    }

    @Override
    public void onStateChanged(State state) {}
}
