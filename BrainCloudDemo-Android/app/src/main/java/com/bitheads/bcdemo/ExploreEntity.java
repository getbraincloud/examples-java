package com.bitheads.bcdemo;

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

    private BCClient brainCloud;
    private Entity entity;
    private String entityName;
    private String entityAge;
    private boolean existingEntity;

    // UI components
    private TextView entityStatus;
    private TextView entityIdField;
    private TextView entityTypeField;
    private EditText entityNameField;
    private EditText entityAgeField;
    private TextView emptyFields;
    private Button createButton;
    private Button deleteButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explore_entity);

        brainCloud = AuthenticateMenu.brainCloud;

        // Get reference to UI components
        TextView bcInitStatus = findViewById(R.id.bc_init_status_tv);
        entityStatus = findViewById(R.id.entity_title_tv);
        entityIdField = findViewById(R.id.entity_id_tv);
        entityTypeField = findViewById(R.id.entity_type_tv);
        entityNameField = findViewById(R.id.entity_name_et);
        entityAgeField = findViewById(R.id.entity_age_et);
        emptyFields = findViewById(R.id.empty_field_tv);
        createButton = findViewById(R.id.create_b);
        deleteButton =findViewById(R.id.delete_b);
        Button backButton = findViewById(R.id.back_b);

        bcInitStatus.setText(brainCloud.getVersion());

        entity = new Entity();

        // Look for existing entities
        entityStatus.setText(R.string.find_entity);
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

                if(!existingEntity){
                    entityStatus.setText(R.string.create_entity);
                    createEntity();
                }
                else{
                    entityStatus.setText(R.string.update_entity);
                    updateEntity();
                }
            }
        });

        // Delete entity
        deleteButton.setOnClickListener(view -> {
            entityStatus.setText(R.string.delete_entity);
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
                Log.d("getPage success!", jsonData.toString());
                parseEntityJSON(jsonData);
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                Log.d("getPage failed: ", jsonError);
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
            Log.d("Parse error: ", "Entity JSON failed to parse");
        }

        displayEntity();
    }

    /**
     * Display Create Entity UI if no entity exists or display existing entity's data
     */
    public void displayEntity(){
        entityNameField.getText().clear();
        entityAgeField.getText().clear();

        if(!existingEntity){
            entityIdField.setText(R.string.default_entity_id);
            entityTypeField.setText(R.string.default_entity_type);
            entityNameField.setHint(R.string.default_entity_name);
            entityAgeField.setHint(R.string.default_entity_age);

            entityStatus.setText(R.string.create_entity_status);
            createButton.setText(R.string.create);
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

            entityStatus.setText(R.string.update_entity_status);
            createButton.setText(R.string.update);
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
                Log.d("createEntity success!", jsonData.toString());
                getEntity();
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                entityStatus.setText(R.string.entity_error);
                Log.d("createEntity failed: ", jsonError);
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
                Log.d("updateEntity success!", jsonData.toString());
                getEntity();
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                entityStatus.setText(R.string.entity_error);
                Log.d("updateEntity failed: ", jsonError);
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
                Log.d("deleteEntity success!", jsonData.toString());
                getEntity();
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                entityStatus.setText(R.string.entity_error);
                Log.d("deleteEntity failed: ", jsonError);
            }
        });
    }
}