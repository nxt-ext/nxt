package nxt;

import nxt.crypto.EncryptedData;

import java.util.HashMap;
import java.util.Map;

public final class CoinShuffler {

    private static class Shuffling {

        private final Long currencyId;
        private final long amount;
        private final byte numberOfParticipants;
        private final short maxInitiationDelay;
        private final short maxContinuationDelay;
        private final short maxFinalizationDelay;
        private final short maxCancellationDelay;

        private final int hashCode;

        Shuffling(Long currencyId, long amount, byte numberOfParticipants, short maxInitiationDelay, short maxContinuationDelay, short maxFinalizationDelay, short maxCancellationDelay) {
            this.currencyId = currencyId;
            this.amount = amount;
            this.numberOfParticipants = numberOfParticipants;
            this.maxInitiationDelay = maxInitiationDelay;
            this.maxContinuationDelay = maxContinuationDelay;
            this.maxFinalizationDelay = maxFinalizationDelay;
            this.maxCancellationDelay = maxCancellationDelay;

            hashCode = currencyId.hashCode() ^ Long.valueOf(amount).hashCode() ^ Byte.valueOf(numberOfParticipants).hashCode() ^ Short.valueOf(maxInitiationDelay).hashCode() ^ Short.valueOf(maxContinuationDelay).hashCode() ^ Short.valueOf(maxFinalizationDelay).hashCode() ^ Short.valueOf(maxCancellationDelay).hashCode();
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            return this.currencyId.equals(((Shuffling)obj).currencyId)
                    && this.amount == ((Shuffling)obj).amount
                    && this.numberOfParticipants == ((Shuffling)obj).numberOfParticipants
                    && this.maxInitiationDelay == ((Shuffling)obj).maxInitiationDelay
                    && this.maxContinuationDelay == ((Shuffling)obj).maxContinuationDelay
                    && this.maxFinalizationDelay == ((Shuffling)obj).maxFinalizationDelay
                    && this.maxCancellationDelay == ((Shuffling)obj).maxCancellationDelay;
        }

    }

    private static final Map<Long, Shuffling> initiatedShufflings = new HashMap<>();
    private static final Map<Long, Shuffling> continuedShufflings = new HashMap<>();
    private static final Map<Long, Shuffling> finalizedShufflings = new HashMap<>();
    private static final Map<Long, Shuffling> cancelledShufflings = new HashMap<>();

    public static boolean isInitiated(Long shufflingId) {
        return initiatedShufflings.containsKey(shufflingId);
    }

    public static boolean isContinued(Long shufflingId) {
        return continuedShufflings.containsKey(shufflingId);
    }

    public static boolean isFinalized(Long shufflingId) {
        return finalizedShufflings.containsKey(shufflingId);
    }

    public static boolean isCancelled(Long shufflingId) {
        return cancelledShufflings.containsKey(shufflingId);
    }

    public static byte getNumberOfParticipants(Long shufflingId) {
        /**/ return 0; // TODO: Implement!
    }

    public static void initiateShuffling(Account account, Long currencyId, long amount, byte numberOfParticipants, short maxInitiationDelay, short maxContinuationDelay, short maxFinalizationDelay, short maxCancellationDelay) {
        // TODO: Implement!
    }

    public static void continueShuffling(Account account, Long shufflingId, EncryptedData recipients) {
        // TODO: Implement!
    }

    public static void finalizeShuffling(Account account, Long shufflingId, Long[] recipients) {
        // TODO: Implement!
    }

    public static void cancelShuffling(Account account, Long shuffling, byte[] nonce) {
        // TODO: Implement!
    }

}
