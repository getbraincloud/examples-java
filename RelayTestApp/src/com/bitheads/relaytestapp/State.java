package com.bitheads.relaytestapp;

import java.util.ArrayList;

import org.json.JSONObject;

public class State
{
    public Screen screen; // Current screen we are on
    public User user; // Our user
    public Lobby lobby; // Lobby with its members as received from brainCloud Lobby Service
    public JSONObject server; // Server info (IP, port, protocol, passcode)
    public ArrayList<Shockwave> shockwaves = new ArrayList<Shockwave>(); // Players' created shockwaves
    public boolean reliable = false;
    public boolean ordered = true;
}
