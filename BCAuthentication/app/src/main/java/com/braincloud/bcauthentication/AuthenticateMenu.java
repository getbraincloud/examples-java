package com.braincloud.bcauthentication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.bitheads.braincloud.client.IServerCallback;
import com.bitheads.braincloud.client.ServiceName;
import com.bitheads.braincloud.client.ServiceOperation;

import org.json.JSONObject;

public class AuthenticateMenu extends AppCompatActivity implements IServerCallback {

    // brainCloud stuff
    public static BCClient brainCloud;

    // UI components
    private TextView bcInitStatus;
    private TextView bcAuthStatus;
    private Spinner authSelect;
    private LinearLayout loginFields;
    private EditText userField;
    private EditText passField;
    private TextView invalidLogin;
    private Button authButton;

    // Other variables
    private String selectedAuth;
    private String userId;
    private String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authenticate_menu);

        // Get reference to UI components
        bcAuthStatus = findViewById(R.id.bc_auth_status_tv);
        bcInitStatus = findViewById(R.id.bc_init_status_tv);
        authSelect = findViewById(R.id.auth_types_s);
        loginFields = findViewById(R.id.login_fields_ll);
        userField = findViewById(R.id.user_field_et);
        passField = findViewById(R.id.pass_field_et);
        authButton = findViewById(R.id.auth_button_b);
        invalidLogin = findViewById(R.id.invalid_login_tv);

        // Create BrainCloudWrapper
        brainCloud = new BCClient();

        // Proceed on successful initialization or halt on fail
        if(brainCloud.getWrapper().getClient().isInitialized()){
           bcInitStatus.setText(brainCloud.getWrapper().getClient().getBrainCloudVersion());

            brainCloud.setApplicationContext(AuthenticateMenu.this);

            brainCloud.getWrapper().getClient().enableLogging(true);
        }
        else{
            authSelect.setVisibility(View.GONE);
            loginFields.setVisibility(View.GONE);
            authButton.setVisibility(View.GONE);

            bcInitStatus.setText(R.string.bc_init_fail);
            
            bcAuthStatus.setText(R.string.retry_init);
        }

        // Create the dropdown menu (Spinner component) to select authentication type
        configureAuthSpinner();

        // Attempt to authenticate using the selected type
        authButton.setOnClickListener(view -> {
            userId = userField.getText().toString();
            password = passField.getText().toString();

            if((userId.isEmpty() || password.isEmpty()) && !selectedAuth.equals("Anonymous")){
                invalidLogin.setVisibility(View.VISIBLE);
            }
            else{
                invalidLogin.setVisibility(View.GONE);
                bcAuthStatus.setText(R.string.attempt_auth);

                brainCloud.authenticate(selectedAuth, userId, password, this);
            }
        });
    }

    @Override
    protected void onRestart(){
        super.onRestart();

        // Resets state of authentication status and dropdown menu
        recreate();
        configureAuthSpinner();

        // TODO - DELETE
        Log.d("BC_LOG", "AS WE CONTINUE ON");
    }

    public void configureAuthSpinner(){
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.auth_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        authSelect.setAdapter(adapter);
        authSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedAuth = adapterView.getItemAtPosition(i).toString();

                userField.getText().clear();
                passField.getText().clear();
                invalidLogin.setVisibility(View.GONE);

                // Display different login components depending on the selected authentication type
                switch(selectedAuth){
                    case "Select Auth Type":
                        loginFields.setVisibility(View.GONE);
                        authButton.setVisibility(View.GONE);
                        break;
                    case "Anonymous":
                        loginFields.setVisibility(View.GONE);
                        authButton.setVisibility(View.VISIBLE);
                        break;
                    case "Universal":
                        loginFields.setVisibility(View.VISIBLE);
                        authButton.setVisibility(View.VISIBLE);
                        userField.setHint(R.string.universal_hint);
                        break;
                    case "Email":
                        loginFields.setVisibility(View.VISIBLE);
                        authButton.setVisibility(View.VISIBLE);
                        userField.setHint(R.string.email_hint);
                        break;
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    @Override
    public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {
        Intent intent = new Intent(getApplication(), BrainCloudMenu.class);
        startActivity(intent);
    }

    @Override
    public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
        Log.d("BC_LOG", jsonError);

        bcAuthStatus.setText(R.string.auth_fail);
    }
}