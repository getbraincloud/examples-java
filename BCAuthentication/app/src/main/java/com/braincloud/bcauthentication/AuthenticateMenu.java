package com.braincloud.bcauthentication;

import androidx.appcompat.app.AppCompatActivity;

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

public class AuthenticateMenu extends AppCompatActivity {

    // brainCloud stuff
    private BCClient _bc;

    // UI component(s)
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
        _bc = new BCClient();

        // Proceed on successful initialization / prompt to retry on fail
        if(_bc.getWrapper().getClient().isInitialized()){
            Log.d("BC_LOG", "Initialization succeeded");

            bcInitStatus.setText(_bc.getWrapper().getClient().getBrainCloudVersion());

            _bc.setApplicationContext(AuthenticateMenu.this);

            _bc.getWrapper().getClient().enableLogging(true);
        }
        else{
            authSelect.setVisibility(View.GONE);
            loginFields.setVisibility(View.GONE);
            authButton.setVisibility(View.GONE);

            bcInitStatus.setText(R.string.bc_init_fail);
            
            bcAuthStatus.setText(R.string.retry_init);
        }

        // Create dropdown menu (Spinner component) to select authentication type
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
                //TODO
            }
        });

        // Attempt to authenticate using the selected type
        authButton.setOnClickListener(view -> {
            userId = userField.getText().toString();
            password = passField.getText().toString();

            // Don't attempt authentication with empty fields
            if((selectedAuth.equals("Universal") || selectedAuth.equals("Email")) && (userId.isEmpty() || password.isEmpty())){
                invalidLogin.setVisibility(View.VISIBLE);
            }
            else{
                _bc.authenticate(selectedAuth, userId, password);
            }
        });
    }
}