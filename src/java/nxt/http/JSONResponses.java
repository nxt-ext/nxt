package nxt.http;

import nxt.Nxt;
import nxt.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class JSONResponses {

    static final JSONStreamAware INCORRECT_ALIAS_LENGTH = incorrect("alias", "(length must be in [1.." + Nxt.MAX_ALIAS_LENGTH + "] range)");
    static final JSONStreamAware INCORRECT_ALIAS = incorrect("alias", "(must contain only digits and latin letters)");
    static final JSONStreamAware INCORRECT_URI_LENGTH = incorrect("uri", "(length must be not longer than " + Nxt.MAX_ALIAS_URI_LENGTH + " characters)");
    static final JSONStreamAware MISSING_SECRET_PHRASE = missing("secretPhrase");
    static final JSONStreamAware MISSING_ALIAS = missing("alias");
    static final JSONStreamAware MISSING_URI = missing("uri");
    static final JSONStreamAware MISSING_FEE = missing("fee");
    static final JSONStreamAware MISSING_DEADLINE = missing("deadline");
    static final JSONStreamAware INCORRECT_DEADLINE = incorrect("deadline");
    static final JSONStreamAware INCORRECT_FEE = incorrect("fee");
    static final JSONStreamAware MISSING_TRANSACTION_BYTES = missing("transactionBytes");
    static final JSONStreamAware INCORRECT_TRANSACTION_BYTES = incorrect("transactionBytes");
    static final JSONStreamAware MISSING_ORDER = missing("order");
    static final JSONStreamAware INCORRECT_ORDER = incorrect("order");
    static final JSONStreamAware UNKNOWN_ORDER = unknown("order");
    static final JSONStreamAware MISSING_HALLMARK = missing("hallmark");
    static final JSONStreamAware INCORRECT_HALLMARK = incorrect("hallmark");
    static final JSONStreamAware MISSING_WEBSITE = missing("website");
    static final JSONStreamAware INCORRECT_WEBSITE = incorrect("website");
    static final JSONStreamAware MISSING_TOKEN = missing("token");
    static final JSONStreamAware INCORRECT_TOKEN = incorrect("token");
    static final JSONStreamAware MISSING_ACCOUNT = missing("account");
    static final JSONStreamAware INCORRECT_ACCOUNT = incorrect("account");
    static final JSONStreamAware MISSING_TIMESTAMP = missing("timestamp");
    static final JSONStreamAware INCORRECT_TIMESTAMP = incorrect("timestamp");
    static final JSONStreamAware UNKNOWN_ACCOUNT = unknown("account");
    static final JSONStreamAware UNKNOWN_ALIAS = unknown("alias");
    static final JSONStreamAware MISSING_ASSET = missing("asset");
    static final JSONStreamAware UNKNOWN_ASSET = unknown("asset");
    static final JSONStreamAware INCORRECT_ASSET = incorrect("asset");
    static final JSONStreamAware MISSING_BLOCK = missing("block");
    static final JSONStreamAware UNKNOWN_BLOCK = unknown("block");
    static final JSONStreamAware INCORRECT_BLOCK = incorrect("block");
    static final JSONStreamAware MISSING_NUMBER_OF_CONFIRMATIONS = missing("numberOfConfirmations");
    static final JSONStreamAware INCORRECT_NUMBER_OF_CONFIRMATIONS = incorrect("numberOfConfirmations");
    static final JSONStreamAware MISSING_PEER = missing("peer");
    static final JSONStreamAware UNKNOWN_PEER = unknown("peer");
    static final JSONStreamAware MISSING_TRANSACTION = missing("transaction");
    static final JSONStreamAware UNKNOWN_TRANSACTION = unknown("transaction");
    static final JSONStreamAware INCORRECT_TRANSACTION = incorrect("transaction");
    static final JSONStreamAware INCORRECT_ASSET_ISSUANCE_FEE = incorrect("fee", "(must be not less than 1'000)");
    static final JSONStreamAware INCORRECT_ASSET_DESCRIPTION = incorrect("description", "(length must be not longer than 1000 characters)");
    static final JSONStreamAware INCORRECT_ASSET_NAME = incorrect("name", "(must contain only digits and latin letters)");
    static final JSONStreamAware INCORRECT_ASSET_NAME_LENGTH = incorrect("name", "(length must be in [3..10] range)");
    static final JSONStreamAware MISSING_NAME = missing("name");
    static final JSONStreamAware MISSING_QUANTITY = missing("quantity");
    static final JSONStreamAware INCORRECT_QUANTITY = incorrect("quantity");
    static final JSONStreamAware INCORRECT_ASSET_QUANTITY = incorrect("quantity", "(must be in [1..1'000'000'000] range)");
    static final JSONStreamAware MISSING_HOST = missing("host");
    static final JSONStreamAware MISSING_DATE = missing("date");
    static final JSONStreamAware MISSING_WEIGHT = missing("weight");
    static final JSONStreamAware INCORRECT_HOST = incorrect("host", "(the length exceeds 100 chars limit)");
    static final JSONStreamAware INCORRECT_WEIGHT = incorrect("weight");
    static final JSONStreamAware INCORRECT_DATE = incorrect("date");
    static final JSONStreamAware MISSING_PRICE = missing("price");
    static final JSONStreamAware INCORRECT_PRICE = incorrect("price");
    static final JSONStreamAware INCORRECT_REFERENCED_TRANSACTION = incorrect("referencedTransaction");
    static final JSONStreamAware MISSING_MESSAGE = missing("message");
    static final JSONStreamAware MISSING_RECIPIENT = missing("recipient");
    static final JSONStreamAware INCORRECT_RECIPIENT = incorrect("recipient");
    static final JSONStreamAware INCORRECT_ARBITRARY_MESSAGE = incorrect("message", "(length must be not longer than \"" + Nxt.MAX_ARBITRARY_MESSAGE_LENGTH + "\" bytes)");
    static final JSONStreamAware MISSING_AMOUNT = missing("amount");
    static final JSONStreamAware INCORRECT_AMOUNT = incorrect("amount");

    static final JSONStreamAware NOT_ENOUGH_FUNDS;
    static {
        JSONObject response = new JSONObject();
        response.put("errorCode", 6);
        response.put("errorDescription", "Not enough funds");
        NOT_ENOUGH_FUNDS = JSON.prepare(response);
    }

    static final JSONStreamAware ASSET_NAME_ALREADY_USED;
    static {
        JSONObject response = new JSONObject();
        response.put("errorCode", 8);
        response.put("errorDescription", "Asset name is already used");
        ASSET_NAME_ALREADY_USED = JSON.prepare(response);
    }

    static final JSONStreamAware ERROR_NOT_ALLOWED;
    static {
        JSONObject response = new JSONObject();
        response.put("errorCode", 7);
        response.put("errorDescription", "Not allowed");
        ERROR_NOT_ALLOWED = JSON.prepare(response);
    }

    static final JSONStreamAware ERROR_INCORRECT_REQUEST;
    static {
        JSONObject response  = new JSONObject();
        response.put("errorCode", 1);
        response.put("errorDescription", "Incorrect request");
        ERROR_INCORRECT_REQUEST = JSON.prepare(response);
    }

    private static JSONStreamAware missing(String paramName) {
        JSONObject response = new JSONObject();
        response.put("errorCode", 3);
        response.put("errorDescription", "\"" + paramName + "\"" + " not specified");
        return JSON.prepare(response);
    }

    private static JSONStreamAware incorrect(String paramName) {
        return incorrect(paramName, "");
    }

    private static JSONStreamAware incorrect(String paramName, String details) {
        JSONObject response = new JSONObject();
        response.put("errorCode", 4);
        response.put("errorDescription", "Incorrect \"" + paramName + "\"" + details);
        return JSON.prepare(response);
    }

    private static JSONStreamAware unknown(String objectName) {
        JSONObject response = new JSONObject();
        response.put("errorCode", 5);
        response.put("errorDescription", "Unknown " + objectName);
        return JSON.prepare(response);
    }

    private JSONResponses() {} // never

}
