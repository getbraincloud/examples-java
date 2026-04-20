package com.bitheads.cursorparty;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Iterator;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.util.Timer;
import java.util.TimerTask;

class GameScreen extends Screen
{
    private static final int SPLOTCH_RADIUS = 16;

    private Timer  _refreshTimer = new Timer();
    private JLabel _lblGameTimer;
    private java.util.List<JLabel> _pingLabels = new java.util.ArrayList<>();

    // Draw an arrow cursor at pixel (x, y) in the given colour
    private static void drawCursor(Graphics2D g2, int x, int y, Color color)
    {
        int[] xPts       = {x,   x+14, x+5 };
        int[] yPts       = {y,   y+5,  y+14};
        int[] xShadow    = {x+2, x+16, x+7 };
        int[] yShadow    = {y+2, y+7,  y+16};

        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillPolygon(xShadow, yShadow, 3);
        g2.setColor(color);
        g2.fillPolygon(xPts, yPts, 3);
        g2.setColor(Color.WHITE);
        g2.drawPolygon(xPts, yPts, 3);
    }

    class PlayArea extends JPanel
    {
        @Override
        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            State state = App.getInstance().state;
            long nowMs  = System.currentTimeMillis();
            int  durationSec = state.splotchDurationSec;

            // ── Persistent splotches (bottom layer) ──────────────────────────
            Composite defaultComposite = g2.getComposite();
            Iterator<Splotch> splotchIt = state.splotches.iterator();
            while (splotchIt.hasNext())
            {
                Splotch s = splotchIt.next();
                long ageMs = nowMs - s.startTimeMs;

                // Remove expired splotches
                if (durationSec >= 0 && ageMs >= durationSec * 1000L)
                {
                    splotchIt.remove();
                    continue;
                }

                // Fade out during the final 3 seconds of life
                float alpha = 1.0f;
                if (durationSec > 0)
                {
                    long remainingMs = durationSec * 1000L - ageMs;
                    if (remainingMs < 3000)
                        alpha = Math.max(0f, remainingMs / 3000.0f);
                }

                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2.setColor(Colors.COLORS[s.colorIndex % Colors.NUM_COLORS]);
                int px = (int)(s.pos.getX() * getWidth())  - SPLOTCH_RADIUS;
                int py = (int)(s.pos.getY() * getHeight()) - SPLOTCH_RADIUS;
                g2.fillOval(px, py, SPLOTCH_RADIUS * 2, SPLOTCH_RADIUS * 2);
            }
            g2.setComposite(defaultComposite);

            // ── Shockwave rings (middle layer) ────────────────────────────────
            for (int i = 0; i < state.shockwaves.size(); ++i)
            {
                Shockwave sw = state.shockwaves.get(i);
                sw.time++;
                if (sw.time >= 30)
                {
                    state.shockwaves.remove(i--);
                    continue;
                }
                int size = sw.time * 128 / 30;
                g2.setColor(sw.color);
                g2.drawArc(
                    (int)(sw.pos.getX() * getWidth())  - size / 2,
                    (int)(sw.pos.getY() * getHeight()) - size / 2,
                    size, size, 0, 360);
            }

            // ── Player cursors (top layer) ────────────────────────────────────
            for (int i = 0; i < state.lobby.members.size(); ++i)
            {
                User member = state.lobby.members.get(i);
                if (member.pos != null)
                {
                    drawCursor(g2,
                        (int)(member.pos.getX() * getWidth()),
                        (int)(member.pos.getY() * getHeight()),
                        Colors.COLORS[member.colorIndex % Colors.NUM_COLORS]);
                }
            }
        }
    }

    public GameScreen()
    {
        panel = new JPanel();
        panel.setLayout(null);
        panel.setBackground(Colors.BG_COLOR);
        refreshUI();
    }

    @Override
    public void onStateChanged(State state)
    {
        refreshUI();
    }

    void refreshUI()
    {
        _refreshTimer.cancel();
        _refreshTimer = new Timer();
        _pingLabels.clear();
        panel.removeAll();
        panel.setBackground(Colors.BG_COLOR);

        State state = App.getInstance().state;
        JFrame frame = App.getInstance().frame;
        Dimension screenRes = frame.getPreferredSize();

        // Game timer — upper centre
        _lblGameTimer = new JLabel("", SwingConstants.CENTER);
        _lblGameTimer.setSize(400, 28);
        _lblGameTimer.setLocation(screenRes.width / 2 - 200, 8);
        _lblGameTimer.setFont(new Font("SansSerif", Font.BOLD, 15));
        _lblGameTimer.setForeground(Colors.TEXT_COLOR);
        panel.add(_lblGameTimer);

        // ── Options panel (left side) ────────────────────────────────────────
        {
            JLabel lblPlayerMasks = new JLabel("Player Mask (For splotches)");
            lblPlayerMasks.setSize(220, 16);
            lblPlayerMasks.setLocation(8, 8);
            lblPlayerMasks.setForeground(Colors.TEXT_COLOR);
            panel.add(lblPlayerMasks);

            int i = 0;
            for (; i < state.lobby.members.size(); ++i)
            {
                User member = state.lobby.members.get(i);
                JCheckBox chk = new JCheckBox(member.name, member.allowSendTo);
                chk.setSize(160, 16);
                chk.setLocation(8, 8 + 32 + i * 16);
                chk.setBackground(Colors.BG_COLOR);
                chk.setForeground(Colors.COLORS[member.colorIndex % Colors.NUM_COLORS]);
                chk.addChangeListener(new ChangeListener()
                {
                    public void stateChanged(ChangeEvent e)
                    {
                        member.allowSendTo = ((AbstractButton)e.getSource()).getModel().isSelected();
                    }
                });
                panel.add(chk);

                // Active relay ping display
                JLabel lblPing = new JLabel("...");
                lblPing.setSize(60, 16);
                lblPing.setLocation(170, 8 + 32 + i * 16);
                lblPing.setFont(new Font("SansSerif", Font.PLAIN, 11));
                lblPing.setForeground(new Color(140, 150, 160));
                panel.add(lblPing);
                _pingLabels.add(lblPing);
            }

            int optY = 8 + 32 + i * 16 + 24;

            JLabel lblRelayOptions = new JLabel("Relay Options (For cursor position)");
            lblRelayOptions.setSize(220, 16);
            lblRelayOptions.setLocation(8, optY);
            lblRelayOptions.setForeground(Colors.TEXT_COLOR);
            panel.add(lblRelayOptions);

            JCheckBox chkReliable = new JCheckBox("Reliable", state.reliable);
            chkReliable.setSize(220, 16);
            chkReliable.setLocation(8, optY + 24);
            chkReliable.setBackground(Colors.BG_COLOR);
            chkReliable.setForeground(Colors.TEXT_COLOR);
            chkReliable.addChangeListener(new ChangeListener()
            {
                public void stateChanged(ChangeEvent e)
                {
                    state.reliable = ((AbstractButton)e.getSource()).getModel().isSelected();
                }
            });
            panel.add(chkReliable);

            JCheckBox chkOrdered = new JCheckBox("Ordered", state.ordered);
            chkOrdered.setSize(220, 16);
            chkOrdered.setLocation(8, optY + 44);
            chkOrdered.setBackground(Colors.BG_COLOR);
            chkOrdered.setForeground(Colors.TEXT_COLOR);
            chkOrdered.addChangeListener(new ChangeListener()
            {
                public void stateChanged(ChangeEvent e)
                {
                    state.ordered = ((AbstractButton)e.getSource()).getModel().isSelected();
                }
            });
            panel.add(chkOrdered);
        }

        // ── Play area (right side) ────────────────────────────────────────────
        {
            JPanel playArea = new PlayArea();
            playArea.setSize(800, 600);
            playArea.setLocation(screenRes.width - 808, screenRes.height / 2 - 300);
            playArea.setBackground(Colors.BG_COLOR);
            panel.add(playArea);

            playArea.addMouseMotionListener(new MouseMotionListener()
            {
                @Override
                public void mouseMoved(MouseEvent e)
                {
                    App.getInstance().onPlayerMove(
                        (float)e.getX() / playArea.getWidth(),
                        (float)e.getY() / playArea.getHeight());
                }
                @Override
                public void mouseDragged(MouseEvent e)
                {
                    App.getInstance().onPlayerMove(
                        (float)e.getX() / playArea.getWidth(),
                        (float)e.getY() / playArea.getHeight());
                }
            });

            playArea.addMouseListener(new MouseListener()
            {
                @Override
                public void mousePressed(MouseEvent e)
                {
                    App.getInstance().onPlayerShockwave(
                        (float)e.getX() / playArea.getWidth(),
                        (float)e.getY() / playArea.getHeight());
                }
                @Override public void mouseClicked(MouseEvent e)  {}
                @Override public void mouseReleased(MouseEvent e) {}
                @Override public void mouseEntered(MouseEvent e)  {}
                @Override public void mouseExited(MouseEvent e)   {}
            });

            _refreshTimer.schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                    playArea.repaint();
                    updateGameTimer();
                    updatePingLabels();
                }
            }, 0, 1000 / 30);
        }

        // ── Bottom buttons ────────────────────────────────────────────────────
        {
            boolean isHost = state.lobby != null &&
                    state.lobby.ownerCxId.equals(state.user.cxId);
            int btnY = screenRes.height / 2 + 310;
            int cx   = screenRes.width / 2;

            if (isHost)
            {
                // Host: "End Match" ends the game for everyone (same as leaving)
                JButton btnEnd = new JButton("End Match");
                btnEnd.setSize(160, 30);
                btnEnd.setLocation(cx - 250, btnY);
                panel.add(btnEnd);
                btnEnd.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        App.getInstance().onEndMatch();
                    }
                });

                // Host: clear splotches for all players mid-game
                JButton btnClear = new JButton("Clear Splotches");
                btnClear.setSize(160, 30);
                btnClear.setLocation(cx - 80, btnY);
                panel.add(btnClear);
                btnClear.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        App.getInstance().onClearSplotches();
                    }
                });

                // Host: Leave also ends the match so no orphaned game remains
                JButton btnLeave = new JButton("Leave");
                btnLeave.setSize(100, 30);
                btnLeave.setLocation(cx + 100, btnY);
                panel.add(btnLeave);
                btnLeave.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        App.getInstance().onEndMatch();
                    }
                });
            }
            else
            {
                JButton btnLeave = new JButton("Leave");
                btnLeave.setSize(200, 30);
                btnLeave.setLocation(cx - 100, btnY);
                panel.add(btnLeave);
                btnLeave.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        App.getInstance().onGameScreenClose();
                    }
                });
            }
        }
    }

    private void updatePingLabels()
    {
        State state = App.getInstance().state;
        if (state.lobby == null || _pingLabels.isEmpty()) return;
        int count = Math.min(_pingLabels.size(), state.lobby.members.size());
        for (int i = 0; i < count; i++)
        {
            int ap = state.lobby.members.get(i).activePing;
            String text = ap < 0 ? "..." : ap >= 999 ? "T/O" : ap + " ms";
            _pingLabels.get(i).setText(text);
        }
    }

    private void updateGameTimer()
    {
        if (_lblGameTimer == null) return;
        State state = App.getInstance().state;

        if (state.gameStartTime == 0)
        {
            _lblGameTimer.setText("Waiting for host...");
            _lblGameTimer.setForeground(Color.GRAY);
            return;
        }

        long elapsedSec = Math.max(0, (System.currentTimeMillis() - state.gameStartTime) / 1000);
        long remaining  = App.MATCH_DURATION_SEC - elapsedSec;

        if (remaining <= 0)
        {
            _lblGameTimer.setText("Match Over");
            _lblGameTimer.setForeground(Color.RED);
        }
        else if (elapsedSec >= App.COUNTDOWN_FROM_SEC)
        {
            _lblGameTimer.setText("Ending in " + remaining + "...");
            _lblGameTimer.setForeground(Color.RED);
        }
        else
        {
            _lblGameTimer.setText(String.format("Game Time: %d:%02d", elapsedSec / 60, elapsedSec % 60));
            _lblGameTimer.setForeground(Colors.TEXT_COLOR);
        }
    }
}
