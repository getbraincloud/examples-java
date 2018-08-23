package com.bitheads.braincloud.client;

import android.content.Context;
import android.content.SharedPreferences;

import com.bitheads.braincloud.services.AsyncMatchService;
import com.bitheads.braincloud.services.AuthenticationService;
import com.bitheads.braincloud.services.DataStreamService;
import com.bitheads.braincloud.services.EntityService;
import com.bitheads.braincloud.services.EventService;
import com.bitheads.braincloud.services.FileService;
import com.bitheads.braincloud.services.FriendService;
import com.bitheads.braincloud.services.GamificationService;
import com.bitheads.braincloud.services.GlobalAppService;
import com.bitheads.braincloud.services.GlobalEntityService;
import com.bitheads.braincloud.services.GlobalStatisticsService;
import com.bitheads.braincloud.services.GroupService;
import com.bitheads.braincloud.services.IdentityService;
import com.bitheads.braincloud.services.MailService;
import com.bitheads.braincloud.services.MatchMakingService;
import com.bitheads.braincloud.services.OneWayMatchService;
import com.bitheads.braincloud.services.PlaybackStreamService;
import com.bitheads.braincloud.services.PlayerStateService;
import com.bitheads.braincloud.services.PlayerStatisticsEventService;
import com.bitheads.braincloud.services.PlayerStatisticsService;
import com.bitheads.braincloud.services.ProductService;
import com.bitheads.braincloud.services.ProfanityService;
import com.bitheads.braincloud.services.PushNotificationService;
import com.bitheads.braincloud.services.RedemptionCodeService;
import com.bitheads.braincloud.services.S3HandlingService;
import com.bitheads.braincloud.services.ScriptService;
import com.bitheads.braincloud.services.SocialLeaderboardService;
import com.bitheads.braincloud.services.TimeService;
import com.bitheads.braincloud.services.TournamentService;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * The BrainCloudWrapper provides some convenience functionality to developers when they are
 * getting started with the authentication system.
 * <p>
 * By using the wrapper authentication methods, the anonymous and profile ids will be automatically
 * persisted upon successful authentication. When authenticating, any stored anonymous/profile ids will
 * be sent to the server. This strategy is useful when using anonymous authentication.
 */
public class BrainCloudWrapper implements IServerCallback {

    private static final String AUTHENTICATION_ANONYMOUS = "anonymous";
    private static final String _SHARED_PREFERENCES = "bcprefs";
    private static final String _DEFAULT_URL = "https://sharedprod.braincloudservers.com/dispatcherv2";

    private static BrainCloudWrapper _instance = null;


    private Context _context = null;

    private boolean _alwaysAllowProfileSwitch = true;
    private IServerCallback _authenticateCallback = null;

    private BrainCloudClient _client = null;
    private String _wrapperName = "";

    /**
     * Returns a singleton instance of the BrainCloudClient, if this is the BrainCloudWrapper Singleton.
     * Otherwise, return an instance of the BrainCloudClient, if this is an instance of the BrainCloudWrapper.
     *
     * @return A singleton instance of the BrainCloudClient.
     */
    public BrainCloudClient getClient() {
        return _client;
    }

    public BrainCloudWrapper() {
        _client = new BrainCloudClient();
    }

    /**
     * Instantiate a copy of the brainCloud wrapper. Don't use getInstance if creating your own copy.
     * 
     * @param wrapperName value used to differentiate saved wrapper data
     */
    public BrainCloudWrapper(String wrapperName) {
        _wrapperName = wrapperName;
        _client = new BrainCloudClient();
    }

    /**
     * Method returns a singleton instance of the BrainCloudWrapper.
     *
     * @return A singleton instance of the BrainCloudWrapper.
     *
     * @deprecated Use of the *singleton* has been deprecated. We recommend that you create your own *variable* to hold an instance of the brainCloudWrapper. Explanation here: http://getbraincloud.com/apidocs/release-3-6-5/
     */
    public static BrainCloudWrapper getInstance() {

        if (BrainCloudClient.EnableSingletonMode == false) {
            throw new AssertionError(BrainCloudClient.SingletonUseErrorMessage);
        }

        if (_instance == null) {
            _instance = new BrainCloudWrapper();
            BrainCloudClient.setInstance(_instance.getClient());
        }

        return _instance;
    }

    /**
     * Returns a singleton instance of the BrainCloudClient.
     *
     * @return A singleton instance of the BrainCloudClient.
     */
    public static BrainCloudClient getBC() {
        return getInstance().getClient();
    }

    /**
     * Sets the context required for saving anonymous and profile ids to the
     * private SharedPreferences file.
     *
     * @param ctx The application context
     */
    public void setContext(Context ctx) {
        _context = ctx;
    }

    /**
     * Method initializes the BrainCloudClient.
     *
     * @param ctx        The application context
     * @param appId      The app id
     * @param secretKey  The secret key for your app
     * @param appVersion The app version
     */
    public void initialize(Context ctx, String appId, String secretKey, String appVersion) {
        setContext(ctx);
        getClient().initialize(appId, secretKey, appVersion, _DEFAULT_URL);
    }

    /**
     * Method initializes the BrainCloudClient.
     *
     * @param ctx        The application context
     * @param appId      The app id
     * @param secretKey  The secret key for your app
     * @param appVersion The app version
     * @param serverUrl  The url to the brainCloud server
     */
    public void initialize(Context ctx, String appId, String secretKey, String appVersion, String serverUrl) {
        setContext(ctx);
        getClient().initialize(appId, secretKey, appVersion, serverUrl);
    }


    /**
     * Method initializes the BrainCloudClient. Make sure to
     * set the context via setContext() if you're using this method.
     *
     * @param appId      The app id
     * @param secretKey  The secret key for your app
     * @param appVersion The app version
     * @param serverUrl  The url to the brainCloud server
     */
    public void initialize(String appId, String secretKey, String appVersion, String serverUrl) {
        getClient().initialize(appId, secretKey, appVersion, serverUrl);
    }

    /**
     * Method initializes the BrainCloudClient. Make sure to
     * set the context via setContext() if you're using this method.
     *
     * @param appId      The app id
     * @param secretKey  The secret key for your app
     * @param appVersion The app version
     */
    public void initialize(String appId, String secretKey, String appVersion) {
        getClient().initialize(appId, secretKey, appVersion);
    }

    protected void initializeIdentity(boolean isAnonymousAuth) {

        // check if we already have saved IDs
        String profileId = getStoredProfileId();
        String anonymousId = getStoredAnonymousId();

        // create an anonymous ID if necessary
        if ((!anonymousId.isEmpty() && profileId.isEmpty()) || anonymousId.isEmpty()) {
            anonymousId = getClient().getAuthenticationService().generateAnonymousId();
            profileId = "";
            setStoredAnonymousId(anonymousId);
            setStoredProfileId(profileId);
        }

        String profileIdToAuthenticateWith = profileId;
        if (!isAnonymousAuth && _alwaysAllowProfileSwitch) {
            profileIdToAuthenticateWith = "";
        }
        setStoredAuthenticationType(isAnonymousAuth ? AUTHENTICATION_ANONYMOUS : "");

        // send our IDs to brainCloud
        getClient().initializeIdentity(profileIdToAuthenticateWith, anonymousId);
    }


    /**
     * Combines the wrapperName and the _SHARED_PREFERENCES to create a unique save name
     *
     * ie. userone_bcprefs
     *
     * @return
     */
    private String getSaveName() {
        String prefix = _wrapperName.isEmpty() ? "" : "_" + _wrapperName;
        return prefix + _SHARED_PREFERENCES;
    }

    /**
     * Returns the stored profile id
     *
     * @return The stored profile id
     */
    public String getStoredProfileId() {
        String saveName = getSaveName();

        SharedPreferences sharedPref = _context.getSharedPreferences(saveName, Context.MODE_PRIVATE);
        return sharedPref.getString("profileId", "");
    }

    /**
     * Sets the stored profile id
     *
     * @param profileId The profile id to set
     */
    public void setStoredProfileId(String profileId) {
        String saveName = getSaveName();

        SharedPreferences sharedPref = _context.getSharedPreferences(saveName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("profileId", profileId);
        editor.commit();
    }


    /**
     * Resets the profile id to empty string
     */
    public void resetStoredProfileId() {
        setStoredProfileId("");
    }

    /**
     * Returns the stored anonymous id
     *
     * @return The stored anonymous id
     */
    String getStoredAnonymousId() {
        SharedPreferences sharedPref = _context.getSharedPreferences(_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPref.getString("anonymousId", "");
    }

    /**
     * Sets the stored anonymous id
     *
     * @param anonymousId The anonymous id to set
     */
    public void setStoredAnonymousId(String anonymousId) {
        SharedPreferences sharedPref = _context.getSharedPreferences(_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("anonymousId", anonymousId);
        editor.commit();
    }

    /**
     * Resets the anonymous id to empty string
     */
    public void resetStoredAnonymousId() {
        setStoredAnonymousId("");
    }

    /**
     * For non-anonymous authentication methods, a profile id will be passed in
     * when this value is set to false. This will generate an error on the server
     * if the profile id passed in does not match the profile associated with the
     * authentication credentials. By default, this value is true.
     *
     * @param alwaysAllow Controls whether the profile id is passed in with
     *                    non-anonymous authentications.
     */
    public void setAlwaysAllowProfileSwitch(boolean alwaysAllow) {
        _alwaysAllowProfileSwitch = alwaysAllow;
    }

    /**
     * Returns the value for always allow profile switch
     *
     * @return Whether to always allow profile switches
     */
    public boolean getAlwaysAllowProfileSwitch() {
        return _alwaysAllowProfileSwitch;
    }

    // these methods are not really used
    protected String getStoredAuthenticationType() {
        SharedPreferences sharedPref = _context.getSharedPreferences(_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPref.getString("authenticationType", "");
    }

    protected void setStoredAuthenticationType(String authenticationType) {
        SharedPreferences sharedPref = _context.getSharedPreferences(_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("authenticationType", authenticationType);
        editor.commit();
    }

    protected void resetStoredAuthenticationType() {
        setStoredAuthenticationType("");
    }

    /**
     * Authenticate a user anonymously with brainCloud - used for apps that
     * don't want to bother the user to login, or for users who are sensitive to
     * their privacy
     *
     * @param callback The callback handler
     */
    public void authenticateAnonymous(IServerCallback callback) {
        _authenticateCallback = callback;

        initializeIdentity(true);

        getClient().getAuthenticationService().authenticateAnonymous(true, this);
    }

    /**
     * Authenticate the user with a custom Email and Password. Note that the
     * client app is responsible for collecting (and storing) the e-mail and
     * potentially password (for convenience) in the client data. For the
     * greatest security, force the user to re-enter their * password at each
     * login. (Or at least give them that option).
     * <p>
     * Note that the password sent from the client to the server is protected
     * via SSL.
     *
     * @param email       The e-mail address of the user
     * @param password    The password of the user
     * @param forceCreate Should a new profile be created for this user if the account
     *                    does not exist?
     * @param callback    The callback handler
     */
    public void authenticateEmailPassword(String email,
                                          String password,
                                          boolean forceCreate,
                                          IServerCallback callback) {
        _authenticateCallback = callback;

        initializeIdentity(false);

        getClient().getAuthenticationService().authenticateEmailPassword(email, password, forceCreate, this);
    }

    /**
     * Authenticate the user via cloud code (which in turn validates the supplied credentials against an external system).
     * This allows the developer to extend brainCloud authentication to support other backend authentication systems.
     * <p>
     * Service Name - Authenticate
     * Server Operation - Authenticate
     *
     * @param userId           The user id
     * @param token            The user token (password etc)
     * @param externalAuthName The name of the cloud script to call for external authentication
     * @param forceCreate      Should a new profile be created for this user if the account
     *                         does not exist?
     * @returns performs the in_success callback on success, in_failure callback on failure
     */
    public void authenticateExternal(String userId,
                                     String token,
                                     String externalAuthName,
                                     boolean forceCreate,
                                     IServerCallback callback) {
        _authenticateCallback = callback;

        initializeIdentity(false);

        getClient().getAuthenticationService().authenticateExternal(userId, token, externalAuthName, forceCreate, this);
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
    public void authenticateFacebook(String fbUserId,
                                     String fbAuthToken,
                                     boolean forceCreate,
                                     IServerCallback callback) {
        _authenticateCallback = callback;

        initializeIdentity(false);

        getClient().getAuthenticationService().authenticateFacebook(fbUserId, fbAuthToken, forceCreate, this);
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
    public void authenticateGoogle(String googleUserId,
                                   String googleAuthToken,
                                   boolean forceCreate,
                                   IServerCallback callback) {
        _authenticateCallback = callback;

        initializeIdentity(false);

        getClient().getAuthenticationService().authenticateGoogle(googleUserId, googleAuthToken, forceCreate, this);
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
    public void authenticateSteam(String steamUserId,
                                  String steamSessionTicket,
                                  boolean forceCreate,
                                  IServerCallback callback) {
        _authenticateCallback = callback;

        initializeIdentity(false);

        getClient().getAuthenticationService().authenticateSteam(steamUserId, steamSessionTicket, forceCreate, this);
    }

    /**
     * Authenticate the user using a Twitter userid, authentication token, and secret from Twitter.
     * <p>
     * Service Name - Authenticate
     * Service Operation - Authenticate
     *
     * @param userId      String representation of Twitter userid
     * @param token       The authentication token derived via the Twitter apis.
     * @param secret      The secret given when attempting to link with Twitter
     * @param forceCreate Should a new profile be created for this user if the account does not exist?
     * @param callback    The callback handler
     * @returns performs the in_success callback on success, in_failure callback on failure
     */
    public void authenticateTwitter(String userId,
                                    String token,
                                    String secret,
                                    boolean forceCreate,
                                    IServerCallback callback) {
        _authenticateCallback = callback;

        initializeIdentity(false);

        getClient().getAuthenticationService().authenticateTwitter(userId, token, secret, forceCreate, this);
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
    public void authenticateUniversal(String userId,
                                      String userPassword,
                                      boolean forceCreate,
                                      IServerCallback callback) {
        _authenticateCallback = callback;

        initializeIdentity(false);

        getClient().getAuthenticationService().authenticateUniversal(userId, userPassword, forceCreate, this);
    }

    /**
     * Re-authenticates the user with brainCloud
     *
     * @param callback The callback handler
     */
    public void reconnect(IServerCallback callback) {
        authenticateAnonymous(callback);
    }

    /**
     * Run callbacks, to be called once per frame from your main thread
     */
    public void runCallbacks() {
        getClient().runCallbacks();
    }


    /**
     * The serverCallback() method returns server data back to the layer
     * interfacing with the BrainCloud library.
     *
     * @param serviceName      - name of the requested service
     * @param serviceOperation - requested operation
     * @param jsonData         - returned data from the server
     */
    public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {
        if (serviceName.equals(ServiceName.authenticationV2) && serviceOperation.equals(ServiceOperation.AUTHENTICATE)) {
            try {
                String profileId = jsonData.getJSONObject("data").getString("profileId");
                if (!profileId.isEmpty()) {
                    setStoredProfileId(profileId);
                }
            } catch (JSONException je) {
                je.printStackTrace();
            }
        }

        if (_authenticateCallback != null) {
            _authenticateCallback.serverCallback(serviceName, serviceOperation, jsonData);
        }
    }

    /**
     * Errors are returned back to the layer which is interfacing with the
     * BrainCloud library through the serverError() callback.
     * <p>
     * A server error might indicate a failure of the client to communicate
     * with the server after N retries.
     *
     * @param serviceName      - name of the requested service
     * @param serviceOperation - requested operation
     * @param statusCode       The error status return code (400, 403, 500, etc)
     * @param reasonCode       The brainCloud reason code (see reason codes on apidocs site)
     * @param jsonError        The error json string
     */
    public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
        if (_authenticateCallback != null) {
            _authenticateCallback.serverError(serviceName, serviceOperation, statusCode, reasonCode, jsonError);
        }
    }


    // brainCloud Services
    public AsyncMatchService getAsyncMatchService() {
        return _client.getAsyncMatchService();
    }

    public DataStreamService getDataStreamService() {
        return _client.getDataStreamService();
    }

    public EntityService getEntityService() {
        return _client.getEntityService();
    }

    public EventService getEventService() {
        return _client.getEventService();
    }

    public FileService getFileService() {
        return _client.getFileService();
    }

    public FriendService getFriendService() {
        return _client.getFriendService();
    }

    public GamificationService getGamificationService() {
        return _client.getGamificationService();
    }

    public GlobalAppService getGlobalAppService() {
        return _client.getGlobalAppService();
    }

    public GlobalEntityService getGlobalEntityService() {
        return _client.getGlobalEntityService();
    }

    public GlobalStatisticsService getGlobalStatisticsService() {
        return _client.getGlobalStatisticsService();
    }

    public GroupService getGroupService() {
        return _client.getGroupService();
    }

    public IdentityService getIdentityService() {
        return _client.getIdentityService();
    }

    public MailService getMailService() {
        return _client.getMailService();
    }

    public MatchMakingService getMatchMakingService() {
        return _client.getMatchMakingService();
    }

    public OneWayMatchService getOneWayMatchService() {
        return _client.getOneWayMatchService();
    }

    public PlaybackStreamService getPlaybackStreamService() {
        return _client.getPlaybackStreamService();
    }

    public PlayerStateService getPlayerStateService() {
        return _client.getPlayerStateService();
    }

    public PlayerStatisticsService getPlayerStatisticsService() {
        return _client.getPlayerStatisticsService();
    }

    public PlayerStatisticsEventService getPlayerStatisticsEventService() {
        return _client.getPlayerStatisticsEventService();
    }

    public ProductService getProductService() {
        return _client.getProductService();
    }

    public ProfanityService getProfanityService() {
        return _client.getProfanityService();
    }

    public PushNotificationService getPushNotificationService() {
        return _client.getPushNotificationService();
    }

    public RedemptionCodeService getRedemptionCodeService() {
        return _client.getRedemptionCodeService();
    }

    public S3HandlingService getS3HandlingService() {
        return _client.getS3HandlingService();
    }

    public ScriptService getScriptService() {
        return _client.getScriptService();
    }

    public SocialLeaderboardService getSocialLeaderboardService() {
        return _client.getSocialLeaderboardService();
    }

    public TimeService getTimeService() {
        return _client.getTimeService();
    }

    public TournamentService getTournamentService() {
        return _client.getTournamentService();
    }

}
