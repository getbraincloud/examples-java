package com.bitheads.bcdemo;

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

    public BrainCloudManager brainCloudManager;
    private String selectedFunc;

    // UI components
    private TextView bcFunctionStatus;
    private TextView bcFuncHelp;
    private Spinner funcSelect;
    private Button exploreFunc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_brain_cloud_menu);

        brainCloudManager = BrainCloudManager.getInstance(BrainCloudMenu.this);

        // Get reference to UI components
        TextView bcInitStatus = findViewById(R.id.bc_menu_init_status_tv);
        bcFunctionStatus = findViewById(R.id.bc_menu_status_tv);
        bcFuncHelp = findViewById(R.id.help_box_tv);
        funcSelect = findViewById(R.id.bc_functions_s);
        exploreFunc = findViewById(R.id.explore_func_b);
        Button logOut = findViewById(R.id.log_out_b);

        bcInitStatus.setText(brainCloudManager.getBrainCloudClientVersion());

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
                case "Experience":
                    exploreXP();
                    break;
                case "Currency":
                    exploreCurrency();
                    break;
                case "Stats":
                    exploreStats();
                    break;
            }
        });

        // Log out of brainCloud
        logOut.setOnClickListener(view -> {
            bcFunctionStatus.setText(R.string.attempt_log_out);
            brainCloudManager.getBrainCloudWrapper().logout(true, this);
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
     * Set up the dropdown menu (Spinner component) to select brainCloud function
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
                        bcFuncHelp.setVisibility(View.GONE);
                        bcFunctionStatus.setText(R.string.explore_bc);
                        break;
                    case "Identity":
                        exploreFunc.setVisibility(View.VISIBLE);
                        bcFuncHelp.setVisibility(View.VISIBLE);
                        bcFuncHelp.setText(R.string.identity_help);
                        bcFunctionStatus.setText(R.string.identity_status);
                        break;
                    case "Entity":
                        exploreFunc.setVisibility(View.VISIBLE);
                        bcFuncHelp.setVisibility(View.VISIBLE);
                        bcFuncHelp.setText(R.string.entity_help);
                        bcFunctionStatus.setText(R.string.entity_status);
                        break;
                    case "Experience":
                        exploreFunc.setVisibility(View.VISIBLE);
                        bcFuncHelp.setVisibility(View.VISIBLE);
                        bcFuncHelp.setText(R.string.xp_help);
                        bcFunctionStatus.setText(R.string.xp_status);
                        break;
                    case "Currency":
                        exploreFunc.setVisibility(View.VISIBLE);
                        bcFuncHelp.setVisibility(View.VISIBLE);
                        bcFuncHelp.setText(R.string.currency_help);
                        bcFunctionStatus.setText(R.string.currency_status);
                        break;
                    case "Stats":
                        exploreFunc.setVisibility(View.VISIBLE);
                        bcFuncHelp.setVisibility(View.VISIBLE);
                        bcFuncHelp.setText(R.string.stats_help);
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
     * Go to the ExploreXP Activity to increment player level
     */
    public void exploreXP(){
        Intent intent = new Intent(getApplication(), ExploreXP.class);
        startActivity(intent);
    }

    /**
     * Go to the ExploreCurrency Activity to award/consume currency
     */
    public void exploreCurrency(){
        Intent intent = new Intent(getApplication(), ExploreCurrency.class);
        startActivity(intent);
    }

    /**
     * Go to the ExploreStats Activity to view player or global statistics
     */
    public void exploreStats(){
        Intent intent = new Intent(getApplication(), ExploreStats.class);
        startActivity(intent);
    }

    @Override
    public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {
        Log.d("Logged out!", jsonData.toString());
        finish();
    }

    @Override
    public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
        Log.d("Log out failed: ", jsonError);

        bcFunctionStatus.setText(R.string.log_out_fail);
    }
}