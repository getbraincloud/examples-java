package com.braincloud.bcauthentication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
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
    private Button exploreFunc;
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
        exploreFunc = findViewById(R.id.explore_func_b);
        logOut = findViewById(R.id.log_out_b);

        bcInitStatus.setText(brainCloud.getWrapper().getClient().getBrainCloudVersion());

        // Create the dropdown menu (Spinner component) to select brainCloud function
        configureFunctionSpinner();

        exploreFunc.setOnClickListener(view -> {
            switch(selectedFunc){
                case "Identity":
                    exploreIdentity();
                    break;
                case "Entity":
                    exploreEntity();
                    break;
                case "XP":
                    exploreXP();
                    break;
                case "Stats":
                    exploreStats();
                    break;
                case "Cloud Code":
                    exploreScripts();
                    break;
            }
        });

        // Log out of brainCloud
        logOut.setOnClickListener(view -> {
            bcFunctionStatus.setText(R.string.attempt_log_out);
            brainCloud.getWrapper().getPlayerStateService().logout(this);
        });
    }

    @Override
    public void onRestart(){
        super.onRestart();

        // Resets state of brainCloud function status and dropdown menu
        recreate();
        configureFunctionSpinner();
    }

    /**
     * Sets up the dropdown menu (Spinner component) to select brainCloud function
     */
    public void configureFunctionSpinner(){
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.bc_functions, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        funcSelect.setAdapter(adapter);
        funcSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedFunc = adapterView.getItemAtPosition(i).toString();

                switch(selectedFunc){
                    case "Select brainCloud Function":
                        exploreFunc.setVisibility(View.GONE);
                        bcFunctionStatus.setText(R.string.explore_bc);
                        break;
                    case "Identity":
                        exploreFunc.setVisibility(View.VISIBLE);
                        bcFunctionStatus.setText(R.string.identity_status);
                        break;
                    case "Entity":
                        exploreFunc.setVisibility(View.VISIBLE);
                        bcFunctionStatus.setText(R.string.entity_status);
                        break;
                    case "XP":
                        exploreFunc.setVisibility(View.VISIBLE);
                        bcFunctionStatus.setText(R.string.xp_status);
                        break;
                    case "Stats":
                        exploreFunc.setVisibility(View.VISIBLE);
                        bcFunctionStatus.setText(R.string.stats_status);
                        break;
                    case "Cloud Code":
                        exploreFunc.setVisibility(View.VISIBLE);
                        bcFunctionStatus.setText(R.string.script_status);
                        break;
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    /**
     * Go to the ExploreIdentity Activity to add a new username or email address to account
     */
    public void exploreIdentity(){
        Intent intent = new Intent(getApplication(), ExploreIdentity.class);
        startActivity(intent);
    }

    /**
     * Go to the ExploreEntity Activity to create or delete entities
     */
    public void exploreEntity(){
        Intent intent = new Intent(getApplication(), ExploreEntity.class);
        startActivity(intent);
    }

    /**
     * Go to the ExploreXP Activity to increment player level and award/consume currency
     */
    public void exploreXP(){
        //TODO
        Intent intent = new Intent(getApplication(), ExploreXP.class);
        startActivity(intent);
    }

    /**
     * Go to the ExploreStats Activity to view player or global statistics
     */
    public void exploreStats(){
        Intent intent = new Intent(getApplication(), ExploreStats.class);
        startActivity(intent);
    }

    /**
     * Go to the ExploreScripts Activity to run Cloud Code scripts
     */
    public void exploreScripts(){
        Intent intent = new Intent(getApplication(), ExploreScripts.class);
        startActivity(intent);
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