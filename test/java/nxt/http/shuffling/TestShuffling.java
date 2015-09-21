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
import nxt.HoldingType;
import nxt.Shuffling;
import nxt.Tester;
import nxt.http.APICall;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class TestShuffling extends BlockchainTest {

    private static final Tester ALICE_RECIPIENT = new Tester("oiketrdgfxyjqhwds");
    private static final Tester BOB_RECIPIENT = new Tester("5ehtrd9oijnkter");
    private static final Tester CHUCK_RECIPIENT = new Tester("sdfxbejytdgfqrwefsrd");
    private static final Tester DAVE_RECIPIENT = new Tester("gh-=e49rsiufzn4^");

    private static final long defaultShufflingAmount = 1500000000;
    private static final long defaultHoldingShufflingAmount = 40000;
    private static final long shufflingAsset = 3320741880585366286L;
    private static final long shufflingCurrency = -5643814336689018857L;

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

        Assert.assertEquals(-Constants.ONE_NXT, ALICE.getBalanceDiff());
        Assert.assertEquals(-(defaultShufflingAmount + Constants.ONE_NXT), ALICE.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-Constants.ONE_NXT, BOB.getBalanceDiff());
        Assert.assertEquals(-(defaultShufflingAmount + Constants.ONE_NXT), BOB.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-Constants.ONE_NXT, CHUCK.getBalanceDiff());
        Assert.assertEquals(-(defaultShufflingAmount + Constants.ONE_NXT), CHUCK.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-Constants.ONE_NXT, DAVE.getBalanceDiff());
        Assert.assertEquals(-(defaultShufflingAmount + Constants.ONE_NXT), DAVE.getUnconfirmedBalanceDiff());

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

        Assert.assertEquals(11 * Constants.ONE_NXT, FORGY.getBalanceDiff());
        Assert.assertEquals(11 * Constants.ONE_NXT, FORGY.getUnconfirmedBalanceDiff());

    }

    @Test
    public void successfulAssetShuffling() {
        String shufflingId = (String)createAssetShuffling(ALICE).get("transaction");
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

        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + 3 * Constants.ONE_NXT), ALICE.getBalanceDiff());
        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + 3 * Constants.ONE_NXT), ALICE.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + 3 * Constants.ONE_NXT), BOB.getBalanceDiff());
        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + 3 * Constants.ONE_NXT), BOB.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + 3 * Constants.ONE_NXT), CHUCK.getBalanceDiff());
        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + 3 * Constants.ONE_NXT), CHUCK.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + 2 * Constants.ONE_NXT), DAVE.getBalanceDiff());
        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + 2 * Constants.ONE_NXT), DAVE.getUnconfirmedBalanceDiff());

        Assert.assertEquals(Constants.SHUFFLING_DEPOSIT_NQT, ALICE_RECIPIENT.getBalanceDiff());
        Assert.assertEquals(Constants.SHUFFLING_DEPOSIT_NQT, ALICE_RECIPIENT.getUnconfirmedBalanceDiff());
        Assert.assertEquals(Constants.SHUFFLING_DEPOSIT_NQT, BOB_RECIPIENT.getBalanceDiff());
        Assert.assertEquals(Constants.SHUFFLING_DEPOSIT_NQT, BOB_RECIPIENT.getUnconfirmedBalanceDiff());
        Assert.assertEquals(Constants.SHUFFLING_DEPOSIT_NQT, CHUCK_RECIPIENT.getBalanceDiff());
        Assert.assertEquals(Constants.SHUFFLING_DEPOSIT_NQT, CHUCK_RECIPIENT.getUnconfirmedBalanceDiff());
        Assert.assertEquals(Constants.SHUFFLING_DEPOSIT_NQT, DAVE_RECIPIENT.getBalanceDiff());
        Assert.assertEquals(Constants.SHUFFLING_DEPOSIT_NQT, DAVE_RECIPIENT.getUnconfirmedBalanceDiff());

        Assert.assertEquals(11 * Constants.ONE_NXT, FORGY.getBalanceDiff());
        Assert.assertEquals(11 * Constants.ONE_NXT, FORGY.getUnconfirmedBalanceDiff());

        Assert.assertEquals(defaultHoldingShufflingAmount, ALICE_RECIPIENT.getAssetQuantityDiff(shufflingAsset));
        Assert.assertEquals(defaultHoldingShufflingAmount, ALICE_RECIPIENT.getUnconfirmedAssetQuantityDiff(shufflingAsset));
        Assert.assertEquals(defaultHoldingShufflingAmount, BOB_RECIPIENT.getAssetQuantityDiff(shufflingAsset));
        Assert.assertEquals(defaultHoldingShufflingAmount, BOB_RECIPIENT.getUnconfirmedAssetQuantityDiff(shufflingAsset));
        Assert.assertEquals(defaultHoldingShufflingAmount, CHUCK_RECIPIENT.getAssetQuantityDiff(shufflingAsset));
        Assert.assertEquals(defaultHoldingShufflingAmount, CHUCK_RECIPIENT.getUnconfirmedAssetQuantityDiff(shufflingAsset));
        Assert.assertEquals(defaultHoldingShufflingAmount, DAVE_RECIPIENT.getAssetQuantityDiff(shufflingAsset));
        Assert.assertEquals(defaultHoldingShufflingAmount, DAVE_RECIPIENT.getUnconfirmedAssetQuantityDiff(shufflingAsset));

        Assert.assertEquals(-defaultHoldingShufflingAmount, ALICE.getAssetQuantityDiff(shufflingAsset));
        Assert.assertEquals(-defaultHoldingShufflingAmount, ALICE.getUnconfirmedAssetQuantityDiff(shufflingAsset));
        Assert.assertEquals(-defaultHoldingShufflingAmount, BOB.getAssetQuantityDiff(shufflingAsset));
        Assert.assertEquals(-defaultHoldingShufflingAmount, BOB.getUnconfirmedAssetQuantityDiff(shufflingAsset));
        Assert.assertEquals(-defaultHoldingShufflingAmount, CHUCK.getAssetQuantityDiff(shufflingAsset));
        Assert.assertEquals(-defaultHoldingShufflingAmount, CHUCK.getUnconfirmedAssetQuantityDiff(shufflingAsset));
        Assert.assertEquals(-defaultHoldingShufflingAmount, DAVE.getAssetQuantityDiff(shufflingAsset));
        Assert.assertEquals(-defaultHoldingShufflingAmount, DAVE.getUnconfirmedAssetQuantityDiff(shufflingAsset));

    }

    @Test
    public void successfulCurrencyShuffling() {
        String shufflingId = (String)createCurrencyShuffling(ALICE).get("transaction");
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

        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + 3 * Constants.ONE_NXT), ALICE.getBalanceDiff());
        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + 3 * Constants.ONE_NXT), ALICE.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + 3 * Constants.ONE_NXT), BOB.getBalanceDiff());
        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + 3 * Constants.ONE_NXT), BOB.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + 3 * Constants.ONE_NXT), CHUCK.getBalanceDiff());
        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + 3 * Constants.ONE_NXT), CHUCK.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + 2 * Constants.ONE_NXT), DAVE.getBalanceDiff());
        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + 2 * Constants.ONE_NXT), DAVE.getUnconfirmedBalanceDiff());

        Assert.assertEquals(Constants.SHUFFLING_DEPOSIT_NQT, ALICE_RECIPIENT.getBalanceDiff());
        Assert.assertEquals(Constants.SHUFFLING_DEPOSIT_NQT, ALICE_RECIPIENT.getUnconfirmedBalanceDiff());
        Assert.assertEquals(Constants.SHUFFLING_DEPOSIT_NQT, BOB_RECIPIENT.getBalanceDiff());
        Assert.assertEquals(Constants.SHUFFLING_DEPOSIT_NQT, BOB_RECIPIENT.getUnconfirmedBalanceDiff());
        Assert.assertEquals(Constants.SHUFFLING_DEPOSIT_NQT, CHUCK_RECIPIENT.getBalanceDiff());
        Assert.assertEquals(Constants.SHUFFLING_DEPOSIT_NQT, CHUCK_RECIPIENT.getUnconfirmedBalanceDiff());
        Assert.assertEquals(Constants.SHUFFLING_DEPOSIT_NQT, DAVE_RECIPIENT.getBalanceDiff());
        Assert.assertEquals(Constants.SHUFFLING_DEPOSIT_NQT, DAVE_RECIPIENT.getUnconfirmedBalanceDiff());

        Assert.assertEquals(11 * Constants.ONE_NXT, FORGY.getBalanceDiff());
        Assert.assertEquals(11 * Constants.ONE_NXT, FORGY.getUnconfirmedBalanceDiff());

        Assert.assertEquals(defaultHoldingShufflingAmount, ALICE_RECIPIENT.getCurrencyUnitsDiff(shufflingCurrency));
        Assert.assertEquals(defaultHoldingShufflingAmount, ALICE_RECIPIENT.getUnconfirmedCurrencyUnitsDiff(shufflingCurrency));
        Assert.assertEquals(defaultHoldingShufflingAmount, BOB_RECIPIENT.getCurrencyUnitsDiff(shufflingCurrency));
        Assert.assertEquals(defaultHoldingShufflingAmount, BOB_RECIPIENT.getUnconfirmedCurrencyUnitsDiff(shufflingCurrency));
        Assert.assertEquals(defaultHoldingShufflingAmount, CHUCK_RECIPIENT.getCurrencyUnitsDiff(shufflingCurrency));
        Assert.assertEquals(defaultHoldingShufflingAmount, CHUCK_RECIPIENT.getUnconfirmedCurrencyUnitsDiff(shufflingCurrency));
        Assert.assertEquals(defaultHoldingShufflingAmount, DAVE_RECIPIENT.getCurrencyUnitsDiff(shufflingCurrency));
        Assert.assertEquals(defaultHoldingShufflingAmount, DAVE_RECIPIENT.getUnconfirmedCurrencyUnitsDiff(shufflingCurrency));

        Assert.assertEquals(-defaultHoldingShufflingAmount, ALICE.getCurrencyUnitsDiff(shufflingCurrency));
        Assert.assertEquals(-defaultHoldingShufflingAmount, ALICE.getUnconfirmedCurrencyUnitsDiff(shufflingCurrency));
        Assert.assertEquals(-defaultHoldingShufflingAmount, BOB.getCurrencyUnitsDiff(shufflingCurrency));
        Assert.assertEquals(-defaultHoldingShufflingAmount, BOB.getUnconfirmedCurrencyUnitsDiff(shufflingCurrency));
        Assert.assertEquals(-defaultHoldingShufflingAmount, CHUCK.getCurrencyUnitsDiff(shufflingCurrency));
        Assert.assertEquals(-defaultHoldingShufflingAmount, CHUCK.getUnconfirmedCurrencyUnitsDiff(shufflingCurrency));
        Assert.assertEquals(-defaultHoldingShufflingAmount, DAVE.getCurrencyUnitsDiff(shufflingCurrency));
        Assert.assertEquals(-defaultHoldingShufflingAmount, DAVE.getUnconfirmedCurrencyUnitsDiff(shufflingCurrency));

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

        Assert.assertEquals(2 * Constants.ONE_NXT, FORGY.getBalanceDiff());
        Assert.assertEquals(2 * Constants.ONE_NXT, FORGY.getUnconfirmedBalanceDiff());

    }

    @Test
    public void registrationNotFinishedAsset() {
        String shufflingId = (String)createAssetShuffling(ALICE).get("transaction");
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

        Assert.assertEquals(0, ALICE.getAssetQuantityDiff(shufflingAsset));
        Assert.assertEquals(0, ALICE.getUnconfirmedAssetQuantityDiff(shufflingAsset));
        Assert.assertEquals(0, BOB.getAssetQuantityDiff(shufflingAsset));
        Assert.assertEquals(0, BOB.getUnconfirmedAssetQuantityDiff(shufflingAsset));

        Assert.assertEquals(2 * Constants.ONE_NXT, FORGY.getBalanceDiff());
        Assert.assertEquals(2 * Constants.ONE_NXT, FORGY.getUnconfirmedBalanceDiff());

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

        Assert.assertEquals(4 * Constants.ONE_NXT + Constants.SHUFFLING_DEPOSIT_NQT, FORGY.getBalanceDiff());
        Assert.assertEquals(4 * Constants.ONE_NXT + Constants.SHUFFLING_DEPOSIT_NQT, FORGY.getUnconfirmedBalanceDiff());

    }

    @Test
    public void processingNotStartedCurrency() {
        String shufflingId = (String)createCurrencyShuffling(ALICE).get("transaction");
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

        Assert.assertEquals(0, ALICE.getCurrencyUnitsDiff(shufflingCurrency));
        Assert.assertEquals(0, ALICE.getUnconfirmedCurrencyUnitsDiff(shufflingCurrency));
        Assert.assertEquals(0, BOB.getCurrencyUnitsDiff(shufflingCurrency));
        Assert.assertEquals(0, BOB.getUnconfirmedCurrencyUnitsDiff(shufflingCurrency));
        Assert.assertEquals(0, CHUCK.getCurrencyUnitsDiff(shufflingCurrency));
        Assert.assertEquals(0, CHUCK.getUnconfirmedCurrencyUnitsDiff(shufflingCurrency));
        Assert.assertEquals(0, DAVE.getCurrencyUnitsDiff(shufflingCurrency));
        Assert.assertEquals(0, DAVE.getUnconfirmedCurrencyUnitsDiff(shufflingCurrency));

        Assert.assertEquals(4 * Constants.ONE_NXT + Constants.SHUFFLING_DEPOSIT_NQT, FORGY.getBalanceDiff());
        Assert.assertEquals(4 * Constants.ONE_NXT + Constants.SHUFFLING_DEPOSIT_NQT, FORGY.getUnconfirmedBalanceDiff());

    }

    @Test
    public void tooManyParticipants() {
        String shufflingId = (String)create(ALICE, 3).get("transaction");
        generateBlock();
        register(shufflingId, BOB);
        register(shufflingId, CHUCK);
        register(shufflingId, DAVE);
        register(shufflingId, FORGY);
        for (int i = 0; i < 10; i++) {
            generateBlock();
        }

        JSONObject getShufflingResponse = getShuffling(shufflingId);
        Assert.assertEquals((long) Shuffling.Stage.CANCELLED.getCode(), getShufflingResponse.get("stage"));

        JSONObject getParticipantsResponse = getShufflingParticipants(shufflingId);
        JSONArray participants = (JSONArray)getParticipantsResponse.get("participants");
        Assert.assertEquals(3, participants.size());
        String shufflingAssignee = (String) getShufflingResponse.get("assignee");
        Assert.assertNull(shufflingAssignee);

        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + Constants.ONE_NXT), ALICE.getBalanceDiff());
        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + Constants.ONE_NXT), ALICE.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-Constants.ONE_NXT, BOB.getBalanceDiff());
        Assert.assertEquals(-Constants.ONE_NXT, BOB.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-Constants.ONE_NXT, CHUCK.getBalanceDiff());
        Assert.assertEquals(-Constants.ONE_NXT, CHUCK.getUnconfirmedBalanceDiff());
        Assert.assertEquals(0, DAVE.getBalanceDiff());
        Assert.assertEquals(0, DAVE.getUnconfirmedBalanceDiff());
        Assert.assertEquals(3 * Constants.ONE_NXT + Constants.SHUFFLING_DEPOSIT_NQT, FORGY.getBalanceDiff());
        Assert.assertEquals(3 * Constants.ONE_NXT + Constants.SHUFFLING_DEPOSIT_NQT, FORGY.getUnconfirmedBalanceDiff());

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

        Assert.assertEquals(7 * Constants.ONE_NXT + Constants.SHUFFLING_DEPOSIT_NQT, FORGY.getBalanceDiff());
        Assert.assertEquals(7 * Constants.ONE_NXT + Constants.SHUFFLING_DEPOSIT_NQT, FORGY.getUnconfirmedBalanceDiff());

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

        Assert.assertEquals(8 * Constants.ONE_NXT + Constants.SHUFFLING_DEPOSIT_NQT, FORGY.getBalanceDiff());
        Assert.assertEquals(8 * Constants.ONE_NXT + Constants.SHUFFLING_DEPOSIT_NQT, FORGY.getUnconfirmedBalanceDiff());

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

        Assert.assertEquals(10 * Constants.ONE_NXT + Constants.SHUFFLING_DEPOSIT_NQT, FORGY.getBalanceDiff());
        Assert.assertEquals(10 * Constants.ONE_NXT + Constants.SHUFFLING_DEPOSIT_NQT, FORGY.getUnconfirmedBalanceDiff());

    }

    @Test
    public void cancelAfterVerifyChuck() {
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
        shufflingStateHash = (String)getShufflingResponse.get("shufflingStateHash");
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

        Assert.assertEquals(13 * Constants.ONE_NXT + Constants.SHUFFLING_DEPOSIT_NQT, FORGY.getBalanceDiff());
        Assert.assertEquals(13 * Constants.ONE_NXT + Constants.SHUFFLING_DEPOSIT_NQT, FORGY.getUnconfirmedBalanceDiff());

    }

    @Test
    public void cancelAfterVerifyChuckInvalidKeys() {
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
        JSONObject cancelResponse = cancel(shufflingId, CHUCK, shufflingStateHash, 0, false);
        JSONObject transactionJSON = (JSONObject)cancelResponse.get("transactionJSON");
        JSONArray keySeeds = (JSONArray)((JSONObject)transactionJSON.get("attachment")).get("keySeeds");
        String s = (String)keySeeds.get(0);
        keySeeds.set(0, "0000000000" + s.substring(10));
        broadcast(transactionJSON, CHUCK);
        generateBlock();
        getShufflingResponse = getShuffling(shufflingId);
        shufflingStateHash = (String)getShufflingResponse.get("shufflingStateHash");
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

        Assert.assertEquals(13 * Constants.ONE_NXT + Constants.SHUFFLING_DEPOSIT_NQT, FORGY.getBalanceDiff());
        Assert.assertEquals(13 * Constants.ONE_NXT + Constants.SHUFFLING_DEPOSIT_NQT, FORGY.getUnconfirmedBalanceDiff());

    }

    @Test
    public void cancelAfterVerifyChuckInvalidKeysAlice() {
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
        shufflingStateHash = (String)getShufflingResponse.get("shufflingStateHash");
        String cancellingAccountId = (String)getShufflingResponse.get("cancellingAccount");
        Assert.assertEquals(Long.toUnsignedString(CHUCK.getId()), cancellingAccountId);
        JSONObject cancelResponse = cancel(shufflingId, ALICE, shufflingStateHash, CHUCK.getId(), false);
        JSONObject transactionJSON = (JSONObject)cancelResponse.get("transactionJSON");
        JSONArray keySeeds = (JSONArray)((JSONObject)transactionJSON.get("attachment")).get("keySeeds");
        String s = (String)keySeeds.get(0);
        keySeeds.set(0, "0000000000" + s.substring(10));
        broadcast(transactionJSON, ALICE);
        generateBlock();
        cancel(shufflingId, BOB, shufflingStateHash, CHUCK.getId());
        for (int i = 0; i < 14; i++) {
            generateBlock();
        }
        getShufflingResponse = getShuffling(shufflingId);
        Assert.assertEquals((long) Shuffling.Stage.CANCELLED.getCode(), getShufflingResponse.get("stage"));

        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + 4 * Constants.ONE_NXT), ALICE.getBalanceDiff());
        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + 4 * Constants.ONE_NXT), ALICE.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-4 * Constants.ONE_NXT, BOB.getBalanceDiff());
        Assert.assertEquals(-4 * Constants.ONE_NXT, BOB.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-3 * Constants.ONE_NXT, CHUCK.getBalanceDiff());
        Assert.assertEquals(-3 * Constants.ONE_NXT, CHUCK.getUnconfirmedBalanceDiff());
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

        Assert.assertEquals(13 * Constants.ONE_NXT + Constants.SHUFFLING_DEPOSIT_NQT, FORGY.getBalanceDiff());
        Assert.assertEquals(13 * Constants.ONE_NXT + Constants.SHUFFLING_DEPOSIT_NQT, FORGY.getUnconfirmedBalanceDiff());

    }

    @Test
    public void cancelAfterVerifyChuckInvalidKeysAlice2() {
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
        shufflingStateHash = (String)getShufflingResponse.get("shufflingStateHash");
        String cancellingAccountId = (String)getShufflingResponse.get("cancellingAccount");
        Assert.assertEquals(Long.toUnsignedString(CHUCK.getId()), cancellingAccountId);
        JSONObject cancelResponse = cancel(shufflingId, ALICE, shufflingStateHash, CHUCK.getId(), false);
        JSONObject transactionJSON = (JSONObject)cancelResponse.get("transactionJSON");
        JSONArray keySeeds = (JSONArray)((JSONObject)transactionJSON.get("attachment")).get("keySeeds");
        String s = (String)keySeeds.get(1);
        keySeeds.set(1, "0000000000" + s.substring(10));
        broadcast(transactionJSON, ALICE);
        generateBlock();
        cancel(shufflingId, BOB, shufflingStateHash, CHUCK.getId());
        for (int i = 0; i < 14; i++) {
            generateBlock();
        }
        getShufflingResponse = getShuffling(shufflingId);
        Assert.assertEquals((long) Shuffling.Stage.CANCELLED.getCode(), getShufflingResponse.get("stage"));

        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + 4 * Constants.ONE_NXT), ALICE.getBalanceDiff());
        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + 4 * Constants.ONE_NXT), ALICE.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-4 * Constants.ONE_NXT, BOB.getBalanceDiff());
        Assert.assertEquals(-4 * Constants.ONE_NXT, BOB.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-3 * Constants.ONE_NXT, CHUCK.getBalanceDiff());
        Assert.assertEquals(-3 * Constants.ONE_NXT, CHUCK.getUnconfirmedBalanceDiff());
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

        Assert.assertEquals(13 * Constants.ONE_NXT + Constants.SHUFFLING_DEPOSIT_NQT, FORGY.getBalanceDiff());
        Assert.assertEquals(13 * Constants.ONE_NXT + Constants.SHUFFLING_DEPOSIT_NQT, FORGY.getUnconfirmedBalanceDiff());

    }

    @Test
    public void badProcessDataAlice() {
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

        JSONObject processResponse = process(shufflingId, ALICE, ALICE_RECIPIENT, false);
        JSONObject transactionJSON = (JSONObject)processResponse.get("transactionJSON");
        JSONArray data = (JSONArray)((JSONObject)transactionJSON.get("attachment")).get("data");
        String s = (String)data.get(0);
        data.set(0, "0000000000" + s.substring(10));
        broadcast(transactionJSON, ALICE);
        generateBlock();
        getShufflingResponse = getShuffling(shufflingId);
        Assert.assertEquals((long) Shuffling.Stage.PROCESSING.getCode(), getShufflingResponse.get("stage"));
        process(shufflingId, BOB, BOB_RECIPIENT);
        generateBlock();
        getShufflingResponse = getShuffling(shufflingId);
        Assert.assertEquals((long) Shuffling.Stage.BLAME.getCode(), getShufflingResponse.get("stage"));
        long cancellingAccountId = Convert.parseUnsignedLong((String) getShufflingResponse.get("cancellingAccount"));
        Assert.assertEquals(BOB.getId(), cancellingAccountId);
        for (int i = 0; i < 14; i++) {
            generateBlock();
        }
        getShufflingResponse = getShuffling(shufflingId);
        Assert.assertEquals((long) Shuffling.Stage.CANCELLED.getCode(), getShufflingResponse.get("stage"));

        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + 2 * Constants.ONE_NXT), ALICE.getBalanceDiff());
        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + 2 * Constants.ONE_NXT), ALICE.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-2 * Constants.ONE_NXT, BOB.getBalanceDiff());
        Assert.assertEquals(-2 * Constants.ONE_NXT, BOB.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-1 * Constants.ONE_NXT, CHUCK.getBalanceDiff());
        Assert.assertEquals(-1 * Constants.ONE_NXT, CHUCK.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-1 * Constants.ONE_NXT, DAVE.getBalanceDiff());
        Assert.assertEquals(-1 * Constants.ONE_NXT, DAVE.getUnconfirmedBalanceDiff());

        Assert.assertNull(ALICE_RECIPIENT.getAccount());
        Assert.assertNull(BOB_RECIPIENT.getAccount());
        Assert.assertNull(CHUCK_RECIPIENT.getAccount());
        Assert.assertNull(DAVE_RECIPIENT.getAccount());

        Assert.assertEquals(6 * Constants.ONE_NXT + Constants.SHUFFLING_DEPOSIT_NQT, FORGY.getBalanceDiff());
        Assert.assertEquals(6 * Constants.ONE_NXT + Constants.SHUFFLING_DEPOSIT_NQT, FORGY.getUnconfirmedBalanceDiff());

    }

    @Test
    public void modifiedProcessDataBob() {
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
        JSONObject processResponse = process(shufflingId, BOB, BOB_RECIPIENT, false);
        JSONObject transactionJSON = (JSONObject)processResponse.get("transactionJSON");
        JSONArray data = (JSONArray)((JSONObject)transactionJSON.get("attachment")).get("data");
        String s = (String)data.get(0);
        data.set(0, "0000000000" + s.substring(10));
        broadcast(transactionJSON, BOB);
        generateBlock();
        getShufflingResponse = getShuffling(shufflingId);
        Assert.assertEquals((long) Shuffling.Stage.PROCESSING.getCode(), getShufflingResponse.get("stage"));
        process(shufflingId, CHUCK, CHUCK_RECIPIENT);
        generateBlock();
        getShufflingResponse = getShuffling(shufflingId);
        Assert.assertEquals((long) Shuffling.Stage.BLAME.getCode(), getShufflingResponse.get("stage"));
        long cancellingAccountId = Convert.parseUnsignedLong((String)getShufflingResponse.get("cancellingAccount"));
        Assert.assertEquals(CHUCK.getId(), cancellingAccountId);
        String shufflingStateHash = (String)getShufflingResponse.get("shufflingStateHash");
        cancel(shufflingId, ALICE, shufflingStateHash, cancellingAccountId);
        JSONObject cancelResponse = cancel(shufflingId, BOB, shufflingStateHash, cancellingAccountId);
        boolean bobCancelFailed = cancelResponse.get("error") != null; // if he happened to modify his own piece
        for (int i = 0; i < 14; i++) {
            generateBlock();
        }

        getShufflingResponse = getShuffling(shufflingId);
        Assert.assertEquals((long) Shuffling.Stage.CANCELLED.getCode(), getShufflingResponse.get("stage"));

        Assert.assertEquals(-3 * Constants.ONE_NXT, ALICE.getBalanceDiff());
        Assert.assertEquals(-3 * Constants.ONE_NXT, ALICE.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + (bobCancelFailed ? 2 : 3) * Constants.ONE_NXT), BOB.getBalanceDiff());
        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + (bobCancelFailed ? 2 : 3) * Constants.ONE_NXT), BOB.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-2 * Constants.ONE_NXT, CHUCK.getBalanceDiff());
        Assert.assertEquals(-2 * Constants.ONE_NXT, CHUCK.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-1 * Constants.ONE_NXT, DAVE.getBalanceDiff());
        Assert.assertEquals(-1 * Constants.ONE_NXT, DAVE.getUnconfirmedBalanceDiff());

        Assert.assertNull(ALICE_RECIPIENT.getAccount());
        Assert.assertNull(BOB_RECIPIENT.getAccount());
        Assert.assertNull(CHUCK_RECIPIENT.getAccount());
        Assert.assertNull(DAVE_RECIPIENT.getAccount());

        Assert.assertEquals((bobCancelFailed ? 8 : 9) * Constants.ONE_NXT + Constants.SHUFFLING_DEPOSIT_NQT, FORGY.getBalanceDiff());
        Assert.assertEquals((bobCancelFailed ? 8 : 9) * Constants.ONE_NXT + Constants.SHUFFLING_DEPOSIT_NQT, FORGY.getUnconfirmedBalanceDiff());

    }

    @Test
    public void modifiedProcessDataChuck() {
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
        JSONObject processResponse = process(shufflingId, CHUCK, CHUCK_RECIPIENT, false);
        JSONObject transactionJSON = (JSONObject)processResponse.get("transactionJSON");
        JSONArray data = (JSONArray)((JSONObject)transactionJSON.get("attachment")).get("data");
        String s = (String)data.get(0);
        data.set(0, "0000000000" + s.substring(10));
        broadcast(transactionJSON, CHUCK);
        generateBlock();
        getShufflingResponse = getShuffling(shufflingId);
        Assert.assertEquals((long) Shuffling.Stage.PROCESSING.getCode(), getShufflingResponse.get("stage"));
        process(shufflingId, DAVE, DAVE_RECIPIENT);
        generateBlock();
        getShufflingResponse = getShuffling(shufflingId);
        Assert.assertEquals((long) Shuffling.Stage.BLAME.getCode(), getShufflingResponse.get("stage"));
        long cancellingAccountId = Convert.parseUnsignedLong((String)getShufflingResponse.get("cancellingAccount"));
        Assert.assertEquals(DAVE.getId(), cancellingAccountId);
        String shufflingStateHash = (String)getShufflingResponse.get("shufflingStateHash");
        cancel(shufflingId, ALICE, shufflingStateHash, cancellingAccountId);
        cancel(shufflingId, BOB, shufflingStateHash, cancellingAccountId);
        JSONObject cancelResponse = cancel(shufflingId, CHUCK, shufflingStateHash, cancellingAccountId);
        boolean chuckCancelFailed = cancelResponse.get("error") != null; // if he happened to modify his own piece
        for (int i = 0; i < 14; i++) {
            generateBlock();
        }

        getShufflingResponse = getShuffling(shufflingId);
        Assert.assertEquals((long) Shuffling.Stage.CANCELLED.getCode(), getShufflingResponse.get("stage"));

        Assert.assertEquals(-3 * Constants.ONE_NXT, ALICE.getBalanceDiff());
        Assert.assertEquals(-3 * Constants.ONE_NXT, ALICE.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-3 * Constants.ONE_NXT, BOB.getBalanceDiff());
        Assert.assertEquals(-3 * Constants.ONE_NXT, BOB.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + (chuckCancelFailed ? 2 : 3) * Constants.ONE_NXT), CHUCK.getBalanceDiff());
        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + (chuckCancelFailed ? 2 : 3) * Constants.ONE_NXT), CHUCK.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-2 * Constants.ONE_NXT, DAVE.getBalanceDiff());
        Assert.assertEquals(-2 * Constants.ONE_NXT, DAVE.getUnconfirmedBalanceDiff());

        Assert.assertNull(ALICE_RECIPIENT.getAccount());
        Assert.assertNull(BOB_RECIPIENT.getAccount());
        Assert.assertNull(CHUCK_RECIPIENT.getAccount());
        Assert.assertNull(DAVE_RECIPIENT.getAccount());

        Assert.assertEquals((chuckCancelFailed ? 10 : 11) * Constants.ONE_NXT + Constants.SHUFFLING_DEPOSIT_NQT, FORGY.getBalanceDiff());
        Assert.assertEquals((chuckCancelFailed ? 10 : 11) * Constants.ONE_NXT + Constants.SHUFFLING_DEPOSIT_NQT, FORGY.getUnconfirmedBalanceDiff());

    }

    @Test
    public void modifiedRecipientKeysDave() {
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
        JSONObject processResponse = process(shufflingId, DAVE, DAVE_RECIPIENT, false);
        JSONObject transactionJSON = (JSONObject)processResponse.get("transactionJSON");
        JSONArray data = (JSONArray)((JSONObject)transactionJSON.get("attachment")).get("recipientPublicKeys");
        String s = (String)data.get(0);
        if (!s.equals(DAVE_RECIPIENT.getPublicKeyStr())) {
            data.set(0, "0000000000" + s.substring(10));
        } else {
            s = (String)data.get(1);
            data.set(1, "0000000000" + s.substring(10));
        }
        broadcast(transactionJSON, DAVE);
        generateBlock();
        getShufflingResponse = getShuffling(shufflingId);
        Assert.assertEquals((long) Shuffling.Stage.VERIFICATION.getCode(), getShufflingResponse.get("stage"));
        String shufflingStateHash = (String)getShufflingResponse.get("shufflingStateHash");
        cancel(shufflingId, ALICE, shufflingStateHash, 0);
        generateBlock();
        getShufflingResponse = getShuffling(shufflingId);
        shufflingStateHash = (String)getShufflingResponse.get("shufflingStateHash");
        cancel(shufflingId, BOB, shufflingStateHash, ALICE.getId());
        cancel(shufflingId, CHUCK, shufflingStateHash, ALICE.getId());
        for (int i = 0; i < 14; i++) {
            generateBlock();
        }

        getShufflingResponse = getShuffling(shufflingId);
        Assert.assertEquals((long) Shuffling.Stage.CANCELLED.getCode(), getShufflingResponse.get("stage"));

        Assert.assertEquals(-3 * Constants.ONE_NXT, ALICE.getBalanceDiff());
        Assert.assertEquals(-3 * Constants.ONE_NXT, ALICE.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-3 * Constants.ONE_NXT, BOB.getBalanceDiff());
        Assert.assertEquals(-3 * Constants.ONE_NXT, BOB.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-3 * Constants.ONE_NXT, CHUCK.getBalanceDiff());
        Assert.assertEquals(-3 * Constants.ONE_NXT, CHUCK.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + 2 * Constants.ONE_NXT), DAVE.getBalanceDiff());
        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + 2 * Constants.ONE_NXT), DAVE.getUnconfirmedBalanceDiff());

        Assert.assertTrue(ALICE_RECIPIENT.getAccount() == null || ALICE_RECIPIENT.getBalanceDiff() == 0);
        Assert.assertTrue(ALICE_RECIPIENT.getAccount() == null || ALICE_RECIPIENT.getUnconfirmedBalanceDiff() == 0);
        Assert.assertTrue(BOB_RECIPIENT.getAccount() == null || BOB_RECIPIENT.getBalanceDiff() == 0);
        Assert.assertTrue(BOB_RECIPIENT.getAccount() == null || BOB_RECIPIENT.getUnconfirmedBalanceDiff() == 0);
        Assert.assertTrue(CHUCK_RECIPIENT.getAccount() == null || CHUCK_RECIPIENT.getBalanceDiff() == 0);
        Assert.assertTrue(CHUCK_RECIPIENT.getAccount() == null || CHUCK_RECIPIENT.getUnconfirmedBalanceDiff() == 0);
        Assert.assertTrue(DAVE_RECIPIENT.getAccount() == null || DAVE_RECIPIENT.getBalanceDiff() == 0);
        Assert.assertTrue(DAVE_RECIPIENT.getAccount() == null || DAVE_RECIPIENT.getUnconfirmedBalanceDiff() == 0);

        Assert.assertEquals(11 * Constants.ONE_NXT + Constants.SHUFFLING_DEPOSIT_NQT, FORGY.getBalanceDiff());
        Assert.assertEquals(11 * Constants.ONE_NXT + Constants.SHUFFLING_DEPOSIT_NQT, FORGY.getUnconfirmedBalanceDiff());

    }

    @Test
    public void duplicateRecipientKeysDave() {
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
        JSONObject processResponse = process(shufflingId, DAVE, DAVE_RECIPIENT, false);
        JSONObject transactionJSON = (JSONObject)processResponse.get("transactionJSON");
        JSONArray data = (JSONArray)((JSONObject)transactionJSON.get("attachment")).get("recipientPublicKeys");
        String s = (String)data.get(0);
        data.set(1, s);
        JSONObject broadcastResponse = broadcast(transactionJSON, DAVE);
        Assert.assertTrue(broadcastResponse.get("error") != null);
        for (int i = 0; i < 14; i++) {
            generateBlock();
        }

        getShufflingResponse = getShuffling(shufflingId);
        Assert.assertEquals((long) Shuffling.Stage.CANCELLED.getCode(), getShufflingResponse.get("stage"));

        Assert.assertEquals(-2 * Constants.ONE_NXT, ALICE.getBalanceDiff());
        Assert.assertEquals(-2 * Constants.ONE_NXT, ALICE.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-2 * Constants.ONE_NXT, BOB.getBalanceDiff());
        Assert.assertEquals(-2 * Constants.ONE_NXT, BOB.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-2 * Constants.ONE_NXT, CHUCK.getBalanceDiff());
        Assert.assertEquals(-2 * Constants.ONE_NXT, CHUCK.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + Constants.ONE_NXT), DAVE.getBalanceDiff());
        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + Constants.ONE_NXT), DAVE.getUnconfirmedBalanceDiff());

        Assert.assertNull(ALICE_RECIPIENT.getAccount());
        Assert.assertNull(BOB_RECIPIENT.getAccount());
        Assert.assertNull(CHUCK_RECIPIENT.getAccount());
        Assert.assertNull(DAVE_RECIPIENT.getAccount());

        Assert.assertEquals(7 * Constants.ONE_NXT + Constants.SHUFFLING_DEPOSIT_NQT, FORGY.getBalanceDiff());
        Assert.assertEquals(7 * Constants.ONE_NXT + Constants.SHUFFLING_DEPOSIT_NQT, FORGY.getUnconfirmedBalanceDiff());

    }

    @Test
    public void duplicateProcessDataChuck() {
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
        JSONObject processResponse = process(shufflingId, CHUCK, CHUCK_RECIPIENT, false);
        JSONObject transactionJSON = (JSONObject)processResponse.get("transactionJSON");
        JSONArray data = (JSONArray)((JSONObject)transactionJSON.get("attachment")).get("data");
        String s = (String)data.get(0);
        data.set(1, s);
        JSONObject broadcastResponse = broadcast(transactionJSON, CHUCK);
        Assert.assertTrue(broadcastResponse.get("error") != null);
        for (int i = 0; i < 14; i++) {
            generateBlock();
        }

        getShufflingResponse = getShuffling(shufflingId);
        Assert.assertEquals((long) Shuffling.Stage.CANCELLED.getCode(), getShufflingResponse.get("stage"));

        Assert.assertEquals(-2 * Constants.ONE_NXT, ALICE.getBalanceDiff());
        Assert.assertEquals(-2 * Constants.ONE_NXT, ALICE.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-2 * Constants.ONE_NXT, BOB.getBalanceDiff());
        Assert.assertEquals(-2 * Constants.ONE_NXT, BOB.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + Constants.ONE_NXT), CHUCK.getBalanceDiff());
        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + Constants.ONE_NXT), CHUCK.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-1 * Constants.ONE_NXT, DAVE.getBalanceDiff());
        Assert.assertEquals(-1 * Constants.ONE_NXT, DAVE.getUnconfirmedBalanceDiff());

        Assert.assertNull(ALICE_RECIPIENT.getAccount());
        Assert.assertNull(BOB_RECIPIENT.getAccount());
        Assert.assertNull(CHUCK_RECIPIENT.getAccount());
        Assert.assertNull(DAVE_RECIPIENT.getAccount());

        Assert.assertEquals(6 * Constants.ONE_NXT + Constants.SHUFFLING_DEPOSIT_NQT, FORGY.getBalanceDiff());
        Assert.assertEquals(6 * Constants.ONE_NXT + Constants.SHUFFLING_DEPOSIT_NQT, FORGY.getUnconfirmedBalanceDiff());

    }

    @Test
    public void duplicateRecipientsBobChuck() {
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
        process(shufflingId, CHUCK, BOB_RECIPIENT);
        generateBlock();
        process(shufflingId, DAVE, DAVE_RECIPIENT);
        generateBlock();

        getShufflingResponse = getShuffling(shufflingId);
        Assert.assertEquals((long) Shuffling.Stage.BLAME.getCode(), getShufflingResponse.get("stage"));

        long cancellingAccountId = Convert.parseUnsignedLong((String)getShufflingResponse.get("cancellingAccount"));
        Assert.assertEquals(DAVE.getId(), cancellingAccountId);
        String shufflingStateHash = (String)getShufflingResponse.get("shufflingStateHash");

        cancel(shufflingId, ALICE, shufflingStateHash, cancellingAccountId);
        cancel(shufflingId, BOB, shufflingStateHash, cancellingAccountId);
        cancel(shufflingId, CHUCK, shufflingStateHash, cancellingAccountId);
        for (int i = 0; i < 14; i++) {
            generateBlock();
        }
        getShufflingResponse = getShuffling(shufflingId);
        Assert.assertEquals((long) Shuffling.Stage.CANCELLED.getCode(), getShufflingResponse.get("stage"));

        Assert.assertEquals(-3 * Constants.ONE_NXT, ALICE.getBalanceDiff());
        Assert.assertEquals(-3 * Constants.ONE_NXT, ALICE.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-3 * Constants.ONE_NXT, BOB.getBalanceDiff());
        Assert.assertEquals(-3 * Constants.ONE_NXT, BOB.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + 3 * Constants.ONE_NXT), CHUCK.getBalanceDiff());
        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + 3 * Constants.ONE_NXT), CHUCK.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-2 * Constants.ONE_NXT, DAVE.getBalanceDiff());
        Assert.assertEquals(-2 * Constants.ONE_NXT, DAVE.getUnconfirmedBalanceDiff());

        Assert.assertNull(ALICE_RECIPIENT.getAccount());
        Assert.assertNull(BOB_RECIPIENT.getAccount());
        Assert.assertNull(CHUCK_RECIPIENT.getAccount());
        Assert.assertNull(DAVE_RECIPIENT.getAccount());

        Assert.assertEquals(11 * Constants.ONE_NXT + Constants.SHUFFLING_DEPOSIT_NQT, FORGY.getBalanceDiff());
        Assert.assertEquals(11 * Constants.ONE_NXT + Constants.SHUFFLING_DEPOSIT_NQT, FORGY.getUnconfirmedBalanceDiff());

    }

    @Test
    public void duplicateRecipientsAliceBob() {
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
        process(shufflingId, BOB, ALICE_RECIPIENT);
        generateBlock();
        process(shufflingId, CHUCK, CHUCK_RECIPIENT);
        generateBlock();
        process(shufflingId, DAVE, DAVE_RECIPIENT);
        generateBlock();

        getShufflingResponse = getShuffling(shufflingId);
        Assert.assertEquals((long) Shuffling.Stage.BLAME.getCode(), getShufflingResponse.get("stage"));

        long cancellingAccountId = Convert.parseUnsignedLong((String)getShufflingResponse.get("cancellingAccount"));
        Assert.assertEquals(DAVE.getId(), cancellingAccountId);
        String shufflingStateHash = (String)getShufflingResponse.get("shufflingStateHash");

        cancel(shufflingId, ALICE, shufflingStateHash, cancellingAccountId);
        cancel(shufflingId, BOB, shufflingStateHash, cancellingAccountId);
        cancel(shufflingId, CHUCK, shufflingStateHash, cancellingAccountId);
        for (int i = 0; i < 14; i++) {
            generateBlock();
        }
        getShufflingResponse = getShuffling(shufflingId);
        Assert.assertEquals((long) Shuffling.Stage.CANCELLED.getCode(), getShufflingResponse.get("stage"));

        Assert.assertEquals(-3 * Constants.ONE_NXT, ALICE.getBalanceDiff());
        Assert.assertEquals(-3 * Constants.ONE_NXT, ALICE.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + 3 * Constants.ONE_NXT), BOB.getBalanceDiff());
        Assert.assertEquals(-(Constants.SHUFFLING_DEPOSIT_NQT + 3 * Constants.ONE_NXT), BOB.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-3 * Constants.ONE_NXT, CHUCK.getBalanceDiff());
        Assert.assertEquals(-3 * Constants.ONE_NXT, CHUCK.getUnconfirmedBalanceDiff());
        Assert.assertEquals(-2 * Constants.ONE_NXT, DAVE.getBalanceDiff());
        Assert.assertEquals(-2 * Constants.ONE_NXT, DAVE.getUnconfirmedBalanceDiff());

        Assert.assertNull(ALICE_RECIPIENT.getAccount());
        Assert.assertNull(BOB_RECIPIENT.getAccount());
        Assert.assertNull(CHUCK_RECIPIENT.getAccount());
        Assert.assertNull(DAVE_RECIPIENT.getAccount());

        Assert.assertEquals(11 * Constants.ONE_NXT + Constants.SHUFFLING_DEPOSIT_NQT, FORGY.getBalanceDiff());
        Assert.assertEquals(11 * Constants.ONE_NXT + Constants.SHUFFLING_DEPOSIT_NQT, FORGY.getUnconfirmedBalanceDiff());

    }


    private JSONObject create(Tester creator) {
        return create(creator, 4);
    }

    private JSONObject create(Tester creator, int participantCount) {
        APICall apiCall = new APICall.Builder("shufflingCreate").
                secretPhrase(creator.getSecretPhrase()).
                feeNQT(Constants.ONE_NXT).
                param("amount", String.valueOf(defaultShufflingAmount)).
                param("participantCount", String.valueOf(participantCount)).
                param("registrationPeriod", 10).
                build();
        JSONObject response = apiCall.invoke();
        Logger.logMessage("shufflingCreateResponse: " + response.toJSONString());
        return response;
    }

    private JSONObject createAssetShuffling(Tester creator) {
        APICall apiCall = new APICall.Builder("shufflingCreate").
                secretPhrase(creator.getSecretPhrase()).
                feeNQT(Constants.ONE_NXT).
                param("amount", String.valueOf(defaultHoldingShufflingAmount)).
                param("participantCount", "4").
                param("registrationPeriod", 10).
                param("holding", Long.toUnsignedString(shufflingAsset)).
                param("holdingType", String.valueOf(HoldingType.ASSET.getCode())).
                build();
        JSONObject response = apiCall.invoke();
        Logger.logMessage("shufflingCreateResponse: " + response.toJSONString());
        return response;
    }

    private JSONObject createCurrencyShuffling(Tester creator) {
        APICall apiCall = new APICall.Builder("shufflingCreate").
                secretPhrase(creator.getSecretPhrase()).
                feeNQT(Constants.ONE_NXT).
                param("amount", String.valueOf(defaultHoldingShufflingAmount)).
                param("participantCount", "4").
                param("registrationPeriod", 10).
                param("holding", Long.toUnsignedString(shufflingCurrency)).
                param("holdingType", String.valueOf(HoldingType.CURRENCY.getCode())).
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
        return process(shufflingId, tester, recipient, true);
    }

    private JSONObject process(String shufflingId, Tester tester, Tester recipient, boolean broadcast) {
        APICall.Builder builder = new APICall.Builder("shufflingProcess").
                param("shuffling", shufflingId).
                param("secretPhrase", tester.getSecretPhrase()).
                param("recipientSecretPhrase", recipient.getSecretPhrase()).
                feeNQT(Constants.ONE_NXT);
        if (!broadcast) {
            builder.param("broadcast", "false");
        }
        APICall apiCall = builder.build();
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
        return cancel(shufflingId, tester, shufflingStateHash, cancellingAccountId, true);
    }

    private JSONObject cancel(String shufflingId, Tester tester, String shufflingStateHash, long cancellingAccountId, boolean broadcast) {
        APICall.Builder builder = new APICall.Builder("shufflingCancel").
                param("shuffling", shufflingId).
                param("secretPhrase", tester.getSecretPhrase()).
                param("shufflingStateHash", shufflingStateHash).
                feeNQT(Constants.ONE_NXT);
        if (cancellingAccountId != 0) {
            builder.param("cancellingAccount", Long.toUnsignedString(cancellingAccountId));
        }
        if (!broadcast) {
            builder.param("broadcast", "false");
        }
        APICall apiCall = builder.build();
        JSONObject response = apiCall.invoke();
        Logger.logDebugMessage("shufflingCancelResponse:" + response);
        return response;
    }

    private JSONObject broadcast(JSONObject transaction, Tester tester) {
        transaction.remove("signature");
        APICall apiCall = new APICall.Builder("signTransaction")
                .param("unsignedTransactionJSON", transaction.toJSONString())
                .param("validate", "false")
                .param("secretPhrase", tester.getSecretPhrase())
                .build();
        JSONObject response = apiCall.invoke();
        if (response.get("transactionJSON") == null) {
            return response;
        }
        apiCall = new APICall.Builder("broadcastTransaction").
                param("transactionJSON", ((JSONObject)response.get("transactionJSON")).toJSONString()).
                feeNQT(Constants.ONE_NXT).
                build();
        response = apiCall.invoke();
        Logger.logDebugMessage("broadcastTransactionResponse:" + response);
        return response;
    }

}
