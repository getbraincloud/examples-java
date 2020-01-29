package com.bitheads.relaytestapp;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

class LobbyScreen extends Screen
{
    public LobbyScreen()
    {
        panel = new JPanel();
        panel.setLayout(null);

        refreshUI();
    }

    @Override
    public void onStateChanged(State state)
    {
        refreshUI();
    }

    void refreshUI()
    {
        panel.removeAll();
        State state = App.getInstance().state;

        JFrame frame = App.getInstance().frame;
        Dimension screenRes = frame.getPreferredSize();

        int x = screenRes.width / 2;

        panel.setBackground(Color.decode("#282c34"));

        // Tittle
        {
            JLabel lblTitle = new JLabel("Lobby", SwingConstants.CENTER);
            lblTitle.setSize(screenRes.width, 40);
            lblTitle.setLocation(0, 40);
            Font font = lblTitle.getFont();
            lblTitle.setFont(new Font(font.getName(), Font.PLAIN, 32));
            lblTitle.setForeground(Color.WHITE);
            panel.add(lblTitle);
        }

        // Color choices
        for (int i = 0; i < 8; ++i)
        {
            JButton btnColor = new JButton(Integer.toString(i));
            btnColor.setSize(45, 30);
            btnColor.setLocation(x - 8 * 40 / 2 + i * 40, 130);
            Font font = btnColor.getFont();
            btnColor.setFont(new Font(font.getName(), Font.PLAIN, 20));
            btnColor.setBackground(Colors.COLORS[i]);
            btnColor.setForeground(Color.WHITE);
            btnColor.setOpaque(true);
            panel.add(btnColor);
            final int index = i;
            
            btnColor.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    App.getInstance().onColorChanged(index);
                }
            });
        }

        // Members
        for (int i = 0; i < state.lobby.members.size(); ++i)
        {
            User member = state.lobby.members.get(i);

            JLabel lblMember = new JLabel(member.name, SwingConstants.CENTER);
            lblMember.setSize(200, 30);
            lblMember.setLocation(x - 100, 200 + i * 30);
            Font font = lblMember.getFont();
            lblMember.setFont(new Font(font.getName(), Font.PLAIN, 20));
            lblMember.setForeground(Colors.COLORS[member.profileId.equals(state.user.profileId) ? state.user.colorIndex : member.colorIndex]);
            panel.add(lblMember);
        }

        // Buttons
        JButton btnLeave = new JButton("Leave");
        btnLeave.setSize(200, 30);
        btnLeave.setLocation(x - 250, 200 + 320);
        panel.add(btnLeave);
        btnLeave.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                App.getInstance().onGameScreenClose();
            }
        });
        
        // We only put a start button if we are the owner
        if (state.lobby.ownerId.equals(state.user.profileId) && !state.user.isReady)
        {
            JButton btnStart = new JButton("Start");
            btnStart.setSize(200, 30);
            btnStart.setLocation(x + 50, 200 + 320);
            panel.add(btnStart);
                
            btnStart.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    App.getInstance().onGameStart();
                }
            });
        }
    }
}
