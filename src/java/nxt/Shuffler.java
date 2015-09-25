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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class Shuffler {

    private static final Map<Long, Map<Long, Shuffler>> shufflingsMap = Collections.synchronizedMap(new HashMap<>());
    private static final Map<Integer, Set<Long>> expirations = Collections.synchronizedMap(new HashMap<>());

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
                    clearExpiration(shuffling);
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
                        shuffler.submitRegister(shuffling);
                    }
                });
                clearExpiration(shuffling);
            }
        }, Shuffling.Event.SHUFFLING_CREATED);

        Shuffling.addListener(shuffling -> {
            Map<Long, Shuffler> shufflerMap = shufflingsMap.get(shuffling.getId());
            if (shufflerMap != null) {
                Shuffler shuffler = shufflerMap.get(shuffling.getAssigneeAccountId());
                if (shuffler != null) {
                    shuffler.submitProcess(shuffling);
                }
                clearExpiration(shuffling);
            }
        }, Shuffling.Event.SHUFFLING_ASSIGNED);

        Shuffling.addListener(shuffling -> {
            Map<Long, Shuffler> shufflerMap = shufflingsMap.get(shuffling.getId());
            if (shufflerMap != null) {
                shufflerMap.values().forEach(shuffler -> shuffler.verify(shuffling));
                clearExpiration(shuffling);
            }
        }, Shuffling.Event.SHUFFLING_PROCESSING_FINISHED);

        Shuffling.addListener(shuffling -> {
            Map<Long, Shuffler> shufflerMap = shufflingsMap.get(shuffling.getId());
            if (shufflerMap != null) {
                shufflerMap.values().forEach(shuffler -> shuffler.cancel(shuffling));
                clearExpiration(shuffling);
            }
        }, Shuffling.Event.SHUFFLING_BLAME_STARTED);

        Shuffling.addListener(Shuffler::scheduleExpiration, Shuffling.Event.SHUFFLING_DONE);

        Shuffling.addListener(Shuffler::scheduleExpiration, Shuffling.Event.SHUFFLING_CANCELLED);

        BlockchainProcessorImpl.getInstance().addListener(block -> {
            Set<Long> expired = expirations.get(block.getHeight());
            if (expired != null) {
                expired.forEach(shufflingsMap::remove);
                expirations.remove(block.getHeight());
            }
        }, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
    }

    private static void scheduleExpiration(Shuffling shuffling) {
        int expirationHeight = Nxt.getBlockchain().getHeight() + 720;
        Set<Long> shufflingIds = expirations.get(expirationHeight);
        if (shufflingIds == null) {
            shufflingIds = new HashSet<>();
            expirations.put(expirationHeight, shufflingIds);
        }
        shufflingIds.add(shuffling.getId());
    }

    private static void clearExpiration(Shuffling shuffling) {
        for (Set shufflingIds : expirations.values()) {
            if (shufflingIds.remove(shuffling.getId())) {
                return;
            }
        }
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
                    submitRegister(shuffling);
                }
                break;
            case PROCESSING:
                if (accountId == shuffling.getAssigneeAccountId()) {
                    submitProcess(shuffling);
                }
                break;
            case VERIFICATION:
                verify(shuffling);
                break;
            case BLAME:
                cancel(shuffling);
                break;
            case DONE:
            case CANCELLED:
                scheduleExpiration(shuffling);
                break;
            default:
                throw new RuntimeException("Unsupported shuffling stage " + shuffling.getStage());
        }
    }

    private void verify(Shuffling shuffling) {
        if (accountId != shuffling.getLastParticipant().getAccountId()) {
            boolean found = false;
            for (byte[] key : shuffling.getRecipientPublicKeys()) {
                if (Arrays.equals(key, recipientPublicKey)) {
                    found = true;
                    break;
                }
            }
            if (found) {
                submitVerify(shuffling);
            } else {
                submitCancel(shuffling);
            }
        }
    }

    private void cancel(Shuffling shuffling) {
        if (accountId == shuffling.getLastParticipant().getAccountId()) {
            return;
        }
        submitCancel(shuffling);
    }

    private void submitRegister(Shuffling shuffling) {
        Attachment.ShufflingRegistration attachment = new Attachment.ShufflingRegistration(shuffling.getId(), shufflingFullHash);
        submitTransaction(attachment);
    }

    private void submitProcess(Shuffling shuffling) {
        Attachment.ShufflingAttachment attachment = shuffling.process(accountId, secretPhrase, recipientPublicKey);
        submitTransaction(attachment);
    }

    private void submitVerify(Shuffling shuffling) {
        Attachment.ShufflingVerification attachment = new Attachment.ShufflingVerification(shuffling.getId(), shuffling.getStateHash());
        submitTransaction(attachment);
    }

    private void submitCancel(Shuffling shuffling) {
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
