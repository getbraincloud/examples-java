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

public class ExploreCurrency extends AppCompatActivity {

    // brainCloud stuff
    public BCClient brainCloud;

    // UI components
    private TextView bcInitStatus;
    private TextView currencyStatus;
    private TextView balanceField;
    private TextView awardedField;
    private TextView consumedField;
    private EditText amountField;
    private Button awardButton;
    private Button consumeButton;
    private Button backButton;

    // Currency specific variables
    private int balance;
    private int awarded;
    private int consumed;
    private String amount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explore_currency);

        // Get brainCloud wrapper
        brainCloud = AuthenticateMenu.brainCloud;

        // Get reference to UI components
        bcInitStatus = findViewById(R.id.bc_init_status_tv);
        currencyStatus = findViewById(R.id.currency_title_tv);
        balanceField = findViewById(R.id.balance_tv);
        awardedField = findViewById(R.id.awarded_tv);
        consumedField = findViewById(R.id.consumed_tv);
        amountField = findViewById(R.id.amount_et);
        awardButton = findViewById(R.id.award_b);
        consumeButton = findViewById(R.id.consume_b);
        backButton = findViewById(R.id.back_b);

        bcInitStatus.setText(brainCloud.getVersion());

        // Get current currency info
        currencyStatus.setText("Updating Currency...");
        getCurrency();

        // Increase user's currency balance by the given amount
        awardButton.setOnClickListener(view -> {
            currencyStatus.setText("Awarding Currency...");
            amount = amountField.getText().toString();
            awardCurrency();
        });

        // Decrease user's currency balance by the given amount
        consumeButton.setOnClickListener(view -> {
            currencyStatus.setText("Consuming Currency...");
            amount = amountField.getText().toString();
            consumeCurrency();
        });

        // Return to BrainCloudMenu Activity to select a different brainCloud function
        backButton.setOnClickListener(view -> finish());
    }

    /**
     * Retrieve player's current currency
     */
    public void getCurrency(){
        brainCloud.getCurrency(new IServerCallback() {
            @Override
            public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {
                parseCurrencyJSON(jsonData);
                displayCurrency();
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                Log.d("BC_LOG", jsonError);
            }
        });
    }

    /**
     * Get player balance and other currency info returned from server
     * @param jsonObject returned JSON containing player balance, awarded, and consumed values
     */
    public void parseCurrencyJSON(JSONObject jsonObject){
        JSONObject data;
        JSONObject currencyMap;
        JSONObject currency;

        String balanceValue;
        String consumedValue;
        String awardedValue;

        try{
            data = jsonObject.getJSONObject("data");
            currencyMap = data.getJSONObject("currencyMap");
            currency = currencyMap.getJSONObject("gems");

            balanceValue = currency.getString("balance");
            consumedValue = currency.getString("consumed");
            awardedValue = currency.getString("awarded");

            balance = Integer.parseInt(balanceValue);
            consumed = Integer.parseInt(consumedValue);
            awarded = Integer.parseInt(awardedValue);
        } catch(JSONException e){
            e.printStackTrace();
            Log.d("BC_LOG", "Currency Parse Error");
        }
    }

    /**
     * Update UI to reflect current currency data
     */
    public void displayCurrency(){
        String balanceValue = Integer.toString(balance);
        String consumedValue = Integer.toString(consumed);
        String awardedValue = Integer.toString(awarded);

        String balanceDisplay = "Balance: " + balanceValue;
        String consumedDisplay = "Consumed: " + consumedValue;
        String awardedDisplay = "Awarded: " + awardedValue;

        balanceField.setText(balanceDisplay);
        consumedField.setText(consumedDisplay);
        awardedField.setText(awardedDisplay);

        currencyStatus.setText("Current Currency");
    }

    /**
     * Increase player's balance
     */
    public void awardCurrency(){
        String scriptName = "AwardCurrency";
        String scriptData = "{\"vcId\": \"gems\", \"vcAmount\": " + amount + "}";

        amountField.getText().clear();

        brainCloud.runCloudCodeScript(scriptName, scriptData, new IServerCallback() {
            @Override
            public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {
                getCurrency();
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                Log.d("BC_LOG", jsonError);
            }
        });
    }

    /**
     * Decrease player's balance
     */
    public void consumeCurrency(){
        String scriptName = "ConsumeCurrency";
        String scriptData = "{\"vcId\": \"gems\", \"vcAmount\": " + amount + "}";

        amountField.getText().clear();

        brainCloud.runCloudCodeScript(scriptName, scriptData, new IServerCallback() {
            @Override
            public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {
                getCurrency();
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                Log.d("BC_LOG", jsonError);
            }
        });
    }
}