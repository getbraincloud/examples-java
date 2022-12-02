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
    private Entity entity;
    private String entityName;
    private String entityAge;
    private boolean existingEntity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explore_entity);

        // Get brainCloud wrapper
        brainCloud = AuthenticateMenu.brainCloud;

        entity = new Entity();

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
        entityStatus.setText("Finding Entity...");
        getEntity();

        // Hide the button until an entity is created
        deleteButton.setVisibility(View.GONE);

        // Create or update entity
        createButton.setOnClickListener(view -> {
            entityName = entityNameField.getText().toString();
            entityAge = entityAgeField.getText().toString();

            if(entityName.isEmpty() || entityAge.isEmpty()){
                emptyFields.setVisibility(View.VISIBLE);
            }
            else{
                emptyFields.setVisibility(View.GONE);

                entity.setName(entityName);
                entity.setAge(entityAge);

                if(existingEntity == false){
                    entityStatus.setText("Creating Entity...");
                    createEntity();
                }
                else{
                    entityStatus.setText("Updating Entity...");
                    updateEntity();
                }
            }
        });

        // Delete entity
        deleteButton.setOnClickListener(view -> {
            entityStatus.setText("Deleting Entity...");
            deleteEntity();
        });

        // Return to BrainCloudMenu Activity to select a different brainCloud function
        backButton.setOnClickListener(view -> finish());
    }

    /**
     * Search for existing User Entity and display result
     */
    public void getEntity(){
        brainCloud.getEntityPage(new IServerCallback() {
            @Override
            public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {

                // Parse the JSON object returned from the server (containing existing entities)
                parseEntityJSON(jsonData);
                displayEntity();
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                Log.d("BC_LOG", "jsonError");
            }
        });
    }

    /**
     * Update local entity with data retrieved from existing User Entity
     * @param jsonObject existing User Entity data from getEntities method
     */
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
                existingEntity = false;
            }
            else{
                existingEntity = true;

                // Get Entity attributes
                for(int i = 0; i < items.length(); i++){
                    JSONObject item = items.getJSONObject(i);
                    JSONObject entityAttr = item.getJSONObject("data");

                    entity.setEntityId(item.getString("entityId"));
                    entity.setEntityType(item.getString("entityType"));
                    entity.setName(entityAttr.getString("name"));
                    entity.setAge(entityAttr.getString("age"));
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
            Log.d("BC_LOG", "PARSE ERROR");
        }
    }

    /**
     * Display Create Entity UI if no entity exists or display existing entity's data
     */
    public void displayEntity(){
        entityNameField.getText().clear();
        entityAgeField.getText().clear();

        if(existingEntity == false){
            entityIdField.setText("Entity ID:");
            entityTypeField.setText("Entity Type:");
            entityNameField.setHint("Entity Name:");
            entityAgeField.setHint("Entity Age:");

            entityStatus.setText("Create a New Entity");
            createButton.setText("Create");
        }
        else{
            String id = "Entity ID: " + entity.getEntityId();
            String type = "Entity Type: " + entity.getEntityType();
            String name = "Entity Name: " + entity.getName();
            String age = "Entity Age: " + entity.getAge();

            entityIdField.setText(id);
            entityTypeField.setText(type);
            entityNameField.setHint(name);
            entityAgeField.setHint(age);

            entityStatus.setText("Update Entity");
            createButton.setText("Update");
            deleteButton.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Create User Entity from local entity's data
     */
    public void createEntity(){
        brainCloud.createEntity(entity, new IServerCallback() {
            @Override
            public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {
                entityStatus.setText("Entity Created!");
                getEntity();
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                entityStatus.setText("Entity Error...");
                Log.d("BC_LOG", jsonError);
            }
        });
    }

    /**
     * Update User Entity with local entity's modified data
     */
    public void updateEntity(){
        brainCloud.updateEntity(entity, new IServerCallback() {
            @Override
            public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {
                entityStatus.setText("Entity Updated!");
                getEntity();
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                entityStatus.setText("Entity Error...!");
                Log.d("BC_LOG", jsonError);
            }
        });
    }

    /**
     * Delete User Entity
     */
    public void deleteEntity(){
        brainCloud.deleteEntity(entity, new IServerCallback() {
            @Override
            public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {
                entityStatus.setText("Entity Deleted!");
                getEntity();
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                entityStatus.setText("Entity Error...");
                Log.d("BC_LOG", jsonError);
            }
        });
    }
}