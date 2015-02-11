package nxt.http.twophased;


import nxt.BlockchainTest;
import nxt.VoteWeighting;
import nxt.http.APICall;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class TestGetAssetPendingTransactions extends BlockchainTest {
    @Test
    public void simpleTransactionLookup() {
        String asset = "18055555436405339905";

        APICall apiCall = new TestCreateTwoPhased.TwoPhasedMoneyTransferBuilder()
                .votingModel(VoteWeighting.VotingModel.ASSET.getCode())
                .holding(Convert.parseUnsignedLong(asset))
                .minBalance(1, VoteWeighting.MinBalanceModel.ASSET.getCode())
                .build();
        JSONObject transactionJSON = TestCreateTwoPhased.issueCreateTwoPhased(apiCall, false);

        apiCall = new APICall.Builder("getAssetPendingTransactions")
                .param("asset", asset)
                .param("firstIndex", 0)
                .param("lastIndex", 10)
                .build();

        JSONObject response = apiCall.invoke();
        Logger.logMessage("getAssetPendingTransactionsResponse:" + response.toJSONString());
        JSONArray transactionsJson = (JSONArray) response.get("transactions");
        Assert.assertTrue(TwoPhasedSuite.searchForTransactionId(transactionsJson, (String) transactionJSON.get("transaction")));
    }
}
