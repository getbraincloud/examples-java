package com.bitheads.braincloud.services;

import com.bitheads.braincloud.client.BrainCloudClient;
import com.bitheads.braincloud.client.IServerCallback;
import com.bitheads.braincloud.client.ServiceName;
import com.bitheads.braincloud.client.ServiceOperation;
import com.bitheads.braincloud.comms.ServerCall;

public class GlobalAppService {

    private BrainCloudClient _client;

    public GlobalAppService(BrainCloudClient client) {
        _client = client;
    }

    /**
     * Method returns all the global properties of a game.
     *
     * @param callback The callback.
     */
    public void readProperties(IServerCallback callback) {
        ServerCall sc = new ServerCall(ServiceName.globalApp, ServiceOperation.READ_PROPERTIES, null, callback);
        _client.sendRequest(sc);
    }

}
