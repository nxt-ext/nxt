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

public class TestShuffling extends BlockchainTest {

    @Test
    public void shufflingProcess() {
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

        apiCall = new APICall.Builder("shufflingProcess").
                param("shuffling", shufflingId).
                param("secretPhrase", secretPhrase1).
                param("recipient", "NXT-XK4R-7VJU-6EQG-7R335").
                feeNQT(Constants.ONE_NXT).
                build();
        JSONObject shufflingProcessResponse = apiCall.invoke();
        Logger.logMessage("shufflingProcessResponse: " + shufflingProcessResponse.toJSONString());
        generateBlock();

        apiCall = new APICall.Builder("shufflingProcess").
                param("shuffling", shufflingId).
                param("secretPhrase", secretPhrase2).
                param("recipient", "NXT-EVHD-5FLM-3NMQ-G46NR").
                feeNQT(Constants.ONE_NXT).
                build();
        shufflingProcessResponse = apiCall.invoke();
        Logger.logMessage("shufflingProcessResponse: " + shufflingProcessResponse.toJSONString());
        generateBlock();

        apiCall = new APICall.Builder("shufflingProcess").
                param("shuffling", shufflingId).
                param("secretPhrase", secretPhrase3).
                param("recipient", "NXT-SZKV-J8TH-GSM9-9LKV6").
                feeNQT(Constants.ONE_NXT).
                build();
        shufflingProcessResponse = apiCall.invoke();
        Logger.logMessage("shufflingProcessResponse: " + shufflingProcessResponse.toJSONString());
        generateBlock();

        apiCall = new APICall.Builder("shufflingProcess").
                param("shuffling", shufflingId).
                param("secretPhrase", secretPhrase4).
                param("recipient", "NXT-E93F-7E8Z-BHJ8-A65RG").
                feeNQT(Constants.ONE_NXT).
                build();
        shufflingProcessResponse = apiCall.invoke();
        Logger.logMessage("shufflingProcessResponse: " + shufflingProcessResponse.toJSONString());
        generateBlock();

        // Verify that each of the participants is also a recipient (not mandatory just for the test)
        apiCall = new APICall.Builder("getShufflingParticipants").
                param("shuffling", shufflingId).
                build();
        getParticipantsResponse = apiCall.invoke();
        Logger.logMessage("getShufflingParticipantsResponse: " + getParticipantsResponse.toJSONString());

        participants = (JSONArray)getParticipantsResponse.get("participants");
        accountMapping = new HashMap<>();
        for (Object participant : participants) {
            String account = (String) ((JSONObject)participant).get("account");
            String recipient = (String) ((JSONObject)participant).get("recipient");
            accountMapping.put(account, recipient);
        }
        for (Map.Entry<String, String> mapping : accountMapping.entrySet()) {
            Assert.assertTrue(accountMapping.get(mapping.getValue()) != null);
        }

        // Verify shuffling
        apiCall = new APICall.Builder("shufflingVerify").
                param("shuffling", shufflingId).
                param("secretPhrase", secretPhrase1).
                feeNQT(Constants.ONE_NXT).
                build();
        apiCall.invoke();
        apiCall = new APICall.Builder("shufflingVerify").
                param("shuffling", shufflingId).
                param("secretPhrase", secretPhrase2).
                feeNQT(Constants.ONE_NXT).
                build();
        apiCall.invoke();
        apiCall = new APICall.Builder("shufflingVerify").
                param("shuffling", shufflingId).
                param("secretPhrase", secretPhrase3).
                feeNQT(Constants.ONE_NXT).
                build();
        apiCall.invoke();
        apiCall = new APICall.Builder("shufflingVerify").
                param("shuffling", shufflingId).
                param("secretPhrase", secretPhrase4).
                feeNQT(Constants.ONE_NXT).
                build();
        response = apiCall.invoke();
        Logger.logDebugMessage("shufflingVerifyResponse:" + response);
        apiCall = new APICall.Builder("shufflingDistribute").
                param("shuffling", shufflingId).
                param("secretPhrase", secretPhrase1).
                feeNQT(Constants.ONE_NXT).
                build();
        Logger.logDebugMessage("shufflingDistributeResponse:" + response);
        response = apiCall.invoke();
        Assert.assertTrue(((String)response.get("error")).contains("Shuffling not ready for distribution"));
        generateBlock();
        apiCall = new APICall.Builder("shufflingDistribute").
                param("shuffling", shufflingId).
                param("secretPhrase", secretPhrase4).
                feeNQT(Constants.ONE_NXT).
                build();
        response = apiCall.invoke();
        Logger.logDebugMessage("shufflingDistributeResponse:" + response);
        Assert.assertTrue(((String)response.get("error")).contains("Only shuffling issuer can trigger distribution"));
        apiCall = new APICall.Builder("shufflingDistribute").
                param("shuffling", shufflingId).
                param("secretPhrase", secretPhrase1).
                feeNQT(Constants.ONE_NXT).
                build();
        response = apiCall.invoke();
        Logger.logDebugMessage("shufflingDistributeResponse:" + response);
        generateBlock();

        apiCall = new APICall.Builder("getShuffling").
                param("shuffling", shufflingId).
                build();
        getShufflingResponse = apiCall.invoke();
        Logger.logMessage("getShufflingResponse: " + getShufflingResponse.toJSONString());
        Assert.assertEquals((long)Shuffling.State.DONE.getCode(), getShufflingResponse.get("state"));

        apiCall = new APICall.Builder("shufflingCancel").
                param("shuffling", shufflingId).
                param("secretPhrase", secretPhrase1).
                feeNQT(Constants.ONE_NXT).
                build();
        response = apiCall.invoke();
        Logger.logDebugMessage("shufflingCancelResponse:" + response);
        Assert.assertTrue(((String)response.get("error")).contains("cannot be cancelled"));
    }

}
