package com.bitheads.cursorparty;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.metal.MetalLookAndFeel;

// Dark ImGui-style palette + flat widgets so the Swing client matches the cpp RelayTestApp.
final class Theme
{
    static final Color BG = new Color(0x0D, 0x10, 0x16);
    static final Color WINDOW = new Color(0x1A, 0x1A, 0x26);   // ImGuiCol_WindowBg
    static final Color FRAME = new Color(0x26, 0x28, 0x36);    // inputs, combos, plain buttons
    static final Color FRAME_HOVER = new Color(0x33, 0x36, 0x48);
    static final Color BORDER = new Color(0x34, 0x37, 0x46);
    static final Color BOARD = new Color(0x23, 0x25, 0x30);    // play-area fill
    static final Color ROW_ME = new Color(0x2E, 0x33, 0x45);
    static final Color TEXT = new Color(0xE6, 0xE8, 0xEE);
    static final Color DIM = new Color(0x8C, 0x91, 0x9E);
    static final Color ACCENT = new Color(0x5C, 0x66, 0xE6);   // Find / Create Lobby
    static final Color TAB = new Color(0x3F, 0x63, 0xD6);      // active tab
    static final Color PILL_ON = new Color(0x2E, 0x7D, 0x4F);  // active filter pill
    static final Color GOOD = new Color(0x5A, 0xE6, 0x73);
    static final Color WARN = new Color(0xFF, 0xC4, 0x4D);
    static final Color BAD = new Color(0xFF, 0x5C, 0x5C);
    static final Color[] RANK = { new Color(0xFF, 0xD6, 0x40), new Color(0xEB, 0xEB, 0xEB), new Color(0xE6, 0x8C, 0x40) };

    static Font font(int size) { return new Font("SansSerif", Font.PLAIN, size); }
    static Font bold(int size) { return new Font("SansSerif", Font.BOLD, size); }

    static Color rankColor(int rank)
    {
        return rank >= 1 && rank <= 3 ? RANK[rank - 1] : TEXT;
    }

    // Swing's default mac look ignores button/combo colours, so switch to Metal and darken it.
    static void install()
    {
        try {
            UIManager.setLookAndFeel(new MetalLookAndFeel());
        } catch (Exception ignored) {}
        Object[][] colors = {
            { "Panel.background", WINDOW }, { "Label.foreground", TEXT },
            { "TextField.background", FRAME }, { "TextField.foreground", TEXT }, { "TextField.caretForeground", TEXT },
            { "TextField.inactiveForeground", DIM },
            { "PasswordField.background", FRAME }, { "PasswordField.foreground", TEXT }, { "PasswordField.caretForeground", TEXT },
            { "TextArea.background", WINDOW }, { "TextArea.foreground", TEXT },
            { "ComboBox.background", FRAME }, { "ComboBox.foreground", TEXT },
            { "ComboBox.selectionBackground", TAB }, { "ComboBox.selectionForeground", TEXT },
            { "ComboBox.buttonBackground", FRAME }, { "ComboBox.buttonDarkShadow", FRAME },
            { "ComboBox.buttonHighlight", FRAME }, { "ComboBox.buttonShadow", FRAME },
            { "CheckBox.background", WINDOW }, { "CheckBox.foreground", TEXT },
            { "ScrollPane.background", WINDOW }, { "Viewport.background", WINDOW },
            { "ScrollBar.background", WINDOW }, { "ScrollBar.thumb", FRAME_HOVER }, { "ScrollBar.track", WINDOW },
            { "ScrollBar.thumbShadow", FRAME_HOVER }, { "ScrollBar.thumbHighlight", FRAME_HOVER },
            { "PopupMenu.background", WINDOW }, { "PopupMenu.border", null },
            { "ToolTip.background", WINDOW }, { "ToolTip.foreground", TEXT },
            { "OptionPane.background", WINDOW }, { "OptionPane.messageForeground", TEXT },
        };
        for (Object[] c : colors)
            UIManager.put(c[0], c[1] instanceof Color ? new ColorUIResource((Color) c[1]) : c[1]);
        UIManager.put("TextField.border", BorderFactory.createEmptyBorder(3, 6, 3, 6));
        UIManager.put("PasswordField.border", BorderFactory.createEmptyBorder(3, 6, 3, 6));
        UIManager.put("ComboBox.border", BorderFactory.createEmptyBorder());
        UIManager.put("ScrollPane.border", BorderFactory.createEmptyBorder());
        UIManager.put("CheckBox.icon", new ToggleIcon(false));
        UIManager.put("RadioButton.icon", new ToggleIcon(true));
        UIManager.put("RadioButton.background", WINDOW);
        UIManager.put("RadioButton.foreground", TEXT);
        UIManager.put("Button.font", font(12));
        UIManager.put("Label.font", font(12));
        UIManager.put("ComboBox.font", font(12));
        UIManager.put("CheckBox.font", font(12));
        UIManager.put("TextField.font", font(12));
        UIManager.put("TextArea.font", font(12));
    }

    // ImGui-style check/radio: dark frame, accent fill when selected.
    static class ToggleIcon implements javax.swing.Icon
    {
        private final boolean _round;

        ToggleIcon(boolean round) { _round = round; }

        @Override public int getIconWidth() { return 16; }
        @Override public int getIconHeight() { return 16; }

        @Override
        public void paintIcon(java.awt.Component c, Graphics g, int x, int y)
        {
            javax.swing.AbstractButton b = (javax.swing.AbstractButton) c;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(b.getModel().isRollover() ? FRAME_HOVER : FRAME);
            if (_round) g2.fillOval(x, y, 15, 15); else g2.fillRoundRect(x, y, 15, 15, 4, 4);
            if (b.isSelected())
            {
                g2.setColor(ACCENT);
                if (_round) g2.fillOval(x + 4, y + 4, 7, 7); else g2.fillRoundRect(x + 3, y + 3, 9, 9, 2, 2);
            }
            g2.dispose();
        }
    }

    // Flat rounded button; `selected` drives the pill/tab look.
    static class FlatButton extends JButton
    {
        Color base;
        Color on;
        boolean selected;
        private boolean _hover;

        FlatButton(String text, Color base, Color on)
        {
            super(text);
            this.base = base;
            this.on = on;
            setUI(new BasicButtonUI());
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setForeground(TEXT);
            setFont(font(12));
            setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { _hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { _hover = false; repaint(); }
            });
        }

        void setSelectedPill(boolean value)
        {
            selected = value;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color c = selected && on != null ? on : base;
            if (!isEnabled()) c = c.darker();
            else if (_hover) c = c.brighter();
            g2.setColor(c);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static FlatButton button(String text) { return new FlatButton(text, FRAME, null); }
    static FlatButton accentButton(String text) { return new FlatButton(text, ACCENT, null); }
    static FlatButton tab(String text) { return new FlatButton(text, FRAME, TAB); }
    static FlatButton pill(String text) { return new FlatButton(text, FRAME, PILL_ON); }

    // Rounded, bordered ImGui-style window.
    static class Box extends JPanel
    {
        private final Color _fill;

        Box(Color fill)
        {
            super(null);
            _fill = fill;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(_fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            g2.setColor(BORDER);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            g2.dispose();
        }
    }

    static Box box() { return new Box(WINDOW); }
    static Box box(Color fill) { return new Box(fill); }

    static JLabel label(String text, Font font, Color color)
    {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    // Small rounded badge (YOU / HOST).
    static JLabel badge(String text, Color bg, Color fg)
    {
        JLabel l = new JLabel(text, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g)
            {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setFont(font(11));
        l.setForeground(fg);
        l.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 5));
        l.setSize(l.getPreferredSize());
        return l;
    }

    static JPanel separator(int x, int y, int w)
    {
        JPanel sep = new JPanel();
        sep.setBounds(x, y, w, 1);
        sep.setBackground(BORDER);
        return sep;
    }

    private Theme() {}
}
