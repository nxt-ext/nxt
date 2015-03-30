package nxt.http.accountControl;

import java.util.Arrays;

import nxt.BlockchainTest;
import nxt.Constants;
import nxt.Nxt;
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
        
        setPhasingOnlyControl(VotingModel.ACCOUNT, null, 1L, null, null, new long[] {id2});
        
        assertPhasingOnly(new PhasingParams(VotingModel.ACCOUNT.getCode(), 0L, 1L, 0L, (byte)0, new long[] {id2}));
    }
    
    @Test
    public void testAccountVoting() throws Exception {
        //all transactions must be approved either by id2 or id3
        setPhasingOnlyControl(VotingModel.ACCOUNT, null, 1L, null, null, new long[] {id2, id3});
        
        Builder builder = new ACTestUtils.Builder("sendMoney", secretPhrase1)
            .recipient(id2)
            .param("amountNQT", 1 * Constants.ONE_NXT);
        
        //no phasing - block
        ACTestUtils.assertTransactionBlocked(builder);
        
      //correct phasing
        setTransactionPhasingParams(builder, 20, VotingModel.ACCOUNT, null, 1L, null, null, new long[] {id2, id3});
        ACTestUtils.assertTransactionSuccess(builder);
        
      //subset of the voters should also be blocked
        setTransactionPhasingParams(builder, 20, VotingModel.ACCOUNT, null, 1L, null, null, new long[] {id2});
        ACTestUtils.assertTransactionBlocked(builder);
        
      //incorrect quorum - even if more restrictive, should also be blocked
        setTransactionPhasingParams(builder, 20, VotingModel.ACCOUNT, null, 2L, null, null, new long[] {id2, id3});
        ACTestUtils.assertTransactionBlocked(builder);
        
        //remove the phasing control
        builder = new ACTestUtils.Builder("setPhasingOnlyControl", secretPhrase1);
        
        setControlPhasingParams(builder, VotingModel.NONE, null, null, null, null, null);
        
        setTransactionPhasingParams(builder, 3, VotingModel.ACCOUNT, null, 1L, null, null, new long[] {id2, id3});
        
        JSONObject removePhasingOnlyJSON = ACTestUtils.assertTransactionSuccess(builder);
        generateBlock();
        
        assertPhasingOnly(new PhasingParams(VotingModel.ACCOUNT.getCode(), 0L, 1L, 0L, (byte)0, new long[] {id2, id3}));
        
        String fullHash = (String) removePhasingOnlyJSON.get("fullHash");
        
        //approve the remove
        builder = new ACTestUtils.Builder("approveTransaction", secretPhrase2)
                .param("transactionFullHash", fullHash);
        ACTestUtils.assertTransactionSuccess(builder);
        
        generateBlock();
        
        //setPhasingOnlyControl is applied here
        generateBlock();
        
        assertNoPhasingOnlyControl();
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
    
    private void setPhasingOnlyControl(VotingModel votingModel, Long holdingId, Long quorum,
            Long minBalance, MinBalanceModel minBalanceModel, long[] whitelist) {
        
        Builder builder = new ACTestUtils.Builder("setPhasingOnlyControl", secretPhrase1);
        
        setControlPhasingParams(builder, votingModel, holdingId, quorum,
                minBalance, minBalanceModel, whitelist);
        
        APICall apiCall = builder.build();
        JSONObject response = apiCall.invoke();
        Logger.logMessage("setPhasingOnlyControl response: " + response.toJSONString());
        generateBlock();
    }

    private void setControlPhasingParams(Builder builder,
            VotingModel votingModel, Long holdingId, Long quorum,
            Long minBalance, MinBalanceModel minBalanceModel, long[] whitelist) {
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
    }
    
    private void setTransactionPhasingParams(Builder builder, int finishAfter, VotingModel votingModel, Long holdingId, Long quorum,
            Long minBalance, MinBalanceModel minBalanceModel, long[] whitelist) {
        
        builder.param("phased", "true");
        
        builder.param("phasingVotingModel", votingModel.getCode());
        
        builder.param("phasingFinishHeight", Nxt.getBlockchain().getHeight() + finishAfter);
        
        if (holdingId != null) {
            builder.param("phasingHolding", holdingId);
        }
        
        if (quorum != null) {
            builder.param("phasingQuorum", quorum);
        }
        
        if (minBalance != null) {
            builder.param("phasingMinBalance", minBalance);
        }
        
        if (minBalanceModel != null) {
            builder.param("phasingMinBalanceModel", minBalanceModel.getCode());
        }
        
        if (whitelist != null) {
            builder.param("phasingWhitelisted", Arrays.stream(whitelist).mapToObj(l -> Long.toUnsignedString(l)).toArray(String[]::new));
        }
    }
}
