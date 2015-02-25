package nxt.http;

import nxt.Constants;
import nxt.CurrencyType;
import nxt.Genesis;
import nxt.TransactionType;
import nxt.VoteWeighting;
import nxt.crypto.HashFunction;
import nxt.peer.Peer;
import nxt.util.Convert;
import nxt.util.JSON;
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
        response.put("maxArbitraryMessageLength", Constants.MAX_ARBITRARY_MESSAGE_LENGTH_2);

        JSONObject transactionJSON = new JSONObject();
        outer:
        for (int type = 0; ; type++) {
            JSONObject typeJSON = new JSONObject();
            JSONObject subtypesJSON = new JSONObject();
            for (int subtype = 0; ; subtype++) {
                TransactionType transactionType = TransactionType.findTransactionType((byte)type, (byte)subtype);
                if (transactionType == null) {
                    if (subtype == 0) {
                        break outer;
                    } else {
                        break;
                    }
                }
                JSONObject subtypeJSON = new JSONObject();
                subtypeJSON.put("name", transactionType.getName());
                subtypeJSON.put("canHaveRecipient", transactionType.canHaveRecipient());
                subtypeJSON.put("mustHaveRecipient", transactionType.mustHaveRecipient());
                subtypesJSON.put(subtype, subtypeJSON);
            }
            typeJSON.put("subtypes", subtypesJSON);
            transactionJSON.put(type, typeJSON);
        }
        response.put("transactionTypes", transactionJSON);

        JSONObject currencyTypes = new JSONObject();
        for (CurrencyType currencyType : CurrencyType.values()) {
            currencyTypes.put(currencyType.toString(), currencyType.getCode());
        }
        response.put("currencyTypes", currencyTypes);

        JSONObject votingModels = new JSONObject();
        for (VoteWeighting.VotingModel votingModel : VoteWeighting.VotingModel.values()) {
            votingModels.put(votingModel.toString(), votingModel.getCode());
        }
        response.put("votingModels", votingModels);

        JSONObject minBalanceModels = new JSONObject();
        for (VoteWeighting.MinBalanceModel minBalanceModel : VoteWeighting.MinBalanceModel.values()) {
            minBalanceModels.put(minBalanceModel.toString(), minBalanceModel.getCode());
        }
        response.put("minBalanceModels", minBalanceModels);

        JSONObject hashFunctions = new JSONObject();
        for (HashFunction hashFunction : HashFunction.values()) {
            hashFunctions.put(hashFunction.toString(), hashFunction.getId());
        }
        response.put("hashAlgorithms", hashFunctions);

        JSONObject peerStates = new JSONObject();
        for (Peer.State peerState : Peer.State.values()) {
            peerStates.put(peerState.toString(), peerState.ordinal());
        }
        response.put("peerStates", peerStates);

        CONSTANTS = JSON.prepare(response);

    }

    private GetConstants() {
        super(new APITag[] {APITag.INFO});
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {
        return CONSTANTS;
    }

}
