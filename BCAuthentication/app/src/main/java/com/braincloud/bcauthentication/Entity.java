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
        this.entityType = "player";
        this.name = "James Reece";
        this.age = "50";
    }

    public String getEntityId(){
        return entityId;
    }

    public void setEntityId(String entityId){
        this.entityId = entityId;
    }

    public String getEntityType(){
        return entityType;
    }

    public void setEntityType(String entityType){
        this.entityType = entityType;
    }

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getAge(){
        return age;
    }

    public void setAge(String age){
        this.age = age;
    }

    /**
     * Create JSON context
     * @return String - JSON context
     */
    public String getJsonData(){
        JSONObject obj = new JSONObject();
        String jsonData;

        try {
            obj.put("name", name);
            obj.put("age", age);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.d("BC_LOG", "JSON DATA ERROR");
        }

        jsonData = obj.toString();

        return jsonData;
    }

    /**
     * Create User Entity ACL
     * @return String - JSON ACL
     */
    public String getJsonAcl(){
        JSONObject obj = new JSONObject();
        String jsonACL;

        try{
            obj.put("other", 2);
        } catch(JSONException e){
            e.printStackTrace();
            Log.d("BC_LOG", "JSON ACL ERROR");
        }

        jsonACL = obj.toString();

        return jsonACL;
    }

}
