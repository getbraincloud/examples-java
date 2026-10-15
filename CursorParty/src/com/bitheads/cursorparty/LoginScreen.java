package com.bitheads.cursorparty;

import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

// cpp login window: centred box, fields with labels to the right.
class LoginScreen extends Screen
{
    JTextField    _txtUsername;
    JPasswordField _txtPassword;

    public LoginScreen()
    {
        Dimension screenRes = App.getInstance().contentSize();

        panel = new JPanel();
        panel.setLayout(null);
        panel.setBackground(Theme.BG);

        int w = 400, h = 150;
        Theme.Box box = Theme.box();
        box.setBounds((screenRes.width - w) / 2, (screenRes.height - h) / 2, w, h);
        panel.add(box);

        box.add(at(Theme.label("Cursor Party " + App.getInstance()._bcWrapper.getClient().getAppVersion(), Theme.font(12), Theme.TEXT), 6, 4, w - 12, 18));
        box.add(at(Theme.label("A new user will be created if it doesn't exist.", Theme.font(12), Theme.TEXT), 16, 32, w - 32, 18));

        _txtUsername = new JTextField();
        box.add(at(_txtUsername, 16, 56, 260, 20));
        box.add(at(Theme.label("Username", Theme.font(12), Theme.TEXT), 284, 56, 100, 20));

        _txtPassword = new JPasswordField();
        box.add(at(_txtPassword, 16, 83, 260, 20));
        box.add(at(Theme.label("Password", Theme.font(12), Theme.TEXT), 284, 83, 100, 20));

        Theme.FlatButton btnConnect = Theme.button("Login");
        box.add(at(btnConnect, 16, 110, 60, 20));

        btnConnect.addActionListener(e -> connect());
        _txtPassword.addActionListener(e -> connect());
        _txtUsername.addActionListener(e -> _txtPassword.requestFocusInWindow());
    }

    private static <T extends java.awt.Component> T at(T c, int x, int y, int w, int h)
    {
        c.setBounds(x, y, w, h);
        return c;
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
