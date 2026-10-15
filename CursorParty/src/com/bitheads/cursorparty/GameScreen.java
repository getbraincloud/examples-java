package com.bitheads.cursorparty;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import javax.swing.AbstractButton;
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
    private static final int SPLOTCH_SIZE   = 64;   // rendered diameter (px)

    private static final int SIDEBAR_W = 220;
    private static final int PLAY_AREA_W = 800;
    private static final int PLAY_AREA_H = 600;
    private static final int HEADER_H = 56;
    private static final int PAD = 14;
    private static final int SIDEBAR_ROW_H = 18;

    private static final long COVERAGE_RECOMPUTE_MS = 250;
    private static final long AUTO_PAINT_INTERVAL_MS = 150; // matches the cpp/js/Godot cross-client standard for hold-to-paint

    // Matches cpp's TIMER_URGENT_SEC/TIMER_WARN_SEC thresholds for the countdown colour.
    private static final long TIMER_URGENT_SEC = 10;
    private static final long TIMER_WARN_SEC = 30;

    // Shared splotch art (white alpha-mask), loaded once and tinted opaque per player colour.
    private static BufferedImage _splatBase;
    private static final Map<Integer, BufferedImage> _splatTintCache = new HashMap<>();

    // Returns PaintSplatter1.png tinted opaque to the given colour (cached by RGB).
    private static BufferedImage getTintedSplat(Color color)
    {
        if (_splatBase == null)
        {
            try {
                _splatBase = ImageIO.read(GameScreen.class.getResource("/resources/PaintSplatter1.png"));
            } catch (IOException | IllegalArgumentException ex) {
                return null;
            }
        }
        if (_splatBase == null) return null;

        return _splatTintCache.computeIfAbsent(color.getRGB(), rgb -> {
            BufferedImage tinted = new BufferedImage(_splatBase.getWidth(), _splatBase.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D tg = tinted.createGraphics();
            tg.drawImage(_splatBase, 0, 0, null);
            tg.setComposite(AlphaComposite.SrcAtop);   // keep the mask's alpha, replace RGB with the player colour
            tg.setColor(new Color(rgb));
            tg.fillRect(0, 0, tinted.getWidth(), tinted.getHeight());
            tg.dispose();
            return tinted;
        });
    }

    private Timer  _refreshTimer = new Timer();
    private JLabel _lblGameTimer;
    private JLabel _lblPing;
    private JPanel _sidebarRowsPanel;

    private boolean _mouseDown = false;
    private long _lastAutoPaintMs = 0;

    // Small filled-circle avatar dot for the sidebar rank rows.
    private static class ColorDot extends JPanel
    {
        private final Color _color;

        ColorDot(Color color, int size)
        {
            _color = color;
            setPreferredSize(new Dimension(size, size));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(_color);
            g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
            g2.dispose();
        }
    }

    private static float coverageFor(State state, String cxId)
    {
        for (Coverage.CoverageEntry e : state.coverage)
            if (e.cxId.equals(cxId)) return e.coveragePct;
        return 0f;
    }

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
                Color splatColor = Colors.COLORS[s.colorIndex % Colors.NUM_COLORS];
                double cx = s.pos.getX() * getWidth();
                double cy = s.pos.getY() * getHeight();
                BufferedImage splat = getTintedSplat(splatColor);
                if (splat != null)
                {
                    // Opaque, player-coloured PaintSplatter1.png, rotated by the network-synced angle.
                    AffineTransform prev = g2.getTransform();
                    g2.translate(cx, cy);
                    g2.rotate(s.angle);
                    g2.drawImage(splat, -SPLOTCH_SIZE / 2, -SPLOTCH_SIZE / 2, SPLOTCH_SIZE, SPLOTCH_SIZE, null);
                    g2.setTransform(prev);
                }
                else
                {
                    // Fallback if the splat image failed to load
                    g2.setColor(splatColor);
                    g2.fillOval((int)cx - SPLOTCH_RADIUS, (int)cy - SPLOTCH_RADIUS, SPLOTCH_RADIUS * 2, SPLOTCH_RADIUS * 2);
                }
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
                    int px = (int)(member.pos.getX() * getWidth());
                    int py = (int)(member.pos.getY() * getHeight());
                    Color c = Colors.COLORS[member.colorIndex % Colors.NUM_COLORS];
                    drawCursor(g2, px, py, c);
                    // cpp labels each cursor with that player's live coverage %
                    g2.setFont(Theme.font(11));
                    g2.setColor(c);
                    g2.drawString(String.format("%.0f%%", coverageFor(state, member.cxId)), px + 12, py - 2);
                }
            }
        }
    }

    public GameScreen()
    {
        panel = new JPanel();
        panel.setLayout(null);
        panel.setBackground(Theme.BG);
        refreshUI();
    }

    @Override
    public void onStateChanged(State state)
    {
        refreshUI();
    }

    private static <T extends java.awt.Component> T at(T c, int x, int y, int w, int h)
    {
        c.setBounds(x, y, w, h);
        return c;
    }

    void refreshUI()
    {
        _refreshTimer.cancel();
        _refreshTimer = new Timer();
        panel.removeAll();
        panel.setBackground(Theme.BG);

        State state = App.getInstance().state;
        Dimension screenRes = App.getInstance().contentSize();

        // Board centred right of the sidebar, inside cpp's framed "Game" window.
        int areaW = screenRes.width - SIDEBAR_W;
        int playAreaX = SIDEBAR_W + (areaW - PLAY_AREA_W) / 2;
        int playAreaY = (screenRes.height - PLAY_AREA_H) / 2 + HEADER_H / 2;
        int headerY = playAreaY - HEADER_H;

        // ── Sidebar (left): live RANK / PLAYER | COVERAGE, one line per player ──
        JPanel sidebar = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g)
            {
                super.paintComponent(g);
                g.setColor(Theme.BORDER);
                g.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
            }
        };
        sidebar.setBounds(0, 0, SIDEBAR_W, screenRes.height);
        sidebar.setBackground(Theme.WINDOW);
        {
            JPanel header = new JPanel(null);
            header.setBackground(new Color(0x29, 0x2B, 0x3A));
            sidebar.add(at(header, 8, 10, SIDEBAR_W - 16, 18));
            header.add(at(Theme.label("RANK / PLAYER", Theme.font(11), Theme.TEXT), 6, 1, 120, 16));
            header.add(at(Theme.label("COVERAGE", Theme.font(11), Theme.TEXT), SIDEBAR_W - 84, 1, 70, 16));

            _sidebarRowsPanel = new JPanel(null);
            _sidebarRowsPanel.setOpaque(false);
            sidebar.add(at(_sidebarRowsPanel, 8, 30, SIDEBAR_W - 16, 300));
            rebuildSidebarRows();

            sidebar.add(buildDebugPanel(state, screenRes.height));
        }

        // ── Game window: timer, ping and Exit Match above the board ────────────
        _lblGameTimer = Theme.label("", Theme.font(13), Theme.TEXT);
        panel.add(at(_lblGameTimer, playAreaX, headerY, 200, 18));

        _lblPing = Theme.label("", Theme.font(11), Theme.DIM);
        panel.add(at(_lblPing, playAreaX, headerY + 22, 200, 16));

        Theme.FlatButton btnExit = Theme.button("Exit Match");
        btnExit.setFont(Theme.font(11));
        panel.add(at(btnExit, playAreaX + PLAY_AREA_W - 92, headerY - 2, 92, 22));
        btnExit.addActionListener(e -> {
            boolean isHost = state.lobby != null && state.lobby.ownerCxId.equals(state.user.cxId);
            if (isHost) App.getInstance().onEndMatch();
            else App.getInstance().onGameScreenClose();
        });
        updateGameTimer();
        updatePing();

        // ── Play area ─────────────────────────────────────────────────────────
        JPanel playArea = new PlayArea();
        playArea.setBackground(Theme.BOARD);
        panel.add(at(playArea, playAreaX, playAreaY, PLAY_AREA_W, PLAY_AREA_H));

        Theme.Box frame = Theme.box();
        panel.add(at(frame, playAreaX - PAD, headerY - PAD, PLAY_AREA_W + PAD * 2, PLAY_AREA_H + HEADER_H + PAD * 2));
        panel.add(sidebar);

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
                _mouseDown = true;
                _lastAutoPaintMs = System.currentTimeMillis();
                App.getInstance().onPlayerShockwave(
                    (float)e.getX() / playArea.getWidth(),
                    (float)e.getY() / playArea.getHeight());
            }
            @Override
            public void mouseReleased(MouseEvent e)
            {
                _mouseDown = false;
            }
            @Override public void mouseClicked(MouseEvent e)  {}
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
                updatePing();
                tickAutoPaint();
                tickCoverageRecompute();
            }
        }, 0, 1000 / 30);
    }

    // cpp's debug panel, kept at the foot of the sidebar: relay options for movement
    // (channel / reliable / ordered), the shockwave send mask, round and lobby.
    private JPanel buildDebugPanel(State state, int screenH)
    {
        int pw = SIDEBAR_W - 16;
        int maskRows = state.lobby.members.size();
        int ph = 236 + maskRows * 20;
        Theme.Box box = Theme.box(new Color(0x1F, 0x21, 0x2E));
        box.setBounds(8, screenH - ph - 84, pw, ph);

        int y = 8;
        if (!state.lobbyRegion.isEmpty())
        {
            box.add(at(Theme.label("Region: " + state.lobbyRegion, Theme.font(11), Theme.DIM), 10, y, pw - 20, 16));
            y += 16;
        }
        box.add(at(Theme.label("Reliable options", Theme.font(11), Theme.TEXT), 10, y, pw - 20, 16));
        box.add(at(Theme.label("Only affect position", Theme.font(11), Theme.DIM), 10, y + 14, pw - 20, 16));
        y += 34;
        box.add(at(Theme.label("Channel", Theme.font(11), Theme.TEXT), 10, y, 60, 16));
        javax.swing.ButtonGroup group = new javax.swing.ButtonGroup();
        for (int i = 0; i < 4; i++)
        {
            final int ch = i;
            javax.swing.JRadioButton rb = new javax.swing.JRadioButton(String.valueOf(i), state.channel == i);
            rb.setOpaque(false);
            rb.setForeground(Theme.TEXT);
            rb.setFont(Theme.font(11));
            rb.addActionListener(e -> state.channel = ch);
            group.add(rb);
            box.add(at(rb, 66 + (i % 2) * 56, y - 2 + (i / 2) * 20, 50, 20));
        }
        y += 44;

        JCheckBox chkReliable = new JCheckBox("Reliable", state.reliable);
        chkReliable.setOpaque(false);
        chkReliable.addChangeListener(e -> state.reliable = ((AbstractButton) e.getSource()).getModel().isSelected());
        box.add(at(chkReliable, 6, y, pw - 12, 20));
        y += 22;
        JCheckBox chkOrdered = new JCheckBox("Ordered", state.ordered);
        chkOrdered.setOpaque(false);
        chkOrdered.addChangeListener(e -> state.ordered = ((AbstractButton) e.getSource()).getModel().isSelected());
        box.add(at(chkOrdered, 6, y, pw - 12, 20));
        y += 26;

        box.add(at(Theme.label("Shockwaves to", Theme.font(11), Theme.TEXT), 10, y, pw - 20, 16));
        y += 18;
        for (User member : state.lobby.members)
        {
            JCheckBox chk = new JCheckBox(member.name, member.allowSendTo);
            chk.setOpaque(false);
            chk.setFont(Theme.font(11));
            chk.setForeground(Colors.COLORS[member.colorIndex % Colors.NUM_COLORS]);
            chk.addChangeListener(e -> member.allowSendTo = ((AbstractButton) e.getSource()).getModel().isSelected());
            box.add(at(chk, 6, y, pw - 12, 20));
            y += 20;
        }
        y += 6;
        box.add(at(Theme.label("Round: " + state.roundNumber, Theme.font(11), Theme.DIM), 10, y, pw - 20, 16));
        box.add(at(Theme.label("Lobby:", Theme.font(11), Theme.DIM), 10, y + 16, pw - 20, 16));
        box.add(at(Theme.label(state.lobby.lobbyId, Theme.font(11), Theme.DIM), 10, y + 30, pw - 20, 16));
        return box;
    }

    // Repeatedly fires onPlayerShockwave while the mouse is held down, throttled
    // to AUTO_PAINT_INTERVAL_MS — matches the cpp/js/Godot "hold to paint" feel.
    // The initial paint-on-press already happens in mousePressed (edge-triggered).
    private void tickAutoPaint()
    {
        if (!_mouseDown) return;
        long now = System.currentTimeMillis();
        if (now - _lastAutoPaintMs < AUTO_PAINT_INTERVAL_MS) return;
        _lastAutoPaintMs = now;

        State state = App.getInstance().state;
        if (state.user.pos == null) return;
        float px = (float) state.user.pos.getX();
        float py = (float) state.user.pos.getY();
        if (px < 0f || px > 1f || py < 0f || py > 1f) return; // cursor has drifted outside the canvas
        App.getInstance().onPlayerShockwave(px, py);
    }

    private void updatePing()
    {
        State state = App.getInstance().state;
        if (_lblPing == null || state.lobby == null) return;

        int ping = -1;
        for (User m : state.lobby.members)
        {
            if (m.cxId.equals(state.user.cxId)) { ping = m.activePing; break; }
        }

        String text = ping < 0 ? "Ping: ..." : ping >= 999 ? "Ping: T/O" : "Ping: " + ping + " ms";
        Color color = ping < 0 ? new Color(140, 150, 160)
                : ping < 100 ? new Color(0x66, 0xEE, 0x88)
                : ping < 200 ? new Color(0xEE, 0xCC, 0x44)
                : new Color(0xEE, 0x66, 0x66);
        _lblPing.setText("<html><font color='" + hex(color) + "'>●</font> " + text + "</html>");
    }

    private static String hex(Color c)
    {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    // Recomputes state.coverage only when the splotch set has actually changed
    // (state.splotchGeneration) and at least COVERAGE_RECOMPUTE_MS has elapsed
    // since the last compute — mirrors the cpp reference client's debounced,
    // recompute-on-change coverage tick rather than a fixed-interval poll.
    // Rebuilds the sidebar rows in the same pass since rank order can shift.
    private void tickCoverageRecompute()
    {
        State state = App.getInstance().state;
        if (state.lobby == null) return;
        if (state.coverageComputedGen == state.splotchGeneration) return;
        long now = System.currentTimeMillis();
        if (now - state.coverageComputedAtMs < COVERAGE_RECOMPUTE_MS) return;

        state.coverageComputedAtMs = now;
        state.coverageComputedGen = state.splotchGeneration;
        state.coverage = new ArrayList<>(Coverage.computeCoverage(state.splotches, state.lobby.members));
        rebuildSidebarRows();
    }

    private void rebuildSidebarRows()
    {
        State state = App.getInstance().state;
        if (_sidebarRowsPanel == null || state.lobby == null) return;

        _sidebarRowsPanel.removeAll();
        List<Coverage.CoverageEntry> entries = !state.coverage.isEmpty()
                ? state.coverage
                : Coverage.computeCoverage(state.splotches, state.lobby.members);

        int y = 0;
        int w = SIDEBAR_W - 16;
        for (Coverage.CoverageEntry entry : entries)
        {
            boolean isMe = entry.cxId.equals(state.user.cxId);
            String name = "?";
            for (User m : state.lobby.members)
            {
                if (m.cxId.equals(entry.cxId)) { name = m.name; break; }
            }
            Color textColor = isMe ? Theme.GOOD : Theme.TEXT;

            JPanel row = new JPanel(null);
            row.setOpaque(isMe);
            row.setBackground(Theme.ROW_ME);
            row.setBounds(0, y, w, SIDEBAR_ROW_H);

            row.add(at(Theme.label("#" + entry.rank, Theme.font(11), Theme.rankColor(entry.rank)), 6, 0, 26, SIDEBAR_ROW_H));
            row.add(at(new ColorDot(Colors.COLORS[entry.colorIndex % Colors.NUM_COLORS], 8), 32, (SIDEBAR_ROW_H - 8) / 2, 8, 8));
            JLabel lblName = Theme.label(name, Theme.font(11), textColor);
            int nameW = Math.min(lblName.getPreferredSize().width, w - 76 - 46 - (isMe ? 34 : 0));
            row.add(at(lblName, 46, 0, nameW, SIDEBAR_ROW_H));
            if (isMe)
            {
                JLabel you = Theme.badge("YOU", Theme.FRAME_HOVER, Theme.TEXT);
                row.add(at(you, 46 + nameW + 6, 1, you.getWidth(), SIDEBAR_ROW_H - 2));
            }
            row.add(at(Theme.label(String.format("%.0f%%", entry.coveragePct), Theme.font(11), textColor), w - 76, 0, 60, SIDEBAR_ROW_H));

            _sidebarRowsPanel.add(row);
            y += SIDEBAR_ROW_H;
        }

        _sidebarRowsPanel.revalidate();
        _sidebarRowsPanel.repaint();
    }

    private void updateGameTimer()
    {
        if (_lblGameTimer == null) return;
        State state = App.getInstance().state;

        if (state.gameStartTime == 0)
        {
            _lblGameTimer.setText("Waiting...");
            _lblGameTimer.setForeground(Color.GRAY);
            return;
        }

        long elapsedSec = Math.max(0, (System.currentTimeMillis() - state.gameStartTime) / 1000);
        long remaining  = Math.max(0, App.MATCH_DURATION_SEC - elapsedSec);
        // The host now broadcasts match_result at MATCH_DURATION_SEC but doesn't actually
        // call endMatch() until RESULT_GRACE_SEC later — gameplay (and this timer) stays
        // live for that extra window instead of the old single 90s cutoff.
        long totalSec = App.MATCH_DURATION_SEC + App.RESULT_GRACE_SEC;

        if (elapsedSec >= totalSec)
        {
            _lblGameTimer.setText("Match Over");
            _lblGameTimer.setForeground(Color.RED);
        }
        else if (elapsedSec >= App.MATCH_DURATION_SEC)
        {
            _lblGameTimer.setText("Finalizing...");
            _lblGameTimer.setForeground(Color.RED);
        }
        else
        {
            _lblGameTimer.setText(String.format("%d:%02d", remaining / 60, remaining % 60));
            _lblGameTimer.setForeground(
                    remaining <= TIMER_URGENT_SEC ? new Color(0xEE, 0x55, 0x55)
                    : remaining <= TIMER_WARN_SEC ? new Color(0xEE, 0xAA, 0x44)
                    : Theme.TEXT);
        }
    }
}
