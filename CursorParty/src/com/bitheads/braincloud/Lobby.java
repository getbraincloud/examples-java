package com.bitheads.braincloud;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

public class Lobby
{
    public String lobbyId;
    public String ownerCxId;
    public ArrayList<User> members = new ArrayList<User>();

    public Lobby(JSONObject lobbyJson, String in_lobbyId)
    {
        State state = App.getInstance().state;
        
        lobbyId = in_lobbyId;
        ownerCxId = lobbyJson.getString("ownerCxId");
        JSONArray jsonMembers = lobbyJson.getJSONArray("members");
        for (int i = 0; i < jsonMembers.length(); ++i)
        {
            JSONObject jsonMember = jsonMembers.getJSONObject(i);
            User user = new User(jsonMember.getString("cxId"), 
                                 jsonMember.getString("name"), 
                                 jsonMember.getJSONObject("extra").getInt("colorIndex"), 
                                 false);
            if (user.cxId.equals(state.user.cxId)) user.allowSendTo = false;   
            members.add(user);
        }
    }
}
