package com.bitheads.cursorparty;

import java.awt.geom.Point2D;

public class Splotch
{
    public Point2D pos;
    public int colorIndex;
    public double angle;   // network-synced rotation (radians) so all clients match
    public long startTimeMs;

    public Splotch(Point2D in_pos, int in_colorIndex, double in_angle)
    {
        pos = in_pos;
        colorIndex = in_colorIndex;
        angle = in_angle;
        startTimeMs = System.currentTimeMillis();
    }
}
