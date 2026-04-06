package com.bitheads.cursorparty;

import java.awt.Color;
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
    JTextField    _txtUsername;
    JPasswordField _txtPassword;

    public LoginScreen()
    {
        JFrame frame = App.getInstance().frame;
        Dimension screenRes = frame.getPreferredSize();

        panel = new JPanel();
        panel.setLayout(null);
        panel.setBackground(Colors.BG_COLOR);

        int x = screenRes.width  / 2 - 100;
        int y = screenRes.height / 2 - 120;

        JLabel lblTitle = new JLabel("brainCloud Cursor Party", SwingConstants.CENTER);
        lblTitle.setSize(screenRes.width, 40);
        lblTitle.setLocation(0, 40);
        lblTitle.setFont(new Font(lblTitle.getFont().getName(), Font.PLAIN, 32));
        lblTitle.setForeground(Colors.TEXT_COLOR);
        panel.add(lblTitle);

        JLabel lblUsername = new JLabel("Username");
        lblUsername.setSize(200, 20);
        lblUsername.setLocation(x, y);
        lblUsername.setForeground(Colors.TEXT_COLOR);
        lblUsername.setToolTipText("Usernames are unique");
        panel.add(lblUsername);

        _txtUsername = new JTextField();
        _txtUsername.setSize(200, 30);
        _txtUsername.setLocation(x, y + 22);
        _txtUsername.setToolTipText("Usernames are unique");
        panel.add(_txtUsername);

        JLabel lblPassword = new JLabel("Password");
        lblPassword.setSize(200, 20);
        lblPassword.setLocation(x, y + 60);
        lblPassword.setForeground(Colors.TEXT_COLOR);
        lblPassword.setToolTipText("Will create a new user if it doesn't exist");
        panel.add(lblPassword);

        _txtPassword = new JPasswordField();
        _txtPassword.setSize(200, 30);
        _txtPassword.setLocation(x, y + 82);
        _txtPassword.setToolTipText("Will create a new user if it doesn't exist");
        panel.add(_txtPassword);

        JButton btnConnect = new JButton("Connect");
        btnConnect.setSize(200, 30);
        btnConnect.setLocation(x, y + 130);
        panel.add(btnConnect);

        JLabel lblVersion = new JLabel("v" + App.getInstance().clientVersion);
        lblVersion.setSize(200, 20);
        lblVersion.setLocation(x, y + 180);
        lblVersion.setForeground(Color.GRAY);
        panel.add(lblVersion);

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
        if (_txtUsername.getText().isEmpty())
        {
            System.out.println("Please specify a username");
            return;
        }
        if (new String(_txtPassword.getPassword()).isEmpty())
        {
            System.out.println("Please specify a password");
            return;
        }
        App.getInstance().brainCloudConnect(
            _txtUsername.getText().toLowerCase(),
            new String(_txtPassword.getPassword()));
    }

    @Override
    public void onStateChanged(State state) {}
}
