package com.braincloud.bcauthentication;

import android.content.Context;
import android.os.CountDownTimer;
import android.util.Log;

import com.bitheads.braincloud.client.BrainCloudWrapper;
import com.bitheads.braincloud.client.IServerCallback;

import org.json.JSONException;
import org.json.JSONObject;

public class BCClient {

    private BrainCloudWrapper _bc;

    public BCClient(){
        _bc = new BrainCloudWrapper();
        _bc.initialize("26205",
                "1f794474-24a1-4dca-9605-9415a798a036",
                "1.0.0",
                "https://api.internal.braincloudservers.com/dispatcherv2");

        // Run callbacks
        new CountDownTimer(10000, 1000) {
            public void onTick(long millisUntilFinished) {
                _bc.runCallbacks();
            }
            public void onFinish() {
                start(); // Restart the timer
            }
        }.start();
    }

    public String getVersion(){
        return _bc.getClient().getBrainCloudVersion();
    }

    public BrainCloudWrapper getWrapper(){
        return _bc;
    }

    public void setApplicationContext(Context appContext){
        _bc.setContext(appContext);
    }

    /**
     * Attempt to authenticate with the selected authentication type
     * @param authType the type of authentication that will be attempted
     * @param user username/email (used for non-anonymous authentication)
     * @param pass password (used for non-anonymous authentication)
     * @param callback callback is passed from the AuthenticateMenu class
     */
    public void authenticate(String authType, String user, String pass, IServerCallback callback){
        switch(authType){
            case "Anonymous":
                _bc.authenticateAnonymous(callback);
                break;
            case "Universal":
                _bc.resetStoredProfileId();
                _bc.resetStoredAnonymousId();
                _bc.authenticateUniversal(user, pass, true, callback);
                break;
            case "Email":
                _bc.resetStoredProfileId();
                _bc.resetStoredAnonymousId();
                _bc.authenticateEmailPassword(user, pass, true, callback);
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
            _bc.getIdentityService().attachEmailIdentity(user, pass, callback);
        }
        else{
            _bc.getIdentityService().attachUniversalIdentity(user, pass, callback);
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
            _bc.getIdentityService().mergeEmailIdentity(user, pass, callback);
        }
        else{
            _bc.getIdentityService().mergeUniversalIdentity(user, pass, callback);
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

        _bc.getEntityService().getPage(context, callback);
    }

    /**
     * Create User Entity
     * @param entity Object containing User Entity data (type, name, age, etc.)
     * @param callback callback is passed from the ExploreEntity class
     */
    public void createEntity(Entity entity, IServerCallback callback){
        _bc.getEntityService().createEntity(
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
        _bc.getEntityService().updateEntity(
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
        _bc.getEntityService().deleteEntity(
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
        _bc.getPlayerStateService().readUserState(callback);
    }

    /**
     * Increase player's experience points
     * @param incrementAmount value to increase points by
     * @param callback callback is passed from the ExploreXP class
     */
    public void incrementXP(int incrementAmount, IServerCallback callback){
        _bc.getPlayerStatisticsService().incrementExperiencePoints(incrementAmount, callback);
    }

    /**
     * Retrieve player's currency data
     * @param callback callback is passed from the ExploreCurrency class
     */
    public void getCurrency(IServerCallback callback){
        _bc.getVirtualCurrencyService().getCurrency("gems", callback);
    }

    /**
     * Run a provided Cloud Code script
     * @param scriptName name of existing script
     * @param scriptData parameter data to be used by the script
     * @param callback callback is passed from the ExploreCurrency class
     */
    public void runCloudCodeScript(String scriptName, String scriptData, IServerCallback callback){
        _bc.getScriptService().runScript(scriptName, scriptData, callback);
    }

    /**
     * Retrieve user/global statistics from brainCloud
     * @param userStat determines which statistics to get (true: user, false: global)
     * @param callback callback is passed from the ExploreStats class
     */
    public void getStatistics(Boolean userStat, IServerCallback callback){
        if(userStat){
            _bc.getPlayerStatisticsService().readAllUserStats(callback);
        }
        else{
            _bc.getGlobalStatisticsService().readAllGlobalStats(callback);
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
            _bc.getPlayerStatisticsService().incrementUserStats(jsonData, callback);
        }
        else{
            _bc.getGlobalStatisticsService().incrementGlobalStats(jsonData, callback);
        }
    }
}