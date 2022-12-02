package com.braincloud.bcauthentication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.bitheads.braincloud.client.IServerCallback;
import com.bitheads.braincloud.client.ServiceName;
import com.bitheads.braincloud.client.ServiceOperation;

import org.json.JSONException;
import org.json.JSONObject;

public class ExploreXP extends AppCompatActivity {

    // brainCloud stuff
    public BCClient brainCloud;

    // UI components
    private TextView bcInitStatus;
    private TextView xpStatus;
    private TextView playerLevelField;
    private TextView playerXpAccruedField;
    private EditText incrementAmountField;
    private Button incrementButton;
    private TextView balanceField;
    private TextView awardedField;
    private EditText awardAmountField;
    private Button awardButton;
    private Button backButton;

    // Other variables
    private int playerLevel;
    private int playerXP;
    private int xpIncrementAmount;
    private int balance;
    private int currencyIncrementAmount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explore_xp);

        // Get brainCloud wrapper
        brainCloud = AuthenticateMenu.brainCloud;

        // Get reference to UI components
        bcInitStatus = findViewById(R.id.bc_init_status_tv);
        xpStatus = findViewById(R.id.xp_title_tv);
        playerLevelField = findViewById(R.id.player_level_tv);
        playerXpAccruedField = findViewById(R.id.player_xp_accrued_tv);
        incrementAmountField = findViewById(R.id.increment_et);
        incrementButton = findViewById(R.id.increment_b);
        balanceField = findViewById(R.id.balance_tv);
        awardedField = findViewById(R.id.awarded_tv);
        awardAmountField = findViewById(R.id.award_amount_et);
        awardButton = findViewById(R.id.award_b);
        backButton = findViewById(R.id.back_b);

        bcInitStatus.setText(brainCloud.getVersion());

        // Load current XP
        xpStatus.setText("Updating XP...");
        updateXP();

        // Increment user's "XP Points" by the given amount
        incrementButton.setOnClickListener(view -> {
            xpStatus.setText("Incrementing XP...");
            incrementXP();
        });

        // Increment user's currency balance by the given amount
        awardButton.setOnClickListener(view -> {
            xpStatus.setText("Incrementing Currency...");
            incrementCurrency();
        });

        // Return to BrainCloudMenu Activity to select a different brainCloud function
        backButton.setOnClickListener(view -> finish());
    }

    public void updateXP(){
        brainCloud.updateXP(new IServerCallback() {
            @Override
            public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {
                parsePlayerStateJSON(jsonData);
                displayXP();
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                Log.d("BC_LOG", jsonError);
            }
        });
    }

    public void parsePlayerStateJSON(JSONObject jsonData){
        JSONObject data;
        String experienceLevel;
        String experiencePoints;

        try {
            data = jsonData.getJSONObject("data");
            experienceLevel = data.getString("experienceLevel");
            experiencePoints = data.getString("experiencePoints");

            playerLevel = Integer.parseInt(experienceLevel);
            playerXP = Integer.parseInt(experiencePoints);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.d("BC_LOG", "XPERROR");
        }
    }


    public void displayXP(){
        String experienceLevel = Integer.toString(playerLevel);
        String experiencePoints = Integer.toString(playerXP);

        String playerLevelDisplay = "Player Level: " + experienceLevel;
        String playerXPDisplay = "Player XP Accrued: " + experiencePoints;

        playerLevelField.setText(playerLevelDisplay);
        playerXpAccruedField.setText(playerXPDisplay);

        xpStatus.setText("Current XP");
    }

    public void incrementXP(){
        xpIncrementAmount = Integer.parseInt(incrementAmountField.getText().toString());
        incrementAmountField.getText().clear();

        brainCloud.incrementXP(xpIncrementAmount, new IServerCallback() {
            @Override
            public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {
                Log.d("BC_LOG", "XP INCREMENT SUCCESS");
                updateXP();
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                Log.d("BC_LOG", jsonError);
            }
        });
    }

    public void incrementCurrency(){
        currencyIncrementAmount = Integer.parseInt(awardAmountField.getText().toString());

        balance += currencyIncrementAmount;

        balanceField.setText("Balance: " + Integer.toString(balance));
    }
}