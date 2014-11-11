package nxt.http;

import nxt.BlockchainProcessor;
import nxt.BlockchainTest;
import nxt.Constants;
import nxt.Helper;
import nxt.Nxt;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class SendMessageTest extends BlockchainTest {

    @Test
    public void sendMessage() {
        JSONObject response = new APICall.Builder("sendMessage").
                param("secretPhrase", testers.get(1).getSecretPhrase()).
                param("recipient", testers.get(2).getStrId()).
                param("message", "hi").
                param("feeNQT", Constants.ONE_NXT).
                build().invoke();
        Logger.logDebugMessage("sendMessage: " + response);
        String transaction = (String) response.get("transaction");
        generateBlock();
        response = new APICall.Builder("readMessage").
                param("secretPhrase", testers.get(2).getSecretPhrase()).
                param("transaction", transaction).
                param("feeNQT", Constants.ONE_NXT).
                build().invoke();
        Logger.logDebugMessage("readMessage: " + response);
        Assert.assertEquals("hi", response.get("message"));
    }

    @BeforeClass
    public static void beforeClass() {
        Nxt.init();
        Nxt.getBlockchainProcessor().addListener(new Helper.BlockListener(), BlockchainProcessor.Event.BLOCK_GENERATED);
        Assert.assertEquals(0, Helper.getCount("unconfirmed_transaction"));
        Assert.assertEquals(0, Helper.getCount("currency"));
    }

    @AfterClass
    public static void afterClass() {
        Assert.assertEquals(0, Helper.getCount("unconfirmed_transaction"));
        Assert.assertEquals(0, Helper.getCount("currency"));
        Nxt.shutdown();
    }


}
