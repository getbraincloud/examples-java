package com.bitheads.relaytestapp;

import java.awt.Color;
import java.awt.geom.Point2D;

public class Shockwave
{
    static int shockwaveNextId = 0;

    public Point2D pos;
    public Color color;
    public int id = shockwaveNextId++;
    public int time = 0;

    public Shockwave(Point2D in_pos, Color in_color)
    {
        pos = in_pos;
        color = in_color;
    }
}
