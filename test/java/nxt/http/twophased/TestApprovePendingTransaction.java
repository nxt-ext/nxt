package nxt.http.twophased;

import nxt.BlockchainTest;
import nxt.Constants;
import nxt.http.APICall;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import nxt.http.twophased.TestCreateTwoPhased.*;

public class TestApprovePendingTransaction extends BlockchainTest {

    @Test
    public void validVoteCasting() {
        APICall apiCall = new TwoPhasedMoneyTransferBuilder().build();
        String transactionId = TestCreateTwoPhased.issueCreateTwoPhased(apiCall, false);
        generateBlock();

        long balance1 = balanceById(id1);
        long balance2 = balanceById(id2);
        long balance3 = balanceById(id3);

        long fee = Constants.ONE_NXT;

        apiCall = new APICall.Builder("approvePendingTransaction")
                .param("secretPhrase", secretPhrase3)
                .param("pendingTransaction", transactionId)
                .param("feeNQT", fee)
                .build();

        JSONObject response = apiCall.invoke();
        Logger.logMessage("approvePendingTransactionResponse:" + response.toJSONString());
        Assert.assertNotNull(response.get("transaction"));
        generateBlock();

        Assert.assertNotEquals(balance1, balanceById(id1));
        Assert.assertNotEquals(balance2, balanceById(id2));
        Assert.assertEquals(fee, balance3 - balanceById(id3));
    }

    @Test
    public void invalidVoteCasting() {
        APICall apiCall = new TwoPhasedMoneyTransferBuilder().build();
        String transactionId = TestCreateTwoPhased.issueCreateTwoPhased(apiCall, false);
        generateBlock();

        long balance1 = balanceById(id1);
        long balance2 = balanceById(id2);
        long balance4 = balanceById(id4);

        long fee = Constants.ONE_NXT;

        apiCall = new APICall.Builder("approvePendingTransaction")
                .param("secretPhrase", secretPhrase4)
                .param("pendingTransaction", transactionId)
                .param("feeNQT", fee)
                .build();

        JSONObject response = apiCall.invoke();
        Logger.logMessage("approvePendingTransactionResponse:" + response.toJSONString());
        Assert.assertNotNull(response.get("error"));
        generateBlock();

        Assert.assertEquals(balance1, balanceById(id1));
        Assert.assertEquals(balance2, balanceById(id2));
        Assert.assertEquals(balance4, balanceById(id4));
    }

}