package com.bitheads.cursorparty;

import java.awt.geom.Point2D;

public class Splotch
{
    public Point2D pos;
    public int colorIndex;
    public long startTimeMs;

    public Splotch(Point2D in_pos, int in_colorIndex)
    {
        pos = in_pos;
        colorIndex = in_colorIndex;
        startTimeMs = System.currentTimeMillis();
    }
}
