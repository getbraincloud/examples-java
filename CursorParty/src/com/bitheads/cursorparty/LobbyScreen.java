package com.bitheads.cursorparty;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

class LobbyScreen extends Screen
{
    private static final int COLS    = 10;
    private static final int ROWS    = Colors.NUM_COLORS / COLS; // 4
    private static final int BTN_W   = 40;
    private static final int BTN_H   = 28;
    private static final int BTN_GAP = 2;

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
        int cx = screenRes.width / 2;

        panel.setBackground(Colors.BG_COLOR);

        // Title
        {
            JLabel lblTitle = new JLabel("Lobby", SwingConstants.CENTER);
            lblTitle.setSize(screenRes.width, 40);
            lblTitle.setLocation(0, 40);
            lblTitle.setFont(new Font(lblTitle.getFont().getName(), Font.PLAIN, 32));
            lblTitle.setForeground(Colors.TEXT_COLOR);
            panel.add(lblTitle);

            // Lobby ID — persistent reference shown below the title
            String lobbyId = state.lobby != null ? state.lobby.lobbyId : "";
            JLabel lblLobbyId = new JLabel(lobbyId, SwingConstants.CENTER);
            lblLobbyId.setSize(screenRes.width, 20);
            lblLobbyId.setLocation(0, 84);
            lblLobbyId.setFont(new Font("Monospaced", Font.PLAIN, 11));
            lblLobbyId.setForeground(new Color(130, 140, 160));
            panel.add(lblLobbyId);
        }

        // Colour picker — 4 rows × 10 = 40 colours
        {
            int paletteW = COLS * (BTN_W + BTN_GAP) - BTN_GAP;
            int paletteX = cx - paletteW / 2;
            int paletteY = 100;

            for (int row = 0; row < ROWS; row++)
            {
                for (int col = 0; col < COLS; col++)
                {
                    int colorIndex = row * COLS + col;
                    int bx = paletteX + col * (BTN_W + BTN_GAP);
                    int by = paletteY + row * (BTN_H + BTN_GAP);

                    JButton btn = new JButton();
                    btn.setSize(BTN_W, BTN_H);
                    btn.setLocation(bx, by);
                    btn.setBackground(Colors.COLORS[colorIndex]);
                    btn.setOpaque(true);
                    btn.setBorderPainted(true);

                    if (colorIndex == state.user.colorIndex)
                        btn.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
                    else
                        btn.setBorder(BorderFactory.createLineBorder(new Color(60, 63, 65), 1));

                    panel.add(btn);
                    final int idx = colorIndex;
                    btn.addActionListener(new ActionListener()
                    {
                        @Override
                        public void actionPerformed(ActionEvent e)
                        {
                            App.getInstance().onColorChanged(idx);
                        }
                    });
                }
            }
        }

        // Members list
        {
            int membersY = 100 + ROWS * (BTN_H + BTN_GAP) + 20;
            Font memberFont = new Font("SansSerif", Font.PLAIN, 18);

            for (int i = 0; i < state.lobby.members.size(); ++i)
            {
                User member = state.lobby.members.get(i);
                int colorIdx = member.cxId.equals(state.user.cxId)
                        ? state.user.colorIndex : member.colorIndex;

                JLabel lbl = new JLabel(member.name, SwingConstants.CENTER);
                lbl.setSize(280, 26);
                lbl.setLocation(cx - 140, membersY + i * 28);
                lbl.setFont(memberFont);
                lbl.setForeground(Colors.COLORS[colorIdx % Colors.NUM_COLORS]);
                panel.add(lbl);
            }
        }

        // Buttons
        int btnY = screenRes.height - 80;

        JButton btnLeave = new JButton("Leave");
        btnLeave.setSize(160, 30);
        btnLeave.setLocation(cx - 200, btnY);
        panel.add(btnLeave);
        btnLeave.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                App.getInstance().onGameScreenClose();
            }
        });

        if (state.lobby.ownerCxId.equals(state.user.cxId) && !state.user.isReady)
        {
            JButton btnStart = new JButton("Start");
            btnStart.setSize(160, 30);
            btnStart.setLocation(cx + 40, btnY);
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
