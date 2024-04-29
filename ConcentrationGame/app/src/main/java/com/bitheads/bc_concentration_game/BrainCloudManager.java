package com.bitheads.bc_concentration_game;

import android.content.Context;
import android.os.CountDownTimer;

import com.bitheads.braincloud.client.BrainCloudWrapperAndroid;

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
                    "1.0.0",
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
}
