package nxt.http.twophased;

import nxt.BlockchainTest;
import nxt.Constants;
import nxt.Nxt;
import nxt.http.APICall;
import nxt.http.twophased.TestCreateTwoPhased.TwoPhasedMoneyTransferBuilder;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class TestApproveTransaction extends BlockchainTest {

    @Test
    public void validVoteCasting() {
        int duration = 10;

        APICall apiCall = new TwoPhasedMoneyTransferBuilder()
                .maxHeight(Nxt.getBlockchain().getHeight() + duration)
                .build();

        JSONObject transactionJSON = TestCreateTwoPhased.issueCreateTwoPhased(apiCall, false);
        generateBlock();

        long balance1 = balanceById(id1);
        long balance2 = balanceById(id2);
        long balance3 = balanceById(id3);

        long fee = Constants.ONE_NXT;

        apiCall = new APICall.Builder("approveTransaction")
                .param("secretPhrase", secretPhrase3)
                .param("transactionFullHash", (String) transactionJSON.get("fullHash"))
                .param("feeNQT", fee)
                .build();

        JSONObject response = apiCall.invoke();
        Logger.logMessage("approvePendingTransactionResponse:" + response.toJSONString());
        Assert.assertNotNull(response.get("transaction"));
        generateBlocks(duration);

        long updBalance1 = balanceById(id1);
        long updBalance2 = balanceById(id2);
        Assert.assertNotEquals("id1 balance: ", balance1, updBalance1);
        Assert.assertNotEquals("id2 balance: ", balance2, updBalance2);
        Assert.assertEquals("fee", fee, balance3 - balanceById(id3));
    }

    @Test
    public void invalidVoteCasting() {
        int duration = 10;

        APICall apiCall = new TwoPhasedMoneyTransferBuilder()
                .maxHeight(Nxt.getBlockchain().getHeight() + duration)
                .build();

        JSONObject transactionJSON = TestCreateTwoPhased.issueCreateTwoPhased(apiCall, false);
        generateBlock();

        long balance1 = balanceById(id1);
        long balance2 = balanceById(id2);
        long balance4 = balanceById(id4);

        long fee = Constants.ONE_NXT;

        apiCall = new APICall.Builder("approveTransaction")
                .param("secretPhrase", secretPhrase4)
                .param("transactionFullHash", (String) transactionJSON.get("fullHash"))
                .param("feeNQT", fee)
                .build();

        JSONObject response = apiCall.invoke();
        Logger.logMessage("approvePendingTransactionResponse:" + response.toJSONString());
        Assert.assertNotNull(response.get("error"));
        generateBlock();

        Assert.assertEquals("id1 balance: ", balance1, balanceById(id1));
        Assert.assertEquals("id2 balance: ", balance2, balanceById(id2));
        Assert.assertEquals("id4 balance: ", balance4, balanceById(id4));

        generateBlocks(duration);

        Assert.assertEquals("id1 balance: ", balance1, balanceById(id1));
        Assert.assertEquals("id2 balance: ", balance2, balanceById(id2));
        Assert.assertEquals("id4 balance: ", balance4, balanceById(id4));
    }

}