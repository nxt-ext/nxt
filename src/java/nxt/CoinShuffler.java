package nxt;

import nxt.crypto.EncryptedData;

public final class CoinShuffler {

    public static boolean isValid(Long shufflingId) {
        /**/ return false; // TODO: Implement!
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
