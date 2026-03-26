package com.bitheads.cursorparty;

import java.awt.Color;

public class Colors
{
    public static final int NUM_COLORS = 40;
    public static final Color BG_COLOR   = Color.decode("#282c34");
    public static final Color TEXT_COLOR = new Color(204, 204, 204);

    public static Color[] COLORS = new Color[]{
        // Row 1: vivid saturated (0-9)
        Color.decode("#FF3333"), // 0  vivid red
        Color.decode("#FF8800"), // 1  vivid orange
        Color.decode("#FFD700"), // 2  gold
        Color.decode("#88FF00"), // 3  vivid lime
        Color.decode("#00EE44"), // 4  vivid green
        Color.decode("#00DDDD"), // 5  vivid cyan
        Color.decode("#00AAFF"), // 6  vivid sky blue
        Color.decode("#3355FF"), // 7  vivid blue
        Color.decode("#AA00FF"), // 8  vivid purple
        Color.decode("#FF00BB"), // 9  vivid magenta
        // Row 2: vivid-medium / complementary (10-19)
        Color.decode("#FF5566"), // 10 coral
        Color.decode("#FFAA00"), // 11 amber
        Color.decode("#AADD00"), // 12 yellow-green
        Color.decode("#00FF88"), // 13 spring green
        Color.decode("#00FFCC"), // 14 aqua
        Color.decode("#0088FF"), // 15 azure
        Color.decode("#8833FF"), // 16 violet
        Color.decode("#FF44AA"), // 17 hot pink
        Color.decode("#77FF33"), // 18 chartreuse
        Color.decode("#FF6688"), // 19 rose
        // Row 3: pastel / light (20-29)
        Color.decode("#FF9999"), // 20 light red
        Color.decode("#FFCC88"), // 21 peach
        Color.decode("#FFFF88"), // 22 pale yellow
        Color.decode("#AAFFAA"), // 23 pale green
        Color.decode("#88FFEE"), // 24 pale cyan
        Color.decode("#AABBFF"), // 25 periwinkle
        Color.decode("#DDBBFF"), // 26 lavender
        Color.decode("#FFBBDD"), // 27 light pink
        Color.decode("#CCFFDD"), // 28 mint
        Color.decode("#FFEECC"), // 29 cream
        // Row 4: medium-depth / muted (30-39)
        Color.decode("#CC1133"), // 30 crimson
        Color.decode("#CC5500"), // 31 burnt orange
        Color.decode("#88AA00"), // 32 olive
        Color.decode("#228855"), // 33 forest green
        Color.decode("#009999"), // 34 deep teal
        Color.decode("#3366AA"), // 35 steel blue
        Color.decode("#7744CC"), // 36 medium purple
        Color.decode("#AA3366"), // 37 dark rose
        Color.decode("#AA6633"), // 38 brown
        Color.decode("#7788AA"), // 39 slate
    };
}
