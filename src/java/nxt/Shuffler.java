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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Shuffler {

    private static final Map<String, Map<Long, Shuffler>> shufflingsMap = Collections.synchronizedMap(new HashMap<>());
    private static final Map<Integer, Set<String>> expirations = new HashMap<>();

    public static Shuffler addOrGetShuffler(String secretPhrase, byte[] recipientPublicKey, byte[] shufflingFullHash) {
        String hash = Convert.toHexString(shufflingFullHash);
        long accountId = Account.getId(Crypto.getPublicKey(secretPhrase));
        synchronized (shufflingsMap) {
            Map<Long, Shuffler> map = shufflingsMap.get(hash);
            if (map == null) {
                map = new HashMap<>();
                shufflingsMap.put(hash, map);
            }
            Shuffler shuffler = map.get(accountId);
            if (recipientPublicKey == null) {
                return shuffler;
            }
            if (shuffler == null) {
                Shuffling shuffling = Shuffling.getShuffling(shufflingFullHash);
                shuffler = new Shuffler(secretPhrase, recipientPublicKey, shufflingFullHash);
                map.put(accountId, shuffler);
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

    public static List<Shuffler> getAllShufflers() {
        List<Shuffler> shufflers = new ArrayList<>();
        synchronized (shufflingsMap) {
            shufflingsMap.values().forEach(shufflerMap -> shufflers.addAll(shufflerMap.values()));
        }
        return shufflers;
    }

    public static List<Shuffler> getShufflingShufflers(byte[] shufflingFullHash) {
        List<Shuffler> shufflers = new ArrayList<>();
        Map<Long, Shuffler> shufflerMap = shufflingsMap.get(Convert.toHexString(shufflingFullHash));
        if (shufflerMap != null) {
            synchronized (shufflingsMap) {
                shufflers.addAll(shufflerMap.values());
            }
        }
        return shufflers;
    }

    public static List<Shuffler> getAccountShufflers(long accountId) {
        List<Shuffler> shufflers = new ArrayList<>();
        synchronized (shufflingsMap) {
            shufflingsMap.values().forEach(shufflerMap -> {
                Shuffler shuffler = shufflerMap.get(accountId);
                if (shuffler != null) {
                    shufflers.add(shuffler);
                }
            });
        }
        return shufflers;
    }

    public static Shuffler getShuffler(long accountId, byte[] shufflingFullHash) {
        Map<Long, Shuffler> shufflerMap = shufflingsMap.get(Convert.toHexString(shufflingFullHash));
        if (shufflerMap != null) {
            synchronized (shufflingsMap) {
                return shufflerMap.get(accountId);
            }
        }
        return null;
    }

    public static Shuffler stopShuffler(long accountId, byte[] shufflingFullHash) {
        Map<Long, Shuffler> shufflerMap = shufflingsMap.get(Convert.toHexString(shufflingFullHash));
        if (shufflerMap != null) {
            synchronized (shufflingsMap) {
                return shufflerMap.remove(accountId);
            }
        }
        return null;
    }

    public static void stopAllShufflers() {
        shufflingsMap.clear();
    }

    static {

        Shuffling.addListener(shuffling -> {
            Map<Long, Shuffler> shufflerMap = getShufflers(shuffling);
            if (shufflerMap != null) {
                synchronized (shufflingsMap) {
                    shufflerMap.values().forEach(shuffler -> {
                        if (shuffler.accountId != shuffling.getIssuerId()) {
                            try {
                                shuffler.submitRegister(shuffling);
                            } catch (RuntimeException e) {
                                Logger.logErrorMessage(e.toString(), e);
                            }
                        }
                    });
                    clearExpiration(shuffling);
                }
            }
        }, Shuffling.Event.SHUFFLING_CREATED);

        Shuffling.addListener(shuffling -> {
            Map<Long, Shuffler> shufflerMap = getShufflers(shuffling);
            if (shufflerMap != null) {
                synchronized (shufflingsMap) {
                    Shuffler shuffler = shufflerMap.get(shuffling.getAssigneeAccountId());
                    if (shuffler != null) {
                        try {
                            shuffler.submitProcess(shuffling);
                        } catch (RuntimeException e) {
                            Logger.logErrorMessage(e.toString(), e);
                        }
                    }
                    clearExpiration(shuffling);
                }
            }
        }, Shuffling.Event.SHUFFLING_ASSIGNED);

        Shuffling.addListener(shuffling -> {
            Map<Long, Shuffler> shufflerMap = getShufflers(shuffling);
            if (shufflerMap != null) {
                synchronized (shufflingsMap) {
                    shufflerMap.values().forEach(shuffler -> {
                        try {
                            shuffler.verify(shuffling);
                        } catch (RuntimeException e) {
                            Logger.logErrorMessage(e.toString(), e);
                        }
                    });
                    clearExpiration(shuffling);
                }
            }
        }, Shuffling.Event.SHUFFLING_PROCESSING_FINISHED);

        Shuffling.addListener(shuffling -> {
            Map<Long, Shuffler> shufflerMap = getShufflers(shuffling);
            if (shufflerMap != null) {
                synchronized (shufflingsMap) {
                    shufflerMap.values().forEach(shuffler -> {
                        try {
                            shuffler.cancel(shuffling);
                        } catch (RuntimeException e) {
                            Logger.logErrorMessage(e.toString(), e);
                        }
                    });
                    clearExpiration(shuffling);
                }
            }
        }, Shuffling.Event.SHUFFLING_BLAME_STARTED);

        Shuffling.addListener(Shuffler::scheduleExpiration, Shuffling.Event.SHUFFLING_DONE);

        Shuffling.addListener(Shuffler::scheduleExpiration, Shuffling.Event.SHUFFLING_CANCELLED);

        BlockchainProcessorImpl.getInstance().addListener(block -> {
            synchronized (shufflingsMap) {
                Set<String> expired = expirations.get(block.getHeight());
                if (expired != null) {
                    expired.forEach(shufflingsMap::remove);
                    expirations.remove(block.getHeight());
                }
            }
        }, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
    }

    private static Map<Long, Shuffler> getShufflers(Shuffling shuffling) {
        return shufflingsMap.get(Convert.toHexString(shuffling.getFullHash()));
    }

    private static void scheduleExpiration(Shuffling shuffling) {
        int expirationHeight = Nxt.getBlockchain().getHeight() + 720;
        synchronized (shufflingsMap) {
            Set<String> shufflingIds = expirations.get(expirationHeight);
            if (shufflingIds == null) {
                shufflingIds = new HashSet<>();
                expirations.put(expirationHeight, shufflingIds);
            }
            shufflingIds.add(Convert.toHexString(shuffling.getFullHash()));
        }
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

    public long getAccountId() {
        return accountId;
    }

    public byte[] getRecipientPublicKey() {
        return recipientPublicKey;
    }

    public byte[] getShufflingFullHash() {
        return shufflingFullHash;
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
        if (shuffling.getParticipant(accountId).getIndex() != shuffling.getParticipantCount() - 1) {
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
        if (accountId == shuffling.getCancellingAccountId()) {
            return;
        }
        if (shuffling.getParticipant(accountId).getIndex() == shuffling.getParticipantCount() - 1) {
            return;
        }
        if (ShufflingParticipant.getData(shuffling.getId(), accountId) == null) {
            return;
        }
        submitCancel(shuffling);
    }

    private void submitRegister(Shuffling shuffling) {
        Logger.logDebugMessage("Account %s registering for shuffling %s", Long.toUnsignedString(accountId), Long.toUnsignedString(shuffling.getId()));
        Attachment.ShufflingRegistration attachment = new Attachment.ShufflingRegistration(shufflingFullHash);
        submitTransaction(attachment);
    }

    private void submitProcess(Shuffling shuffling) {
        Logger.logDebugMessage("Account %s processing shuffling %s", Long.toUnsignedString(accountId), Long.toUnsignedString(shuffling.getId()));
        Attachment.ShufflingAttachment attachment = shuffling.process(accountId, secretPhrase, recipientPublicKey);
        submitTransaction(attachment);
    }

    private void submitVerify(Shuffling shuffling) {
        Logger.logDebugMessage("Account %s verifying shuffling %s", Long.toUnsignedString(accountId), Long.toUnsignedString(shuffling.getId()));
        Attachment.ShufflingVerification attachment = new Attachment.ShufflingVerification(shuffling.getId(), shuffling.getStateHash());
        submitTransaction(attachment);
    }

    private void submitCancel(Shuffling shuffling) {
        Logger.logDebugMessage("Account %s cancelling shuffling %s", Long.toUnsignedString(accountId), Long.toUnsignedString(shuffling.getId()));
        Attachment.ShufflingCancellation attachment = shuffling.revealKeySeeds(secretPhrase, shuffling.getCancellingAccountId(), shuffling.getStateHash());
        submitTransaction(attachment);
    }

    private void submitTransaction(Attachment.ShufflingAttachment attachment) {
        if (hasUnconfirmedTransaction(attachment)) {
            Logger.logDebugMessage("Transaction already submitted");
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
