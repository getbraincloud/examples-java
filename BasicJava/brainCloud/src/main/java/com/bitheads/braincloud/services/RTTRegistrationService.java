package com.bitheads.braincloud.services;

import com.bitheads.braincloud.client.BrainCloudClient;
import com.bitheads.braincloud.client.IServerCallback;
import com.bitheads.braincloud.client.ServiceName;
import com.bitheads.braincloud.client.ServiceOperation;
import com.bitheads.braincloud.comms.ServerCall;

public class RTTRegistrationService {

    private BrainCloudClient _client;

    public RTTRegistrationService(BrainCloudClient client) {
        _client = client;
    }

    /**
     * Requests the event server address
     *
     * @param callback The callback.
     */
    public void requestClientConnection(IServerCallback callback) {
        ServerCall sc = new ServerCall(ServiceName.rttRegistration, ServiceOperation.REQUEST_CLIENT_CONNECTION, null, callback);
        _client.sendRequest(sc);
    }
}
