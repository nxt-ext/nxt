package nxt.http;

import nxt.Constants;
import nxt.Genesis;
import nxt.TransactionType;
import nxt.util.Convert;
import nxt.util.JSON;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetConstants extends APIServlet.APIRequestHandler {

    static final GetConstants instance = new GetConstants();

    private static final JSONStreamAware CONSTANTS;

    static {

        JSONObject response = new JSONObject();
        response.put("genesisBlockId", Convert.toUnsignedLong(Genesis.GENESIS_BLOCK_ID));
        response.put("genesisAccountId", Convert.toUnsignedLong(Genesis.CREATOR_ID));
        response.put("maxBlockPayloadLength", Constants.MAX_PAYLOAD_LENGTH);
        response.put("maxArbitraryMessageLength", Constants.MAX_ARBITRARY_MESSAGE_LENGTH);

        JSONArray transactionTypes = new JSONArray();
        JSONObject transactionType = new JSONObject();
        transactionType.put("value", TransactionType.Payment.ORDINARY.getType());
        transactionType.put("description", "Payment");
        JSONArray subtypes = new JSONArray();
        JSONObject subtype = new JSONObject();
        subtype.put("value", TransactionType.Payment.ORDINARY.getSubtype());
        subtype.put("description", "Ordinary payment");
        subtypes.add(subtype);
        transactionType.put("subtypes", subtypes);
        transactionTypes.add(transactionType);
        transactionType = new JSONObject();
        transactionType.put("value", TransactionType.Messaging.ARBITRARY_MESSAGE.getType());
        transactionType.put("description", "Messaging");
        subtypes = new JSONArray();
        subtype = new JSONObject();
        subtype.put("value", TransactionType.Messaging.ARBITRARY_MESSAGE.getSubtype());
        subtype.put("description", "Arbitrary message");
        subtypes.add(subtype);
        subtype = new JSONObject();
        subtype.put("value", TransactionType.Messaging.ALIAS_ASSIGNMENT.getSubtype());
        subtype.put("description", "Alias assignment");
        subtypes.add(subtype);
        subtype = new JSONObject();
        subtype.put("value", TransactionType.Messaging.POLL_CREATION.getSubtype());
        subtype.put("description", "Poll creation");
        subtypes.add(subtype);
        subtype = new JSONObject();
        subtype.put("value", TransactionType.Messaging.VOTE_CASTING.getSubtype());
        subtype.put("description", "Vote casting");
        subtypes.add(subtype);
        transactionType.put("subtypes", subtypes);
        transactionTypes.add(transactionType);
        transactionType = new JSONObject();
        transactionType.put("value", TransactionType.ColoredCoins.ASSET_ISSUANCE.getType());
        transactionType.put("description", "Colored coins");
        subtypes = new JSONArray();
        subtype = new JSONObject();
        subtype.put("value", TransactionType.ColoredCoins.ASSET_ISSUANCE.getSubtype());
        subtype.put("description", "Asset issuance");
        subtypes.add(subtype);
        subtype = new JSONObject();
        subtype.put("value", TransactionType.ColoredCoins.ASSET_TRANSFER.getSubtype());
        subtype.put("description", "Asset transfer");
        subtypes.add(subtype);
        subtype = new JSONObject();
        subtype.put("value", TransactionType.ColoredCoins.ASK_ORDER_PLACEMENT.getSubtype());
        subtype.put("description", "Ask order placement");
        subtypes.add(subtype);
        subtype = new JSONObject();
        subtype.put("value", TransactionType.ColoredCoins.BID_ORDER_PLACEMENT.getSubtype());
        subtype.put("description", "Bid order placement");
        subtypes.add(subtype);
        subtype = new JSONObject();
        subtype.put("value", TransactionType.ColoredCoins.ASK_ORDER_CANCELLATION.getSubtype());
        subtype.put("description", "Ask order cancellation");
        subtypes.add(subtype);
        subtype = new JSONObject();
        subtype.put("value", TransactionType.ColoredCoins.BID_ORDER_CANCELLATION.getSubtype());
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

        CONSTANTS = JSON.prepare(response);

    }

    private GetConstants() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {
        return CONSTANTS;
    }

}
