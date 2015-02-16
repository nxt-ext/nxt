package nxt.http.votingsystem;

import nxt.BlockchainTest;
import nxt.Constants;
import nxt.http.APICall;
import nxt.http.votingsystem.TestCreatePoll.CreatePollBuilder;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class TestCastVote extends BlockchainTest {
    @Test
    public void validVoteCasting() {
        APICall apiCall = new CreatePollBuilder().build();
        String poll = TestCreatePoll.issueCreatePoll(apiCall, false);
        generateBlock();

        apiCall = new APICall.Builder("castVote")
                .param("secretPhrase", secretPhrase1)
                .param("poll", poll)
                .param("vote1", 1)
                .param("vote2", 0)
                .param("feeNQT", Constants.ONE_NXT)
                .build();

        JSONObject response = apiCall.invoke();
        Logger.logMessage("voteCasting:" + response.toJSONString());
        Assert.assertNull(response.get("error"));
        generateBlock();

        apiCall = new APICall.Builder("getPollResult").param("poll", poll).build();
        JSONObject getPollResponse = apiCall.invoke();
        Logger.logMessage("getPollResultResponse:" + getPollResponse.toJSONString());
        JSONArray results = (JSONArray)getPollResponse.get("results");
        JSONObject ringoVote = (JSONObject)results.get(0);
        long ringoResult = (Long) ringoVote.get("Ringo");
        Assert.assertEquals(1, ringoResult);
    }

    @Test
    public void invalidVoteCasting() {
        APICall apiCall = new CreatePollBuilder().build();
        String poll = TestCreatePoll.issueCreatePoll(apiCall, false);
        generateBlock();

        apiCall = new APICall.Builder("castVote")
                .param("secretPhrase", secretPhrase1)
                .param("poll", poll)
                .param("vote1", 1)
                .param("vote2", 1)
                .param("vote3", 1)
                .param("feeNQT", Constants.ONE_NXT)
                .build();

        JSONObject response = apiCall.invoke();
        Logger.logMessage("voteCasting:" + response.toJSONString());
        Assert.assertNotNull(response.get("error"));
    }


}