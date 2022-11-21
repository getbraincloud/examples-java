package com.braincloud.bcauthentication;

import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.util.Log;

import com.bitheads.braincloud.client.BrainCloudWrapper;
import com.bitheads.braincloud.client.IServerCallback;
import com.bitheads.braincloud.client.ServiceName;
import com.bitheads.braincloud.client.ServiceOperation;

import org.json.JSONObject;

public class BCClient implements IServerCallback {

    private BrainCloudWrapper _bc;

    private IServerCallback theCallback = this;

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

    // Authenticate using the selected type
    public void authenticate(String authType, String userId, String password){
        switch(authType){
            case "Anonymous":
                _bc.authenticateAnonymous(theCallback);
                break;
            case "Universal":
                _bc.authenticateUniversal(userId, password, true, theCallback);
                break;
            case "Email":
                _bc.authenticateEmailPassword(userId, password, true, theCallback);
                break;
        }

    }

    @Override
    public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {
        Log.d("BC_LOG", "Authentication succeeded");

        //TODO
        //enterBCMenu();
    }

    @Override
    public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
        Log.d("BC_LOG", "Authentication failed");
        Log.d("BC_LOG", jsonError);
    }
}