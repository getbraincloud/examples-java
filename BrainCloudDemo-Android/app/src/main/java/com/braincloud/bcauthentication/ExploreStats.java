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

    private BCClient brainCloud;
    private boolean viewUserStat;

    // UI components
    private TextView statStatus;
    private Button toggleStatView;
    private LinearLayout statField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explore_stats);

        // Get brainCloud wrapper
        brainCloud = AuthenticateMenu.brainCloud;

        // Get reference to UI components
        TextView bcInitStatus = findViewById(R.id.bc_init_status_tv);
        statStatus = findViewById(R.id.stats_title_tv);
        toggleStatView = findViewById(R.id.toggle_stat_b);
        statField = findViewById(R.id.statistics_field_ll);
        Button backButton = findViewById(R.id.back_b);

        bcInitStatus.setText(brainCloud.getVersion());

        toggleStatView.setVisibility(View.GONE);

        statStatus.setText(R.string.loading);
        getStatistics();

        toggleStatView.setOnClickListener(view -> {
            statStatus.setText(R.string.loading);
            toggleStatType();
        });

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
     * Get the size of the UserStat ArrayList
     * @return number of items (int) in the UserStat ArrayList
     */
    public int getCount(ArrayList<Statistic> statisticsList){
        return statisticsList.size();
    }

    /**
     * Retrieve user/global statistics depending on which view is selected
     */
    public void getStatistics(){
        brainCloud.getStatistics(viewUserStat, new IServerCallback() {
            @Override
            public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {
                Log.d("readUserStats success!", jsonData.toString());
                parseStats(jsonData);
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                Log.d("Failed to get stats: ", jsonError);
            }
        });
    }

    /**
     * Create list of statistics from data returned from brainCloud
     * @param jsonData JSON object containing statistic data
     */
    public void parseStats(JSONObject jsonData){
        JSONObject data;
        JSONObject statistics;
        String name;
        String value;
        ArrayList<Statistic> stats = new ArrayList<>();

        try {
            data = jsonData.getJSONObject("data");
            statistics = data.getJSONObject("statistics");

            JSONArray key = statistics.names();

            if(key != null){
                for(int i = 0; i < key.length(); i++){
                    name = key.getString(i);
                    value = statistics.getString(name);

                    stats.add(new Statistic());
                    stats.get(i).setName(name);
                    stats.get(i).setValue(Long.parseLong(value));
                }

                displayStats(stats);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create separate views for each statistic
     */
    public void displayStats(ArrayList<Statistic> statisticsList){
        for(int i = 0; i < getCount(statisticsList); i++){
            View stat = getLayoutInflater().inflate(R.layout.statistic_entry, null, false);

            Statistic statistic = getStatisticAtIndex(i, statisticsList);

            // Get UI components
            TextView statName = stat.findViewById(R.id.stat_name_tv);
            TextView statValue = stat.findViewById(R.id.stat_value_tv);
            Button increment = stat.findViewById(R.id.increment_b);

            // Display user statistic names and values
            statName.setText(statistic.getName());
            statValue.setText(Long.toString(statistic.getValue()));

            // Increment displayed user statistic value and send increment request to server
            increment.setOnClickListener(view -> {
                int value = Integer.parseInt(statValue.getText().toString());
                value++;
                statValue.setText(Integer.toString(value));
                incrementStats(statName.getText().toString());
            });

            // Add user statistic entry to main view
            statField.addView(stat);
        }

        // Update status to reflect which statistics are being displayed
        toggleStatView.setVisibility(View.VISIBLE);
        if(viewUserStat){
            statStatus.setText(R.string.user_stats);
            toggleStatView.setText(R.string.view_global);
        }
        else{
            statStatus.setText(R.string.global_stats);
            toggleStatView.setText(R.string.view_user);
        }
    }

    /**
     * Increments the provided statistic [hardcoded increment by 1 for now]
     * @param statName the statistic to be incremented
     */
    public void incrementStats(String statName){
        String jsonData = "{\"" + statName + "\":1}";

        brainCloud.incrementStatistics(viewUserStat, jsonData, new IServerCallback() {
            @Override
            public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {
                Log.d("increment success: ", jsonData.toString());
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                Log.d("increment failed: ", jsonError);
            }
        });
    }

    /**
     * Switches between viewing user statistics and global statistics
     */
    public void toggleStatType(){
        toggleStatView.setVisibility(View.GONE);
        statField.removeAllViews();

        if(viewUserStat){
            viewUserStat = false;
        }
        else{
            viewUserStat = true;
        }

        // Reload statistics
        getStatistics();
    }
}