package nxt.http;

import nxt.Genesis;
import nxt.util.JSON;
import nxt.Nxt;
import nxt.Transaction;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

final class GetConstants extends HttpRequestHandler {

    static final GetConstants instance = new GetConstants();

    private static final JSONStreamAware CONSTANTS;

    static {

        JSONObject response = new JSONObject();
        response.put("genesisBlockId", Convert.convert(Genesis.GENESIS_BLOCK_ID));
        response.put("genesisAccountId", Convert.convert(Genesis.CREATOR_ID));
        response.put("maxBlockPayloadLength", Nxt.MAX_PAYLOAD_LENGTH);
        response.put("maxArbitraryMessageLength", Nxt.MAX_ARBITRARY_MESSAGE_LENGTH);

        JSONArray transactionTypes = new JSONArray();
        JSONObject transactionType = new JSONObject();
        transactionType.put("value", Transaction.TYPE_PAYMENT);
        transactionType.put("description", "Payment");
        JSONArray subtypes = new JSONArray();
        JSONObject subtype = new JSONObject();
        subtype.put("value", Transaction.SUBTYPE_PAYMENT_ORDINARY_PAYMENT);
        subtype.put("description", "Ordinary payment");
        subtypes.add(subtype);
        transactionType.put("subtypes", subtypes);
        transactionTypes.add(transactionType);
        transactionType = new JSONObject();
        transactionType.put("value", Transaction.TYPE_MESSAGING);
        transactionType.put("description", "Messaging");
        subtypes = new JSONArray();
        subtype = new JSONObject();
        subtype.put("value", Transaction.SUBTYPE_MESSAGING_ARBITRARY_MESSAGE);
        subtype.put("description", "Arbitrary message");
        subtypes.add(subtype);
        subtype = new JSONObject();
        subtype.put("value", Transaction.SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT);
        subtype.put("description", "Alias assignment");
        subtypes.add(subtype);
        transactionType.put("subtypes", subtypes);
        transactionTypes.add(transactionType);
        transactionType = new JSONObject();
        transactionType.put("value", Transaction.TYPE_COLORED_COINS);
        transactionType.put("description", "Colored coins");
        subtypes = new JSONArray();
        subtype = new JSONObject();
        subtype.put("value", Transaction.SUBTYPE_COLORED_COINS_ASSET_ISSUANCE);
        subtype.put("description", "Asset issuance");
        subtypes.add(subtype);
        subtype = new JSONObject();
        subtype.put("value", Transaction.SUBTYPE_COLORED_COINS_ASSET_TRANSFER);
        subtype.put("description", "Asset transfer");
        subtypes.add(subtype);
        subtype = new JSONObject();
        subtype.put("value", Transaction.SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT);
        subtype.put("description", "Ask order placement");
        subtypes.add(subtype);
        subtype = new JSONObject();
        subtype.put("value", Transaction.SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT);
        subtype.put("description", "Bid order placement");
        subtypes.add(subtype);
        subtype = new JSONObject();
        subtype.put("value", Transaction.SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION);
        subtype.put("description", "Ask order cancellation");
        subtypes.add(subtype);
        subtype = new JSONObject();
        subtype.put("value", Transaction.SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION);
        subtype.put("description", "Bid order cancellation");
        subtypes.add(subtype);
        transactionType.put("subtypes", subtypes);
        transactionTypes.add(transactionType);
        response.put("transactionTypes", transactionTypes);

        JSONArray peerStates = new JSONArray();
        JSONObject peerState = new JSONObject();
        peerState.put("value", 0);
        peerState.put("description", "Non-connected");
        peerStates.add(peerState);
        peerState = new JSONObject();
        peerState.put("value", 1);
        peerState.put("description", "Connected");
        peerStates.add(peerState);
        peerState = new JSONObject();
        peerState.put("value", 2);
        peerState.put("description", "Disconnected");
        peerStates.add(peerState);
        response.put("peerStates", peerStates);

        CONSTANTS = JSON.getJSONStreamAware(response);

    }

    private GetConstants() {}

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) {
        return CONSTANTS;
    }

}
