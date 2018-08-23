package com.bitheads.braincloud.services;

import com.bitheads.braincloud.client.BrainCloudClient;
import com.bitheads.braincloud.client.IServerCallback;
import com.bitheads.braincloud.client.ServiceName;
import com.bitheads.braincloud.client.ServiceOperation;
import com.bitheads.braincloud.comms.ServerCall;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class to handle data stream api calls.
 */
public class DataStreamService {
    private BrainCloudClient _client;

    public DataStreamService(BrainCloudClient client) {
        _client = client;
    }

    private enum Parameter {
        eventName,
        eventProperties
    }

    /**
     * Creates custom data stream page event
     *
     * @param eventName Name of event
     * @param jsonEventProperties Properties of event
     */
    public void customPageEvent(String eventName, String jsonEventProperties, IServerCallback callback) {
        try {
            JSONObject data = new JSONObject();
            data.put(Parameter.eventName.name(), eventName);

            if (StringUtil.IsOptionalParameterValid(jsonEventProperties)) {
                JSONObject jsonEventPropertiesObj = new JSONObject(jsonEventProperties);
                data.put(Parameter.eventProperties.name(), jsonEventPropertiesObj);
            }

            ServerCall serverCall = new ServerCall(ServiceName.dataStream,
                    ServiceOperation.CUSTOM_PAGE_EVENT, data, callback);
            _client.sendRequest(serverCall);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates custom data stream screen event
     *
     * @param eventName Name of event
     * @param jsonEventProperties Properties of event
     */
    public void customScreenEvent(String eventName, String jsonEventProperties, IServerCallback callback) {
        try {
            JSONObject data = new JSONObject();
            data.put(Parameter.eventName.name(), eventName);

            if (StringUtil.IsOptionalParameterValid(jsonEventProperties)) {
                JSONObject jsonEventPropertiesObj = new JSONObject(jsonEventProperties);
                data.put(Parameter.eventProperties.name(), jsonEventPropertiesObj);
            }

            ServerCall serverCall = new ServerCall(ServiceName.dataStream,
                    ServiceOperation.CUSTOM_SCREEN_EVENT, data, callback);
            _client.sendRequest(serverCall);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates custom data stream track event
     *
     * @param eventName Name of event
     * @param jsonEventProperties Properties of event
     */
    public void customTrackEvent(String eventName, String jsonEventProperties, IServerCallback callback) {
        try {
            JSONObject data = new JSONObject();
            data.put(Parameter.eventName.name(), eventName);

            if (StringUtil.IsOptionalParameterValid(jsonEventProperties)) {
                JSONObject jsonEventPropertiesObj = new JSONObject(jsonEventProperties);
                data.put(Parameter.eventProperties.name(), jsonEventPropertiesObj);
            }

            ServerCall serverCall = new ServerCall(ServiceName.dataStream,
                    ServiceOperation.CUSTOM_TRACK_EVENT, data, callback);
            _client.sendRequest(serverCall);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
