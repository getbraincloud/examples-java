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
    private LinearLayout statField;
    private Button backButton;

    // Statistic specific variables
    private ArrayList<Statistic> userStatistics;
    private ArrayList<Statistic> globalStatistics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explore_stats);

        // Get brainCloud wrapper
        brainCloud = AuthenticateMenu.brainCloud;

        // Get reference to UI components
        bcInitStatus = findViewById(R.id.bc_init_status_tv);
        statStatus = findViewById(R.id.stats_title_tv);
        statField = findViewById(R.id.statistics_field_ll);
        backButton = findViewById(R.id.back_b);

        bcInitStatus.setText(brainCloud.getVersion());

        statStatus.setText(R.string.loading);
        getUserStats();

        // Return to BrainCloudMenu Activity to select a different brainCloud function
        backButton.setOnClickListener(view -> finish());
    }

    /**
     * Retrieves the user statistic at the given index in the UserStat ArrayList
     * @param index index of the user statistic within the UserStat ArrayList to retrieve
     * @return the UserStat at the given index or null if it is not found
     */
    public Statistic getStatisticAtIndex(int index, ArrayList<Statistic> statisticsList){
        if(!statisticsList.isEmpty()){
            if(index >= 0 && index < getCount(statisticsList)){
                return statisticsList.get(index);
            }
        }

        return null;
    }

    /**
     * Returns the size of the UserStat ArrayList
     * @return number of items (int) in the UserStat ArrayList
     */
    public int getCount(ArrayList<Statistic> statisticsList){
        return statisticsList.size();
    }

    /**
     * Retrieve user's statistics
     */
    public void getUserStats(){
        brainCloud.getUserStats(new IServerCallback() {
            @Override
            public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {
                parseUserStatsJSON(jsonData);
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                Log.d("readStats failed: ", jsonError);
            }
        });
    }

    /**
     * Create ArrayList of user statistics from data returned from server
     * @param jsonData JSONObject contain user statistics returned from server
     */
    public void parseUserStatsJSON(JSONObject jsonData){
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

                    userStatistics.add(new Statistic());
                    userStatistics.get(i).setName(name);
                    userStatistics.get(i).setValue(Long.parseLong(value));
                }

                displayUserStats(userStatistics);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create separate views for each user statistic
     */
    public void displayUserStats(ArrayList<Statistic> statisticsList){
        for(int i = 0; i < getCount(statisticsList); i++){
            View userStat = getLayoutInflater().inflate(R.layout.statistic_entry, null, false);

            Statistic statistic = getStatisticAtIndex(i, statisticsList);

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
                incrementUserStats(userStatName.getText().toString());
            });

            // Add user statistic entry to main view
            statField.addView(userStat);
        }

        statStatus.setText(R.string.stats_update);
    }

    /**
     * Increment given user statistic by given amount (hardcoded 1 for now)
     * @param statName name of the user statistic to increment
     */
    public void incrementUserStats(String statName){
        String jsonData = "{\"" + statName + "\":1}";

        brainCloud.incrementUserStats(jsonData, new IServerCallback() {
            @Override
            public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {
                //TODO
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                Log.d("incrementStats failed: ", jsonError);
            }
        });
    }
}