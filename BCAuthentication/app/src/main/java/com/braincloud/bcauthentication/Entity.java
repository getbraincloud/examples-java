package com.braincloud.bcauthentication;

import android.util.Log;

import com.bitheads.braincloud.client.BrainCloudWrapper;
import com.bitheads.braincloud.client.IServerCallback;

import org.json.JSONException;
import org.json.JSONObject;

public class Entity {

    private String entityId;
    private String entityType;
    private String name;
    private String age;

    public Entity(){
        this.entityId = "";
        this.entityType = "player";
        this.name = "James Reece";
        this.age = "20";
    }

    public String getEntityId(){
        return entityId;
    }

    public String getEntityType(){
        return entityType;
    }

    public String getJsonData(String name, String age){
        JSONObject obj = new JSONObject();
        String jsonData;

        try {
            obj.put("name", name);
            obj.put("age", age);
        } catch (JSONException e) {
            e.printStackTrace();    //TODO
            Log.d("BC_LOG", "JSON DATA ERROR");
        }

        jsonData = obj.toString();

        return jsonData;
    }

    public String getJsonAcl(){
        JSONObject obj = new JSONObject();
        String jsonACL;

        try{
            obj.put("other", 2);
        } catch(JSONException e){
            e.printStackTrace();    //TODO
            Log.d("BC_LOG", "JSON ACL ERROR");
        }

        jsonACL = obj.toString();

        return jsonACL;
    }

}
