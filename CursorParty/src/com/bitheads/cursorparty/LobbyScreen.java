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
import javax.swing.SwingUtilities;

class LobbyScreen extends Screen
{
    private static final int COLS    = 10;
    private static final int ROWS    = Colors.NUM_COLORS / COLS; // 4
    private static final int BTN_W   = 40;
    private static final int BTN_H   = 28;
    private static final int BTN_GAP = 2;

    private JLabel _lblStatus;
    private JLabel _lblStatusTimer;
    private javax.swing.Timer _statusTimer;

    public LobbyScreen()
    {
        panel = new JPanel();
        panel.setLayout(null);
        refreshUI();

        // Swing timer updates the elapsed-time counter every second while status is active
        _statusTimer = new javax.swing.Timer(1000, null);
        _statusTimer.addActionListener(e -> {
            if (App.getInstance().state.screen != LobbyScreen.this) {
                _statusTimer.stop();
                return;
            }
            updateStatusTimer();
        });
        _statusTimer.start();
    }

    @Override
    public void onStateChanged(State state)
    {
        refreshUI();
    }

    private void updateStatusTimer()
    {
        State state = App.getInstance().state;
        if (_lblStatusTimer == null || state.lobbyStatusText.isEmpty()) return;
        long elapsedSec = Math.max(0, (System.currentTimeMillis() - state.lobbyStatusStartTime) / 1000);
        long min = elapsedSec / 60;
        long sec = elapsedSec % 60;
        _lblStatusTimer.setText(String.format("%d:%02d", min, sec));
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

        // Status banner — shown while STARTING / ROOM_READY provisioning is in progress
        _lblStatus = null;
        _lblStatusTimer = null;
        if (!state.lobbyStatusText.isEmpty())
        {
            int bannerY = screenRes.height - 130;

            JPanel statusBanner = new JPanel(null);
            statusBanner.setSize(screenRes.width, 44);
            statusBanner.setLocation(0, bannerY);
            statusBanner.setBackground(new Color(0x0F, 0x24, 0x61));
            panel.add(statusBanner);

            _lblStatus = new JLabel(state.lobbyStatusText, SwingConstants.CENTER);
            _lblStatus.setSize(screenRes.width, 22);
            _lblStatus.setLocation(0, 2);
            _lblStatus.setFont(new Font("SansSerif", Font.BOLD, 14));
            _lblStatus.setForeground(Color.WHITE);
            statusBanner.add(_lblStatus);

            _lblStatusTimer = new JLabel("0:00", SwingConstants.CENTER);
            _lblStatusTimer.setSize(screenRes.width, 18);
            _lblStatusTimer.setLocation(0, 24);
            _lblStatusTimer.setFont(new Font("Monospaced", Font.PLAIN, 12));
            _lblStatusTimer.setForeground(new Color(190, 200, 220));
            statusBanner.add(_lblStatusTimer);

            updateStatusTimer();
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
