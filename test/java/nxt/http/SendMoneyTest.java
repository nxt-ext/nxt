package nxt.http;

import nxt.BlockchainTest;
import nxt.Constants;
import nxt.Nxt;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class SendMoneyTest extends BlockchainTest {

    @Test
    public void sendMoney() {
        JSONObject response = new APICall.Builder("sendMoney").
                param("secretPhrase", testers.get(1).getSecretPhrase()).
                param("recipient", testers.get(2).getStrId()).
                param("amountNQT", 100 * Constants.ONE_NXT).
                param("feeNQT", Constants.ONE_NXT).
                build().invoke();
        Logger.logDebugMessage("sendMoney: " + response);
        // Forger
        Assert.assertEquals(0, testers.get(0).getBalanceDiff());
        Assert.assertEquals(0, testers.get(0).getUnconfirmedBalanceDiff());
        // Sender
        Assert.assertEquals(0, testers.get(1).getBalanceDiff());
        Assert.assertEquals(-100 * Constants.ONE_NXT - Constants.ONE_NXT, testers.get(1).getUnconfirmedBalanceDiff());
        // Recipient
        Assert.assertEquals(0, testers.get(2).getBalanceDiff());
        Assert.assertEquals(0, testers.get(2).getUnconfirmedBalanceDiff());
        generateBlock();
        // Forger
        Assert.assertEquals(Constants.ONE_NXT, testers.get(0).getBalanceDiff());
        Assert.assertEquals(Constants.ONE_NXT, testers.get(0).getUnconfirmedBalanceDiff());
        // Sender
        Assert.assertEquals(-100 * Constants.ONE_NXT - Constants.ONE_NXT, testers.get(1).getBalanceDiff());
        Assert.assertEquals(-100 * Constants.ONE_NXT - Constants.ONE_NXT, testers.get(1).getUnconfirmedBalanceDiff());
        // Recipient
        Assert.assertEquals(100 * Constants.ONE_NXT, testers.get(2).getBalanceDiff());
        Assert.assertEquals(100 * Constants.ONE_NXT, testers.get(2).getUnconfirmedBalanceDiff());
    }

    @Test
    public void sendTooMuchMoney() {
        JSONObject response = new APICall.Builder("sendMoney").
                param("secretPhrase", testers.get(1).getSecretPhrase()).
                param("recipient", testers.get(2).getStrId()).
                param("amountNQT", testers.get(1).getInitialBalance()).
                param("feeNQT", Constants.ONE_NXT).
                build().invoke();
        Logger.logDebugMessage("sendMoney: " + response);
        Assert.assertEquals((long)6, response.get("errorCode"));
    }

    @Test
    public void sendAndReturn() {
        JSONObject response = new APICall.Builder("sendMoney").
                param("secretPhrase", testers.get(1).getSecretPhrase()).
                param("recipient", testers.get(2).getStrId()).
                param("amountNQT", 100 * Constants.ONE_NXT).
                param("feeNQT", Constants.ONE_NXT).
                build().invoke();
        Logger.logDebugMessage("sendMoney1: " + response);
        response = new APICall.Builder("sendMoney").
                param("secretPhrase", testers.get(2).getSecretPhrase()).
                param("recipient", testers.get(1).getStrId()).
                param("amountNQT", 100 * Constants.ONE_NXT).
                param("feeNQT", Constants.ONE_NXT).
                build().invoke();
        Logger.logDebugMessage("sendMoney2: " + response);
        // Forger
        Assert.assertEquals(0, testers.get(0).getBalanceDiff());
        Assert.assertEquals(0, testers.get(0).getUnconfirmedBalanceDiff());
        // Sender
        Assert.assertEquals(0, testers.get(1).getBalanceDiff());
        Assert.assertEquals(-100 * Constants.ONE_NXT - Constants.ONE_NXT, testers.get(1).getUnconfirmedBalanceDiff());
        // Recipient
        Assert.assertEquals(0, testers.get(2).getBalanceDiff());
        Assert.assertEquals(-100 * Constants.ONE_NXT - Constants.ONE_NXT, testers.get(2).getUnconfirmedBalanceDiff());
        generateBlock();
        // Forger
        Assert.assertEquals(2*Constants.ONE_NXT, testers.get(0).getBalanceDiff());
        Assert.assertEquals(2*Constants.ONE_NXT, testers.get(0).getUnconfirmedBalanceDiff());
        // Sender
        Assert.assertEquals(-Constants.ONE_NXT, testers.get(1).getBalanceDiff());
        Assert.assertEquals(-Constants.ONE_NXT, testers.get(1).getUnconfirmedBalanceDiff());
        // Recipient
        Assert.assertEquals(-Constants.ONE_NXT, testers.get(2).getBalanceDiff());
        Assert.assertEquals(-Constants.ONE_NXT, testers.get(2).getUnconfirmedBalanceDiff());
    }

    @Test
    public void signAndBroadcastBytes() {
        JSONObject response = new APICall.Builder("sendMoney").
                param("publicKey", testers.get(1).getPublicKeyStr()).
                param("recipient", testers.get(2).getStrId()).
                param("amountNQT", 100 * Constants.ONE_NXT).
                param("feeNQT", Constants.ONE_NXT).
                build().invoke();
        Logger.logDebugMessage("sendMoney: " + response);
        generateBlock();
        // No change transaction not broadcast
        Assert.assertEquals(0, testers.get(1).getBalanceDiff());
        Assert.assertEquals(0, testers.get(1).getUnconfirmedBalanceDiff());
        Assert.assertEquals(0, testers.get(2).getBalanceDiff());
        Assert.assertEquals(0, testers.get(2).getUnconfirmedBalanceDiff());

        response = new APICall.Builder("signTransaction").
                param("secretPhrase", testers.get(1).getSecretPhrase()).
                param("unsignedTransactionBytes", (String)response.get("unsignedTransactionBytes")).
                build().invoke();
        Logger.logDebugMessage("signTransaction: " + response);

        response = new APICall.Builder("broadcastTransaction").
                param("transactionBytes", (String)response.get("transactionBytes")).
                build().invoke();
        Logger.logDebugMessage("broadcastTransaction: " + response);
        generateBlock();

        // Sender
        Assert.assertEquals(-100 * Constants.ONE_NXT - Constants.ONE_NXT, testers.get(1).getBalanceDiff());
        Assert.assertEquals(-100 * Constants.ONE_NXT - Constants.ONE_NXT, testers.get(1).getUnconfirmedBalanceDiff());
        // Recipient
        Assert.assertEquals(100 * Constants.ONE_NXT, testers.get(2).getBalanceDiff());
        Assert.assertEquals(100 * Constants.ONE_NXT, testers.get(2).getUnconfirmedBalanceDiff());
    }

    @Test
    public void signAndBroadcastJSON() {
        JSONObject response = new APICall.Builder("sendMoney").
                param("publicKey", testers.get(1).getPublicKeyStr()).
                param("recipient", testers.get(2).getStrId()).
                param("amountNQT", 100 * Constants.ONE_NXT).
                param("feeNQT", Constants.ONE_NXT).
                build().invoke();
        Logger.logDebugMessage("sendMoney: " + response);
        generateBlock();
        // No change transaction not broadcast
        Assert.assertEquals(0, testers.get(1).getBalanceDiff());
        Assert.assertEquals(0, testers.get(1).getUnconfirmedBalanceDiff());
        Assert.assertEquals(0, testers.get(2).getBalanceDiff());
        Assert.assertEquals(0, testers.get(2).getUnconfirmedBalanceDiff());

        response = new APICall.Builder("signTransaction").
                param("secretPhrase", testers.get(1).getSecretPhrase()).
                param("unsignedTransactionJSON", response.get("transactionJSON").toString()).
                build().invoke();
        Logger.logDebugMessage("signTransaction: " + response);

        response = new APICall.Builder("broadcastTransaction").
                param("transactionBytes", (String)response.get("transactionBytes")).
                build().invoke();
        Logger.logDebugMessage("broadcastTransaction: " + response);
        generateBlock();

        // Sender
        Assert.assertEquals(-100 * Constants.ONE_NXT - Constants.ONE_NXT, testers.get(1).getBalanceDiff());
        Assert.assertEquals(-100 * Constants.ONE_NXT - Constants.ONE_NXT, testers.get(1).getUnconfirmedBalanceDiff());
        // Recipient
        Assert.assertEquals(100 * Constants.ONE_NXT, testers.get(2).getBalanceDiff());
        Assert.assertEquals(100 * Constants.ONE_NXT, testers.get(2).getUnconfirmedBalanceDiff());
    }

    @BeforeClass
    public static void beforeClass() {
        Nxt.init();
    }

    @AfterClass
    public static void afterClass() {
        Nxt.shutdown();
    }


}
