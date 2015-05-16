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
        if (transactionJSON == null) {
            Assert.fail("transactionJSON is null");
        }
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
        Logger.logMessage("approvePhasedTransactionResponse:" + response.toJSONString());
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
        if (transactionJSON == null) {
            Assert.fail("transactionJSON is null");
        }
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
        Logger.logMessage("approvePhasedTransactionResponse:" + response.toJSONString());
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

    @Test
    public void sendMoneyPhasedNoVoting() {
        long fee = 2*Constants.ONE_NXT;
        JSONObject response = new APICall.Builder("sendMoney").
                param("secretPhrase", testers.get(1).getSecretPhrase()).
                param("recipient", testers.get(2).getStrId()).
                param("amountNQT", 100 * Constants.ONE_NXT).
                param("feeNQT", fee).
                param("phased", "true").
                param("phasingFinishHeight", baseHeight + 2).
                param("phasingVotingModel", -1).
                build().invoke();
        Logger.logDebugMessage("sendMoney: " + response);

        generateBlock();
        // Transaction is not applied yet, fee is paid
        // Forger
        Assert.assertEquals(fee, testers.get(0).getBalanceDiff());
        Assert.assertEquals(fee, testers.get(0).getUnconfirmedBalanceDiff());
        // Sender
        Assert.assertEquals(-fee, testers.get(1).getBalanceDiff());
        Assert.assertEquals(-100 * Constants.ONE_NXT - fee, testers.get(1).getUnconfirmedBalanceDiff());
        // Recipient
        Assert.assertEquals(0, testers.get(2).getBalanceDiff());
        Assert.assertEquals(0, testers.get(2).getUnconfirmedBalanceDiff());

        generateBlock();
        // Transaction is applied
        // Sender
        Assert.assertEquals(-100 * Constants.ONE_NXT - fee, testers.get(1).getBalanceDiff());
        Assert.assertEquals(-100 * Constants.ONE_NXT - fee, testers.get(1).getUnconfirmedBalanceDiff());
        // Recipient
        Assert.assertEquals(100 * Constants.ONE_NXT, testers.get(2).getBalanceDiff());
        Assert.assertEquals(100 * Constants.ONE_NXT, testers.get(2).getUnconfirmedBalanceDiff());
    }

    @Test
    public void sendMoneyPhasedByTransactionHash() {
        JSONObject response = getSignedBytes();
        Logger.logDebugMessage("signedSendMessage: " + response);
        String fullHash = (String)response.get("fullHash");
        Assert.assertEquals(64, fullHash.length());
        String approvalTransactionBytes = (String)response.get("transactionBytes");

        long fee = 2 * Constants.ONE_NXT;
        response = new APICall.Builder("sendMoney").
                param("secretPhrase", testers.get(1).getSecretPhrase()).
                param("recipient", testers.get(2).getStrId()).
                param("amountNQT", 100 * Constants.ONE_NXT).
                param("feeNQT", fee).
                param("phased", "true").
                param("phasingFinishHeight", baseHeight + 3).
                param("phasingVotingModel", 4).
                param("phasingLinkedFullHash", fullHash).
                param("phasingQuorum", 1).
                build().invoke();
        Logger.logDebugMessage("sendMoney: " + response);

        generateBlock();
        // Transaction is not applied yet
        // Sender
        Assert.assertEquals(-fee, testers.get(1).getBalanceDiff());
        Assert.assertEquals(-100 * Constants.ONE_NXT - fee, testers.get(1).getUnconfirmedBalanceDiff());
        // Recipient
        Assert.assertEquals(0, testers.get(2).getBalanceDiff());
        Assert.assertEquals(0, testers.get(2).getUnconfirmedBalanceDiff());

        response = new APICall.Builder("broadcastTransaction").
                param("transactionBytes", approvalTransactionBytes).
                build().invoke();
        Logger.logDebugMessage("broadcastTransaction: " + response);
        generateBlock();

        // Transaction is still not applied since finish height not reached
        // Sender
        Assert.assertEquals(-fee, testers.get(1).getBalanceDiff());
        Assert.assertEquals(-100 * Constants.ONE_NXT - fee, testers.get(1).getUnconfirmedBalanceDiff());
        // Recipient
        Assert.assertEquals(0, testers.get(2).getBalanceDiff());
        Assert.assertEquals(0, testers.get(2).getUnconfirmedBalanceDiff());

        generateBlock();
        // Transaction is applied
        // Sender
        Assert.assertEquals(-100 * Constants.ONE_NXT - fee, testers.get(1).getBalanceDiff());
        Assert.assertEquals(-100 * Constants.ONE_NXT - fee, testers.get(1).getUnconfirmedBalanceDiff());
        // Recipient
        Assert.assertEquals(100 * Constants.ONE_NXT, testers.get(2).getBalanceDiff());
        Assert.assertEquals(100 * Constants.ONE_NXT, testers.get(2).getUnconfirmedBalanceDiff());
    }

    @Test
    public void sendMoneyPhasedByTransactionHash2of3() {
        JSONObject response = getSignedBytes();
        Logger.logDebugMessage("signedSendMessage: " + response);
        String fullHash1 = (String)response.get("fullHash");
        Assert.assertEquals(64, fullHash1.length());
        String approvalTransactionBytes1 = (String)response.get("transactionBytes");
        response = getSignedBytes();
        Logger.logDebugMessage("signedSendMessage: " + response);
        String fullHash2 = (String)response.get("fullHash");
        Assert.assertEquals(64, fullHash1.length());
        response = getSignedBytes();
        Logger.logDebugMessage("signedSendMessage: " + response);
        String fullHash3 = (String)response.get("fullHash");
        Assert.assertEquals(64, fullHash1.length());
        String approvalTransactionBytes3 = (String)response.get("transactionBytes");

        long fee = 2 * Constants.ONE_NXT;
        response = new APICall.Builder("sendMoney").
                param("secretPhrase", testers.get(1).getSecretPhrase()).
                param("recipient", testers.get(2).getStrId()).
                param("amountNQT", 100 * Constants.ONE_NXT).
                param("feeNQT", fee).
                param("phased", "true").
                param("phasingFinishHeight", baseHeight + 2).
                param("phasingVotingModel", 4).
                param("phasingLinkedFullHash", new String[] { fullHash1, fullHash2, fullHash3 }).
                param("phasingQuorum", 2).
                build().invoke();
        Logger.logDebugMessage("sendMoney: " + response);

        generateBlock();
        // Transaction is not applied yet
        // Sender
        Assert.assertEquals(-fee, testers.get(1).getBalanceDiff());
        Assert.assertEquals(-100 * Constants.ONE_NXT - fee, testers.get(1).getUnconfirmedBalanceDiff());
        // Recipient
        Assert.assertEquals(0, testers.get(2).getBalanceDiff());
        Assert.assertEquals(0, testers.get(2).getUnconfirmedBalanceDiff());

        response = new APICall.Builder("broadcastTransaction").
                param("transactionBytes", approvalTransactionBytes1).
                build().invoke();
        Logger.logDebugMessage("broadcastTransaction: " + response);
        response = new APICall.Builder("broadcastTransaction").
                param("transactionBytes", approvalTransactionBytes3).
                build().invoke();
        Logger.logDebugMessage("broadcastTransaction: " + response);
        generateBlock();

        // Transaction is applied since 2 out 3 hashes were provided
        // Sender
        Assert.assertEquals(-100 * Constants.ONE_NXT - fee, testers.get(1).getBalanceDiff());
        Assert.assertEquals(-100 * Constants.ONE_NXT - fee, testers.get(1).getUnconfirmedBalanceDiff());
        // Recipient
        Assert.assertEquals(100 * Constants.ONE_NXT, testers.get(2).getBalanceDiff());
        Assert.assertEquals(100 * Constants.ONE_NXT, testers.get(2).getUnconfirmedBalanceDiff());
    }

    @Test
    public void sendMoneyPhasedByTransactionHashNotApplied() {
        long fee = 2 * Constants.ONE_NXT;
        JSONObject response = new APICall.Builder("sendMoney").
                param("secretPhrase", testers.get(1).getSecretPhrase()).
                param("recipient", testers.get(2).getStrId()).
                param("amountNQT", 100 * Constants.ONE_NXT).
                param("feeNQT", fee).
                param("phased", "true").
                param("phasingFinishHeight", baseHeight + 2).
                param("phasingVotingModel", 4).
                param("phasingLinkedFullHash", "a13bbe67211fea8d59b2621f1e0118bb242dc5000d428a23a8bd47491a05d681"). // this hash does not match any transaction
                param("phasingQuorum", 1).
                build().invoke();
        Logger.logDebugMessage("sendMoney: " + response);

        generateBlock();
        // Transaction is not applied yet
        // Sender
        Assert.assertEquals(-fee, testers.get(1).getBalanceDiff());
        Assert.assertEquals(-100 * Constants.ONE_NXT - fee, testers.get(1).getUnconfirmedBalanceDiff());
        // Recipient
        Assert.assertEquals(0, testers.get(2).getBalanceDiff());
        Assert.assertEquals(0, testers.get(2).getUnconfirmedBalanceDiff());

        generateBlock();
        // Transaction is rejected since full hash does not match
        // Sender
        Assert.assertEquals(-fee, testers.get(1).getBalanceDiff());
        Assert.assertEquals(-fee, testers.get(1).getUnconfirmedBalanceDiff());
        // Recipient
        Assert.assertEquals(0, testers.get(2).getBalanceDiff());
        Assert.assertEquals(0, testers.get(2).getUnconfirmedBalanceDiff());
    }

    @Test
    public void setAliasPhasedByTransactionHashInvalid() {
        JSONObject response = getSignedBytes();
        Logger.logDebugMessage("signedSendMessage: " + response);
        String fullHash = (String)response.get("fullHash");
        Assert.assertEquals(64, fullHash.length());
        String approvalTransactionBytes = (String)response.get("transactionBytes");

        long fee = 2 * Constants.ONE_NXT;
        String alias = "alias" + System.currentTimeMillis();
        response = new APICall.Builder("setAlias").
                param("secretPhrase", testers.get(1).getSecretPhrase()).
                param("aliasName", alias).
                param("feeNQT", fee).
                param("phased", "true").
                param("phasingFinishHeight", baseHeight + 4).
                param("phasingVotingModel", 4).
                param("phasingLinkedFullHash", fullHash).
                param("phasingQuorum", 1).
                build().invoke();
        Logger.logDebugMessage("setAlias: " + response);

        generateBlock();
        response = new APICall.Builder("getAlias").
                param("aliasName", alias).
                build().invoke();
        Logger.logDebugMessage("getAlias: " + response);
        Assert.assertEquals((long)5, response.get("errorCode"));

        response = new APICall.Builder("broadcastTransaction").
                param("transactionBytes", approvalTransactionBytes).
                build().invoke();
        Logger.logDebugMessage("broadcastTransaction: " + response);
        generateBlock();

        // allocate the same alias immediately
        response = new APICall.Builder("setAlias").
                param("secretPhrase", testers.get(2).getSecretPhrase()).
                param("aliasName", alias).
                param("feeNQT", fee).
                build().invoke();
        Logger.logDebugMessage("setSameAlias: " + response);
        generateBlock();
        // phased setAlias transaction is applied but invalid
        response = new APICall.Builder("getAlias").
                param("aliasName", alias).
                build().invoke();
        Logger.logDebugMessage("getAlias: " + response);
        Assert.assertEquals(testers.get(2).getStrId(), response.get("account"));
        generateBlock();
        // phased setAlias transaction is applied but invalid
        response = new APICall.Builder("getAlias").
                param("aliasName", alias).
                build().invoke();
        Logger.logDebugMessage("getAlias: " + response);
        Assert.assertEquals(testers.get(2).getStrId(), response.get("account"));
    }

    private JSONObject getSignedBytes() {
        JSONObject response = new APICall.Builder("sendMessage").
                param("publicKey", testers.get(3).getPublicKeyStr()).
                param("recipient", testers.get(1).getStrId()).
                param("message", "approval notice").
                param("feeNQT", Constants.ONE_NXT).
                build().invoke();
        Logger.logDebugMessage("sendMessage not broadcasted: " + response);
        response = new APICall.Builder("signTransaction").
                param("secretPhrase", testers.get(3).getSecretPhrase()).
                param("unsignedTransactionBytes", (String)response.get("unsignedTransactionBytes")).
                build().invoke();
        return response;
    }
}