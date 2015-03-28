package nxt.http.accountControl;

import java.util.Arrays;

import nxt.BlockchainTest;
import nxt.PhasingParams;
import nxt.VoteWeighting.MinBalanceModel;
import nxt.VoteWeighting.VotingModel;
import nxt.http.APICall;
import nxt.http.APICall.Builder;
import nxt.util.Logger;

import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class PhasingOnlyTest extends BlockchainTest {
    @Test
    public void testSetAndGet() throws Exception {
        
        assertNoPhasingOnlyControl();
        
        setPhasingOnly(VotingModel.ACCOUNT, null, 1L, null, null, new long[] {id2});
        
        assertPhasingOnly(new PhasingParams(VotingModel.ACCOUNT.getCode(), 0L, 1L, 0L, (byte)0, new long[] {id2}));
    }
    
    private void assertNoPhasingOnlyControl() {
        Builder builder = new APICall.Builder("getPhasingOnlyControl")
            .param("account", Long.toUnsignedString(id1));
        
        JSONObject response = builder.build().invoke();
        
        byte votingMode = ((Long) response.get("phasingVotingModel")).byteValue();
        
        Assert.assertEquals(VotingModel.NONE.getCode(), votingMode);
    }
    
    private void assertPhasingOnly(PhasingParams expected) {
        Builder builder = new APICall.Builder("getPhasingOnlyControl")
            .param("account", Long.toUnsignedString(id1));

        JSONObject response = builder.build().invoke();

        PhasingParams actual = new PhasingParams(response);
        Assert.assertEquals(expected, actual);
    }
    
    private void setPhasingOnly(VotingModel votingModel, Long holdingId, Long quorum,
            Long minBalance, MinBalanceModel minBalanceModel, long[] whitelist) {
        
        Builder builder = new APICall.Builder("setPhasingOnlyControl")
                .secretPhrase(secretPhrase1)
                .feeNQT(0);
        if (votingModel != null) {
            builder.param("controlVotingModel", votingModel.getCode());
        }
        
        if (holdingId != null) {
            builder.param("controlHolding", holdingId);
        }
        
        if (quorum != null) {
            builder.param("controlQuorum", quorum);
        }
        
        if (minBalance != null) {
            builder.param("controlMinBalance", minBalance);
        }
        
        if (minBalanceModel != null) {
            builder.param("controlMinBalanceModel", minBalanceModel.getCode());
        }
        
        if (whitelist != null) {
            builder.param("controlWhitelisted", Arrays.stream(whitelist).mapToObj(l -> Long.toUnsignedString(l)).toArray(String[]::new));
        }
        
        APICall apiCall = builder.build();
        JSONObject response = apiCall.invoke();
        Logger.logMessage("setPhasingOnlyControl response: " + response.toJSONString());
        generateBlock();
    }
}
