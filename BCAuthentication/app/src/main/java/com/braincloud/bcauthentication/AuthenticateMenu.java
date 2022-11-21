package com.braincloud.bcauthentication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.bitheads.braincloud.client.BrainCloudWrapper;
import com.bitheads.braincloud.client.IServerCallback;
import com.bitheads.braincloud.client.ServiceName;
import com.bitheads.braincloud.client.ServiceOperation;

import org.json.JSONObject;

public class AuthenticateMenu extends AppCompatActivity {

    // brainCloud stuff
    private BCClient _bc;

    // UI components
    private TextView bcInitStatus;
    private TextView bcAuthStatus;
    private Button authButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authenticate_menu);

        // Get reference to UI components
        bcAuthStatus = findViewById(R.id.bc_auth_status_tv);
        bcInitStatus = findViewById(R.id.bc_init_status_tv);
        authButton = findViewById(R.id.auth_button_b);

        // Create BrainCloudWrapper
        _bc = new BCClient();

        // Proceed on successful initialization / prompt to retry on fail
        if(_bc.getWrapper().getClient().isInitialized()){
            Log.d("BC_LOG", "Initialization succeeded");

            bcInitStatus.setText(_bc.getWrapper().getClient().getBrainCloudVersion());

            _bc.setApplicationContext(AuthenticateMenu.this);
            _bc.getWrapper().getClient().enableLogging(true);
        }
        else{
            bcInitStatus.setText(R.string.bc_init_fail);
            
            bcAuthStatus.setText(R.string.retry_init);
        }

        // Authenticate
        authButton.setOnClickListener(view -> {
            _bc.getWrapper().authenticateAnonymous(new IServerCallback() {
                @Override
                public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {
                    Log.d("BC_LOG", "Authentication succeeded");
                    bcAuthStatus.setText(R.string.auth_success);
                }

                @Override
                public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                    Log.d("BC_LOG", jsonError);
                    bcAuthStatus.setText(R.string.auth_fail);
                }
            });
            bcAuthStatus.setText(R.string.attempt_auth);
        });
    }
}