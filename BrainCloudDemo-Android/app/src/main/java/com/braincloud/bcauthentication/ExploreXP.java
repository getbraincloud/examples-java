package com.braincloud.bcauthentication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.bitheads.braincloud.client.IServerCallback;
import com.bitheads.braincloud.client.ServiceName;
import com.bitheads.braincloud.client.ServiceOperation;

import org.json.JSONException;
import org.json.JSONObject;

public class ExploreXP extends AppCompatActivity {

    private BCClient brainCloud;
    private int playerLevel;
    private int playerXP;

    // UI components
    private TextView xpStatus;
    private TextView playerLevelField;
    private TextView playerXpAccruedField;
    private EditText incrementAmountField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explore_xp);

        brainCloud = AuthenticateMenu.brainCloud;

        // Get reference to UI components
        TextView bcInitStatus = findViewById(R.id.bc_init_status_tv);
        xpStatus = findViewById(R.id.xp_title_tv);
        playerLevelField = findViewById(R.id.player_level_tv);
        playerXpAccruedField = findViewById(R.id.player_xp_accrued_tv);
        incrementAmountField = findViewById(R.id.increment_et);
        Button incrementButton = findViewById(R.id.increment_b);
        Button backButton = findViewById(R.id.back_b);

        bcInitStatus.setText(brainCloud.getVersion());

        // Get current XP info
        xpStatus.setText(R.string.update_xp);
        getXP();

        // Increase user's "XP Points" by the given amount
        incrementButton.setOnClickListener(view -> {
            xpStatus.setText(R.string.increment_xp);
            incrementXP();
        });

        // Return to BrainCloudMenu Activity to select a different brainCloud function
        backButton.setOnClickListener(view -> finish());
    }

    /**
     * Retrieve player's current XP
     */
    public void getXP(){
        brainCloud.getXP(new IServerCallback() {
            @Override
            public void serverCallback(ServiceName serviceName,ServiceOperation serviceOperation, JSONObject jsonData) {
                Log.d("readUserState success!", jsonData.toString());
                parsePlayerStateJSON(jsonData);
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                Log.d("readUserState failed: ", jsonError);
            }
        });
    }

    /**
     * Get player level and points returned from server
     * @param jsonData returned JSON containing player level and points data
     */
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
            Log.d("Parse error: ", "player XP JSON failed to parse");
        }

        displayXP();
    }

    /**
     * Update UI to reflect current XP data
     */
    public void displayXP(){
        String experienceLevel = Integer.toString(playerLevel);
        String experiencePoints = Integer.toString(playerXP);

        String playerLevelDisplay = "Player Level: " + experienceLevel;
        String playerXPDisplay = "Player XP Accrued: " + experiencePoints;

        playerLevelField.setText(playerLevelDisplay);
        playerXpAccruedField.setText(playerXPDisplay);

        xpStatus.setText(R.string.current_xp);
    }

    /**
     * Increase player's experience points
     */
    public void incrementXP(){
        int xpIncrementAmount = Integer.parseInt(incrementAmountField.getText().toString());
        incrementAmountField.getText().clear();

        brainCloud.incrementXP(xpIncrementAmount, new IServerCallback() {
            @Override
            public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {
                Log.d("incrementXP success!", jsonData.toString());
                getXP();
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                Log.d("incrementXP failed: ", jsonError);
            }
        });
    }
}