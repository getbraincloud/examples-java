package com.braincloud.bcauthentication;

import android.content.Context;
import android.os.CountDownTimer;
import android.util.Log;

import com.bitheads.braincloud.client.BrainCloudWrapper;
import com.bitheads.braincloud.client.IServerCallback;

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
                _bc.authenticateUniversal(user, pass, true, callback);
                break;
            case "Email":
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
}