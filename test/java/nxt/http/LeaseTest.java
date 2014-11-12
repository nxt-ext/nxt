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

public class LeaseTest extends BlockchainTest {

    @Test
    public void lease() {
        JSONObject response = new APICall.Builder("leaseBalance").
                param("secretPhrase", testers.get(2).getSecretPhrase()).
                param("recipient", testers.get(1).getStrId()).
                param("period", "2").
                param("feeNQT", Constants.ONE_NXT).
                build().invoke();
        Logger.logDebugMessage("leaseBalance: " + response);
        response = new APICall.Builder("leaseBalance").
                param("secretPhrase", testers.get(3).getSecretPhrase()).
                param("recipient", testers.get(1).getStrId()).
                param("period", "3").
                param("feeNQT", Constants.ONE_NXT).
                build().invoke();
        Logger.logDebugMessage("leaseBalance: " + response);
        generateBlock();
        JSONObject lesseeResponse = new APICall.Builder("getAccount").
                param("account", testers.get(1).getRsAccount()).
                build().invoke();
        Logger.logDebugMessage("getLesseeAccount: " + lesseeResponse);
        Assert.assertEquals(testers.get(1).getInitialBalance() / Constants.ONE_NXT, lesseeResponse.get("effectiveBalanceNXT"));

        JSONObject leasedResponse1 = new APICall.Builder("getAccount").
                param("account", testers.get(2).getRsAccount()).
                build().invoke();
        Logger.logDebugMessage("getLeasedAccount: " + leasedResponse1);
        Assert.assertEquals(testers.get(1).getRsAccount(), leasedResponse1.get("currentLesseeRS"));
        Assert.assertEquals((long)(baseHeight + 1 + 1), leasedResponse1.get("currentLeasingHeightFrom"));
        Assert.assertEquals((long)(baseHeight + 1 + 1 + 2), leasedResponse1.get("currentLeasingHeightTo"));
        JSONObject leasedResponse2 = new APICall.Builder("getAccount").
                param("account", testers.get(3).getRsAccount()).
                build().invoke();
        Logger.logDebugMessage("getLeasedAccount: " + leasedResponse1);
        Assert.assertEquals(testers.get(1).getRsAccount(), leasedResponse2.get("currentLesseeRS"));
        Assert.assertEquals((long)(baseHeight + 1 + 1), leasedResponse2.get("currentLeasingHeightFrom"));
        Assert.assertEquals((long)(baseHeight + 1 + 1 + 3), leasedResponse2.get("currentLeasingHeightTo"));
        generateBlock();
        lesseeResponse = new APICall.Builder("getAccount").
                param("account", testers.get(1).getRsAccount()).
                build().invoke();
        Logger.logDebugMessage("getLesseeAccount: " + lesseeResponse);
        Assert.assertEquals((testers.get(1).getInitialBalance() + testers.get(2).getInitialBalance() + testers.get(3).getInitialBalance()) / Constants.ONE_NXT - 2 /* fees */,
                lesseeResponse.get("effectiveBalanceNXT"));
        generateBlock();
        generateBlock();
        lesseeResponse = new APICall.Builder("getAccount").
                param("account", testers.get(1).getRsAccount()).
                build().invoke();
        Logger.logDebugMessage("getLesseeAccount: " + lesseeResponse);
        Assert.assertEquals((testers.get(1).getInitialBalance() + testers.get(3).getInitialBalance()) / Constants.ONE_NXT - 1 /* fees */,
                lesseeResponse.get("effectiveBalanceNXT"));
        generateBlock();
        lesseeResponse = new APICall.Builder("getAccount").
                param("account", testers.get(1).getRsAccount()).
                build().invoke();
        Logger.logDebugMessage("getLesseeAccount: " + lesseeResponse);
        Assert.assertEquals((testers.get(1).getInitialBalance()) / Constants.ONE_NXT,
                lesseeResponse.get("effectiveBalanceNXT"));
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
