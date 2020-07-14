package com.bitheads.relaytestapp;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

public class Lobby
{
    public String lobbyId;
    public String ownerId;
    public ArrayList<User> members = new ArrayList<User>();

    public Lobby(JSONObject lobbyJson, String in_lobbyId)
    {
        State state = App.getInstance().state;
        
        lobbyId = in_lobbyId;
        ownerId = lobbyJson.getString("owner");
        JSONArray jsonMembers = lobbyJson.getJSONArray("members");
        for (int i = 0; i < jsonMembers.length(); ++i)
        {
            JSONObject jsonMember = jsonMembers.getJSONObject(i);
            User user = new User(jsonMember.getString("profileId"), 
                                 jsonMember.getString("name"), 
                                 jsonMember.getJSONObject("extra").getInt("colorIndex"), 
                                 false);
            if (user.profileId.equals(state.user.profileId)) user.allowSendTo = false;   
            members.add(user);
        }
    }
}
