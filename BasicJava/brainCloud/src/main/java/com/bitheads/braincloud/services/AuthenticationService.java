package com.bitheads.braincloud.services;

import com.bitheads.braincloud.client.AuthenticationType;
import com.bitheads.braincloud.client.BrainCloudClient;
import com.bitheads.braincloud.client.IServerCallback;
import com.bitheads.braincloud.client.ServiceName;
import com.bitheads.braincloud.client.ServiceOperation;
import com.bitheads.braincloud.comms.ServerCall;

import org.json.JSONException;
import org.json.JSONObject;

public class AuthenticationService {

    private BrainCloudClient _client;

    public AuthenticationService(BrainCloudClient client) {
        _client = client;
    }

    private enum Parameter {
        externalId,
        authenticationToken,
        authenticationType,
        gameId,
        forceCreate,
        releasePlatform,
        version,
        externalAuthName,
        profileId,
        anonymousId,
        upgradeAppId,
        countryCode,
        languageCode,
        timeZoneOffset
    }

    private String _anonymousId;
    private String _profileId;

    public String getAnonymousId() {
        return _anonymousId;
    }

    public void setAnonymousId(String anonymousId) {
        _anonymousId = anonymousId;
    }

    public String getProfileId() {
        return _profileId;
    }

    public void setProfileId(String profileId) {
        _profileId = profileId;
    }

    /**
     * Initialize - initializes the identity service with a saved
     * anonymous installation ID and most recently used profile ID
     *
     * @param anonymousId The anonymous installation id that was generated for this device
     * @param profileId   The id of the profile id that was most recently used by the app (on this device)
     */
    public void initialize(String profileId, String anonymousId) {
        _anonymousId = anonymousId;
        _profileId = profileId;
    }

    /**
     * Used to clear the saved profile id - to use in cases when the user is
     * attempting to switch to a different game profile.
     */
    public void clearSavedProfileId() {
        _profileId = "";
    }

    /**
     * Used to create the anonymous installation id for the brainCloud profile.
     * @return A unique Anonymous ID
     */
    public String generateAnonymousId() {
        return java.util.UUID.randomUUID().toString();
    }

    /**
     * Authenticate a user anonymously with brainCloud - used for apps that
     * don't want to bother the user to login, or for users who are sensitive to
     * their privacy
     *
     * @param forceCreate Should a new profile be created if it does not exist?
     * @param callback    The callback handler
     */
    public void authenticateAnonymous(boolean forceCreate, IServerCallback callback) {
        authenticate(_anonymousId, "", AuthenticationType.Anonymous, null, forceCreate, callback);
    }

    /**
     * Authenticate the user with a custom Email and Password. Note that the
     * client app is responsible for collecting (and storing) the e-mail and
     * potentially password (for convenience) in the client data. For the
     * greatest security, force the user to re-enter their * password at each
     * login. (Or at least give them that option).
     * <p/>
     * Note that the password sent from the client to the server is protected
     * via SSL.
     *
     * @param email       The e-mail address of the user
     * @param password    The password of the user
     * @param forceCreate Should a new profile be created for this user if the account
     *                    does not exist?
     * @param callback    The callback handler
     */
    public void authenticateEmailPassword(String email, String password, boolean forceCreate, IServerCallback callback) {
        authenticate(email, password, AuthenticationType.Email, null, forceCreate, callback);
    }

    /**
     * Authenticate the user via cloud code (which in turn validates the supplied credentials against an external system).
     * This allows the developer to extend brainCloud authentication to support other backend authentication systems.
     * <p/>
     * Service Name - Authenticate
     * Server Operation - Authenticate
     *
     * @param userId           The user id
     * @param token            The user token (password etc)
     * @param externalAuthName The name of the cloud script to call for external authentication
     * @param forceCreate      Should a new profile be created for this user if the account
     *                         does not exist?
     */
    public void authenticateExternal(
            String userId,
            String token,
            String externalAuthName,
            boolean forceCreate,
            IServerCallback callback) {
        authenticate(userId, token, AuthenticationType.External, externalAuthName, forceCreate, callback);
    }

    /**
     * Authenticate the user with brainCloud using their Facebook Credentials
     *
     * @param fbUserId    The facebook id of the user
     * @param fbAuthToken The validated token from the Facebook SDK (that will be
     *                    further validated when sent to the bC service)
     * @param forceCreate Should a new profile be created for this user if the account
     *                    does not exist?
     * @param callback    The callback handler
     */
    public void authenticateFacebook(String fbUserId, String fbAuthToken, boolean forceCreate, IServerCallback callback) {
        authenticate(fbUserId, fbAuthToken, AuthenticationType.Facebook, null, forceCreate, callback);
    }

    /**
     * Authenticate the user using a google userid(email address) and google
     * authentication token.
     *
     * @param googleUserId    String representation of google+ userid (email)
     * @param googleAuthToken The authentication token derived via the google apis.
     * @param forceCreate     Should a new profile be created for this user if the account
     *                        does not exist?
     * @param callback        The callback handler
     */
    public void authenticateGoogle(String googleUserId, String googleAuthToken, boolean forceCreate, IServerCallback callback) {
        authenticate(googleUserId, googleAuthToken, AuthenticationType.Google, null, forceCreate, callback);
    }

    /**
     * Authenticate the user using a steam userid and session ticket (without
     * any validation on the userid).
     *
     * @param steamUserId        String representation of 64 bit steam id
     * @param steamSessionTicket The session ticket of the user (hex encoded)
     * @param forceCreate        Should a new profile be created for this user if the account
     *                           does not exist?
     * @param callback           The callback handler
     */
    public void authenticateSteam(String steamUserId, String steamSessionTicket, boolean forceCreate, IServerCallback callback) {
        authenticate(steamUserId, steamSessionTicket, AuthenticationType.Steam, null, forceCreate, callback);
    }

    /**
     * Authenticate the user using a Twitter userid, authentication token, and secret from Twitter.
     * <p/>
     * Service Name - Authenticate
     * Service Operation - Authenticate
     *
     * @param userId      String representation of Twitter userid
     * @param token       The authentication token derived via the Twitter apis.
     * @param secret      The secret given when attempting to link with Twitter
     * @param forceCreate Should a new profile be created for this user if the account does not exist?
     * @param callback    The callback handler
     */
    public void authenticateTwitter(String userId,
                                    String token,
                                    String secret,
                                    boolean forceCreate,
                                    IServerCallback callback) {
        String tokenSecretCombo = token + ":" + secret;
        authenticate(userId, tokenSecretCombo, AuthenticationType.Twitter, null, forceCreate, callback);
    }


    /**
     * Authenticate the user using a userid and password (without any validation
     * on the userid). Similar to AuthenticateEmailPassword - except that that
     * method has additional features to allow for e-mail validation, password
     * resets, etc.
     *
     * @param userId       The e-mail address of the user
     * @param userPassword The password of the user
     * @param forceCreate  Should a new profile be created for this user if the account
     *                     does not exist?
     * @param callback     The callback handler
     */
    public void authenticateUniversal(String userId, String userPassword, boolean forceCreate, IServerCallback callback) {
        authenticate(userId, userPassword, AuthenticationType.Universal, null, forceCreate, callback);
    }

    /**
     * Authenticate the user using a Parse user ID authentication token.
     *
     * @param userId    String representation of Parse user ID
     * @param authenticationToken The authentication token derived via the google apis.
     * @param forceCreate     Should a new profile be created for this user if the account
     *                        does not exist?
     * @param callback        The callback handler
     */
    public void authenticateParse(String userId, String authenticationToken, boolean forceCreate, IServerCallback callback) {
        authenticate(userId, authenticationToken, AuthenticationType.Parse, null, forceCreate, callback);
    }

    /**
     * Reset Email password - Sends a password reset email to the specified
     * address
     *
     * @param email    The email address to send the reset email to.
     * @param callback The callback handler
     *
     * Note the follow error reason codes:
     * SECURITY_ERROR (40209) - If the email address cannot be found.
     */
    public void resetEmailPassword(String email, IServerCallback callback) {
        try {
            JSONObject message = new JSONObject();
            message.put(Parameter.externalId.name(), email);
            message.put(Parameter.gameId.name(), _client.getAppId());

            ServerCall serverCall = new ServerCall(
                    ServiceName.authenticationV2,
                    ServiceOperation.RESET_EMAIL_PASSWORD, message,
                    callback);
            _client.sendRequest(serverCall);
        } catch (JSONException ignored) {
        }
    }

    private void authenticate(
            String externalId,
            String authenticationToken,
            AuthenticationType authenticationType,
            String externalAuthName,
            boolean forceCreate,
            IServerCallback callback) {
        try {
            JSONObject message = new JSONObject();
            message.put(Parameter.externalId.name(), externalId);
            message.put(Parameter.authenticationToken.name(), authenticationToken);
            message.put(Parameter.authenticationType.name(), authenticationType.toString());
            message.put(Parameter.forceCreate.name(), forceCreate);

            message.put(Parameter.profileId.name(), _profileId);
            message.put(Parameter.anonymousId.name(), _anonymousId);
            message.put(Parameter.gameId.name(), _client.getAppId());
            message.put(Parameter.releasePlatform.name(), _client.getReleasePlatform());
            message.put(Parameter.upgradeAppId.name(), _client.getAppVersion());
            message.put(Parameter.version.name(), _client.getBrainCloudVersion());

            if (StringUtil.IsOptionalParameterValid(externalAuthName)) {
                message.put(Parameter.externalAuthName.name(), externalAuthName);
            }
            message.put(Parameter.countryCode.name(), _client.getCountryCode());
            message.put(Parameter.languageCode.name(), _client.getLanguageCode());
            message.put(Parameter.timeZoneOffset.name(), _client.getTimeZoneOffset());
            message.put("clientLib", "java");

            ServerCall serverCall = new ServerCall(
                    ServiceName.authenticationV2,
                    ServiceOperation.AUTHENTICATE, message, callback);
            _client.sendRequest(serverCall);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
