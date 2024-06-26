package com.bitheads.bcdemo;

import android.content.Context;
import android.os.CountDownTimer;
import android.util.Log;

import com.bitheads.braincloud.client.BrainCloudWrapperAndroid;
import com.bitheads.braincloud.client.IServerCallback;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Handles brainCloud requests/data.
 */
public class BrainCloudManager {
    private static BrainCloudManager instance;

    private static BrainCloudWrapperAndroid brainCloudWrapper;

    private BrainCloudManager(){

    }

    public static synchronized BrainCloudManager getInstance(Context context){
        if(instance == null){
            Context appContext = context.getApplicationContext();
            instance = new BrainCloudManager();

            brainCloudWrapper = new BrainCloudWrapperAndroid();

            /*
             *  TODO:  Initialize with your app's IDs.
             *         Found in the brainCloud portal (Design > Core App Info > Application IDs)
             */
            brainCloudWrapper.initialize(
                    appContext,
                    "",
                    "",
                    "2.0.0",
                    "https://api.internal.braincloudservers.com/dispatcherv2"
            );

            // Run callbacks
            new CountDownTimer(10000, 1000) {
                public void onTick(long millisUntilFinished) {
                    brainCloudWrapper.runCallbacks();
                }
                public void onFinish() {
                    start(); // Restart the timer
                }
            }.start();
        }

        return instance;
    }

    public BrainCloudWrapperAndroid getBrainCloudWrapper(){
        return brainCloudWrapper;
    }

    public String getBrainCloudClientVersion() {
        return brainCloudWrapper.getClient().getBrainCloudVersion();
    }

    /**
     * Attempt to authenticate with the selected authentication type
     * @param authType the type of authentication that will be attempted
     * @param user username/email (used for non-anonymous authentication)
     * @param pass password (used for non-anonymous authentication)
     * @param callback proceed to BrainCloudMenu on success or display error message on fail
     */
    public void authenticate(String authType, String user, String pass, IServerCallback callback){
        switch(authType){
            case "Anonymous":
                brainCloudWrapper.authenticateAnonymous(callback);
                break;
            case "Universal":
                brainCloudWrapper.authenticateUniversal(user, pass, true, callback);
                break;
            case "Email":
                brainCloudWrapper.authenticateEmailPassword(user, pass, true, callback);
                break;
        }
    }

    /**
     * Attach the new identity to the currently authenticated profile
     * @param idType determines if the new identity is Email or Universal
     * @param user username/email of the new identity
     * @param pass password of the new identity
     * @param callback callback is passed from the ExploreIdentity class
     */
    public void attachIdentity(String idType, String user, String pass, IServerCallback callback){
        if(idType.equals("Email")){
            brainCloudWrapper.getIdentityService().attachEmailIdentity(user, pass, callback);
        }
        else{
            brainCloudWrapper.getIdentityService().attachUniversalIdentity(user, pass, callback);
        }
    }

    /**
     * Merge the existing identity with the currently authenticated profile
     * @param idType determines if the identity is Email or Universal
     * @param user username/email of the identity
     * @param pass password of the identity
     * @param callback callback is passed from the ExploreIdentity class
     */
    public void mergeIdentity(String idType, String user, String pass, IServerCallback callback){
        if(idType.equals("Email")){
            brainCloudWrapper.getIdentityService().mergeEmailIdentity(user, pass, callback);
        }
        else{
            brainCloudWrapper.getIdentityService().mergeUniversalIdentity(user, pass, callback);
        }
    }

    /**
     * Search for existing User Entity
     * @param callback callback is passed from the ExploreIdentity class
     */
    public void getEntityPage(IServerCallback callback){
        JSONObject pagination = new JSONObject();
        JSONObject searchCriteria = new JSONObject();
        JSONObject sortCriteria = new JSONObject();
        JSONObject jsonContext = new JSONObject();

        try {
            pagination.put("rowsPerPage", 50);
            pagination.put("pageNumber", 1);

            searchCriteria.put("entityType", "player");

            sortCriteria.put("createdAt", 1);
            sortCriteria.put("updatedAt", -1);

            jsonContext.put("pagination", pagination);
            jsonContext.put("searchCriteria", searchCriteria);
            jsonContext.put("sortCriteria", sortCriteria);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.d("JSON Error", "getPage jsonContext was not created");
        }

        String context = jsonContext.toString();

        brainCloudWrapper.getEntityService().getPage(context, callback);
    }

    /**
     * Create User Entity
     * @param entity Object containing User Entity data (type, name, age, etc.)
     * @param callback callback is passed from the ExploreEntity class
     */
    public void createEntity(Entity entity, IServerCallback callback){
        brainCloudWrapper.getEntityService().createEntity(
                entity.getEntityType(),
                entity.getJsonData(),
                entity.getJsonAcl(),
                callback
        );
    }

    /**
     * Update existing User Entity with new data
     * @param entity Object containing User Entity data (id, type, name, etc.)
     * @param callback callback is passed from the ExploreEntity class
     */
    public void updateEntity(Entity entity, IServerCallback callback){
        brainCloudWrapper.getEntityService().updateEntity(
                entity.getEntityId(),
                entity.getEntityType(),
                entity.getJsonData(),
                entity.getJsonAcl(),
                -1,
                callback
        );
    }

    /**
     * Delete User Entity
     * @param entity Object containing User Entity Data (id)
     * @param callback callback is passed from the ExploreEntity class
     */
    public void deleteEntity(Entity entity, IServerCallback callback){
        brainCloudWrapper.getEntityService().deleteEntity(
                entity.getEntityId(),
                -1,
                callback
        );
    }

    /**
     * Retrieve player's current XP
     * @param callback callback is passed from the ExploreXP class
     */
    public void getXP(IServerCallback callback){
        brainCloudWrapper.getPlayerStateService().readUserState(callback);
    }

    /**
     * Increase player's experience points
     * @param incrementAmount value to increase points by
     * @param callback callback is passed from the ExploreXP class
     */
    public void incrementXP(int incrementAmount, IServerCallback callback){
        brainCloudWrapper.getPlayerStatisticsService().incrementExperiencePoints(incrementAmount, callback);
    }

    /**
     * Retrieve player's currency data
     * @param callback callback is passed from the ExploreCurrency class
     */
    public void getCurrency(IServerCallback callback){
        brainCloudWrapper.getVirtualCurrencyService().getCurrency("gems", callback);
    }

    /**
     * Run a provided Cloud Code script
     * @param scriptName name of existing script
     * @param scriptData parameter data to be used by the script
     * @param callback callback is passed from the ExploreCurrency class
     */
    public void runCloudCodeScript(String scriptName, String scriptData, IServerCallback callback){
        brainCloudWrapper.getScriptService().runScript(scriptName, scriptData, callback);
    }

    /**
     * Retrieve user/global statistics from brainCloud
     * @param userStat determines which statistics to get (true: user, false: global)
     * @param callback callback is passed from the ExploreStats class
     */
    public void getStatistics(Boolean userStat, IServerCallback callback){
        if(userStat){
            brainCloudWrapper.getPlayerStatisticsService().readAllUserStats(callback);
        }
        else{
            brainCloudWrapper.getGlobalStatisticsService().readAllGlobalStats(callback);
        }
    }

    /**
     * Increments the provided statistic
     * @param userStat determines if the statistic is global or user
     * @param jsonData JSON formatted String containing statistic name and increment data
     * @param callback callback is passed from the ExploreStats class
     */
    public void incrementStatistics(Boolean userStat, String jsonData, IServerCallback callback){
        if(userStat){
            brainCloudWrapper.getPlayerStatisticsService().incrementUserStats(jsonData, callback);
        }
        else{
            brainCloudWrapper.getGlobalStatisticsService().incrementGlobalStats(jsonData, callback);
        }
    }


}
