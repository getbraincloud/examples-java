package com.braincloud.basic_java;

//android specific includes
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

//braincloud specific includes.
import com.bitheads.braincloud.client.*;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import org.json.JSONObject;

import javax.xml.transform.Result;

public class Login extends AppCompatActivity implements  IServerCallback
{
    IServerCallback theCallback;

    //Create brainCloud Wrapper.
    public static BCClient _bc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);

        //Create brainCloud Wrapper.
        _bc = new BCClient();

        //give it context within the main activity.
        _bc.setApplicationContext(Login.this);

        //set the callback to this class
        theCallback = this;

        //get a reference to the button of the app
        Button loginButton = findViewById(R.id.loginButton);

        //when this button is clicked create an inline class so that we can keep the keep unique to this class.
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                //get reference to the objects on screen.
                EditText usernameEditText = findViewById(R.id.usernameEditText);
                EditText passwordEditText = findViewById(R.id.passwordEditText);

                //get what was entered into the text fields.
                String emailEntered = usernameEditText.getText().toString();
                String passwordEntered = passwordEditText.getText().toString();

                //attempt to authenticate
                TextView statusTextView = findViewById(R.id.statusTextView);
                //convert the result to a string
                statusTextView.setText("Authenticating");

                //Authenticate with e-mail and password
                _bc.GetWrapper().authenticateEmailPassword(emailEntered, passwordEntered, true, theCallback);
                //_bc.GetWrapper().authenticateUniversal(emailEntered, passwordEntered, true, theCallback);

                //this is the new way to get the firebase token.
                FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener( Login.this,  new OnSuccessListener<InstanceIdResult>() {
                    @Override
                    public void onSuccess(InstanceIdResult instanceIdResult) {
                        MyFirebaseMessagingService.FirebaseTokenID = instanceIdResult.getToken();
                        Log.e("NEW_TOKEN", MyFirebaseMessagingService.FirebaseTokenID );


                        //set the device up to receive pushnotifications
                        _bc.GetWrapper().getPushNotificationService().registerPushNotificationToken(Platform.GooglePlayAndroid, MyFirebaseMessagingService.FirebaseTokenID, theCallback);
                    }
                });

            }
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

        Log.e("AUTHENTICATED", MyFirebaseMessagingService.FirebaseTokenID );
    }

    public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError)
    {
        TextView statusTextView = findViewById(R.id.statusTextView);
        statusTextView.setText("Fail!");
    }
}
