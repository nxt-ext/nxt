package nxt.http.votingsystem;

import nxt.BlockchainTest;
import nxt.Constants;
import nxt.Nxt;
import nxt.http.APICall;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.junit.Test;

public class TestVoting extends BlockchainTest {

    @Test
    public void createPoll() {
        APICall apiCall = new CreatePollBuilder().build();
        JSONObject createPollResponse = apiCall.invoke();
        Logger.logMessage("createPollResponse: " + createPollResponse.toJSONString());
    }

    public static class CreatePollBuilder extends APICall.Builder {

        public CreatePollBuilder() {
            super("createPoll");
            secretPhrase(secretPhrase1);
            feeNQT(10 * Constants.ONE_NXT);
            param("name", "Test1");
            param("description", "The most cool Beatles guy?");
            param("finishHeight", Nxt.getBlockchain().getHeight() + 100);
            param("votingModel", Constants.VOTING_MODEL_ACCOUNT);
            param("minNumberOfOptions", 1);
            param("maxNumberOfOptions", 2);
            param("minRangeValue", 0);
            param("maxRangeValue", 1);
            param("minBalance", 10 * Constants.ONE_NXT);
            param("minBalanceModel", Constants.VOTING_MINBALANCE_BYBALANCE);
            param("option1", "Ringo");
            param("option2", "Paul");
            param("option2", "John");
        }
    }
}
