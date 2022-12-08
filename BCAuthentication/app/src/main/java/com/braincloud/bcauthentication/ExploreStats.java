package com.braincloud.bcauthentication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bitheads.braincloud.client.IServerCallback;
import com.bitheads.braincloud.client.ServiceName;
import com.bitheads.braincloud.client.ServiceOperation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ExploreStats extends AppCompatActivity {

    // brainCloud stuff
    private BCClient brainCloud;

    // UI components
    private TextView bcInitStatus;
    private TextView statStatus;
    private LinearLayout userStatField;
    private Button backButton;

    // Statistic specific variables
    private ArrayList<UserStat> userStatistics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explore_stats);

        // Get brainCloud wrapper
        brainCloud = AuthenticateMenu.brainCloud;

        // Get reference to UI components
        bcInitStatus = findViewById(R.id.bc_init_status_tv);
        statStatus = findViewById(R.id.stats_title_tv);
        userStatField = findViewById(R.id.user_statistics_field_ll);
        backButton = findViewById(R.id.back_b);

        bcInitStatus.setText(brainCloud.getVersion());

        statStatus.setText(R.string.loading);
        requestUserStatistics();

        // Return to BrainCloudMenu Activity to select a different brainCloud function
        backButton.setOnClickListener(view -> finish());
    }

    /**
     * Retrieves the user statistic at the given index in the UserStat ArrayList
     * @param index index of the user statistic within the UserStat ArrayList to retrieve
     * @return the UserStat at the given index or null if it is not found
     */
    public UserStat getStatisticAtIndex(int index){
        if(!userStatistics.isEmpty()){
            if(index >= 0 && index < getCount()){
                return userStatistics.get(index);
            }
        }

        return null;
    }

    /**
     * Returns the size of the UserStat ArrayList
     * @return number of items (int) in the UserStat ArrayList
     */
    public int getCount(){
        return userStatistics.size();
    }

    /**
     * Retrieve player's statistics
     */
    public void requestUserStatistics(){
        brainCloud.requestUserStatistics(new IServerCallback() {
            @Override
            public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {
                Log.d("BC_LOG", "User Stats Read!");
                parseStatisticJSON(jsonData);
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                Log.d("BC_LOG", "User Stats Failed...");
            }
        });
    }

    /**
     * Create ArrayList of user statistics from data returned from server
     * @param jsonData JSONObject contain user statistics returned from server
     */
    public void parseStatisticJSON(JSONObject jsonData){
        JSONObject data;
        JSONObject statistics;
        String name;
        String value;
        userStatistics = new ArrayList<>();

        try {
            data = jsonData.getJSONObject("data");
            statistics = data.getJSONObject("statistics");

            JSONArray key = statistics.names();

            if(key != null){
                for(int i = 0; i < key.length(); i++){
                    name = key.getString(i);
                    value = statistics.getString(name);

                    userStatistics.add(new UserStat());
                    userStatistics.get(i).setName(name);
                    userStatistics.get(i).setValue(Long.parseLong(value));
                }

                Log.d("BC_LOG", "PARSED STATS");
                displayStatistic();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create separate views for each user statistic
     */
    public void displayStatistic(){
        for(int i = 0; i < getCount(); i++){
            View userStat = getLayoutInflater().inflate(R.layout.user_statistic, null, false);

            UserStat statistic = getStatisticAtIndex(i);

            // Get UI components
            TextView userStatName = userStat.findViewById(R.id.stat_name_tv);
            TextView userStatValue = userStat.findViewById(R.id.stat_value_tv);
            Button increment = userStat.findViewById(R.id.increment_b);

            // Display user statistic names and values
            userStatName.setText(statistic.getName());
            userStatValue.setText(Long.toString(statistic.getValue()));

            // Increment displayed user statistic value and send increment request to server
            increment.setOnClickListener(view -> {
                int value = Integer.parseInt(userStatValue.getText().toString());
                value++;
                userStatValue.setText(Integer.toString(value));
                incrementStat(userStatName.getText().toString());
            });

            // Add user statistic entry to main view
            userStatField.addView(userStat);
        }

        statStatus.setText(R.string.stats_update);
    }

    /**
     * Increment given player statistic by given amount (hardcoded 1 for now)
     * @param statName name of the player statistic to increment
     */
    public void incrementStat(String statName){
        String jsonData = "{\"" + statName + "\":1}";

        brainCloud.incrementUserStatistic(jsonData, new IServerCallback() {
            @Override
            public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {
                Log.d("BC_LOG", "Increment success");
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                Log.d("BC_LOG", jsonError);
            }
        });
    }
}