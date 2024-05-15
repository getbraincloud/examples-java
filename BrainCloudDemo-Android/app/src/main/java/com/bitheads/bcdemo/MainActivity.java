package com.bitheads.bcdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.bitheads.braincloud.client.IServerCallback;
import com.bitheads.braincloud.client.ServiceName;
import com.bitheads.braincloud.client.ServiceOperation;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private Context appContext;
    private BrainCloudManager brainCloudManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appContext = MainActivity.this;
        brainCloudManager = BrainCloudManager.getInstance(appContext);

        brainCloudManager.getBrainCloudWrapper().getClient().enableLogging(true);

        if(brainCloudManager.getBrainCloudWrapper().canReconnect()){
            Log.d("brainCloud Message", "canReconnect is true");

           brainCloudManager.getBrainCloudWrapper().reconnect(new IServerCallback() {
                @Override
                public void serverCallback(ServiceName serviceName,
                                           ServiceOperation serviceOperation,
                                           JSONObject jsonData) {
                    Log.v("brainCloud msg", "reconnect success: " + jsonData.toString());
                    goToBrainCloudMenu();
                }

                @Override
                public void serverError(ServiceName serviceName,
                                        ServiceOperation serviceOperation,
                                        int statusCode,
                                        int reasonCode,
                                        String jsonError) {
                    Log.v("brainCloud msg", "reconnect fail: " + jsonError);
                    goToAuthenticationMenu();
                }
            });


        }
        else{
            Log.d("brainCloud Message", "canReconnect is false");
            goToAuthenticationMenu();
        }
    }

    private void goToAuthenticationMenu(){
        Intent intent = new Intent(this, AuthenticateMenu.class);
        startActivity(intent);

        finish();
    }

    private void goToBrainCloudMenu(){
        Intent intent = new Intent(this, BrainCloudMenu.class);
        startActivity(intent);

        finish();
    }
}