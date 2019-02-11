package com.bitheads.braincloud.services;

import com.bitheads.braincloud.client.BrainCloudClient;
import com.bitheads.braincloud.client.IServerCallback;
import com.bitheads.braincloud.client.ServiceName;
import com.bitheads.braincloud.client.ServiceOperation;
import com.bitheads.braincloud.comms.ServerCall;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by David St-Louis on 2018-07-04
 */
public class LobbyService {

    private enum Parameter {
        lobbyType,
        rating,
        maxSteps,
        algo,
        filterJson,
        otherUserCxIds,
        settings,
        isReady,
        extraJson,
        teamCode,
        lobbyId,
        cxId,
        signalData,
        toTeamCode
    }

    private BrainCloudClient _client;

    public LobbyService(BrainCloudClient client) {
        _client = client;
    }

    /**
     * Creates a new lobby.
     * 
     * Sends LOBBY_JOIN_SUCCESS message to the user, with full copy of lobby data Sends LOBBY_MEMBER_JOINED to all lobby members, with copy of member data
     *
     * Service Name - Lobby
     * Service Operation - CREATE_LOBBY
     *
     * @param lobbyType The type of lobby to look for. Lobby types are defined in the portal.
     * @param rating The skill rating to use for finding the lobby. Provided as a separate parameter because it may not exactly match the user's rating (especially in cases where parties are involved).
     * @param otherUserCxIds Array of other users (i.e. party members) to add to the lobby as well. Will constrain things so that only lobbies with room for all players will be considered.
     * @param isReady Initial ready-status of this user.
     * @param extraJson Initial extra-data about this user.
     * @param teamCode Preferred team for this user, if applicable. Send "" or null for automatic assignment.
     * @param settings Configuration data for the room.
     */
    public void createLobby(String lobbyType, int rating, ArrayList<String> otherUserCxIds, Boolean isReady, String extraJson, String teamCode, String settings, IServerCallback callback) {
        try {
            JSONObject data = new JSONObject();
            data.put(Parameter.lobbyType.name(), lobbyType);
            data.put(Parameter.rating.name(), rating);
            if (otherUserCxIds != null) {
                data.put(Parameter.otherUserCxIds.name(), new JSONArray(otherUserCxIds));
            }
            data.put(Parameter.isReady.name(), isReady);
            if (StringUtil.IsOptionalParameterValid(extraJson)) {
                data.put(Parameter.extraJson.name(), new JSONObject(extraJson));
            }
            data.put(Parameter.teamCode.name(), teamCode);
            if (StringUtil.IsOptionalParameterValid(settings)) {
                data.put(Parameter.settings.name(), new JSONObject(settings));
            }

            ServerCall sc = new ServerCall(ServiceName.lobby,
                    ServiceOperation.CREATE_LOBBY, data, callback);
            _client.sendRequest(sc);
        } catch (JSONException je) {
            je.printStackTrace();
        }
    }

    /**
     * Finds a lobby matching the specified parameters. Asynchronous - returns 200 to indicate that matchmaking has started.
     *
     * Service Name - Lobby
     * Service Operation - FIND_LOBBY
     *
     * @param lobbyType The type of lobby to look for. Lobby types are defined in the portal.
     * @param rating The skill rating to use for finding the lobby. Provided as a separate parameter because it may not exactly match the user's rating (especially in cases where parties are involved).
     * @param maxSteps The maximum number of steps to wait when looking for an applicable lobby. Each step is ~5 seconds.
     * @param algo The algorithm to use for increasing the search scope.
     * @param filterJson Used to help filter the list of rooms to consider. Passed to the matchmaking filter, if configured.
     * @param otherUserCxIds Array of other users (i.e. party members) to add to the lobby as well. Will constrain things so that only lobbies with room for all players will be considered.
     * @param isReady Initial ready-status of this user.
     * @param extraJson Initial extra-data about this user.
     * @param teamCode Preferred team for this user, if applicable. Send "" or null for automatic assignment
     */
    public void findLobby(String lobbyType, int rating, int maxSteps, String algo, String filterJson, ArrayList<String> otherUserCxIds, Boolean isReady, String extraJson, String teamCode, IServerCallback callback) {
        try {
            JSONObject data = new JSONObject();
            data.put(Parameter.lobbyType.name(), lobbyType);
            data.put(Parameter.rating.name(), rating);
            data.put(Parameter.maxSteps.name(), maxSteps);
            if (StringUtil.IsOptionalParameterValid(algo)) {
                data.put(Parameter.algo.name(), new JSONObject(algo));
            }
            if (StringUtil.IsOptionalParameterValid(filterJson)) {
                data.put(Parameter.filterJson.name(), new JSONObject(filterJson));
            }
            if (otherUserCxIds != null) {
                data.put(Parameter.otherUserCxIds.name(), new JSONArray(otherUserCxIds));
            }
            data.put(Parameter.isReady.name(), isReady);
            if (StringUtil.IsOptionalParameterValid(extraJson)) {
                data.put(Parameter.extraJson.name(), new JSONObject(extraJson));
            }
            data.put(Parameter.teamCode.name(), teamCode);

            ServerCall sc = new ServerCall(ServiceName.lobby,
                    ServiceOperation.FIND_LOBBY, data, callback);
            _client.sendRequest(sc);
        } catch (JSONException je) {
            je.printStackTrace();
        }
    }

    /**
     * Adds the caller to the lobby entry queue and will create a lobby if none are found.
     *
     * Service Name - Lobby
     * Service Operation - FIND_OR_CREATE_LOBBY
     *
     * @param lobbyType The type of lobby to look for. Lobby types are defined in the portal.
     * @param rating The skill rating to use for finding the lobby. Provided as a separate parameter because it may not exactly match the user's rating (especially in cases where parties are involved).
     * @param maxSteps The maximum number of steps to wait when looking for an applicable lobby. Each step is ~5 seconds.
     * @param algo The algorithm to use for increasing the search scope.
     * @param filterJson Used to help filter the list of rooms to consider. Passed to the matchmaking filter, if configured.
     * @param otherUserCxIds Array of other users (i.e. party members) to add to the lobby as well. Will constrain things so that only lobbies with room for all players will be considered.
     * @param settings Configuration data for the room.
     * @param isReady Initial ready-status of this user.
     * @param extraJson Initial extra-data about this user.
     * @param teamCode Preferred team for this user, if applicable. Send "" or null for automatic assignment.
     */
    public void findOrCreateLobby(String lobbyType, int rating, int maxSteps, String algo, String filterJson, ArrayList<String> otherUserCxIds, String settings, Boolean isReady, String extraJson, String teamCode, IServerCallback callback) {
        try {
            JSONObject data = new JSONObject();
            data.put(Parameter.lobbyType.name(), lobbyType);
            data.put(Parameter.rating.name(), rating);
            data.put(Parameter.maxSteps.name(), maxSteps);
            if (StringUtil.IsOptionalParameterValid(algo)) {
                data.put(Parameter.algo.name(), new JSONObject(algo));
            }
            if (StringUtil.IsOptionalParameterValid(filterJson)) {
                data.put(Parameter.filterJson.name(), new JSONObject(filterJson));
            }
            if (otherUserCxIds != null) {
                data.put(Parameter.otherUserCxIds.name(), new JSONArray(otherUserCxIds));
            }
            if (StringUtil.IsOptionalParameterValid(settings)) {
                data.put(Parameter.settings.name(), new JSONObject(settings));
            }
            data.put(Parameter.isReady.name(), isReady);
            if (StringUtil.IsOptionalParameterValid(extraJson)) {
                data.put(Parameter.extraJson.name(), new JSONObject(extraJson));
            }
            data.put(Parameter.teamCode.name(), teamCode);

            ServerCall sc = new ServerCall(ServiceName.lobby,
                    ServiceOperation.FIND_OR_CREATE_LOBBY, data, callback);
            _client.sendRequest(sc);
        } catch (JSONException je) {
            je.printStackTrace();
        }
    }

    /**
     * Returns the data for the specified lobby, including member data.
     *
     * Service Name - Lobby
     * Service Operation - GET_LOBBY_DATA
     *
     * @param lobbyId Id of chosen lobby.
     */
    public void getLobbyData(String lobbyId, IServerCallback callback) {
        try {
            JSONObject data = new JSONObject();
            data.put(Parameter.lobbyId.name(), lobbyId);

            ServerCall sc = new ServerCall(ServiceName.lobby,
                    ServiceOperation.GET_LOBBY_DATA, data, callback);
            _client.sendRequest(sc);
        } catch (JSONException je) {
            je.printStackTrace();
        }
    }

    /**
     * Causes the caller to leave the specified lobby. If the user was the owner, a new owner will be chosen. If user was the last member, the lobby will be deleted.
     *
     * Service Name - Lobby
     * Service Operation - LEAVE_LOBBY
     *
     * @param lobbyId Id of chosen lobby.
     */
    public void leaveLobby(String lobbyId, IServerCallback callback) {
        try {
            JSONObject data = new JSONObject();
            data.put(Parameter.lobbyId.name(), lobbyId);

            ServerCall sc = new ServerCall(ServiceName.lobby,
                    ServiceOperation.LEAVE_LOBBY, data, callback);
            _client.sendRequest(sc);
        } catch (JSONException je) {
            je.printStackTrace();
        }
    }

    /**
     * Evicts the specified user from the specified lobby. The caller must be the owner of the lobby.
     *
     * Service Name - Lobby
     * Service Operation - REMOVE_MEMBER
     *
     * @param lobbyId Id of chosen lobby.
     * @param cxId Specified member to be removed from the lobby.
     */
    public void removeMember(String lobbyId, String cxId, IServerCallback callback) {
        try {
            JSONObject data = new JSONObject();
            data.put(Parameter.lobbyId.name(), lobbyId);
            data.put(Parameter.cxId.name(), cxId);

            ServerCall sc = new ServerCall(ServiceName.lobby,
                    ServiceOperation.REMOVE_MEMBER, data, callback);
            _client.sendRequest(sc);
        } catch (JSONException je) {
            je.printStackTrace();
        }
    }

    /**
     * Sends LOBBY_SIGNAL_DATA message to all lobby members.
     *
     * Service Name - Lobby
     * Service Operation - SEND_SIGNAL
     *
     * @param lobbyId Id of chosen lobby.
     * @param signalData Signal data to be sent.
     */
    public void sendSignal(String lobbyId, String signalData, IServerCallback callback) {
        try {
            JSONObject data = new JSONObject();
            data.put(Parameter.lobbyId.name(), lobbyId);
            if (StringUtil.IsOptionalParameterValid(signalData)) {
                data.put(Parameter.signalData.name(), new JSONObject(signalData));
            }

            ServerCall sc = new ServerCall(ServiceName.lobby,
                    ServiceOperation.SEND_SIGNAL, data, callback);
            _client.sendRequest(sc);
        } catch (JSONException je) {
            je.printStackTrace();
        }
    }

    /**
     * Switches to the specified team (if allowed.)
     * 
     * Sends LOBBY_MEMBER_UPDATED to all lobby members, with copy of member data
     *
     * Service Name - Lobby
     * Service Operation - SWITCH_TEAM
     *
     * @param lobbyId Id of chosen lobby.
     * @param toTeamCode Specified team code.
     */
    public void switchTeam(String lobbyId, String toTeamCode, IServerCallback callback) {
        try {
            JSONObject data = new JSONObject();
            data.put(Parameter.lobbyId.name(), lobbyId);
            data.put(Parameter.toTeamCode.name(), toTeamCode);

            ServerCall sc = new ServerCall(ServiceName.lobby,
                    ServiceOperation.SWITCH_TEAM, data, callback);
            _client.sendRequest(sc);
        } catch (JSONException je) {
            je.printStackTrace();
        }
    }

    /**
     * Updates the ready status and extra json for the given lobby member.
     *
     * Service Name - Lobby
     * Service Operation - UPDATE_READY
     *
     * @param lobbyId The type of lobby to look for. Lobby types are defined in the portal.
     * @param isReady Initial ready-status of this user.
     * @param extraJson Initial extra-data about this user.
     */
    public void updateReady(String lobbyId, Boolean isReady, String extraJson, IServerCallback callback) {
        try {
            JSONObject data = new JSONObject();
            data.put(Parameter.lobbyId.name(), lobbyId);
            data.put(Parameter.isReady.name(), isReady);
            if (StringUtil.IsOptionalParameterValid(extraJson)) {
                data.put(Parameter.extraJson.name(), new JSONObject(extraJson));
            }

            ServerCall sc = new ServerCall(ServiceName.lobby,
                    ServiceOperation.UPDATE_READY, data, callback);
            _client.sendRequest(sc);
        } catch (JSONException je) {
            je.printStackTrace();
        }
    }

    /**
     * Updates the ready status and extra json for the given lobby member.
     *
     * Service Name - Lobby
     * Service Operation - UPDATE_SETTINGS
     *
     * @param lobbyId Id of the specfified lobby.
     * @param settings Configuration data for the room.
     */
    public void updateSettings(String lobbyId, String settings, IServerCallback callback) {
        try {
            JSONObject data = new JSONObject();
            data.put(Parameter.lobbyId.name(), lobbyId);
            if (StringUtil.IsOptionalParameterValid(settings)) {
                data.put(Parameter.settings.name(), new JSONObject(settings));
            }

            ServerCall sc = new ServerCall(ServiceName.lobby,
                    ServiceOperation.UPDATE_SETTINGS, data, callback);
            _client.sendRequest(sc);
        } catch (JSONException je) {
            je.printStackTrace();
        }
    }
}
