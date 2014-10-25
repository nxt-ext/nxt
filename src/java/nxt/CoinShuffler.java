package nxt;

import nxt.crypto.EncryptedData;
import nxt.util.Listener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//TODO: convert to database
public final class CoinShuffler {

    private static enum State {
        INITIATED,
        CONTINUED,
        FINALIZED,
        CANCELLED
    }

    private static class Shuffling {

        private final long currencyId;
        private final long amount;
        private final byte numberOfParticipants;
        private final short maxInitiationDelay;
        private final short maxContinuationDelay;
        private final short maxFinalizationDelay;
        private final short maxCancellationDelay;

        private final int hashCode;

        private State state;
        private int lastActionTimestamp;
        private final List<Long> participants;
        private final Map<Long, EncryptedData> encryptedRecipients;
        private final Map<Long, long[]> decryptedRecipients;
        private final Map<Long, byte[]> keys;

        Shuffling(long currencyId, long amount, byte numberOfParticipants, short maxInitiationDelay, short maxContinuationDelay, short maxFinalizationDelay, short maxCancellationDelay) {
            this.currencyId = currencyId;
            this.amount = amount;
            this.numberOfParticipants = numberOfParticipants;
            this.maxInitiationDelay = maxInitiationDelay;
            this.maxContinuationDelay = maxContinuationDelay;
            this.maxFinalizationDelay = maxFinalizationDelay;
            this.maxCancellationDelay = maxCancellationDelay;

            hashCode = (int)(currencyId ^ (currencyId >>> 32)) ^ (int)(amount ^ (amount >>> 32)) ^ (int)numberOfParticipants ^
                    (int)maxInitiationDelay ^ (int)maxContinuationDelay ^ (int)maxFinalizationDelay ^ (int)maxCancellationDelay;

            state = State.INITIATED;
            lastActionTimestamp = BlockchainImpl.getInstance().getLastBlock().getTimestamp();
            participants = new ArrayList<>(numberOfParticipants);
            encryptedRecipients = new HashMap<>();
            decryptedRecipients = new HashMap<>();
            keys = new HashMap<>();
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            return this.currencyId == ((Shuffling)obj).currencyId
                    && this.amount == ((Shuffling)obj).amount
                    && this.numberOfParticipants == ((Shuffling)obj).numberOfParticipants
                    && this.maxInitiationDelay == ((Shuffling)obj).maxInitiationDelay
                    && this.maxContinuationDelay == ((Shuffling)obj).maxContinuationDelay
                    && this.maxFinalizationDelay == ((Shuffling)obj).maxFinalizationDelay
                    && this.maxCancellationDelay == ((Shuffling)obj).maxCancellationDelay;
        }

        public void addParticipant(Long accountId) {
            participants.add(accountId);
            if (participants.size() == numberOfParticipants) {
                state = State.CONTINUED;
            }
            lastActionTimestamp = BlockchainImpl.getInstance().getLastBlock().getTimestamp();
        }

    }

    static {
        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
                for (Map.Entry<Long, Shuffling> shufflingEntry : shufflings.entrySet()) {
                    Long shufflingId = shufflingEntry.getKey();
                    Shuffling shuffling = shufflingEntry.getValue();
                    switch (shuffling.state) {
                        case INITIATED: {
                            if (block.getTimestamp() - shuffling.lastActionTimestamp > shuffling.maxInitiationDelay) {
                                for (Long accountId : shuffling.participants) {
                                    Account.getAccount(accountId).addToCurrencyAndUnconfirmedCurrencyUnits(shuffling.currencyId, shuffling.amount);
                                }
                                shufflings.remove(shufflingId);
                            }
                        } break;

                        case CONTINUED: {
                            if (block.getTimestamp() - shuffling.lastActionTimestamp > shuffling.maxContinuationDelay) {
                                boolean rogueIsPenalised = false;
                                for (Long accountId : shuffling.participants) {
                                    if (!rogueIsPenalised && shuffling.encryptedRecipients.get(accountId) == null) {
                                        rogueIsPenalised = true;
                                        continue;
                                    }
                                    Account.getAccount(accountId).addToCurrencyAndUnconfirmedCurrencyUnits(shuffling.currencyId, shuffling.amount);
                                }
                                shufflings.remove(shufflingId);
                            }
                        } break;

                        case FINALIZED: {
                            if (block.getTimestamp() - shuffling.lastActionTimestamp > shuffling.maxFinalizationDelay) {
                                boolean rogueIsPenalised = false;
                                for (Long accountId : shuffling.participants) {
                                    if (!rogueIsPenalised && shuffling.decryptedRecipients.get(accountId) == null) {
                                        rogueIsPenalised = true;
                                        continue;
                                    }
                                    Account.getAccount(accountId).addToCurrencyAndUnconfirmedCurrencyUnits(shuffling.currencyId, shuffling.amount);
                                }
                                shufflings.remove(shufflingId);
                            }
                        } break;

                        default: {
                            if (block.getTimestamp() - shuffling.lastActionTimestamp > shuffling.maxCancellationDelay) {
                                boolean rogueIsPenalised = false;
                                for (Long accountId : shuffling.participants) {
                                    if (!rogueIsPenalised && shuffling.keys.get(accountId) == null) {
                                        rogueIsPenalised = true;
                                        continue;
                                    }
                                    Account.getAccount(accountId).addToCurrencyAndUnconfirmedCurrencyUnits(shuffling.currencyId, shuffling.amount);
                                }
                                shufflings.remove(shufflingId);
                            }
                        }
                    }
                }
            }
        }, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
    }

    private static final Map<Long, Shuffling> shufflings = new HashMap<>();

    public static boolean isInitiated(Long shufflingId) {
        Shuffling shuffling = shufflings.get(shufflingId);
        return shuffling != null && shuffling.state == State.INITIATED;
    }

    public static boolean isContinued(Long shufflingId) {
        Shuffling shuffling = shufflings.get(shufflingId);
        return shuffling != null && shuffling.state == State.CONTINUED;
    }

    public static boolean isFinalized(Long shufflingId) {
        Shuffling shuffling = shufflings.get(shufflingId);
        return shuffling != null && shuffling.state == State.FINALIZED;
    }

    public static boolean isCancelled(Long shufflingId) {
        Shuffling shuffling = shufflings.get(shufflingId);
        return shuffling != null && shuffling.state == State.CANCELLED;
    }

    public static byte getNumberOfParticipants(Long shufflingId) {
        return shufflings.get(shufflingId).numberOfParticipants;
    }

    public static boolean isParticipant(Long accountId, Long shufflingId) {
        Shuffling shuffling = shufflings.get(shufflingId);
        return shuffling != null && shuffling.participants.contains(accountId);
    }

    public static boolean sentEncryptedRecipients(Long accountId, Long shufflingId) {
        return shufflings.get(shufflingId).encryptedRecipients.get(accountId) != null;
    }

    public static boolean sentDecryptedRecipients(Long accountId, Long shufflingId) {
        return shufflings.get(shufflingId).decryptedRecipients.get(accountId) != null;
    }

    public static boolean sentKeys(Long accountId, Long shufflingId) {
        return shufflings.get(shufflingId).keys.get(accountId) != null;
    }

    public static void initiateShuffling(Long transactionId, Account account, Long currencyId, long amount, byte numberOfParticipants, short maxInitiationDelay, short maxContinuationDelay, short maxFinalizationDelay, short maxCancellationDelay) {
        account.addToCurrencyUnits(currencyId, -amount);

        Shuffling newShuffling = new Shuffling(currencyId, amount, numberOfParticipants, maxInitiationDelay, maxContinuationDelay, maxFinalizationDelay, maxCancellationDelay);

        for (Shuffling existingShuffling : shufflings.values()) {
            if (existingShuffling.state == State.INITIATED && newShuffling.equals(existingShuffling)) {
                existingShuffling.addParticipant(account.getId());
                return;
            }
        }

        shufflings.put(transactionId, newShuffling);
    }

    public static void continueShuffling(Account account, Long shufflingId, EncryptedData recipients) {
        Shuffling shuffling = shufflings.get(shufflingId);
        shuffling.encryptedRecipients.put(account.getId(), recipients);
        if (shuffling.encryptedRecipients.size() == shuffling.numberOfParticipants) {
            shuffling.state = State.FINALIZED;
        }
        shuffling.lastActionTimestamp = BlockchainImpl.getInstance().getLastBlock().getTimestamp();
    }

    public static void finalizeShuffling(Account account, Long shufflingId, long[] recipients) {
        Shuffling shuffling = shufflings.get(shufflingId);
        if (shuffling.decryptedRecipients.size() > 0 && !Arrays.equals(recipients, shuffling.decryptedRecipients.values().toArray(new long[0][0])[0])) {
            shuffling.state = State.CANCELLED;
            shuffling.lastActionTimestamp = BlockchainImpl.getInstance().getLastBlock().getTimestamp();
            return;
        }
        shuffling.decryptedRecipients.put(account.getId(), recipients);
        if (shuffling.decryptedRecipients.size() == shuffling.numberOfParticipants) {
            for (Long recipientAccountId : shuffling.decryptedRecipients.values().toArray(new Long[0][0])[0]) {
                Account.getAccount(recipientAccountId).addToCurrencyAndUnconfirmedCurrencyUnits(shuffling.currencyId, shuffling.amount);
            }
            shufflings.remove(shufflingId);
        } else {
            shuffling.lastActionTimestamp = BlockchainImpl.getInstance().getLastBlock().getTimestamp();
        }
    }

    public static void cancelShuffling(Account account, Long shufflingId, byte[] keys) {
        Shuffling shuffling = shufflings.get(shufflingId);
        shuffling.keys.put(account.getId(), keys);
        if (shuffling.keys.size() == shuffling.numberOfParticipants) {
            // TODO: Decrypt and analyze data to find rogues
        } else {
            shuffling.state = State.CANCELLED;
            shuffling.lastActionTimestamp = BlockchainImpl.getInstance().getLastBlock().getTimestamp();
        }
    }

}
