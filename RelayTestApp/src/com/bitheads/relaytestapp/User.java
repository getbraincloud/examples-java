package com.bitheads.relaytestapp;

import java.awt.Point;

public class User
{
    public String profileId = "";
    public String name = "";
    public int colorIndex = 7;
    public boolean isReady = false;
    public Point pos = null;
    public boolean allowSendTo = true;

    public User(String in_profileId, String in_name, int in_colorIndex, boolean in_isReady)
    {
        profileId = in_profileId;
        name = in_name;
        colorIndex = in_colorIndex;
        isReady = in_isReady;
    }
}
