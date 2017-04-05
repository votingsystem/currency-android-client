package org.currency.util;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public enum OperationType {

    @JsonProperty("BUNDLE_STATE")
    BUNDLE_STATE("/api/currency/bundle-state"),
    @JsonProperty("CURRENCY_SEND")
    CURRENCY_SEND("/api/currency/send"),
    @JsonProperty("CURRENCY_WALLET_CHANGE")
    CURRENCY_WALLET_CHANGE(null),
    @JsonProperty("CURRENCY_REQUEST")
    CURRENCY_REQUEST("/api/currency/request"),
    @JsonProperty("CURRENCY_STATE")
    CURRENCY_STATE("/api/currency/hash"),
    @JsonProperty("CLOSE_SESSION")
    CLOSE_SESSION(null),
    @JsonProperty("DELIVERY_WITH_PAYMENT")
    DELIVERY_WITH_PAYMENT(null),
    @JsonProperty("QR_INFO")
    QR_INFO("/api/currency-qr/info"),
    @JsonProperty("CURRENCY_ACCOUNTS_INFO")
    CURRENCY_ACCOUNTS_INFO("/api/balance/user"),
    @JsonProperty("TIMESTAMP_REQUEST")
    TIMESTAMP_REQUEST("/api/timestamp"),
    @JsonProperty("TIMESTAMP_REQUEST_DISCRETE")
    TIMESTAMP_REQUEST_DISCRETE("/api/timestamp/discrete"),
    @JsonProperty("USER_INFO")
    USER_INFO("/api/user{date-path}"),
    @JsonProperty("MSG_TO_DEVICE")
    MSG_TO_DEVICE(null),
    @JsonProperty("GET_METADATA")
    GET_METADATA("/api/metadata"),
    @JsonProperty("SEARCH_USER")
    SEARCH_USER("/api/user/search?searchText={searchText}"),
    @JsonProperty("TRANSACTION_INFO")
    TRANSACTION_INFO(null),
    @JsonProperty("TRANSACTION_FROM_BANK")
    TRANSACTION_FROM_BANK(null),
    @JsonProperty("TRANSACTION_FROM_USER")
    TRANSACTION_FROM_USER(null),
    @JsonProperty("CURRENCY_CHANGE")
    CURRENCY_CHANGE(null),
    @JsonProperty("GET_TAG")
    GET_TAG("/api/tag?tag={tag-name}"),
    @JsonProperty("GET_TAG_LIST")
    GET_TAG_LIST("/api/tag/list"),
    @JsonProperty("CURRENCY_PERIOD_INIT")
    CURRENCY_PERIOD_INIT(null),
    @JsonProperty("GET_TRUSTED_CERTS")
    GET_TRUSTED_CERTS("/api/certs/trusted"),
    @JsonProperty("GET_CURRENCY_STATUS")
    GET_CURRENCY_STATUS("/api/currency/state"),
    @JsonProperty("GET_CURRENCY_BUNDLE_STATUS")
    GET_CURRENCY_BUNDLE_STATUS("/api/currency/bundle-state"),
    @JsonProperty("GET_TRANSACTION")
    GET_TRANSACTION("/api/transaction"),
    @JsonProperty("GET_CURRENCY_TRANSACTION")
    GET_CURRENCY_TRANSACTION("/api/transaction/currency"),
    @JsonProperty("INIT_MOBILE_SESSION")
    INIT_MOBILE_SESSION("/api/device/init-mobile-session"),
    @JsonProperty("SESSION_CERTIFICATION_DATA")
    SESSION_CERTIFICATION_DATA("/api/device/session-certification-data"),
    @JsonProperty("SESSION_CERTIFICATION")
    SESSION_CERTIFICATION("/api/cert-issuer/session-csr"),
    @JsonProperty("INIT_BROWSER_SESSION")
    INIT_BROWSER_SESSION("/api/device/init-browser-session"),
    @JsonProperty("CONNECTED_DEVICES")
    CONNECTED_DEVICES("/api/device/connected-device-by-user-uuid"),
    @JsonProperty("DEVICE_BY_UUID")
    DEVICE_BY_UUID("/api/device/uuid"),
    @JsonProperty("SEARCH_USER_BY_DEVICE")
    SEARCH_USER_BY_DEVICE("/api/user/searchByDevice?{query}"),
    @JsonProperty("PAYMENT_CONFIRM")
    PAYMENT_CONFIRM(null),
    @JsonProperty("SELECT_DEVICE")
    SELECT_DEVICE(null),
    @JsonProperty("PROCESS_URL")
    PROCESS_URL(null),
    @JsonProperty("REGISTER_DEVICE")
    REGISTER_DEVICE("/api/cert-issuer/register-device"),
    @JsonProperty("DELIVERY_WITHOUT_PAYMENT")
    DELIVERY_WITHOUT_PAYMENT(null),
    @JsonProperty("CERTIFICATION_REQUEST")
    CERTIFICATION_REQUEST(null),
    @JsonProperty("INIT_SESSION")
    INIT_SESSION("/api/device/init-session"),
    @JsonProperty("BANK_NEW")
    BANK_NEW("/api/user/new-bank");


    private String url;

    OperationType(String url) {
        this.url = url;
    }

    public String getUrl(String entityId) {
        return entityId + url;
    }

    public static String getSearchURL(String systemEntityId, String searchText) {
        return SEARCH_USER.getUrl(systemEntityId).replace("{searchText}",searchText);
    }

    public static String getTagSearchURL(String systemEntityId, String searchText) {
        return GET_TAG.getUrl(systemEntityId).replace("{tag-name}",searchText);
    }

    public static String getUserInfoByDateServiceURL(String systemEntityId, Date date) {
        return USER_INFO.getUrl(systemEntityId).replace("{date-path}", DateUtils.getPath(date));
    }

    public static String getSearchServiceURL(String systemEntityId, String phone, String email) {
        String query = phone != null? "phone=" + phone.replace(" ", "").trim() + "&":"";
        if(email != null) query = query + "email=" + email.trim();
        return SEARCH_USER_BY_DEVICE.getUrl(systemEntityId).replace("{query}", query);
    }

}
