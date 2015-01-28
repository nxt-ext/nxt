package nxt.http.twophased;

import nxt.BlockchainTest;
import nxt.Constants;
import nxt.Nxt;
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
            param("amountNQT", 10 * Constants.ONE_NXT);
            param("isPending", "true");
            param("pendingVotingModel", Constants.VOTING_MODEL_ACCOUNT);
            param("pendingQuorum", 1);
            param("pendingWhitelisted", Convert.toUnsignedLong(id3));
            param("pendingMaxHeight", height + 15);
        }

        public TwoPhasedMoneyTransferBuilder maxHeight(int maxHeight) {
            param("pendingMaxHeight", maxHeight);
            return this;
        }

        public TwoPhasedMoneyTransferBuilder quorum(int quorum) {
            param("pendingQuorum", quorum);
            return this;
        }

        public TwoPhasedMoneyTransferBuilder whitelisted(long accountId) {
            param("pendingWhitelisted", Convert.toUnsignedLong(accountId));
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

    }
}