package com.bitheads.cursorparty;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

class LoadingScreen extends Screen
{
    private static final int WARMUP_WARNING_SEC = 60;

    private JLabel _lblStatus;
    private JLabel _lblTimer;
    private long   _startTime;

    public LoadingScreen(String text)
    {
        JFrame frame = App.getInstance().frame;
        Dimension screenRes = frame.getPreferredSize();

        panel = new JPanel();
        panel.setLayout(null);
        panel.setBackground(Colors.BG_COLOR);

        _startTime = App.getInstance().state.lobbySearchStartTime;
        if (_startTime == 0) _startTime = System.currentTimeMillis();

        Font base = new Font("SansSerif", Font.PLAIN, 16);

        _lblStatus = new JLabel(text, SwingConstants.CENTER);
        _lblStatus.setSize(screenRes.width, 26);
        _lblStatus.setLocation(0, screenRes.height / 2 - 26);
        _lblStatus.setFont(base);
        _lblStatus.setForeground(Colors.TEXT_COLOR);
        panel.add(_lblStatus);

        _lblTimer = new JLabel("", SwingConstants.CENTER);
        _lblTimer.setSize(screenRes.width, 22);
        _lblTimer.setLocation(0, screenRes.height / 2 + 4);
        _lblTimer.setFont(new Font("SansSerif", Font.PLAIN, 14));
        _lblTimer.setForeground(Colors.TEXT_COLOR);
        panel.add(_lblTimer);

        // Show lobby ID when we already have one (joining server after lobby found)
        State appState = App.getInstance().state;
        if (appState.lobby != null)
        {
            JLabel lblLobbyId = new JLabel(appState.lobby.lobbyId, SwingConstants.CENTER);
            lblLobbyId.setSize(screenRes.width, 18);
            lblLobbyId.setLocation(0, screenRes.height / 2 + 30);
            lblLobbyId.setFont(new Font("Monospaced", Font.PLAIN, 11));
            lblLobbyId.setForeground(new Color(130, 140, 160));
            panel.add(lblLobbyId);
        }

        // Swing timer — fires on EDT, self-stops when screen changes
        javax.swing.Timer uiTimer = new javax.swing.Timer(1000, null);
        uiTimer.addActionListener(e -> {
            if (App.getInstance().state.screen != LoadingScreen.this) {
                uiTimer.stop();
                return;
            }
            updateTimer();
        });
        uiTimer.setInitialDelay(0);
        uiTimer.start();
    }

    private void updateTimer()
    {
        long elapsedSec = Math.max(0, (System.currentTimeMillis() - _startTime) / 1000);
        long minutes = elapsedSec / 60;
        long seconds = elapsedSec % 60;

        if (elapsedSec >= WARMUP_WARNING_SEC)
        {
            Color warn = new Color(255, 200, 0);
            _lblStatus.setForeground(warn);
            _lblTimer.setForeground(warn);
            _lblStatus.setText("Server may be warming up...");
        }
        _lblTimer.setText(String.format("%d:%02d", minutes, seconds));
    }

    @Override
    public void onStateChanged(State state) {}
}
