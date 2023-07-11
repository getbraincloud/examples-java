package com.bitheads.bc_concentration_game;

//braincloud specific includes
import android.content.Context;
import android.os.CountDownTimer;

import com.bitheads.braincloud.client.*;

public class BCClient
{
    private BrainCloudWrapperAndroid _bcWrapper;
    //private BCClientCallbackListener _bcCallBackListener;

    public BCClient()
    {
        //Initialize BrainCloud wrapper
        _bcWrapper = new BrainCloudWrapperAndroid("bcWrapper");

        //TODO Replace values with application IDs
        //the info filled out is according to the app created on braincloud.
        //_bcWrapper.initialize("appId", "secretKey", "appVersion", "serverUrl");

        //create a timer that will run the callbacks of our wrapper
        new CountDownTimer(10000, 1000) {
            public void onTick(long millisUntilFinished) {
                _bcWrapper.runCallbacks();
            }
            public void onFinish() {
                start(); // just restart the timer
            }
        }.start();
    }

    public BrainCloudWrapperAndroid GetWrapper()
    {
        return _bcWrapper;
    }

    //since this is android, need to set the application context. Don't forget to do this in your main activity.
    public void setApplicationContext(Context appContext)
    {
        _bcWrapper.setContext(appContext);
    }


}
