package com.bitheads.cursorparty;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

// cpp loading dialog: 280x155 centred box; lobby searches add a timer, lobby id and Cancel.
class LoadingScreen extends Screen
{
    private static final int WARMUP_WARNING_SEC = 60;

    private final JLabel _lblStatus;
    private JLabel _lblTimer;
    private final long _startTime;

    public LoadingScreen(String text)
    {
        Dimension screenRes = App.getInstance().contentSize();
        State appState = App.getInstance().state;

        panel = new JPanel();
        panel.setLayout(null);
        panel.setBackground(Theme.BG);

        int w = 280, h = 155;
        Theme.Box box = Theme.box();
        box.setBounds((screenRes.width - w) / 2, (screenRes.height - h) / 2, w, h);
        panel.add(box);

        _startTime = appState.lobbySearchStartTime != 0 ? appState.lobbySearchStartTime : System.currentTimeMillis();
        boolean searching = text.startsWith("Joining");

        int y = 14;
        _lblStatus = centred(text, Theme.font(13), Theme.TEXT, y, w);
        box.add(_lblStatus);

        if (searching)
        {
            y += 22;
            _lblTimer = centred("0:00", Theme.font(13), Theme.DIM, y, w);
            box.add(_lblTimer);
            if (appState.lobby != null)
            {
                y += 22;
                box.add(centred(appState.lobby.lobbyId, Theme.font(11), Theme.DIM, y, w));
            }

            Theme.FlatButton btnCancel = Theme.button("Cancel");
            btnCancel.setBounds((w - 120) / 2, h - 34, 120, 24);
            btnCancel.addActionListener(e -> App.getInstance().onGameScreenClose());
            box.add(btnCancel);

            // Swing timer: fires on the EDT, stops itself once the screen changes.
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
    }

    private static JLabel centred(String text, java.awt.Font font, Color color, int y, int w)
    {
        JLabel l = Theme.label(text, font, color);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        l.setBounds(0, y, w, 18);
        return l;
    }

    private void updateTimer()
    {
        long elapsedSec = Math.max(0, (System.currentTimeMillis() - _startTime) / 1000);
        if (elapsedSec >= WARMUP_WARNING_SEC)
        {
            _lblStatus.setForeground(Theme.WARN);
            _lblStatus.setText("Server may be warming up...");
        }
        _lblTimer.setText(String.format("%d:%02d", elapsedSec / 60, elapsedSec % 60));
    }

    @Override
    public void onStateChanged(State state) {}
}
