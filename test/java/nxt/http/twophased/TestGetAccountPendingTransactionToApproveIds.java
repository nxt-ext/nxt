package nxt.http.twophased;

import nxt.BlockchainTest;
import nxt.Constants;
import nxt.http.APICall;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import nxt.http.twophased.TestCreateTwoPhased.*;

public class TestGetAccountPendingTransactionToApproveIds extends BlockchainTest {

    @Test
    public void simpleTransactionLookup() {
        APICall apiCall = new TwoPhasedMoneyTransferBuilder().build();
        String transactionId = TestCreateTwoPhased.issueCreateTwoPhased(apiCall, false);

        generateBlock();

        apiCall = new APICall.Builder("getVoterPendingTransactions")
                .param("account", Convert.toUnsignedLong(id3))
                .param("firstIndex", 0)
                .param("lastIndex", 10)
                .build();

        JSONObject response = apiCall.invoke();
        Logger.logMessage("getVoterPendingTransactionsResponse:" + response.toJSONString());
        //Assert.assertTrue(((JSONArray) response.get("transactionIds")).contains(transactionId));
    }

    @Test
    public void transactionLookupAfterVote() {

        APICall apiCall = new TwoPhasedMoneyTransferBuilder()
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
        Logger.logMessage("approvePendingTransactionResponse:" + response.toJSONString());

        generateBlock();

        apiCall = new APICall.Builder("getVoterPendingTransactions")
                .param("account", Convert.toUnsignedLong(id3))
                .param("firstIndex", 0)
                .param("lastIndex", 9)
                .build();

        response = apiCall.invoke();
        Logger.logMessage("getVoterPendingTransactionsResponse:" + response.toJSONString());
        //Assert.assertTrue(((JSONArray) response.get("transactionIds")).contains(transactionId));
    }
}
