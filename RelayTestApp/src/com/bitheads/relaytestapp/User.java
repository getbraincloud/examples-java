package com.bitheads.relaytestapp;

import java.awt.Point;
import java.awt.geom.Point2D;

public class User
{
    public String cxId = "";
    public String name = "";
    public int colorIndex = 7;
    public boolean isReady = false;
    public Point2D pos = null;
    public boolean allowSendTo = true;

    public User(String in_cxId, String in_name, int in_colorIndex, boolean in_isReady)
    {
        cxId = in_cxId;
        name = in_name;
        colorIndex = in_colorIndex;
        isReady = in_isReady;
    }
}
