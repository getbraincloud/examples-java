package com.braincloud.bcauthentication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.bitheads.braincloud.client.IServerCallback;
import com.bitheads.braincloud.client.ServiceName;
import com.bitheads.braincloud.client.ServiceOperation;

import org.json.JSONObject;

public class ExploreIdentity extends AppCompatActivity implements View.OnClickListener {

    // brainCloud stuff
    public BCClient brainCloud;

    // UI components
    private TextView bcInitStatus;
    private TextView identityStatus;
    private TextView identityHelp;
    private Spinner identityTypes;
    private EditText userField;
    private EditText passField;
    private TextView invalidLogin;
    private Button attachButton;
    private Button mergeButton;
    private Button backButton;

    // Other variables
    private String idType;
    private String userId;
    private String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explore_identity);

        // Get brainCloud wrapper
        brainCloud = AuthenticateMenu.brainCloud;

        // Get reference to UI components
        bcInitStatus = findViewById(R.id.bc_init_status_tv);
        identityStatus = findViewById(R.id.identity_title_tv);
        identityHelp = findViewById(R.id.identity_help_tv);
        identityTypes = findViewById(R.id.id_types_s);
        userField = findViewById(R.id.user_field_et);
        passField = findViewById(R.id.pass_field_et);
        invalidLogin = findViewById(R.id.empty_field_tv);
        attachButton = findViewById(R.id.attach_b);
        mergeButton = findViewById(R.id.merge_b);
        backButton = findViewById(R.id.identity_back_b);

        bcInitStatus.setText(brainCloud.getWrapper().getClient().getBrainCloudVersion());

        // Display help message
        identityHelp.setText(R.string.identity_help);

        // Create the dropdown menu (Spinner component) to select identity type to update
        configureIdSpinner();

        // Attach username/email to currently logged in profile
        attachButton.setOnClickListener(this);

        // Merge existing username/email with currently logged in profile
        mergeButton.setOnClickListener(this);

        // Return to BrainCloudMenu Activity to select a different brainCloud function
        backButton.setOnClickListener(view -> finish());
    }

    /**
     * Set up the dropdown menu (Spinner component) to select identity type
     */
    private void configureIdSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.identity_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        identityTypes.setAdapter(adapter);
        identityTypes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                idType = adapterView.getItemAtPosition(i).toString();

                userField.getText().clear();
                passField.getText().clear();
                invalidLogin.setVisibility(View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    @Override
    public void onClick(View view) {
        userId = userField.getText().toString();
        password = passField.getText().toString();

        if(userId.isEmpty() || password.isEmpty()){
            invalidLogin.setVisibility(View.VISIBLE);
        }
        else{
            invalidLogin.setVisibility(View.GONE);

            if(view.getId() == R.id.attach_b){
                identityStatus.setText(R.string.attaching);

                brainCloud.attachIdentity(idType, userId, password, new IServerCallback() {
                    @Override
                    public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {
                        identityStatus.setText(R.string.attach_success);
                    }

                    @Override
                    public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                        identityStatus.setText(R.string.attach_fail);
                        Log.d("BC_LOG", jsonError);
                    }
                });
            }
            else{
                identityStatus.setText(R.string.merging);

                brainCloud.mergeIdentity(idType, userId, password, new IServerCallback() {
                    @Override
                    public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {
                        identityStatus.setText(R.string.merge_success);
                    }

                    @Override
                    public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                        identityStatus.setText(R.string.merge_fail);
                        Log.d("BC_LOG", jsonError);
                    }
                });
            }
        }
    }
}