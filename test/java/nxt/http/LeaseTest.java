package nxt.http;

import nxt.BlockchainTest;
import nxt.Constants;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class LeaseTest extends BlockchainTest {

    @Test
    public void lease() {
        // #2 & #3 lease their balance to %1
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

        // effective balance hasn't changed since lease is not in effect yet
        JSONObject lesseeResponse = new APICall.Builder("getAccount").
                param("account", testers.get(1).getRsAccount()).
                build().invoke();
        Logger.logDebugMessage("getLesseeAccount: " + lesseeResponse);
        Assert.assertEquals(testers.get(1).getInitialEffectiveBalance(), lesseeResponse.get("effectiveBalanceNXT"));

        // lease is registered
        JSONObject leasedResponse1 = new APICall.Builder("getAccount").
                param("account", testers.get(2).getRsAccount()).
                build().invoke();
        Logger.logDebugMessage("getLeasedAccount: " + leasedResponse1);
        Assert.assertEquals(testers.get(1).getRsAccount(), leasedResponse1.get("currentLesseeRS"));
        Assert.assertEquals((long) (baseHeight + 1 + 1), leasedResponse1.get("currentLeasingHeightFrom"));
        Assert.assertEquals((long) (baseHeight + 1 + 1 + 2), leasedResponse1.get("currentLeasingHeightTo"));
        JSONObject leasedResponse2 = new APICall.Builder("getAccount").
                param("account", testers.get(3).getRsAccount()).
                build().invoke();
        Logger.logDebugMessage("getLeasedAccount: " + leasedResponse1);
        Assert.assertEquals(testers.get(1).getRsAccount(), leasedResponse2.get("currentLesseeRS"));
        Assert.assertEquals((long) (baseHeight + 1 + 1), leasedResponse2.get("currentLeasingHeightFrom"));
        Assert.assertEquals((long) (baseHeight + 1 + 1 + 3), leasedResponse2.get("currentLeasingHeightTo"));
        generateBlock();


        lesseeResponse = new APICall.Builder("getAccount").
                param("account", testers.get(1).getRsAccount()).
                build().invoke();
        Logger.logDebugMessage("getLesseeAccount: " + lesseeResponse);
        Assert.assertEquals(testers.get(1).getInitialEffectiveBalance() + testers.get(2).getInitialEffectiveBalance() + testers.get(3).getInitialEffectiveBalance() - 2 + 1,
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
}
