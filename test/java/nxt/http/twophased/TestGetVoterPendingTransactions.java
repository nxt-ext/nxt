package nxt.http.twophased;

import nxt.BlockchainTest;
import nxt.Constants;
import nxt.http.APICall;
import nxt.http.twophased.TestCreateTwoPhased.TwoPhasedMoneyTransferBuilder;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class TestGetVoterPendingTransactions extends BlockchainTest {

    @Test
    public void simpleTransactionLookup() {
        APICall apiCall = new TwoPhasedMoneyTransferBuilder().build();
        JSONObject transactionJSON = TestCreateTwoPhased.issueCreateTwoPhased(apiCall, false);
        String transactionId = (String) transactionJSON.get("transaction");

        generateBlock();

        apiCall = new APICall.Builder("getVoterPendingTransactions")
                .param("account", Convert.toUnsignedLong(id3))
                .param("firstIndex", 0)
                .param("lastIndex", 10)
                .build();

        JSONObject response = apiCall.invoke();
        Logger.logMessage("getVoterPendingTransactionsResponse:" + response.toJSONString());
        JSONArray transactionsJson = (JSONArray) response.get("transactions");
        Assert.assertTrue(TwoPhasedSuite.searchForTransactionId(transactionsJson, transactionId));
    }

    @Test
    public void transactionLookupAfterVote() {

        APICall apiCall = new TwoPhasedMoneyTransferBuilder()
                .quorum(3)
                .build();
        JSONObject transactionJSON = TestCreateTwoPhased.issueCreateTwoPhased(apiCall, false);
        String transactionId = (String) transactionJSON.get("transaction");

        generateBlock();

        long fee = Constants.ONE_NXT;
        apiCall = new APICall.Builder("approveTransaction")
                .param("secretPhrase", secretPhrase3)
                .param("transaction", transactionId)
                .param("feeNQT", fee)
                .build();
        JSONObject response = apiCall.invoke();
        Logger.logMessage("approvePendingTransactionResponse:" + response.toJSONString());

        generateBlock();

        apiCall = new APICall.Builder("getVoterPendingTransactions")
                .param("account", Convert.toUnsignedLong(id3))
                .param("firstIndex", 0)
                .param("lastIndex", 9)
                .build();

        response = apiCall.invoke();
        Logger.logMessage("getVoterPendingTransactionsResponse:" + response.toJSONString());
        JSONArray transactionsJson = (JSONArray) response.get("transactions");
        Assert.assertTrue(TwoPhasedSuite.searchForTransactionId(transactionsJson, transactionId));
    }
}
