package com.bitheads.bc_concentration_game;

//android specific includes

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bitheads.braincloud.client.IServerCallback;
import com.bitheads.braincloud.client.Platform;
import com.bitheads.braincloud.client.ServiceName;
import com.bitheads.braincloud.client.ServiceOperation;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONObject;

public class Login extends AppCompatActivity implements  IServerCallback
{
    IServerCallback theCallback;

    private BrainCloudManager brainCloudManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);

        brainCloudManager = BrainCloudManager.getInstance(Login.this);

        //set the callback to this class
        theCallback = this;

        // Attempt reconnect authentication
        if(brainCloudManager.getBrainCloudWrapper().canReconnect()){
            Log.d("brainCloud Message", "canReconnect is true");

            TextView statusTextView1 = findViewById(R.id.statusTextView);
            statusTextView1.setText("Reconnecting");

            brainCloudManager.getBrainCloudWrapper().reconnect(new IServerCallback() {
                @Override
                public void serverCallback(ServiceName serviceName,
                                           ServiceOperation serviceOperation,
                                           JSONObject jsonData) {
                    Log.v("brainCloud msg", "reconnect success: " + jsonData.toString());

                    //change the app activity
                    Intent loadApp = new Intent(getApplication(), theGame.class);
                    startActivity(loadApp);
                }

                @Override
                public void serverError(ServiceName serviceName,
                                        ServiceOperation serviceOperation,
                                        int statusCode,
                                        int reasonCode,
                                        String jsonError) {
                    Log.v("brainCloud msg", "reconnect fail: " + jsonError);
                    displayLoginMenu();
                }
            });


        }
        else{
            Log.d("brainCloud Message", "canReconnect is false");
            displayLoginMenu();
        }
    }

    private void displayLoginMenu(){
        //get a reference to the button of the app
        Button loginButton = findViewById(R.id.loginButton);

        //attempt to authenticate
        TextView statusTextView = findViewById(R.id.statusTextView);

        //show detection
        statusTextView.setText(brainCloudManager.getBrainCloudWrapper().getReleasePlatform().toString());

        //when this button is clicked create an inline class so that we can keep it unique to this class.
        loginButton.setOnClickListener(v -> {
            //get reference to the objects on screen.
            EditText usernameEditText = findViewById(R.id.usernameEditText);
            EditText passwordEditText = findViewById(R.id.passwordEditText);

            //get what was entered into the text fields.
            String emailEntered = usernameEditText.getText().toString();
            String passwordEntered = passwordEditText.getText().toString();

            //attempt to authenticate
            TextView statusTextView1 = findViewById(R.id.statusTextView);
            //convert the result to a string
            statusTextView1.setText("Authenticating");

            //Authenticate with e-mail and password
            Log.d("braincloud", "authenticating...");
            brainCloudManager.getBrainCloudWrapper().authenticateEmailPassword(emailEntered, passwordEntered, true, theCallback);

            //this is the new way to get the firebase token.
            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.w("NEW_TOKEN", "getInstanceId failed", task.getException());
                    return;
                }

                // Get new Instance ID token
                String token = task.getResult();

                Log.i("NEW_TOKEN", token);
                brainCloudManager.getBrainCloudWrapper().getPushNotificationService().registerPushNotificationToken(Platform.GooglePlayAndroid, token, theCallback);

            });
        });
    }

    //callback functions
    public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData)
    {
        TextView statusTextView = findViewById(R.id.statusTextView);
        //convert the result to a string
        statusTextView.setText("Success!");

        //change the app activity
        Intent loadApp = new Intent(getApplication(), theGame.class);
        startActivity(loadApp);
    }

    public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError)
    {
        TextView statusTextView = findViewById(R.id.statusTextView);
        statusTextView.setText("Fail!");
        Log.d("BC_LOG", jsonError);
    }
}
