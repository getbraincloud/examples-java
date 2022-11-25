package com.braincloud.bcauthentication;

import android.content.Context;
import android.os.CountDownTimer;

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
     * Attempt to authenticate via the selected authentication type
     * @param selectedAuth the type of authentication that will be attempted
     * @param userId username/email (used for non-anonymous authentication)
     * @param password password (used for non-anonymous authentication)
     * @param callback callback is passed from the AuthenticateMenu class
     */
    public void authenticate(String selectedAuth, String userId, String password, IServerCallback callback){
        switch(selectedAuth){
            case "Anonymous":
                _bc.authenticateAnonymous(callback);
                break;
            case "Universal":
                _bc.authenticateUniversal(userId, password, true, callback);
                break;
            case "Email":
                _bc.authenticateEmailPassword(userId, password, true, callback);
                break;
        }
    }
}