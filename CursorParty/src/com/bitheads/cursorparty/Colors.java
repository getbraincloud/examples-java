package com.bitheads.cursorparty;

import java.awt.Color;

public class Colors
{
    public static final int NUM_COLORS = 40;
    public static final Color BG_COLOR   = Color.decode("#282c34");
    public static final Color TEXT_COLOR = new Color(204, 204, 204);

    public static Color[] COLORS = new Color[]{
        // Row 1: Vivid saturated (0-9)
        Color.decode("#FF3333"), // 0  vivid red
        Color.decode("#FF8C00"), // 1  orange
        Color.decode("#FFD700"), // 2  gold
        Color.decode("#AAFF00"), // 3  lime
        Color.decode("#00CC44"), // 4  green
        Color.decode("#00FFFF"), // 5  cyan
        Color.decode("#00AAFF"), // 6  sky blue
        Color.decode("#3355FF"), // 7  blue
        Color.decode("#AA00FF"), // 8  purple
        Color.decode("#FF00FF"), // 9  magenta

        // Row 2: Vivid-medium (10-19)
        Color.decode("#FF6655"), // 10 coral
        Color.decode("#FFAA00"), // 11 amber
        Color.decode("#AAFF33"), // 12 yellow-green
        Color.decode("#00FF88"), // 13 spring green
        Color.decode("#00FFCC"), // 14 aqua
        Color.decode("#0099FF"), // 15 azure
        Color.decode("#8855FF"), // 16 violet
        Color.decode("#FF33BB"), // 17 hot pink
        Color.decode("#CCFF00"), // 18 chartreuse
        Color.decode("#FF0066"), // 19 rose

        // Row 3: Pastel/light (20-29)
        Color.decode("#FFAAAA"), // 20 light red
        Color.decode("#FFDDAA"), // 21 peach
        Color.decode("#FFFFAA"), // 22 pale yellow
        Color.decode("#AAFFAA"), // 23 pale green
        Color.decode("#AAFFFF"), // 24 pale cyan
        Color.decode("#AAAAFF"), // 25 periwinkle
        Color.decode("#DDAAFF"), // 26 lavender
        Color.decode("#FFAAEE"), // 27 light pink
        Color.decode("#AAFFDD"), // 28 mint
        Color.decode("#FFFFEE"), // 29 cream

        // Row 4: Medium-depth/muted (30-39)
        Color.decode("#AA0000"), // 30 crimson
        Color.decode("#BB5500"), // 31 burnt orange
        Color.decode("#777700"), // 32 olive
        Color.decode("#006622"), // 33 forest green
        Color.decode("#006666"), // 34 deep teal
        Color.decode("#334488"), // 35 steel blue
        Color.decode("#6600AA"), // 36 medium purple
        Color.decode("#AA0055"), // 37 dark rose
        Color.decode("#664422"), // 38 brown
        Color.decode("#445566"), // 39 slate
    };
}
