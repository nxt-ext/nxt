package nxt.http.twophased;

import nxt.BlockchainTest;
import nxt.Constants;
import nxt.Nxt;
import nxt.VoteWeighting;
import nxt.http.APICall;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;


public class TestCreateTwoPhased extends BlockchainTest {

    static String issueCreateTwoPhased(APICall apiCall, boolean shouldFail) {
        JSONObject twoPhased = apiCall.invoke();
        Logger.logMessage("two-phased sendMoney: " + twoPhased.toJSONString());

        generateBlock();

        try {
            String transactionId = (String) twoPhased.get("transaction");

            if ((!shouldFail && transactionId == null)
                    || (shouldFail && transactionId != null)) Assert.fail();

            return transactionId;
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
            param("phasingFinishHeight", height + 15);
        }

        public TwoPhasedMoneyTransferBuilder votingModel(byte model) {
            param("phasingVotingModel", model);
            return this;
        }

        public TwoPhasedMoneyTransferBuilder maxHeight(int maxHeight) {
            param("phasingFinishHeight", maxHeight);
            return this;
        }

        public TwoPhasedMoneyTransferBuilder minBalance(long minBalance) {
            param("phasingMinBalance", minBalance);
            return this;
        }

        public TwoPhasedMoneyTransferBuilder quorum(int quorum) {
            param("phasingQuorum", quorum);
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
    }


    @Test
    public void createValidtwoPhasedMoneyTransfer() {
        APICall apiCall = new TwoPhasedMoneyTransferBuilder().build();
        issueCreateTwoPhased(apiCall, false);
    }

    @Test
    public void createInvalidtwoPhasedMoneyTransfer() {
        int height = Nxt.getBlockchain().getHeight();

        APICall apiCall = new TwoPhasedMoneyTransferBuilder().maxHeight(height + 5).build();
        issueCreateTwoPhased(apiCall, true);

        apiCall = new TwoPhasedMoneyTransferBuilder().maxHeight(height + 100000).build();
        issueCreateTwoPhased(apiCall, true);

        apiCall = new TwoPhasedMoneyTransferBuilder().quorum(0).build();
        issueCreateTwoPhased(apiCall, true);

        apiCall = new TwoPhasedMoneyTransferBuilder().blacklisted(id3).build();
        issueCreateTwoPhased(apiCall, true);

        apiCall = new TwoPhasedMoneyTransferBuilder().votingModel(VoteWeighting.VotingModel.ASSET.getCode()).build();
        issueCreateTwoPhased(apiCall, true);

        apiCall = new TwoPhasedMoneyTransferBuilder().votingModel(VoteWeighting.VotingModel.ASSET.getCode())
                .minBalance(50).build();
        issueCreateTwoPhased(apiCall, true);
    }
}