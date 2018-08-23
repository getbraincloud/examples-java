package com.bitheads.braincloud.services;

import com.bitheads.braincloud.client.BrainCloudClient;
import com.bitheads.braincloud.client.IServerCallback;
import com.bitheads.braincloud.client.ServiceName;
import com.bitheads.braincloud.client.ServiceOperation;
import com.bitheads.braincloud.comms.ServerCall;

import org.json.JSONException;
import org.json.JSONObject;

public class ProductService {

    private enum Parameter {
        platform,
        user_currency,
        category,
        vc_id,
        vc_amount,
        language,
        productId,
        orderId,
        token,
        levelName
    }

    private BrainCloudClient _client;

    public ProductService(BrainCloudClient client) {
        _client = client;
    }

    /**
     * Gets the player's currency for the given currency type
     * or all currency types if null passed in.
     *
     * Service Name - Product
     * Service Operation - GetPlayerVC
     *
     * @param currencyType The currency type to retrieve or null
     * if all currency types are being requested.
     * @param callback The callback.
     */
    public void getCurrency(String currencyType, IServerCallback callback) {
        try {
            JSONObject data = new JSONObject();
            data.put(Parameter.vc_id.name(), currencyType);

            ServerCall sc = new ServerCall(ServiceName.product, ServiceOperation.GET_PLAYER_VC, data, callback);
            _client.sendRequest(sc);
        } catch (JSONException je) {
            je.printStackTrace();
        }
    }

    /**
     * @deprecated Method is now available in Cloud Code only for security
     * If you need to use it client side, enable 'Allow Currency Calls from Client' on the dashboard
     */
    @Deprecated
    public void awardCurrency(String currencyType, long amount, IServerCallback callback) {
        try {
            JSONObject data = new JSONObject();
            data.put(Parameter.vc_id.name(), currencyType);
            data.put(Parameter.vc_amount.name(), amount);

            ServerCall sc = new ServerCall(ServiceName.product, ServiceOperation.AWARD_VC, data, callback);
            _client.sendRequest(sc);
        } catch (JSONException je) {
            je.printStackTrace();
        }
    }

    /**
     * @deprecated Method is recommended to be used in Cloud Code only for security
     * If you need to use it client side, enable 'Allow Currency Calls from Client' on the brainCloud dashboard
     */
    @Deprecated
    public void consumeCurrency(String in_currencyType, long in_amount, IServerCallback callback) {
        try {
            JSONObject data = new JSONObject();
            data.put(Parameter.vc_id.name(), in_currencyType);
            data.put(Parameter.vc_amount.name(), in_amount);

            ServerCall sc = new ServerCall(ServiceName.product, ServiceOperation.CONSUME_VC, data, callback);
            _client.sendRequest(sc);
        } catch (JSONException je) {
            je.printStackTrace();
        }
    }

    /**
     * @deprecated Method is recommended to be used in Cloud Code only for security
     * If you need to use it client side, enable 'Allow Currency Calls from Client' on the brainCloud dashboard
     */
    @Deprecated
    public void resetCurrency(IServerCallback callback) {
        ServerCall sc = new ServerCall(ServiceName.product, ServiceOperation.RESET_PLAYER_VC, null, callback);
        _client.sendRequest(sc);
    }

    /**
     * Method gets the active sales inventory for the passed-in
     * currency type.
     *
     * Service Name - Product
     * Service Operation - GetInventory
     *
     * @param platform The store platform. Valid stores are:
     * - itunes
     * - facebook
     * - appworld
     * - steam
     * - windows
     * - windowsPhone
     * - googlePlay
     * @param userCurrency The currency type to retrieve the sales inventory for.
     * @param callback The callback.
     */
    public void getSalesInventory(String platform,
                                  String userCurrency,
                                  IServerCallback callback) {
        getSalesInventoryByCategory(platform, userCurrency, null, callback);
    }

    /**
     * Method gets the active sales inventory for the passed-in
     * currency type.
     *
     * Service Name - Product
     * Service Operation - GetInventory
     *
     * @param platform The store platform. Valid stores are:
     * - itunes
     * - facebook
     * - appworld
     * - steam
     * - windows
     * - windowsPhone
     * - googlePlay
     * @param userCurrency The currency type to retrieve the sales inventory for.
     * @param category The product category
     * @param callback The callback.
     */
    public void getSalesInventoryByCategory(String platform,
                                            String userCurrency,
                                            String category,
                                            IServerCallback callback) {
        try {
            JSONObject data = new JSONObject();
            data.put(Parameter.platform.name(), platform);

            if (StringUtil.IsOptionalParameterValid(userCurrency)) {
                data.put(Parameter.user_currency.name(), userCurrency);
            }
            if (StringUtil.IsOptionalParameterValid(category)) {
                data.put(Parameter.category.name(), category);
            }

            ServerCall sc = new ServerCall(ServiceName.product, ServiceOperation.GET_INVENTORY, data, callback);
            _client.sendRequest(sc);
        } catch (JSONException je) {
            je.printStackTrace();
        }
    }

    /**
     * Returns the eligible promotions for the player.
     *
     * Service Name - Product
     * Service Operation - EligiblePromotions
     *
     * @param callback The callback.
     */
    public void getEligiblePromotions(IServerCallback callback) {
        ServerCall sc = new ServerCall(ServiceName.product, ServiceOperation.ELIGIBLE_PROMOTIONS, null, callback);
        _client.sendRequest(sc);
    }

    /**
     * Confirm GooglePlay Purchase. On success, the player will be awarded the
     * associated currencies.
     *
     * Service Name - product
     * Service Operation - CONFIRM_GOOGLEPLAY_PURCHASE
     *
     * @param orderId   GooglePlay order id
     * @param productId  GooglePlay product id
     * @param token   GooglePlay token string
     * @param callback  The callback.
     */
    public void confirmGooglePlayPurchase(String orderId, String productId, String token, IServerCallback callback) {
        JSONObject data = new JSONObject();
        try {
            data.put(Parameter.orderId.name(), orderId);
            data.put(Parameter.productId.name(), productId);
            data.put(Parameter.token.name(), token);
        } catch (JSONException je) {
            je.printStackTrace();
        }
        ServerCall sc = new ServerCall(ServiceName.product, ServiceOperation.CONFIRM_GOOGLEPLAY_PURCHASE, data, callback);
        _client.sendRequest(sc);
    }

    /**
     * Awards currency in a parent app.
     *
     * Service Name - Product
     * Service Operation - AWARD_PARENT_VC
     *
     * @param currencyType The ID of the parent currency
     * @param amount The amount of currency to award
     * @param parentLevel The level of the parent containing the currency
     * @param callback The method to be invoked when the server response is received
     */
    public void awardParentCurrency(String currencyType,
                                    long amount,
                                    String parentLevel,
                                    IServerCallback callback) {
        try {
            JSONObject data = new JSONObject();
            data.put(Parameter.vc_id.name(), currencyType);
            data.put(Parameter.vc_amount.name(), amount);
            data.put(Parameter.levelName.name(), parentLevel);

            ServerCall sc = new ServerCall(ServiceName.product, ServiceOperation.AWARD_PARENT_VC, data, callback);
            _client.sendRequest(sc);
        } catch (JSONException je) {
            je.printStackTrace();
        }
    }

    /**
     * Consumes currency in a parent app.
     *
     * Service Name - Product
     * Service Operation - CONSUME_PARENT_VC
     *
     * @param currencyType The ID of the parent currency
     * @param amount The amount of currency to consume
     * @param parentLevel The level of the parent containing the currency
     * @param callback The method to be invoked when the server response is received
     */
    public void consumeParentCurrency(String currencyType,
                                      long amount,
                                      String parentLevel,
                                      IServerCallback callback) {
        try {
            JSONObject data = new JSONObject();
            data.put(Parameter.vc_id.name(), currencyType);
            data.put(Parameter.vc_amount.name(), amount);
            data.put(Parameter.levelName.name(), parentLevel);

            ServerCall sc = new ServerCall(ServiceName.product, ServiceOperation.CONSUME_PARENT_VC, data, callback);
            _client.sendRequest(sc);
        } catch (JSONException je) {
            je.printStackTrace();
        }
    }

    /**
     * Gets information on a currency in a parent app.
     *
     * Service Name - Product
     * Service Operation - GET_PARENT_VC
     *
     * @param currencyType The ID of the parent currency or null to retrieve all parent currencies
     * @param parentLevel The level of the parent containing the currency
     * @param callback The method to be invoked when the server response is received
     */
    public void getParentCurrency(String currencyType,
                                  String parentLevel,
                                  IServerCallback callback) {
        try {
            JSONObject data = new JSONObject();
            if (StringUtil.IsOptionalParameterValid(currencyType)) {
                data.put(Parameter.vc_id.name(), currencyType);
            }
            data.put(Parameter.levelName.name(), parentLevel);

            ServerCall sc = new ServerCall(ServiceName.product, ServiceOperation.GET_PARENT_VC, data, callback);
            _client.sendRequest(sc);
        } catch (JSONException je) {
            je.printStackTrace();
        }
    }

    /**
     * Resets all currencies in a parent app.
     *
     * Service Name - Product
     * Service Operation - RESET_PARENT_VC
     *
     * @param parentLevel The level of the parent containing the currencies
     * @param callback The method to be invoked when the server response is received
     */
    public void resetParentCurrency(String parentLevel,
                                    IServerCallback callback) {
        try {
            JSONObject data = new JSONObject();
            data.put(Parameter.levelName.name(), parentLevel);

            ServerCall sc = new ServerCall(ServiceName.product, ServiceOperation.RESET_PARENT_VC, data, callback);
            _client.sendRequest(sc);
        } catch (JSONException je) {
            je.printStackTrace();
        }
    }

}
