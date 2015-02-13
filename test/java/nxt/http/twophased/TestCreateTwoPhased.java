package nxt.http.twophased;

import nxt.BlockchainTest;
import nxt.Constants;
import nxt.Nxt;
import nxt.VoteWeighting;
import nxt.http.APICall;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;


public class TestCreateTwoPhased extends BlockchainTest {

    static JSONObject issueCreateTwoPhased(APICall apiCall, boolean shouldFail) {
        JSONObject twoPhased = apiCall.invoke();
        Logger.logMessage("two-phased sendMoney: " + twoPhased.toJSONString());

        generateBlock();

        try {
            String transactionId = (String) twoPhased.get("transaction");

            if ((!shouldFail && transactionId == null)
                    || (shouldFail && transactionId != null)) Assert.fail();
            return twoPhased;
        } catch (Throwable t) {
            if (!shouldFail) Assert.fail(t.getMessage());
            return null;
        }
    }

    public static class TwoPhasedMoneyTransferBuilder extends APICall.Builder {

        public TwoPhasedMoneyTransferBuilder() {
            super("sendMoney");

            int height = Nxt.getBlockchain().getHeight();

            secretPhrase(secretPhrase1);
            feeNQT(Constants.ONE_NXT);
            recipient(id2);
            param("amountNQT", 50 * Constants.ONE_NXT);
            param("phased", "true");
            param("phasingVotingModel", VoteWeighting.VotingModel.ACCOUNT.getCode());
            param("phasingQuorum", 1);
            param("phasingWhitelisted", Convert.toUnsignedLong(id3));
            param("phasingFinishHeight", height + 50);
        }

        public TwoPhasedMoneyTransferBuilder votingModel(byte model) {
            param("phasingVotingModel", model);
            return this;
        }

        public TwoPhasedMoneyTransferBuilder maxHeight(int maxHeight) {
            param("phasingFinishHeight", maxHeight);
            return this;
        }

        public TwoPhasedMoneyTransferBuilder minBalance(long minBalance, byte minBalanceModel) {
            param("phasingMinBalance", minBalance);
            param("phasingMinBalanceModel", minBalanceModel);
            return this;
        }

        public TwoPhasedMoneyTransferBuilder quorum(int quorum) {
            param("phasingQuorum", quorum);
            return this;
        }

        public TwoPhasedMoneyTransferBuilder noWhitelist() {
            param("phasingWhitelisted", "");
            return this;
        }

        public TwoPhasedMoneyTransferBuilder whitelisted(long accountId) {
            param("phasingWhitelisted", Convert.toUnsignedLong(accountId));
            return this;
        }

        public TwoPhasedMoneyTransferBuilder blacklisted(long accountId) {
            param("phasingBlacklisted", Convert.toUnsignedLong(accountId));
            return this;
        }

        public TwoPhasedMoneyTransferBuilder holding(long accountId) {
            param("phasingHolding", Convert.toUnsignedLong(accountId));
            return this;
        }
    }


    @Test
    public void validMoneyTransfer() {
        APICall apiCall = new TwoPhasedMoneyTransferBuilder().build();
        issueCreateTwoPhased(apiCall, false);
    }

    @Test
    public void invalidMoneyTransfer() {
        int height = Nxt.getBlockchain().getHeight();

        APICall apiCall = new TwoPhasedMoneyTransferBuilder().maxHeight(height + 5).build();
        issueCreateTwoPhased(apiCall, true);

        apiCall = new TwoPhasedMoneyTransferBuilder().maxHeight(height + 100000).build();
        issueCreateTwoPhased(apiCall, true);

        apiCall = new TwoPhasedMoneyTransferBuilder().quorum(0).build();
        issueCreateTwoPhased(apiCall, true);

        apiCall = new TwoPhasedMoneyTransferBuilder().noWhitelist().build();
        issueCreateTwoPhased(apiCall, true);

        apiCall = new TwoPhasedMoneyTransferBuilder().blacklisted(id3).build();
        issueCreateTwoPhased(apiCall, true);

        apiCall = new TwoPhasedMoneyTransferBuilder().votingModel(VoteWeighting.VotingModel.ASSET.getCode()).build();
        issueCreateTwoPhased(apiCall, true);

        apiCall = new TwoPhasedMoneyTransferBuilder().votingModel(VoteWeighting.VotingModel.ASSET.getCode())
                .minBalance(50, VoteWeighting.MinBalanceModel.ASSET.getCode())
                .build();
        issueCreateTwoPhased(apiCall, true);
    }

    @Test
    public void unconfirmed() {
        List<String> transactionIds = new ArrayList<>(10);

        for(int i=0; i < 10; i++){
            APICall apiCall = new TwoPhasedMoneyTransferBuilder().build();
            JSONObject transactionJSON = issueCreateTwoPhased(apiCall, false);
            String idString = (String) transactionJSON.get("transaction");
            transactionIds.add(idString);
        }

        APICall apiCall = new TwoPhasedMoneyTransferBuilder().build();
        JSONObject unconfirmed = apiCall.invoke();

        JSONObject response = TestGetAccountPendingTransactions.pendingTransactionsApiCall().invoke();
        Logger.logMessage("getAccountPendingTransactionsResponse:" + response.toJSONString());
        JSONArray transactionsJson = (JSONArray) response.get("transactions");

        for(String idString:transactionIds){
            Assert.assertTrue(TwoPhasedSuite.searchForTransactionId(transactionsJson, idString));
        }
    }
}