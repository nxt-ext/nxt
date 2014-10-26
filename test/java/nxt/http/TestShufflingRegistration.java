package nxt.http;

import nxt.BlockchainTest;
import nxt.Constants;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.junit.Test;

public class TestShufflingRegistration extends BlockchainTest {

    @Test
    public void addParticipants() {
        APICall apiCall = new APICall.Builder("shufflingCreate").
                secretPhrase(secretPhrase1).
                feeNQT(Constants.ONE_NXT).
                param("isCurrency", "0").
                param("amountNQT", "10000000").
                param("participantCount", "4").
                param("cancellationHeight", Integer.MAX_VALUE).
                build();
        JSONObject response = apiCall.invoke();
        Logger.logMessage("shufflingCreateResponse: " + response.toJSONString());
        generateBlock();
        apiCall = new APICall.Builder("shufflingRegister").
                secretPhrase(secretPhrase1).
                feeNQT(Constants.ONE_NXT).
                param("shuffling", "0").
                build();
        response = apiCall.invoke();
        Logger.logMessage("shufflingRegisterResponse: " + response.toJSONString());
        generateBlock();
    }
}
