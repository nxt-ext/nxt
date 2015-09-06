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

import nxt.crypto.AnonymouslyEncryptedData;
import nxt.crypto.Crypto;
import nxt.db.DbClause;
import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.DbUtils;
import nxt.db.VersionedEntityDbTable;
import nxt.util.Convert;
import nxt.util.Listener;
import nxt.util.Listeners;
import nxt.util.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Shuffling {

    public enum Event {
        SHUFFLING_CREATED, SHUFFLING_CANCELLED, SHUFFLING_DONE
    }

    public enum Stage {
        REGISTRATION((byte)1),
        PROCESSING((byte)2),
        VERIFICATION((byte)3),
        CANCELLED((byte)4) {
            @Override
            public boolean isCancellationAllowed() {
                return false;
            }
        },
        DONE((byte)5) {
            @Override
            public boolean isCancellationAllowed() {
                return false;
            }
        };

        private final byte code;

        Stage(byte code) {
            this.code = code;
        }

        private static Stage get(byte code) {
            for (Stage stage : Stage.values()) {
                if (stage.code == code) {
                    return stage;
                }
            }
            throw new IllegalArgumentException("No matching stage for " + code);
        }

        public byte getCode() {
            return code;
        }

        public boolean isCancellationAllowed() {
            return true;
        }
    }

    static {
        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
                //TODO: optimize, use cancellation_height and avoid table scan
                Connection con = null;
                try {
                    con = Db.db.getConnection();
                    PreparedStatement pstmt = con.prepareStatement("SELECT * FROM shuffling WHERE stage <> ? and stage <> ? AND latest = TRUE");
                    int i = 0;
                    pstmt.setByte(++i, Stage.CANCELLED.getCode());
                    pstmt.setByte(++i, Stage.DONE.getCode());
                    DbIterator<Shuffling> shufflings = shufflingTable.getManyBy(con, pstmt, false);
                    for (Shuffling shuffling : shufflings) {
                        // Cancel the shuffling in case the blockchain reached its cancellation height
                        if (block.getHeight() > shuffling.getCancellationHeight() && shuffling.getStage().isCancellationAllowed()) {
                            shuffling.cancel(AccountLedger.LedgerEvent.CURRENCY_SHUFFLING, block.getId());
                        }
                    }
                } catch (SQLException e) {
                    DbUtils.close(con);
                    throw new RuntimeException(e.toString(), e);
                }
            }
        }, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
    }

    private static final Listeners<Shuffling, Event> listeners = new Listeners<>();

    private static final DbKey.LongKeyFactory<Shuffling> shufflingDbKeyFactory = new DbKey.LongKeyFactory<Shuffling>("id") {

        @Override
        public DbKey newKey(Shuffling transfer) {
            return transfer.dbKey;
        }

    };

    //TODO: trim completed shufflings after 1440 blocks?
    private static final VersionedEntityDbTable<Shuffling> shufflingTable = new VersionedEntityDbTable<Shuffling>("shuffling", shufflingDbKeyFactory) {

        @Override
        protected Shuffling load(Connection con, ResultSet rs) throws SQLException {
            return new Shuffling(rs);
        }

        @Override
        protected void save(Connection con, Shuffling shuffling) throws SQLException {
            shuffling.save(con);
        }

    };

    public static DbIterator<Shuffling> getAll(int from, int to) {
        return shufflingTable.getAll(from, to);
    }

    public static int getCount() {
        return shufflingTable.getCount();
    }

    public static boolean addListener(Listener<Shuffling> listener, Event eventType) {
        return listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<Shuffling> listener, Event eventType) {
        return listeners.removeListener(listener, eventType);
    }

    public static Shuffling getShuffling(long shufflingId) {
        return shufflingTable.get(shufflingDbKeyFactory.newKey(shufflingId));
    }

    static void addShuffling(Transaction transaction, Attachment.MonetarySystemShufflingCreation attachment) {
        Shuffling shuffling = new Shuffling(transaction, attachment);
        shufflingTable.insert(shuffling);
        ShufflingParticipant.addParticipant(shuffling.getId(), transaction.getSenderId());
        listeners.notify(shuffling, Event.SHUFFLING_CREATED);
    }

    static void addParticipant(Transaction transaction, Attachment.MonetarySystemShufflingRegistration attachment) {
        long shufflingId = attachment.getShufflingId();
        long participantId = transaction.getSenderId();
        ShufflingParticipant.addParticipant(shufflingId, participantId);

        // Update the shuffling assignee to point to the new participant and update the next pointer of the existing participant
        // to the new participant
        Shuffling shuffling = Shuffling.getShuffling(shufflingId);
        ShufflingParticipant participant = ShufflingParticipant.getParticipant(shufflingId, shuffling.getAssigneeAccountId());
        participant.setNextAccountId(participantId);
        shuffling.setAssigneeAccountId(participantId);

        // Check if participant registration is complete and if so update the shuffling
        if (ShufflingParticipant.getCount(shufflingId) == shuffling.participantCount) {
            shuffling.assigneeAccountId = shuffling.issuerId;
            shuffling.setStage(Stage.PROCESSING); // update the db
        }
    }

    static void init() {}

    private final long id;
    private final DbKey dbKey;
    private final long currencyId;
    private final long issuerId;
    private final long amount;
    private final byte participantCount;
    private final int cancellationHeight;

    private Stage stage;
    private long assigneeAccountId;

    private Shuffling(Transaction transaction, Attachment.MonetarySystemShufflingCreation attachment) {
        this.id = transaction.getId();
        this.dbKey = shufflingDbKeyFactory.newKey(this.id);
        this.currencyId = attachment.getCurrencyId();
        this.issuerId = transaction.getSenderId();
        this.amount = attachment.getAmount();
        this.participantCount = attachment.getParticipantCount();
        this.cancellationHeight = attachment.getCancellationHeight();
        this.stage = Stage.REGISTRATION;
        this.assigneeAccountId = issuerId;
    }

    private Shuffling(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.dbKey = shufflingDbKeyFactory.newKey(this.id);
        this.currencyId = rs.getLong("currency_id");
        this.issuerId = rs.getLong("issuer_id");
        this.amount = rs.getLong("amount");
        this.participantCount = rs.getByte("participant_count");
        this.cancellationHeight = rs.getInt("cancellation_height");
        this.stage = Stage.get(rs.getByte("stage"));
        this.assigneeAccountId = rs.getLong("assignee_account_Id");
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO shuffling (id, currency_id, "
                + "issuer_id, amount, participant_count, cancellation_height, stage, assignee_account_id, "
                + "height, latest) "
                + "KEY (id, height) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, this.id);
            DbUtils.setLongZeroToNull(pstmt, ++i, this.currencyId);
            pstmt.setLong(++i, this.issuerId);
            pstmt.setLong(++i, this.amount);
            pstmt.setLong(++i, this.participantCount);
            pstmt.setInt(++i, this.cancellationHeight);
            pstmt.setByte(++i, this.getStage().getCode());
            pstmt.setLong(++i, this.assigneeAccountId);
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }

    public long getId() {
        return id;
    }

    public boolean isCurrency() {
        return currencyId != 0;
    }

    public long getCurrencyId() { return currencyId; }

    public long getIssuerId() {
        return issuerId;
    }

    public long getAmount() {
        return amount;
    }

    public byte getParticipantCount() {
        return participantCount;
    }

    public int getCancellationHeight() {
        return cancellationHeight;
    }

    public Stage getStage() {
        return stage;
    }

    private void setStage(Stage stage) {
        this.stage = stage;
        shufflingTable.insert(this);
    }

    public long getAssigneeAccountId() {
        return assigneeAccountId;
    }

    private void setAssigneeAccountId(long assigneeAccountId) {
        this.assigneeAccountId = assigneeAccountId;
        shufflingTable.insert(this);
    }

    private byte[] getNonce() {
        byte[] nonce = new byte[8];
        for (int i = 0; i < 8; i++) {
            nonce[i] = (byte)(id >> (8 * i));
        }
        return nonce;
    }

    public byte[][] process(final long accountId, final String secretPhrase, final byte[] recipientPublicKey) {
        List<ShufflingParticipant> shufflingParticipants = new ArrayList<>();
        ShufflingParticipant thisParticipant = null;
        // Read the participant list for the shuffling
        try (DbIterator<ShufflingParticipant> participants = ShufflingParticipant.getParticipants(id)) {
            ShufflingParticipant lastParticipant = null;
            for (ShufflingParticipant participant : participants) {
                if (lastParticipant == null && participant.getAccountId() != issuerId) {
                    throw new RuntimeException("Issuer is not the first participant");
                } else if (lastParticipant != null && lastParticipant.getNextAccountId() != participant.getAccountId()) {
                    throw new RuntimeException(String.format("Shuffling participants out of order, %s, %s",
                            lastParticipant.getNextAccountId(), participant.getAccountId()));
                }
                shufflingParticipants.add(participant);
                lastParticipant = participant;
                if (participant.getAccountId() == accountId) {
                    thisParticipant = participant;
                }
            }
        }
        if (thisParticipant == null) {
            throw new RuntimeException("Account " + Long.toUnsignedString(accountId) + " is not a participant in " + Long.toUnsignedString(id));
        }
        // Read the encrypted participant data for the sender account token (the first sender won't have any data)
        byte[][] data = thisParticipant.getData();

        // decrypt the tokens bundled in the current data
        List<byte[]> outputDataList = new ArrayList<>();
        for (byte[] bytes : data) {
            AnonymouslyEncryptedData encryptedData = AnonymouslyEncryptedData.readEncryptedData(bytes);
            try {
                outputDataList.add(encryptedData.decrypt(secretPhrase));
            } catch (Exception e) {
                Logger.logMessage("Decryption failed", e);
                //decryption failed, need to cancel and enter blame phase
                return null;
            }
        }

        // Calculate the token for the current sender by iteratively encrypting it using the public key of all the participants
        // which did not perform shuffle processing yet
        // If we are that last participant to process then we do not encrypt our recipient
        byte[] bytesToEncrypt = recipientPublicKey;
        byte[] nonce = getNonce();
        for (int i = shufflingParticipants.size() - 1; i >= 0; i--) {
            ShufflingParticipant participant = shufflingParticipants.get(i);
            if (participant == thisParticipant) {
                break;
            }
            byte[] participantPublicKey = Account.getPublicKey(participant.getAccountId());
            if (Logger.isDebugEnabled()) {
                Logger.logDebugMessage(String.format("encryptTo %s by %s bytes %s",
                        Convert.rsAccount(participant.getAccountId()), Convert.rsAccount(accountId), Arrays.toString(bytesToEncrypt)));
            }
            AnonymouslyEncryptedData encryptedData = AnonymouslyEncryptedData.encrypt(bytesToEncrypt, secretPhrase, participantPublicKey, nonce);
            bytesToEncrypt = encryptedData.getBytes();
            if (Logger.isDebugEnabled()) {
                Logger.logDebugMessage(String.format("encryptTo data %s publicKey %s",
                        Arrays.toString(encryptedData.getData()), Arrays.toString(encryptedData.getPublicKey())));
            }
        }
        outputDataList.add(bytesToEncrypt);

        // Shuffle the tokens and save the shuffled tokens as the participant data
        Collections.shuffle(outputDataList, Crypto.getSecureRandom());
        return outputDataList.toArray(new byte[outputDataList.size()][]);
    }

    public byte[][] revealKeySeeds(final String secretPhrase) {
        long accountId = Account.getId(Crypto.getPublicKey(secretPhrase));
        try (DbIterator<ShufflingParticipant> participants = ShufflingParticipant.getParticipants(id)) {
            while (participants.hasNext()) {
                if (participants.next().getAccountId() == accountId) {
                    break;
                }
            }
            if (!participants.hasNext()) {
                return Convert.EMPTY_BYTES;
            }
            ShufflingParticipant nextParticipant = participants.next();
            byte[][] data = nextParticipant.getData();
            final byte[] nonce = getNonce();
            final List<byte[]> keySeeds = new ArrayList<>();
            byte[] nextParticipantPublicKey = Account.getPublicKey(nextParticipant.getAccountId());
            byte[] keySeed = Crypto.getKeySeed(secretPhrase, nextParticipantPublicKey, nonce);
            keySeeds.add(keySeed);
            byte[] publicKey = Crypto.getPublicKey(keySeed);
            byte[] decryptedBytes = null;
            // find the data that we encrypted
            for (byte[] bytes : data) {
                AnonymouslyEncryptedData encryptedData = AnonymouslyEncryptedData.readEncryptedData(bytes);
                if (Arrays.equals(encryptedData.getPublicKey(), publicKey)) {
                    decryptedBytes = encryptedData.decrypt(keySeed);
                    break;
                }
            }
            if (decryptedBytes == null) {
                throw new RuntimeException("None of the encrypted data could be decrypted");
            }
            // decrypt all iteratively, adding the key seeds to the result
            while (participants.hasNext()) {
                nextParticipantPublicKey = Account.getPublicKey(participants.next().getAccountId());
                keySeed = Crypto.getKeySeed(secretPhrase, nextParticipantPublicKey, nonce);
                keySeeds.add(keySeed);
                AnonymouslyEncryptedData encryptedData = AnonymouslyEncryptedData.readEncryptedData(decryptedBytes);
                decryptedBytes = encryptedData.decrypt(keySeed);
            }
            return keySeeds.toArray(new byte[keySeeds.size()][]);
        }
    }

    void updateParticipantData(Transaction transaction, Attachment.MonetarySystemShufflingProcessing attachment) {
        long participantId = transaction.getSenderId();
        byte[][] data = attachment.getData();
        ShufflingParticipant senderParticipant = ShufflingParticipant.getParticipant(this.id, participantId);
        long nextParticipantId = senderParticipant.getNextAccountId();
        if (nextParticipantId == 0) {
            // store recipient public keys in the data field of the shuffle issuer
            nextParticipantId = this.issuerId;
            setStage(Stage.VERIFICATION);
        }
        setAssigneeAccountId(nextParticipantId);
        ShufflingParticipant.updateData(this.id, nextParticipantId, data);
        senderParticipant.setProcessingComplete();
    }

    void verify(long accountId) {
        ShufflingParticipant.getParticipant(id, accountId).verify();
    }

    void distribute(AccountLedger.LedgerEvent event, long eventId) {
        byte[][] recipientPublicKeys = getRecipientPublicKeys();
        for (byte[] recipientPublicKey : recipientPublicKeys) {
            Account recipientAccount = Account.getAccount(Account.getId(recipientPublicKey));
            if (recipientAccount != null && !Arrays.equals(recipientAccount.getPublicKey(), recipientPublicKey)) {
                //TODO: penalty?
                cancel(event, eventId);
                return;
            }
        }
        try (DbIterator<ShufflingParticipant> participants = ShufflingParticipant.getParticipants(id)) {
            for (ShufflingParticipant participant : participants) {
                Account participantAccount = Account.getAccount(participant.getAccountId());
                if (isCurrency()) {
                    participantAccount.addToCurrencyUnits(event, eventId, currencyId, -amount);
                    participantAccount.addToBalanceNQT(event, eventId, -Constants.SHUFFLE_DEPOSIT_NQT);
                } else {
                    participantAccount.addToBalanceNQT(event, eventId, -amount);
                }
            }
        }
        for (byte[] recipientPublicKey : recipientPublicKeys) {
            long recipientId = Account.getId(recipientPublicKey);
            Account recipientAccount = Account.addOrGetAccount(recipientId);
            recipientAccount.apply(recipientPublicKey);
            if (isCurrency()) {
                recipientAccount.addToCurrencyAndUnconfirmedCurrencyUnits(event, eventId, currencyId, amount);
                recipientAccount.addToBalanceAndUnconfirmedBalanceNQT(event, eventId, Constants.SHUFFLE_DEPOSIT_NQT);
            } else {
                recipientAccount.addToBalanceAndUnconfirmedBalanceNQT(event, eventId, amount);
            }
        }
        setStage(Stage.DONE);
        listeners.notify(this, Event.SHUFFLING_DONE);
    }

    //TODO: find out which of the participants are at fault, after all reveal their keys
    void cancel(AccountLedger.LedgerEvent event, long eventId) {
        try (DbIterator<ShufflingParticipant> participants = ShufflingParticipant.getParticipants(id)) {
            for (ShufflingParticipant participant : participants) {
                Account participantAccount = Account.getAccount(participant.getAccountId());
                if (isCurrency()) {
                    participantAccount.addToUnconfirmedCurrencyUnits(event, eventId, currencyId, amount);
                    participantAccount.addToUnconfirmedBalanceNQT(event, eventId, Constants.SHUFFLE_DEPOSIT_NQT);
                } else {
                    participantAccount.addToUnconfirmedBalanceNQT(event, eventId, amount);
                }
            }
        }
        setStage(Stage.CANCELLED);
        listeners.notify(this, Event.SHUFFLING_CANCELLED);
    }

    boolean isRegistrationAllowed() {
        return stage == Stage.REGISTRATION;
    }

    public boolean isProcessingAllowed() {
        return stage == Stage.PROCESSING;
    }

    boolean isVerificationAllowed() {
        return stage == Stage.VERIFICATION;
    }

    /**
     * Distribution is allowed only if the shuffling is in verification stage and all participants verified their recipient account
     * @return is distribution allowed
     */
    boolean isDistributionAllowed() {
        if (!isVerificationAllowed()) {
            return false;
        }
        try (DbIterator<ShufflingParticipant> participants = ShufflingParticipant.getParticipants(id)) {
            for (ShufflingParticipant participant : participants) {
                if (!participant.isVerified()) {
                    return false;
                }
            }
        }
        return true;
    }

    //TODO: a participant may have to cancel if unable to decrypt the data sent by the previous participants, in process stage
    /**
     * Shuffling issuer can cancel the shuffling at any time but participants can only cancel during the verification stage
     *
     * @param senderId the sender of the transaction
     * @return is cancellation allowed
     */
    boolean isCancellationAllowed(long senderId) {
        if (senderId == issuerId) {
            return stage.isCancellationAllowed();
        } else {
            return stage == Stage.VERIFICATION || stage == Stage.CANCELLED;
            // if cancel transaction is also used to reveal keys, multiple cancels should be allowed
        }
    }

    public boolean isParticipant(long senderId) {
        return ShufflingParticipant.getParticipant(id, senderId) != null;
    }

    boolean isParticipantVerified(long senderId) {
        return ShufflingParticipant.getParticipant(id, senderId).isVerified();
    }

    public boolean isParticipantProcessingComplete(long senderId) {
        return ShufflingParticipant.getParticipant(id, senderId).isProcessingComplete();
    }

    public byte[][] getRecipientPublicKeys() {
        return ShufflingParticipant.getParticipant(id, issuerId).getData();
    }

    static void cancelShuffling(AccountLedger.LedgerEvent event, long eventId, long currencyId) {
        try (DbIterator<Shuffling> shufflings = shufflingTable.getManyBy(new DbClause.LongClause("currency_id", currencyId), 0, -1)) {
            for (Shuffling shuffling : shufflings) {
                if (shuffling.getStage().isCancellationAllowed()) {
                    shuffling.cancel(event, eventId);
                } //TODO: else? prevent deletion of currencies with shuffling in non-cancelable state?
            }
        }
    }
}
