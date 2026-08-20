package com.bitheads.cursorparty;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

public class Lobby
{
    public String lobbyId;
    public String ownerCxId;
    public ArrayList<User> members = new ArrayList<User>();
    // This-lobby chat (via Lobby service SendSignal). state.lobby is rebuilt from
    // scratch on every RTT lobby event (joins/updates, not just chat), so the
    // caller must copy this field forward from the outgoing Lobby instance or
    // every MEMBER_JOIN/UPDATE silently wipes the chat history.
    public ArrayList<ChatMessage> chatMessages = new ArrayList<ChatMessage>();

    public Lobby(JSONObject lobbyJson, String in_lobbyId)
    {
        State state = App.getInstance().state;

        lobbyId = in_lobbyId;
        ownerCxId = lobbyJson.getString("ownerCxId");
        JSONArray jsonMembers = lobbyJson.getJSONArray("members");
        for (int i = 0; i < jsonMembers.length(); ++i)
        {
            JSONObject jsonMember = jsonMembers.getJSONObject(i);
            JSONObject extra = jsonMember.getJSONObject("extra");
            User user = new User(jsonMember.getString("cxId"),
                                 jsonMember.getString("name"),
                                 extra.getInt("colorIndex"),
                                 false);
            user.profileId = jsonMember.optString("profileId", "");
            if (user.cxId.equals(state.user.cxId)) user.allowSendTo = false;
            // Parse per-region ping data shared by this member via lobby extra
            if (extra.has("pings")) {
                JSONObject pingsJson = extra.getJSONObject("pings");
                for (String region : pingsJson.keySet()) {
                    user.pings.put(region, pingsJson.getInt(region));
                }
            }
            members.add(user);
        }
    }
}
