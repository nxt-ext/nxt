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

package nxt.http.shuffling;

import nxt.BlockchainTest;
import nxt.Constants;
import nxt.Shuffling;
import nxt.Tester;
import nxt.http.APICall;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class TestShuffling extends BlockchainTest {

    private static Tester ALICE_RECIPIENT = new Tester("oiketrdgfxyjqhwds");
    private static Tester BOB_RECIPIENT = new Tester("5ehtrd9oijnkter");
    private static Tester CHUCK_RECIPIENT = new Tester("sdfxbejytdgfqrwefsrd");
    private static Tester DAVE_RECIPIENT = new Tester("gh-=e49rsiufzn4^");

    private static long defaultShufflingAmount = 1500000000;

    @Test
    public void successfulShuffling() {
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
        Assert.assertEquals(4, participants.size());
        String shufflingAssignee = (String) getShufflingResponse.get("assignee");
        Assert.assertEquals(Long.toUnsignedString(ALICE.getId()), shufflingAssignee);

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
        Assert.assertEquals((long) Shuffling.Stage.DONE.getCode(), getShufflingResponse.get("stage"));

        Assert.assertEquals(-(defaultShufflingAmount + 3 * Constants.ONE_NXT), ALICE.getBalanceDiff());
        Assert.assertEquals(-(defaultShufflingAmount + 3 * Constants.ONE_NXT), ALICE.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-(defaultShufflingAmount + 3 * Constants.ONE_NXT), BOB.getBalanceDiff());
        Assert.assertEquals(-(defaultShufflingAmount + 3 * Constants.ONE_NXT), BOB.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-(defaultShufflingAmount + 3 * Constants.ONE_NXT), CHUCK.getBalanceDiff());
        Assert.assertEquals(-(defaultShufflingAmount + 3 * Constants.ONE_NXT), CHUCK.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-(defaultShufflingAmount + 2 * Constants.ONE_NXT), DAVE.getBalanceDiff());
        Assert.assertEquals(-(defaultShufflingAmount + 2 * Constants.ONE_NXT), DAVE.getUnconfirmedBalanceDiff());

        Assert.assertEquals(defaultShufflingAmount, ALICE_RECIPIENT.getBalanceDiff());
        Assert.assertEquals(defaultShufflingAmount, ALICE_RECIPIENT.getUnconfirmedBalanceDiff());
        Assert.assertEquals(defaultShufflingAmount, BOB_RECIPIENT.getBalanceDiff());
        Assert.assertEquals(defaultShufflingAmount, BOB_RECIPIENT.getUnconfirmedBalanceDiff());
        Assert.assertEquals(defaultShufflingAmount, CHUCK_RECIPIENT.getBalanceDiff());
        Assert.assertEquals(defaultShufflingAmount, CHUCK_RECIPIENT.getUnconfirmedBalanceDiff());
        Assert.assertEquals(defaultShufflingAmount, DAVE_RECIPIENT.getBalanceDiff());
        Assert.assertEquals(defaultShufflingAmount, DAVE_RECIPIENT.getUnconfirmedBalanceDiff());
    }

    @Test
    public void registrationNotFinished() {
        String shufflingId = (String)create(ALICE).get("transaction");
        generateBlock();
        register(shufflingId, BOB);
        for (int i = 0; i < 9; i++) {
            generateBlock();
        }

        JSONObject getShufflingResponse = getShuffling(shufflingId);
        Assert.assertEquals((long) Shuffling.Stage.CANCELLED.getCode(), getShufflingResponse.get("stage"));

        JSONObject getParticipantsResponse = getShufflingParticipants(shufflingId);
        JSONArray participants = (JSONArray)getParticipantsResponse.get("participants");
        Assert.assertEquals(2, participants.size());
        String shufflingAssignee = (String) getShufflingResponse.get("assignee");
        Assert.assertNull(shufflingAssignee);

        Assert.assertEquals(-Constants.ONE_NXT, ALICE.getBalanceDiff());
        Assert.assertEquals(-Constants.ONE_NXT, ALICE.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-Constants.ONE_NXT, BOB.getBalanceDiff());
        Assert.assertEquals(-Constants.ONE_NXT, BOB.getUnconfirmedBalanceDiff());

        Assert.assertNull(ALICE_RECIPIENT.getAccount());
        Assert.assertNull(BOB_RECIPIENT.getAccount());
    }

    @Test
    public void processingNotStarted() {
        String shufflingId = (String)create(ALICE).get("transaction");
        generateBlock();
        register(shufflingId, BOB);
        generateBlock();
        register(shufflingId, CHUCK);
        generateBlock();
        register(shufflingId, DAVE);
        for (int i = 0; i < 10; i++) {
            generateBlock();
        }

        JSONObject getShufflingResponse = getShuffling(shufflingId);
        Assert.assertEquals((long) Shuffling.Stage.CANCELLED.getCode(), getShufflingResponse.get("stage"));

        JSONObject getParticipantsResponse = getShufflingParticipants(shufflingId);
        JSONArray participants = (JSONArray)getParticipantsResponse.get("participants");
        Assert.assertEquals(4, participants.size());
        String shufflingAssignee = (String) getShufflingResponse.get("assignee");
        Assert.assertNull(shufflingAssignee);

        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + Constants.ONE_NXT), ALICE.getBalanceDiff());
        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + Constants.ONE_NXT), ALICE.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-Constants.ONE_NXT, BOB.getBalanceDiff());
        Assert.assertEquals(-Constants.ONE_NXT, BOB.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-Constants.ONE_NXT, CHUCK.getBalanceDiff());
        Assert.assertEquals(-Constants.ONE_NXT, CHUCK.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-Constants.ONE_NXT, DAVE.getBalanceDiff());
        Assert.assertEquals(-Constants.ONE_NXT, DAVE.getUnconfirmedBalanceDiff());

        Assert.assertNull(ALICE_RECIPIENT.getAccount());
        Assert.assertNull(BOB_RECIPIENT.getAccount());
        Assert.assertNull(CHUCK_RECIPIENT.getAccount());
        Assert.assertNull(DAVE_RECIPIENT.getAccount());
    }

    @Test
    public void processingNotFinished() {
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
        Assert.assertEquals(4, participants.size());
        String shufflingAssignee = (String) getShufflingResponse.get("assignee");
        Assert.assertEquals(Long.toUnsignedString(ALICE.getId()), shufflingAssignee);

        process(shufflingId, ALICE, ALICE_RECIPIENT);
        generateBlock();
        process(shufflingId, BOB, BOB_RECIPIENT);
        generateBlock();
        process(shufflingId, CHUCK, CHUCK_RECIPIENT);

        for (int i = 0; i < 10; i++) {
            generateBlock();
        }

        getShufflingResponse = getShuffling(shufflingId);
        Assert.assertEquals((long) Shuffling.Stage.CANCELLED.getCode(), getShufflingResponse.get("stage"));

        Assert.assertEquals(-2 * Constants.ONE_NXT, ALICE.getBalanceDiff());
        Assert.assertEquals(-2 * Constants.ONE_NXT, ALICE.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-2 * Constants.ONE_NXT, BOB.getBalanceDiff());
        Assert.assertEquals(-2 * Constants.ONE_NXT, BOB.getUnconfirmedBalanceDiff());
        Assert.assertEquals(- 2 * Constants.ONE_NXT, CHUCK.getBalanceDiff());
        Assert.assertEquals(- 2 * Constants.ONE_NXT, CHUCK.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + Constants.ONE_NXT), DAVE.getBalanceDiff());
        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + Constants.ONE_NXT), DAVE.getUnconfirmedBalanceDiff());

        Assert.assertNull(ALICE_RECIPIENT.getAccount());
        Assert.assertNull(BOB_RECIPIENT.getAccount());
        Assert.assertNull(CHUCK_RECIPIENT.getAccount());
        Assert.assertNull(DAVE_RECIPIENT.getAccount());
    }

    @Test
    public void verifyNotStarted() {
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
        Assert.assertEquals(4, participants.size());
        String shufflingAssignee = (String) getShufflingResponse.get("assignee");
        Assert.assertEquals(Long.toUnsignedString(ALICE.getId()), shufflingAssignee);

        process(shufflingId, ALICE, ALICE_RECIPIENT);
        generateBlock();
        process(shufflingId, BOB, BOB_RECIPIENT);
        generateBlock();
        process(shufflingId, CHUCK, CHUCK_RECIPIENT);
        generateBlock();
        process(shufflingId, DAVE, DAVE_RECIPIENT);

        for (int i = 0; i < 14; i++) {
            generateBlock();
        }

        getShufflingResponse = getShuffling(shufflingId);
        Assert.assertEquals((long) Shuffling.Stage.CANCELLED.getCode(), getShufflingResponse.get("stage"));

        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + 2 * Constants.ONE_NXT), ALICE.getBalanceDiff());
        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + 2 * Constants.ONE_NXT), ALICE.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-2 * Constants.ONE_NXT, BOB.getBalanceDiff());
        Assert.assertEquals(-2 * Constants.ONE_NXT, BOB.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-2 * Constants.ONE_NXT, CHUCK.getBalanceDiff());
        Assert.assertEquals(-2 * Constants.ONE_NXT, CHUCK.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-2 * Constants.ONE_NXT, DAVE.getBalanceDiff());
        Assert.assertEquals(-2 * Constants.ONE_NXT, DAVE.getUnconfirmedBalanceDiff());

        Assert.assertNotNull(ALICE_RECIPIENT.getAccount());
        Assert.assertNotNull(BOB_RECIPIENT.getAccount());
        Assert.assertNotNull(CHUCK_RECIPIENT.getAccount());
        Assert.assertNotNull(DAVE_RECIPIENT.getAccount());

        Assert.assertEquals(0, ALICE_RECIPIENT.getBalanceDiff());
        Assert.assertEquals(0, ALICE_RECIPIENT.getUnconfirmedBalanceDiff());
        Assert.assertEquals(0, BOB_RECIPIENT.getBalanceDiff());
        Assert.assertEquals(0, BOB_RECIPIENT.getUnconfirmedBalanceDiff());
        Assert.assertEquals(0, CHUCK_RECIPIENT.getBalanceDiff());
        Assert.assertEquals(0, CHUCK_RECIPIENT.getUnconfirmedBalanceDiff());
        Assert.assertEquals(0, DAVE_RECIPIENT.getBalanceDiff());
        Assert.assertEquals(0, DAVE_RECIPIENT.getUnconfirmedBalanceDiff());
    }

    @Test
    public void verifyNotFinished() {
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
        Assert.assertEquals(4, participants.size());
        String shufflingAssignee = (String) getShufflingResponse.get("assignee");
        Assert.assertEquals(Long.toUnsignedString(ALICE.getId()), shufflingAssignee);

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
        for (int i = 0; i < 14; i++) {
            generateBlock();
        }
        getShufflingResponse = getShuffling(shufflingId);
        Assert.assertEquals((long) Shuffling.Stage.CANCELLED.getCode(), getShufflingResponse.get("stage"));

        Assert.assertEquals(-3 * Constants.ONE_NXT, ALICE.getBalanceDiff());
        Assert.assertEquals(-3 * Constants.ONE_NXT, ALICE.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-3 * Constants.ONE_NXT, BOB.getBalanceDiff());
        Assert.assertEquals(-3 * Constants.ONE_NXT, BOB.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + 2 * Constants.ONE_NXT), CHUCK.getBalanceDiff());
        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + 2 * Constants.ONE_NXT), CHUCK.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-2 * Constants.ONE_NXT, DAVE.getBalanceDiff());
        Assert.assertEquals(-2 * Constants.ONE_NXT, DAVE.getUnconfirmedBalanceDiff());

        Assert.assertNotNull(ALICE_RECIPIENT.getAccount());
        Assert.assertNotNull(BOB_RECIPIENT.getAccount());
        Assert.assertNotNull(CHUCK_RECIPIENT.getAccount());
        Assert.assertNotNull(DAVE_RECIPIENT.getAccount());

        Assert.assertEquals(0, ALICE_RECIPIENT.getBalanceDiff());
        Assert.assertEquals(0, ALICE_RECIPIENT.getUnconfirmedBalanceDiff());
        Assert.assertEquals(0, BOB_RECIPIENT.getBalanceDiff());
        Assert.assertEquals(0, BOB_RECIPIENT.getUnconfirmedBalanceDiff());
        Assert.assertEquals(0, CHUCK_RECIPIENT.getBalanceDiff());
        Assert.assertEquals(0, CHUCK_RECIPIENT.getUnconfirmedBalanceDiff());
        Assert.assertEquals(0, DAVE_RECIPIENT.getBalanceDiff());
        Assert.assertEquals(0, DAVE_RECIPIENT.getUnconfirmedBalanceDiff());
    }

    @Test
    public void cancelAfterVerify() {
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
        Assert.assertEquals(4, participants.size());
        String shufflingAssignee = (String) getShufflingResponse.get("assignee");
        Assert.assertEquals(Long.toUnsignedString(ALICE.getId()), shufflingAssignee);

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
        cancel(shufflingId, CHUCK, shufflingStateHash, 0);
        generateBlock();
        getShufflingResponse = getShuffling(shufflingId);
        String cancellingAccountId = (String)getShufflingResponse.get("cancellingAccount");
        Assert.assertEquals(Long.toUnsignedString(CHUCK.getId()), cancellingAccountId);
        cancel(shufflingId, ALICE, shufflingStateHash, CHUCK.getId());
        cancel(shufflingId, BOB, shufflingStateHash, CHUCK.getId());
        for (int i = 0; i < 14; i++) {
            generateBlock();
        }
        getShufflingResponse = getShuffling(shufflingId);
        Assert.assertEquals((long) Shuffling.Stage.CANCELLED.getCode(), getShufflingResponse.get("stage"));

        Assert.assertEquals(-4 * Constants.ONE_NXT, ALICE.getBalanceDiff());
        Assert.assertEquals(-4 * Constants.ONE_NXT, ALICE.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-4 * Constants.ONE_NXT, BOB.getBalanceDiff());
        Assert.assertEquals(-4 * Constants.ONE_NXT, BOB.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + 3 * Constants.ONE_NXT), CHUCK.getBalanceDiff());
        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + 3 * Constants.ONE_NXT), CHUCK.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-2 * Constants.ONE_NXT, DAVE.getBalanceDiff());
        Assert.assertEquals(-2 * Constants.ONE_NXT, DAVE.getUnconfirmedBalanceDiff());

        Assert.assertNotNull(ALICE_RECIPIENT.getAccount());
        Assert.assertNotNull(BOB_RECIPIENT.getAccount());
        Assert.assertNotNull(CHUCK_RECIPIENT.getAccount());
        Assert.assertNotNull(DAVE_RECIPIENT.getAccount());

        Assert.assertEquals(0, ALICE_RECIPIENT.getBalanceDiff());
        Assert.assertEquals(0, ALICE_RECIPIENT.getUnconfirmedBalanceDiff());
        Assert.assertEquals(0, BOB_RECIPIENT.getBalanceDiff());
        Assert.assertEquals(0, BOB_RECIPIENT.getUnconfirmedBalanceDiff());
        Assert.assertEquals(0, CHUCK_RECIPIENT.getBalanceDiff());
        Assert.assertEquals(0, CHUCK_RECIPIENT.getUnconfirmedBalanceDiff());
        Assert.assertEquals(0, DAVE_RECIPIENT.getBalanceDiff());
        Assert.assertEquals(0, DAVE_RECIPIENT.getUnconfirmedBalanceDiff());
    }

    private JSONObject create(Tester creator) {
        APICall apiCall = new APICall.Builder("shufflingCreate").
                secretPhrase(creator.getSecretPhrase()).
                feeNQT(Constants.ONE_NXT).
                param("amount", String.valueOf(defaultShufflingAmount)).
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

    private JSONObject cancel(String shufflingId, Tester tester, String shufflingStateHash, long cancellingAccountId) {
        APICall.Builder builder = new APICall.Builder("shufflingCancel").
                param("shuffling", shufflingId).
                param("secretPhrase", tester.getSecretPhrase()).
                param("shufflingStateHash", shufflingStateHash).
                feeNQT(Constants.ONE_NXT);
        if (cancellingAccountId != 0) {
            builder.param("cancellingAccount", cancellingAccountId);
        }
        APICall apiCall = builder.build();
        JSONObject response = apiCall.invoke();
        Logger.logDebugMessage("shufflingCancelResponse:" + response);
        return response;
    }

}
