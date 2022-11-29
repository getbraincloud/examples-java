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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ExploreEntity extends AppCompatActivity {

    // brainCloud stuff
    private BCClient brainCloud;

    // UI components
    private TextView bcInitStatus;
    private TextView entityStatus;
    private TextView entityIdField;
    private TextView entityTypeField;
    private EditText entityNameField;
    private EditText entityAgeField;
    private TextView emptyFields;
    private Button createButton;
    private Button deleteButton;
    private Button backButton;

    // Other variables
    private String entityId;
    private String entityType;
    private String entityName;
    private String entityAge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explore_entity);

        // Get brainCloud wrapper
        brainCloud = AuthenticateMenu.brainCloud;

        // Get reference to UI components
        bcInitStatus = findViewById(R.id.bc_init_status_tv);
        entityStatus = findViewById(R.id.entity_title_tv);
        entityIdField = findViewById(R.id.entity_id_tv);
        entityTypeField = findViewById(R.id.entity_type_tv);
        entityNameField = findViewById(R.id.entity_name_et);
        entityAgeField = findViewById(R.id.entity_age_et);
        emptyFields = findViewById(R.id.empty_field_tv);
        createButton = findViewById(R.id.create_b);
        deleteButton =findViewById(R.id.delete_b);
        backButton = findViewById(R.id.back_b);

        bcInitStatus.setText(brainCloud.getVersion());

        // Look for existing entities
        getEntities();

        // Hide the button until an entity is created
        deleteButton.setVisibility(View.GONE);

        // Create entity, enable edit/save and delete buttons
        createButton.setOnClickListener(view -> {
            entityName = entityNameField.getText().toString();
            entityAge = entityAgeField.getText().toString();

            if(entityName.isEmpty() || entityAge.isEmpty()){
                emptyFields.setVisibility(View.VISIBLE);
            }
            else{
                emptyFields.setVisibility(View.GONE);

                createEntity();
                getEntities();
            }
        });

        // Return to BrainCloudMenu Activity to select a different brainCloud function
        backButton.setOnClickListener(view -> finish());
    }

    //TODO THIS ALL NEEDS TO BE CLEANED
    public void getEntities(){
        // Get existing entities??
        brainCloud.getEntityPage(new IServerCallback() {
            @Override
            public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {

                // Parse the JSON object returned from the server (containing existing entities)
                parseEntityJSON(jsonData);

                displayEntity();
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                Log.d("BC_LOG", jsonError);
            }
        });
    }

    public void parseEntityJSON(JSONObject jsonObject){
        JSONObject data;
        JSONObject results;
        JSONArray items;

        try {
            data = jsonObject.getJSONObject("data");
            results = data.getJSONObject("results");
            items = results.getJSONArray("items");

            // If there are no existing entities
            if(items.length() == 0){
                Log.d("BC_LOG", "No Entities Found...");
            }
            else{
                // Get Entity attributes
                for(int i = 0; i < items.length(); i++){
                    JSONObject item = items.getJSONObject(i);
                    JSONObject entityAttr = item.getJSONObject("data");

                    entityId = item.getString("entityId");
                    entityType = item.getString("entityType");
                    entityName = entityAttr.getString("name");
                    entityAge = entityAttr.getString("age");
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();    //TODO
            Log.d("BC_LOG", "PARSE ERROR");
        }
    }

    public void displayEntity(){
        entityNameField.getText().clear();
        entityAgeField.getText().clear();

        entityIdField.setText("Entity ID: " + entityId);
        entityTypeField.setText("Entity Type: " + entityType);
        entityNameField.setHint("Entity Name: " + entityName);
        entityAgeField.setHint("Entity Age: " + entityAge);
    }

    public void createEntity(){
        brainCloud.createEntity(entityName, entityAge, new IServerCallback() {
            @Override
            public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {
                Log.d("BC_LOG", "ENTITY CREATED");
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                Log.d("BC_LOG", "ENTITY NOT CREATED");
            }
        });
    }

    public void updateEntity(){
        if(entityId.isEmpty()){
            Log.d("BC_LOG", "No Entity ID...");
        }
        else{
            brainCloud.updateEntity(entityName, entityAge, new IServerCallback() {
                @Override
                public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {

                }

                @Override
                public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {

                }
            });
        }
    }
}