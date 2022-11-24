package com.braincloud.bcauthentication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.bitheads.braincloud.client.IServerCallback;
import com.bitheads.braincloud.client.ServiceName;
import com.bitheads.braincloud.client.ServiceOperation;

import org.json.JSONObject;

public class BrainCloudMenu extends AppCompatActivity implements IServerCallback {

    // brainCloud stuff
    public BCClient brainCloud;

    // UI components
    private TextView bcInitStatus;
    private TextView bcFunctionStatus;
    private Spinner funcSelect;
    private Button logOut;

    // Other variables
    private String selectedFunc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_brain_cloud_menu);

        // Get brainCloud wrapper
        brainCloud = AuthenticateMenu.brainCloud;

        // Get reference to UI components
        bcInitStatus = findViewById(R.id.bc_menu_init_status_tv);
        bcFunctionStatus = findViewById(R.id.bc_menu_status_tv);
        funcSelect = findViewById(R.id.bc_functions_s);
        logOut = findViewById(R.id.log_out_b);

        bcInitStatus.setText(brainCloud.getWrapper().getClient().getBrainCloudVersion());

        // Create the dropdown menu (Spinner component) to select brainCloud function
        configureFunctionSpinner();

        // Log out of brainCloud
        logOut.setOnClickListener(view -> {
            bcFunctionStatus.setText(R.string.attempt_log_out);
            brainCloud.getWrapper().getPlayerStateService().logout(this);
        });
    }

    public void configureFunctionSpinner(){
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.bc_functions, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        funcSelect.setAdapter(adapter);
        funcSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedFunc = adapterView.getItemAtPosition(i).toString();

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    @Override
    public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {
        Log.d("BC_LOG", "LOGGED OUT");

        finish();
    }

    @Override
    public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
        Log.d("BC_LOG", jsonError);

        bcFunctionStatus.setText(R.string.log_out_fail);
    }
}