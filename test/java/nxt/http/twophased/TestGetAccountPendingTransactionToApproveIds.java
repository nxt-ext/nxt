package nxt.http.twophased;

import nxt.BlockchainTest;
import nxt.Constants;
import nxt.Nxt;
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
    public void validTransactionLookup() {
        APICall apiCall = new TwoPhasedMoneyTransferBuilder().build();
        String transactionId = TestCreateTwoPhased.issueCreateTwoPhased(apiCall, false);

        generateBlock();

        apiCall = new APICall.Builder("getAccountPendingTransactionToApproveIds")
                .param("account", Convert.toUnsignedLong(id3))
                .param("firstIndex", 0)
                .param("lastIndex", 10)
                .build();

        JSONObject response = apiCall.invoke();
        Logger.logMessage("getAccountPendingTransactionToApproveIds:" + response.toJSONString());
        Assert.assertTrue(((JSONArray) response.get("transactionIds")).contains(transactionId));
    }
}
