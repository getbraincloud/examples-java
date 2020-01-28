package com.bitheads.relaytestapp;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

class MainMenuScreen extends Screen
{
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

        // Lobby choices
        JButton btnPlay = new JButton("Play");
        btnPlay.setSize(200, 30);
        btnPlay.setLocation(x - 100, y + 70);
        panel.add(btnPlay);

        // Event listeners
        btnPlay.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                App.getInstance().onPlayClicked();
            }
        });
    }
}
