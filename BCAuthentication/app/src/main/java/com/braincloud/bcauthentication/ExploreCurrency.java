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

public class ExploreCurrency extends AppCompatActivity {

    public BCClient brainCloud;
    private int balance;
    private int awarded;
    private int consumed;
    private String amount;

    // UI components
    private TextView currencyStatus;
    private TextView balanceField;
    private TextView awardedField;
    private TextView consumedField;
    private EditText amountField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explore_currency);

        brainCloud = AuthenticateMenu.brainCloud;

        // Get reference to UI components
        TextView bcInitStatus = findViewById(R.id.bc_init_status_tv);
        currencyStatus = findViewById(R.id.currency_title_tv);
        balanceField = findViewById(R.id.balance_tv);
        awardedField = findViewById(R.id.awarded_tv);
        consumedField = findViewById(R.id.consumed_tv);
        amountField = findViewById(R.id.amount_et);
        Button awardButton = findViewById(R.id.award_b);
        Button consumeButton = findViewById(R.id.consume_b);
        Button backButton = findViewById(R.id.back_b);

        bcInitStatus.setText(brainCloud.getVersion());

        // Get current currency info
        currencyStatus.setText(R.string.update_currency);
        getCurrency();

        // Increase user's currency balance by the given amount
        awardButton.setOnClickListener(view -> {
            currencyStatus.setText(R.string.award_currency);
            amount = amountField.getText().toString();
            awardCurrency();
        });

        // Decrease user's currency balance by the given amount
        consumeButton.setOnClickListener(view -> {
            currencyStatus.setText(R.string.consume_currency);
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
                Log.d("getCurrency success!", jsonData.toString());
                parseCurrencyJSON(jsonData);
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                Log.d("getCurrency failed: ", jsonError);
            }
        });
    }

    /**
     * Get player balance and other currency info returned from server
     * "Gems" are being used in this example and must be created in the brainCloud portal
     * Go to Design > Marketplace > Virtual Currencies to create "gems"
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
        } catch (JSONException e){
            e.printStackTrace();
            Log.d("Parse Error: ", "Currency JSON failed to parse");
        }

        displayCurrency();
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

        currencyStatus.setText(R.string.current_currency);
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
                Log.d("runScript success!", jsonData.toString());
                getCurrency();
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                Log.d("runScript failed: ", jsonError);
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
                Log.d("runScript success!", jsonData.toString());
                getCurrency();
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                Log.d("runScript failed: ", jsonError);
            }
        });
    }
}