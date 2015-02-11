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

public class TestGetCurrencyPendingTransactions extends BlockchainTest {
    @Test
    public void simpleTransactionLookup() {
        String currency = "17287739300802062230";

        APICall apiCall = new TestCreateTwoPhased.TwoPhasedMoneyTransferBuilder()
                .votingModel(VoteWeighting.VotingModel.CURRENCY.getCode())
                .holding(Convert.parseUnsignedLong(currency))
                .minBalance(1, VoteWeighting.MinBalanceModel.CURRENCY.getCode())
                .build();
        JSONObject transactionJSON = TestCreateTwoPhased.issueCreateTwoPhased(apiCall, false);

        apiCall = new APICall.Builder("getCurrencyPendingTransactions")
                .param("currency", currency)
                .param("firstIndex", 0)
                .param("lastIndex", 10)
                .build();

        JSONObject response = apiCall.invoke();
        Logger.logMessage("getCurrencyPendingTransactionsResponse:" + response.toJSONString());
        JSONArray transactionsJson = (JSONArray) response.get("transactions");
        Assert.assertTrue(TwoPhasedSuite.searchForTransactionId(transactionsJson, (String) transactionJSON.get("transaction")));
    }
}
