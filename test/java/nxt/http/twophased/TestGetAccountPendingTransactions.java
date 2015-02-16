package nxt.http.twophased;


import nxt.BlockchainTest;
import nxt.http.APICall;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class TestGetAccountPendingTransactions extends BlockchainTest {

    static APICall pendingTransactionsApiCall() {
        return new APICall.Builder("getAccountPendingTransactions")
                .param("account", Convert.toUnsignedLong(id1))
                .param("firstIndex", 0)
                .param("lastIndex", 10)
                .build();
    }

    @Test
    public void simpleTransactionLookup() {
        APICall apiCall = new TestCreateTwoPhased.TwoPhasedMoneyTransferBuilder().build();
        JSONObject transactionJSON = TestCreateTwoPhased.issueCreateTwoPhased(apiCall, false);
        generateBlock();

        JSONObject response = pendingTransactionsApiCall().invoke();
        Logger.logMessage("getAccountPendingTransactionsResponse:" + response.toJSONString());
        JSONArray transactionsJson = (JSONArray) response.get("transactions");
        Assert.assertTrue(TwoPhasedSuite.searchForTransactionId(transactionsJson, (String) transactionJSON.get("transaction")));
    }

    @Test
    public void multiple() {
        JSONObject response = pendingTransactionsApiCall().invoke();
        Logger.logMessage("getAccountPendingTransactionsResponse:" + response.toJSONString());
        JSONArray transactionsJson = (JSONArray) response.get("transactions");
        int transactionsSize0 = transactionsJson.size();

        APICall apiCall = new TestCreateTwoPhased.TwoPhasedMoneyTransferBuilder().build();
        JSONObject transactionJSON1 = TestCreateTwoPhased.issueCreateTwoPhased(apiCall, false);
        JSONObject transactionJSON2 = TestCreateTwoPhased.issueCreateTwoPhased(apiCall, false);
        generateBlock();

        response = pendingTransactionsApiCall().invoke();
        Logger.logMessage("getAccountPendingTransactionsResponse:" + response.toJSONString());
        transactionsJson = (JSONArray) response.get("transactions");

        int transactionsSize = transactionsJson.size();

        Assert.assertTrue(transactionsSize - transactionsSize0 == 2);
        Assert.assertTrue(TwoPhasedSuite.searchForTransactionId(transactionsJson, (String) transactionJSON1.get("transaction")));
        Assert.assertTrue(TwoPhasedSuite.searchForTransactionId(transactionsJson, (String) transactionJSON2.get("transaction")));
    }

    @Test
    public void sorting() {
        for (int i = 0; i < 15; i++) {
            APICall apiCall = new TestCreateTwoPhased.TwoPhasedMoneyTransferBuilder().build();
            TestCreateTwoPhased.issueCreateTwoPhased(apiCall, false);
        }

        JSONObject response = pendingTransactionsApiCall().invoke();
        Logger.logMessage("getAccountPendingTransactionsResponse:" + response.toJSONString());
        JSONArray transactionsJson = (JSONArray) response.get("transactions");

        //sorting check
        int prevHeight = Integer.MAX_VALUE;
        for (Object transactionsJsonObj : transactionsJson) {
            JSONObject transactionObject = (JSONObject) transactionsJsonObj;
            int height = ((Long) transactionObject.get("height")).intValue();
            Assert.assertTrue(height <= prevHeight);
            prevHeight = height;
        }
    }
}

