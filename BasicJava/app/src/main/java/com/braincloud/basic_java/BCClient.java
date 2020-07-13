package com.braincloud.basic_java;

//braincloud specific includes
import android.content.Context;
import android.os.CountDownTimer;

import com.bitheads.braincloud.comms.*;
import com.bitheads.braincloud.client.*;
import com.bitheads.braincloud.services.*;

public class BCClient
{
    private BrainCloudWrapper _bcWrapper;
    //private BCClientCallbackListener _bcCallBackListener;

    public BCClient()
    {
        //Initialize BrainCloud wrapper
        _bcWrapper = new BrainCloudWrapper("bcWrapper");
        //the info filled out is according to the app created on braincloud.
        _bcWrapper.initialize("23712", "62459b71-98be-49c6-9d93-66a0a7adcfd4", "1.0.0");

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

    public BrainCloudWrapper GetWrapper()
    {
        return _bcWrapper;
    }

    //since this is android, need to set the application context. Don't forget to do this in your main activity.
    public void setApplicationContext(Context appContext)
    {
        _bcWrapper.setContext(appContext);
    }


}
