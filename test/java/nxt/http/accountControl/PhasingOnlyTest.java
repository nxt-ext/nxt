/******************************************************************************
 * Copyright © 2013-2015 The Nxt Core Developers.                             *
 *                                                                            *
 * See the AUTHORS.txt, DEVELOPER-AGREEMENT.txt and LICENSE.txt files at      *
 * the top-level directory of this distribution for the individual copyright  *
 * holder information and the developer policies on copyright and licensing.  *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * Nxt software, including this file, may be copied, modified, propagated,    *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

package nxt.http.accountControl;

import nxt.BlockchainTest;
import nxt.Constants;
import nxt.Nxt;
import nxt.PhasingParams;
import nxt.VoteWeighting.MinBalanceModel;
import nxt.VoteWeighting.VotingModel;
import nxt.http.APICall;
import nxt.http.APICall.Builder;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class PhasingOnlyTest extends BlockchainTest {
    @Test
    public void testSetAndGet() throws Exception {
        
        assertNoPhasingOnlyControl();
        
        setPhasingOnlyControl(VotingModel.ACCOUNT, null, 1L, null, null, new long[] {BOB.getId()});
        
        assertPhasingOnly(new PhasingParams(VotingModel.ACCOUNT.getCode(), 0L, 1L, 0L, (byte)0, new long[] {BOB.getId()}));
    }
    
    @Test
    public void testAccountVoting() throws Exception {
        //all transactions must be approved either by BOB or CHUCK
        setPhasingOnlyControl(VotingModel.ACCOUNT, null, 1L, null, null, new long[] {BOB.getId(), CHUCK.getId()});
        
        Builder builder = new ACTestUtils.Builder("sendMoney", ALICE.getSecretPhrase())
            .recipient(BOB.getId())
            .param("amountNQT", 1 * Constants.ONE_NXT);
        
        //no phasing - block
        ACTestUtils.assertTransactionBlocked(builder);
        
      //correct phasing
        setTransactionPhasingParams(builder, 20, VotingModel.ACCOUNT, null, 1L, null, null, new long[] {BOB.getId(), CHUCK.getId()});
        ACTestUtils.assertTransactionSuccess(builder);
        
      //subset of the voters should also be blocked
        setTransactionPhasingParams(builder, 20, VotingModel.ACCOUNT, null, 1L, null, null, new long[] {BOB.getId()});
        ACTestUtils.assertTransactionBlocked(builder);
        
      //incorrect quorum - even if more restrictive, should also be blocked
        setTransactionPhasingParams(builder, 20, VotingModel.ACCOUNT, null, 2L, null, null, new long[] {BOB.getId(), CHUCK.getId()});
        ACTestUtils.assertTransactionBlocked(builder);
        
        //remove the phasing control
        builder = new ACTestUtils.Builder("setPhasingOnlyControl", ALICE.getSecretPhrase());
        
        setControlPhasingParams(builder, VotingModel.NONE, null, null, null, null, null);
        
        setTransactionPhasingParams(builder, 3, VotingModel.ACCOUNT, null, 1L, null, null, new long[] {BOB.getId(), CHUCK.getId()});
        
        JSONObject removePhasingOnlyJSON = ACTestUtils.assertTransactionSuccess(builder);
        generateBlock();
        
        assertPhasingOnly(new PhasingParams(VotingModel.ACCOUNT.getCode(), 0L, 1L, 0L, (byte)0, new long[] {BOB.getId(), CHUCK.getId()}));
        
        String fullHash = (String) removePhasingOnlyJSON.get("fullHash");
        
        //approve the remove
        builder = new ACTestUtils.Builder("approveTransaction", BOB.getSecretPhrase())
                .param("transactionFullHash", fullHash);
        ACTestUtils.assertTransactionSuccess(builder);
        
        generateBlock();

        assertNoPhasingOnlyControl();
    }
    
    @Test
    public void testRejectingPendingTransaction() throws Exception {

        Builder builder = new ACTestUtils.Builder("sendMoney", ALICE.getSecretPhrase())
            .recipient(BOB.getId())
            .param("amountNQT", 1 * Constants.ONE_NXT);
    
        setTransactionPhasingParams(builder, 4, VotingModel.ACCOUNT, null, 1L, null, null, new long[] {BOB.getId(), CHUCK.getId()});
        JSONObject sendMoneyJSON = ACTestUtils.assertTransactionSuccess(builder);
        generateBlock();
        
        builder = new ACTestUtils.Builder("setPhasingOnlyControl", ALICE.getSecretPhrase());
        
        setControlPhasingParams(builder, VotingModel.ACCOUNT, null, 1L, null, null, new long[] {DAVE.getId()});
        
        ACTestUtils.assertTransactionSuccess(builder);
        
        generateBlock();
        
        long balanceBeforeTransactionRejection = ACTestUtils.getAccountBalance(ALICE.getId(), "unconfirmedBalanceNQT");
        
        String fullHash = (String) sendMoneyJSON.get("fullHash");
        
        //approve the pending transaction
        builder = new ACTestUtils.Builder("approveTransaction", BOB.getSecretPhrase())
                .param("transactionFullHash", fullHash);
        ACTestUtils.assertTransactionSuccess(builder);
        
        generateBlock();

        //the sendMoney finish height
        generateBlock();

        //Assert the unconfirmed balance is recovered
        Assert.assertEquals(balanceBeforeTransactionRejection + 1 * Constants.ONE_NXT, 
                ACTestUtils.getAccountBalance(ALICE.getId(), "unconfirmedBalanceNQT"));
    }
    
    @Test
    public void testBalanceVoting() {
        setPhasingOnlyControl(VotingModel.NQT, null, 100 * Constants.ONE_NXT, null, null, null);
        
        Builder builder = new ACTestUtils.Builder("sendMoney", ALICE.getSecretPhrase())
            .recipient(BOB.getId())
            .param("amountNQT", 1 * Constants.ONE_NXT);
        
        //no phasing - block
        ACTestUtils.assertTransactionBlocked(builder);
        
        setTransactionPhasingParams(builder, 20, VotingModel.NQT, null, 100 * Constants.ONE_NXT, null, null, new long[] {DAVE.getId()});
        ACTestUtils.assertTransactionBlocked(builder);
        
        setTransactionPhasingParams(builder, 20, VotingModel.ACCOUNT, null, 1L, null, null, new long[] {BOB.getId(), CHUCK.getId()});
        ACTestUtils.assertTransactionBlocked(builder);
        
        setTransactionPhasingParams(builder, 20, VotingModel.NQT, null, 100 * Constants.ONE_NXT + 1, null, null, null);
        ACTestUtils.assertTransactionBlocked(builder);
        
        builder = new ACTestUtils.Builder("sendMoney", ALICE.getSecretPhrase())
            .recipient(BOB.getId())
            .param("amountNQT", 1 * Constants.ONE_NXT);
        
        setTransactionPhasingParams(builder, 20, VotingModel.NQT, null, 100 * Constants.ONE_NXT, null, null, null);
        ACTestUtils.assertTransactionSuccess(builder);
    }
    
    @Test
    public void testAssetVoting() {
        Builder builder = new ACTestUtils.AssetBuilder(ALICE.getSecretPhrase(), "TestAsset");
        String assetId = (String) ACTestUtils.assertTransactionSuccess(builder).get("transaction");
        generateBlock();
        
        builder = new ACTestUtils.AssetBuilder(ALICE.getSecretPhrase(), "TestAsset2");
        String asset2Id = (String) ACTestUtils.assertTransactionSuccess(builder).get("transaction");
        generateBlock();
        
        setPhasingOnlyControl(VotingModel.ASSET, assetId, 100L, null, null, null);
        
        builder = new ACTestUtils.Builder("sendMoney", ALICE.getSecretPhrase())
            .recipient(BOB.getId())
            .param("amountNQT", 1 * Constants.ONE_NXT);
        ACTestUtils.assertTransactionBlocked(builder);
        
        setTransactionPhasingParams(builder, 20, VotingModel.ASSET, asset2Id, 100L, null, null, null);
        ACTestUtils.assertTransactionBlocked(builder);
        
        setTransactionPhasingParams(builder, 20, VotingModel.ASSET, assetId, 100L, null, null, null);
        ACTestUtils.assertTransactionSuccess(builder);
    }
    
    @Test
    public void testCurrencyVoting() {
        Builder builder = new ACTestUtils.CurrencyBuilder().naming("testa", "TESTA", "Test AC");
        String currencyId = (String) ACTestUtils.assertTransactionSuccess(builder).get("transaction");
        generateBlock();
        
        builder = new ACTestUtils.CurrencyBuilder().naming("testb", "TESTB", "Test AC");
        String currency2Id = (String) ACTestUtils.assertTransactionSuccess(builder).get("transaction");
        generateBlock();
        
        setPhasingOnlyControl(VotingModel.CURRENCY, currencyId, 100L, null, null, null);
        
        builder = new ACTestUtils.Builder("sendMoney", ALICE.getSecretPhrase())
            .recipient(BOB.getId())
            .param("amountNQT", 1 * Constants.ONE_NXT);
        ACTestUtils.assertTransactionBlocked(builder);
        
        setTransactionPhasingParams(builder, 20, VotingModel.CURRENCY, currency2Id, 100L, null, null, null);
        ACTestUtils.assertTransactionBlocked(builder);
        
        setTransactionPhasingParams(builder, 20, VotingModel.CURRENCY, currencyId, 100L, null, null, null);
        ACTestUtils.assertTransactionSuccess(builder);
    }
    
    
    private void assertNoPhasingOnlyControl() {
        Builder builder = new APICall.Builder("getPhasingOnlyControl")
            .param("account", Long.toUnsignedString(ALICE.getId()));
        
        JSONObject response = builder.build().invoke();
        Assert.assertTrue(response.isEmpty());
    }
    
    private void assertPhasingOnly(PhasingParams expected) {
        Builder builder = new APICall.Builder("getPhasingOnlyControl")
            .param("account", Long.toUnsignedString(ALICE.getId()));

        JSONObject response = builder.build().invoke();
        Logger.logMessage("getPhasingOnlyControl response: " + response.toJSONString());
        Assert.assertEquals(expected.getVoteWeighting().getVotingModel().getCode(), ((Long) response.get("votingModel")).byteValue());
        Assert.assertEquals(expected.getQuorum(), Convert.parseLong(response.get("quorum")));
        Assert.assertEquals(expected.getWhitelist().length, ((JSONArray) response.get("whitelist")).size());
        Assert.assertEquals(expected.getVoteWeighting().getHoldingId(), Convert.parseUnsignedLong((String)response.get("holding")));
        Assert.assertEquals(expected.getVoteWeighting().getMinBalance(), Convert.parseLong(response.get("minBalance")));
        Assert.assertEquals(expected.getVoteWeighting().getMinBalanceModel().getCode(), ((Long) response.get("minBalanceModel")).byteValue());
    }
    
    private void setPhasingOnlyControl(VotingModel votingModel, String holdingId, Long quorum,
            Long minBalance, MinBalanceModel minBalanceModel, long[] whitelist) {
        
        Builder builder = new ACTestUtils.Builder("setPhasingOnlyControl", ALICE.getSecretPhrase());
        
        setControlPhasingParams(builder, votingModel, holdingId, quorum,
                minBalance, minBalanceModel, whitelist);
        
        APICall apiCall = builder.build();
        JSONObject response = apiCall.invoke();
        Logger.logMessage("setPhasingOnlyControl response: " + response.toJSONString());
        
        String result = (String) response.get("transaction");
        Assert.assertNotNull(result);
        
        generateBlock();
    }

    private void setControlPhasingParams(Builder builder,
            VotingModel votingModel, String holdingId, Long quorum,
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
    
    private void setTransactionPhasingParams(Builder builder, int finishAfter, VotingModel votingModel, String holdingId, Long quorum,
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
