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

package nxt;

import nxt.crypto.Crypto;
import nxt.util.Convert;
import nxt.util.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public final class Shuffler {

    private static final Map<Long, Map<Long, Shuffler>> shufflingsMap = Collections.synchronizedMap(new HashMap<>());

    public static Shuffler addOrGetShuffler(String secretPhrase, byte[] recipientPublicKey, byte[] shufflingFullHash) {
        long shufflingId = Convert.fullHashToId(shufflingFullHash);
        long accountId = Account.getId(Crypto.getPublicKey(secretPhrase));
        synchronized (shufflingsMap) {
            Map<Long, Shuffler> map = shufflingsMap.get(shufflingId);
            if (map == null) {
                map = new HashMap<>();
                shufflingsMap.put(shufflingId, map);
            }
            Shuffler shuffler = map.get(accountId);
            if (recipientPublicKey == null) {
                return shuffler;
            }
            if (shuffler == null) {
                shuffler = new Shuffler(secretPhrase, recipientPublicKey, shufflingFullHash);
                map.put(accountId, shuffler);
                Shuffling shuffling = Shuffling.getShuffling(Convert.fullHashToId(shufflingFullHash));
                if (shuffling != null) {
                    shuffler.init(shuffling);
                }
            } else if (!Arrays.equals(shuffler.recipientPublicKey, recipientPublicKey)) {
                throw new IllegalArgumentException("A shuffler with different recipientPublicKey already started");
            } else if (!Arrays.equals(shuffler.shufflingFullHash, shufflingFullHash)) {
                throw new RuntimeException("A shuffler with different shufflingFullHash already started");
            }
            return shuffler;
        }
    }

    static {

        Shuffling.addListener(shuffling -> {
            Map<Long, Shuffler> shufflerMap = shufflingsMap.get(shuffling.getId());
            if (shufflerMap != null) {
                shufflerMap.values().forEach(shuffler -> {
                    if (shuffler.accountId != shuffling.getIssuerId()) {
                        shuffler.register(shuffling);
                    }
                });
            }
        }, Shuffling.Event.SHUFFLING_CREATED);

        Shuffling.addListener(shuffling -> {
            Map<Long, Shuffler> shufflerMap = shufflingsMap.get(shuffling.getId());
            if (shufflerMap != null) {
                Shuffler shuffler = shufflerMap.get(shuffling.getAssigneeAccountId());
                if (shuffler != null) {
                    shuffler.process(shuffling);
                }
            }
        }, Shuffling.Event.SHUFFLING_ASSIGNED);

        Shuffling.addListener(shuffling -> {
            Map<Long, Shuffler> shufflerMap = shufflingsMap.get(shuffling.getId());
            if (shufflerMap != null) {
                shufflerMap.values().forEach(shuffler -> {
                    if (shuffler.accountId != shuffling.getLastParticipant().getAccountId()) {
                        boolean found = false;
                        for (byte[] key : shuffling.getRecipientPublicKeys()) {
                            if (Arrays.equals(key, shuffler.recipientPublicKey)) {
                                found = true;
                                break;
                            }
                        }
                        if (found) {
                            shuffler.verify(shuffling);
                        } else {
                            shuffler.cancel(shuffling);
                        }
                    }
                });
            }
        }, Shuffling.Event.SHUFFLING_PROCESSING_FINISHED);

        Shuffling.addListener(shuffling -> {
            Map<Long, Shuffler> shufflerMap = shufflingsMap.get(shuffling.getId());
            if (shufflerMap != null) {
                shufflerMap.values().forEach(shuffler -> {
                    if (shuffler.accountId != shuffling.getLastParticipant().getAccountId()) {
                        shuffler.cancel(shuffling);
                    }
                });
            }
        }, Shuffling.Event.SHUFFLING_BLAME_STARTED);
    }

    private final long accountId;
    private final String secretPhrase;
    private final byte[] recipientPublicKey;
    private final byte[] shufflingFullHash;

    private Shuffler(String secretPhrase, byte[] recipientPublicKey, byte[] shufflingFullHash) {
        this.secretPhrase = secretPhrase;
        this.accountId = Account.getId(Crypto.getPublicKey(secretPhrase));
        this.recipientPublicKey = recipientPublicKey;
        this.shufflingFullHash = shufflingFullHash;
    }

    private void init(Shuffling shuffling) {
        switch (shuffling.getStage()) {
            case REGISTRATION:
                ShufflingParticipant shufflingParticipant = shuffling.getParticipant(accountId);
                if (shufflingParticipant == null) {
                    register(shuffling);
                }
                break;
            case PROCESSING:
                if (accountId == shuffling.getAssigneeAccountId()) {
                    process(shuffling);
                }
                break;
            case VERIFICATION:
                if (accountId != shuffling.getLastParticipant().getAccountId()) {
                    boolean found = false;
                    for (byte[] key : shuffling.getRecipientPublicKeys()) {
                        if (Arrays.equals(key, recipientPublicKey)) {
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                        verify(shuffling);
                    } else {
                        cancel(shuffling);
                    }
                }
                break;
            case BLAME:
                if (accountId != shuffling.getLastParticipant().getAccountId()) {
                    cancel(shuffling);
                }
                break;
        }
    }

    private void register(Shuffling shuffling) {
        Attachment.ShufflingRegistration attachment = new Attachment.ShufflingRegistration(shuffling.getId(), shufflingFullHash);
        submitTransaction(attachment);
    }

    private void process(Shuffling shuffling) {
        Attachment.ShufflingAttachment attachment = shuffling.process(accountId, secretPhrase, recipientPublicKey);
        submitTransaction(attachment);
    }

    private void verify(Shuffling shuffling) {
        Attachment.ShufflingVerification attachment = new Attachment.ShufflingVerification(shuffling.getId(), shuffling.getStateHash());
        submitTransaction(attachment);
    }

    private void cancel(Shuffling shuffling) {
        Attachment.ShufflingCancellation attachment = shuffling.revealKeySeeds(secretPhrase, shuffling.getCancellingAccountId(), shuffling.getStateHash());
        submitTransaction(attachment);
    }

    private void submitTransaction(Attachment.ShufflingAttachment attachment) {
        if (hasUnconfirmedTransaction(attachment)) {
            return;
        }
        try {
            Transaction.Builder builder = Nxt.newTransactionBuilder(Crypto.getPublicKey(secretPhrase), 0, 0,
                    (short) 1440, attachment);
            Transaction transaction = builder.build(secretPhrase);
            TransactionProcessorImpl.getInstance().broadcast(transaction);
        } catch (NxtException.ValidationException e) {
            Logger.logErrorMessage("Error submitting shuffler transaction", e);
        }
    }

    private boolean hasUnconfirmedTransaction(Attachment.ShufflingAttachment shufflingAttachment) {
        for (UnconfirmedTransaction unconfirmedTransaction : TransactionProcessorImpl.getInstance().getWaitingTransactions()) {
            if (unconfirmedTransaction.getSenderId() != accountId) {
                continue;
            }
            Attachment attachment = unconfirmedTransaction.getAttachment();
            if (!attachment.getClass().equals(shufflingAttachment.getClass())) {
                continue;
            }
            if (Arrays.equals(shufflingAttachment.getShufflingStateHash(), ((Attachment.ShufflingAttachment)attachment).getShufflingStateHash())) {
                return true;
            }
        }
        return false;
    }

}
