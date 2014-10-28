package nxt.http;

import nxt.BlockchainTest;
import nxt.Constants;
import nxt.Shuffling;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class TestShufflingRegistration extends BlockchainTest {

    @Test
    public void addParticipants() {
        APICall apiCall = new APICall.Builder("shufflingCreate").
                secretPhrase(secretPhrase1).
                feeNQT(Constants.ONE_NXT).
                param("isCurrency", "0").
                param("amountNQT", "10000000").
                param("participantCount", "4").
                param("cancellationHeight", Integer.MAX_VALUE).
                build();
        JSONObject response = apiCall.invoke();
        Logger.logMessage("shufflingCreateResponse: " + response.toJSONString());
        generateBlock();
        String shufflingId = (String) response.get("transaction");
        apiCall = new APICall.Builder("shufflingRegister").
                secretPhrase(secretPhrase2).
                feeNQT(Constants.ONE_NXT).
                param("shuffling", shufflingId).
                build();
        response = apiCall.invoke();
        Logger.logMessage("shufflingRegisterResponse: " + response.toJSONString());
        generateBlock();
        apiCall = new APICall.Builder("shufflingRegister").
                secretPhrase(secretPhrase3).
                feeNQT(Constants.ONE_NXT).
                param("shuffling", shufflingId).
                build();
        response = apiCall.invoke();
        Logger.logMessage("shufflingRegisterResponse: " + response.toJSONString());
        generateBlock();
        apiCall = new APICall.Builder("shufflingRegister").
                secretPhrase(secretPhrase4).
                feeNQT(Constants.ONE_NXT).
                param("shuffling", shufflingId).
                build();
        response = apiCall.invoke();
        Logger.logMessage("shufflingRegisterResponse: " + response.toJSONString());
        generateBlock();
        apiCall = new APICall.Builder("getShuffling").
                param("shuffling", shufflingId).
                build();
        JSONObject getShufflingResponse = apiCall.invoke();
        Logger.logMessage("getShufflingResponse: " + getShufflingResponse.toJSONString());

        apiCall = new APICall.Builder("getShufflingParticipants").
                param("shuffling", shufflingId).
                build();
        JSONObject getParticipantsResponse = apiCall.invoke();
        Logger.logMessage("getShufflingParticipantsResponse: " + getParticipantsResponse.toJSONString());

        Assert.assertEquals((long)Shuffling.State.PROCESSING.getCode(), getShufflingResponse.get("state"));
        String shufflingAssignee = (String) getShufflingResponse.get("assignee");
        JSONArray participants = (JSONArray)getParticipantsResponse.get("participants");
        Map<String, String> accountMapping = new HashMap<>();
        for (Object participant : participants) {
            String account = (String) ((JSONObject)participant).get("account");
            String nextAccount = (String) ((JSONObject)participant).get("nextAccount");
            accountMapping.put(account, nextAccount);
        }
        String account1 = accountMapping.get(shufflingAssignee);
        Assert.assertTrue(account1 != null);
        String account2 = accountMapping.get(account1);
        Assert.assertTrue(account2 != null);
        String account3 = accountMapping.get(account2);
        Assert.assertTrue(account3 != null);
        String account4 = accountMapping.get(account3);
        Assert.assertTrue(account4 != null);
        String nullAccount = accountMapping.get(account4);
        Assert.assertTrue(nullAccount == null);
    }
}
