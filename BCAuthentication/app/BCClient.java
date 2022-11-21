package com.braincloud.bcauthentication;

import android.content.Context;
import android.os.CountDownTimer;

import com.bitheads.braincloud.client.BrainCloudWrapper;

public class BCClient {

    private BrainCloudWrapper _bc;

    public BCClient(){
        _bc = new BrainCloudWrapper("default");
        _bc.initialize("26205",
                "1f794474-24a1-4dca-9605-9415a798a036",
                "1.0.0",
                "https://api.internal.braincloudservers.com/dispatcherV2");

        //create a timer that will run the callbacks of our wrapper
        new CountDownTimer(10000, 1000) {
            public void onTick(long millisUntilFinished) {
                _bc.runCallbacks();
            }
            public void onFinish() {
                start(); // just restart the timer
            }
        }.start();
    }

    public BrainCloudWrapper getWrapper(){
        return _bc;
    }

    public void setApplicationContext(Context appContext){
        _bc.setContext(appContext);
    }
}