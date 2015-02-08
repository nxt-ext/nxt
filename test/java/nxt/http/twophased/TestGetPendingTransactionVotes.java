package nxt.http.twophased;


import org.json.simple.JSONArray;
import org.junit.Assert;
import nxt.BlockchainTest;
import nxt.Constants;
import nxt.http.APICall;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.junit.Test;

public class TestGetPendingTransactionVotes extends BlockchainTest {

    @Test
    public void transactionVotes() {

        APICall apiCall = new TestCreateTwoPhased.TwoPhasedMoneyTransferBuilder()
                .quorum(3)
                .build();
        String transactionId = TestCreateTwoPhased.issueCreateTwoPhased(apiCall, false);

        generateBlock();

        long fee = Constants.ONE_NXT;
        apiCall = new APICall.Builder("approveTransaction")
                .param("secretPhrase", secretPhrase3)
                .param("transaction", transactionId)
                .param("feeNQT", fee)
                .build();
        JSONObject response = apiCall.invoke();
        Logger.logMessage("approveTransactionResponse:" + response.toJSONString());

        generateBlock();

        apiCall = new APICall.Builder("getPhasingVotes")
                .param("transaction", transactionId)
                .build();
        response = apiCall.invoke();
        Logger.logMessage("getPhasingVotesResponse:" + response.toJSONString());

        Assert.assertNull(response.get("errorCode"));
        Assert.assertEquals(1, ((Long) response.get("votes")).intValue());

        apiCall = new APICall.Builder("getPhasingVotes")
                .param("transaction", transactionId)
                .param("includeVoters", "true")
                .build();
        response = apiCall.invoke();
        Logger.logMessage("getPhasingVotesResponse:" + response.toJSONString());

        Assert.assertNull(response.get("errorCode"));
        Assert.assertEquals(1, ((Long) response.get("votes")).intValue());
        Assert.assertNotNull(response.get("voters"));
        Assert.assertEquals(1, ((JSONArray)response.get("voters")).size());
    }

}