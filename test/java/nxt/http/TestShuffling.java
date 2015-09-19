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

package nxt.http;

import nxt.BlockchainTest;
import nxt.Constants;
import nxt.Shuffling;
import nxt.Tester;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

//TODO: fix test
public class TestShuffling extends BlockchainTest {

    private static Tester ALICE_RECIPIENT = new Tester("oiketrdgfxyjqhwds");
    private static Tester BOB_RECIPIENT = new Tester("5ehtrd9oijnkter");
    private static Tester CHUCK_RECIPIENT = new Tester("sdfxbejytdgfqrwefsrd");
    private static Tester DAVE_RECIPIENT = new Tester("gh-=e49rsiufzn4^");

    @Test
    public void shufflingProcess() {
        String shufflingId = (String)create(ALICE).get("transaction");
        generateBlock();
        register(shufflingId, BOB);
        generateBlock();
        register(shufflingId, CHUCK);
        generateBlock();
        register(shufflingId, DAVE);
        generateBlock();

        JSONObject getShufflingResponse = getShuffling(shufflingId);
        Assert.assertEquals((long) Shuffling.Stage.PROCESSING.getCode(), getShufflingResponse.get("stage"));

        JSONObject getParticipantsResponse = getShufflingParticipants(shufflingId);
        JSONArray participants = (JSONArray)getParticipantsResponse.get("participants");
        Assert.assertEquals(participants.size(), 4);
        String shufflingAssignee = (String) getShufflingResponse.get("assignee");
        Assert.assertEquals(shufflingAssignee, Long.toUnsignedString(ALICE.getId()));

        process(shufflingId, ALICE, ALICE_RECIPIENT);
        generateBlock();
        process(shufflingId, BOB, BOB_RECIPIENT);
        generateBlock();
        process(shufflingId, CHUCK, CHUCK_RECIPIENT);
        generateBlock();
        process(shufflingId, DAVE, DAVE_RECIPIENT);
        generateBlock();

        getShufflingResponse = getShuffling(shufflingId);
        Assert.assertEquals((long) Shuffling.Stage.VERIFICATION.getCode(), getShufflingResponse.get("stage"));
        String shufflingStateHash = (String)getShufflingResponse.get("shufflingStateHash");

        verify(shufflingId, ALICE, shufflingStateHash);
        verify(shufflingId, BOB, shufflingStateHash);
        verify(shufflingId, CHUCK, shufflingStateHash);
        generateBlock();
        getShufflingResponse = getShuffling(shufflingId);

    }

    private JSONObject create(Tester creator) {
        APICall apiCall = new APICall.Builder("shufflingCreate").
                secretPhrase(creator.getSecretPhrase()).
                feeNQT(Constants.ONE_NXT).
                param("amount", "1500000000").
                param("participantCount", "4").
                param("registrationPeriod", 10).
                build();
        JSONObject response = apiCall.invoke();
        Logger.logMessage("shufflingCreateResponse: " + response.toJSONString());
        return response;
    }

    private JSONObject register(String shufflingId, Tester tester) {
        APICall apiCall = new APICall.Builder("shufflingRegister").
                secretPhrase(tester.getSecretPhrase()).
                feeNQT(Constants.ONE_NXT).
                param("shuffling", shufflingId).
                build();
        JSONObject response = apiCall.invoke();
        Logger.logMessage("shufflingRegisterResponse: " + response.toJSONString());
        return response;
    }

    private JSONObject getShuffling(String shufflingId) {
        APICall apiCall = new APICall.Builder("getShuffling").
                param("shuffling", shufflingId).
                build();
        JSONObject getShufflingResponse = apiCall.invoke();
        Logger.logMessage("getShufflingResponse: " + getShufflingResponse.toJSONString());
        return getShufflingResponse;
    }

    private JSONObject getShufflingParticipants(String shufflingId) {
        APICall apiCall = new APICall.Builder("getShufflingParticipants").
                param("shuffling", shufflingId).
                build();
        JSONObject getParticipantsResponse = apiCall.invoke();
        Logger.logMessage("getShufflingParticipantsResponse: " + getParticipantsResponse.toJSONString());
        return getParticipantsResponse;
    }

    private JSONObject process(String shufflingId, Tester tester, Tester recipient) {
        APICall apiCall = new APICall.Builder("shufflingProcess").
                param("shuffling", shufflingId).
                param("secretPhrase", tester.getSecretPhrase()).
                param("recipientSecretPhrase", recipient.getSecretPhrase()).
                feeNQT(Constants.ONE_NXT).
                build();
        JSONObject shufflingProcessResponse = apiCall.invoke();
        Logger.logMessage("shufflingProcessResponse: " + shufflingProcessResponse.toJSONString());
        return shufflingProcessResponse;
    }

    private JSONObject verify(String shufflingId, Tester tester, String shufflingStateHash) {
        APICall apiCall = new APICall.Builder("shufflingVerify").
                param("shuffling", shufflingId).
                param("secretPhrase", tester.getSecretPhrase()).
                param("shufflingStateHash", shufflingStateHash).
                feeNQT(Constants.ONE_NXT).
                build();
        JSONObject response = apiCall.invoke();
        Logger.logDebugMessage("shufflingVerifyResponse:" + response);
        return response;
    }

    private JSONObject cancel(String shufflingId, Tester tester, String shufflingStateHash) {
        APICall apiCall = new APICall.Builder("shufflingCancel").
                param("shuffling", shufflingId).
                param("secretPhrase", tester.getSecretPhrase()).
                param("shufflingStateHash", shufflingStateHash).
                feeNQT(Constants.ONE_NXT).
                build();
        JSONObject response = apiCall.invoke();
        Logger.logDebugMessage("shufflingCancelResponse:" + response);
        return response;
    }

}
