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
        REGISTRATION((byte)0, new byte[]{1,4}),
        PROCESSING((byte)1, new byte[]{2,3,4}),
        VERIFICATION((byte)2, new byte[]{3,4,5}),
        BLAME((byte)3, new byte[]{3,4}),
        CANCELLED((byte)4, new byte[]{}),
        DONE((byte)5, new byte[]{});

        private final byte code;
        private final byte[] allowedNext;

        Stage(byte code, byte[] allowedNext) {
            this.code = code;
            this.allowedNext = allowedNext;
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

        public boolean canBecome(Stage nextStage) {
            return Arrays.binarySearch(allowedNext, nextStage.code) >= 0;
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
                        if (block.getHeight() > shuffling.getCancellationHeight() && shuffling.getStage().canBecome(Stage.CANCELLED)) {
                            shuffling.cancel(block);
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

    public static int getCurrencyShufflingCount(long currencyId) {
        return shufflingTable.getCount(new DbClause.LongClause("currency_id", currencyId));
    }

    static void addShuffling(Transaction transaction, Attachment.MonetarySystemShufflingCreation attachment) {
        Shuffling shuffling = new Shuffling(transaction, attachment);
        shufflingTable.insert(shuffling);
        ShufflingParticipant.addParticipant(shuffling.getId(), transaction.getSenderId(), 0);
        listeners.notify(shuffling, Event.SHUFFLING_CREATED);
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
    private long cancellingAccountId;

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
        this.assigneeAccountId = rs.getLong("assignee_account_id");
        this.cancellingAccountId = rs.getLong("cancelling_account_id");
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO shuffling (id, currency_id, "
                + "issuer_id, amount, participant_count, cancellation_height, stage, assignee_account_id, "
                + "cancelling_account_id, height, latest) "
                + "KEY (id, height) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, this.id);
            DbUtils.setLongZeroToNull(pstmt, ++i, this.currencyId);
            pstmt.setLong(++i, this.issuerId);
            pstmt.setLong(++i, this.amount);
            pstmt.setLong(++i, this.participantCount);
            pstmt.setInt(++i, this.cancellationHeight);
            pstmt.setByte(++i, this.getStage().getCode());
            pstmt.setLong(++i, this.assigneeAccountId);
            pstmt.setLong(++i, this.cancellingAccountId);
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
        if (!this.stage.canBecome(stage)) {
            throw new IllegalStateException(String.format("Shuffling in stage %s cannot go to stage %s", this.stage, stage));
        }
        this.stage = stage;
    }

    public long getAssigneeAccountId() {
        return assigneeAccountId;
    }

    public long getCancellingAccountId() {
        return cancellingAccountId;
    }

    void cancelBy(ShufflingParticipant participant, byte[][] keySeeds) {
        participant.setKeySeeds(keySeeds);
        setStage(Stage.BLAME);
        if (this.cancellingAccountId == 0) {
            this.cancellingAccountId = participant.getAccountId();
        }
        shufflingTable.insert(this);
    }

    private byte[] getNonce() {
        byte[] nonce = new byte[8];
        for (int i = 0; i < 8; i++) {
            nonce[i] = (byte)(id >> (8 * i));
        }
        return nonce;
    }

    public Attachment.MonetarySystemShufflingProcessing process(final long accountId, final String secretPhrase, final byte[] recipientPublicKey) {
        byte[][] data = Convert.EMPTY_BYTES;
        byte[] previousDataTransactionFullHash = Convert.EMPTY_BYTE;
        List<ShufflingParticipant> shufflingParticipants = new ArrayList<>();
        Nxt.getBlockchain().readLock();
        // Read the participant list for the shuffling
        try (DbIterator<ShufflingParticipant> participants = ShufflingParticipant.getParticipants(id)) {
            for (ShufflingParticipant participant : participants) {
                shufflingParticipants.add(participant);
                if (participant.getNextAccountId() == accountId) {
                    data = participant.getData();
                    previousDataTransactionFullHash = participant.getDataTransactionFullHash();
                }
            }
        } finally {
            Nxt.getBlockchain().readUnlock();
        }
        // decrypt the tokens bundled in the current data
        List<byte[]> outputDataList = new ArrayList<>();
        for (byte[] bytes : data) {
            AnonymouslyEncryptedData encryptedData = AnonymouslyEncryptedData.readEncryptedData(bytes);
            try {
                outputDataList.add(encryptedData.decrypt(secretPhrase));
            } catch (Exception e) {
                Logger.logMessage("Decryption failed", e);
                // skip data that fails to decrypt, which will result in output list shorter than expected
            }
        }
        // Calculate the token for the current sender by iteratively encrypting it using the public key of all the participants
        // which did not perform shuffle processing yet
        // If we are that last participant to process then we do not encrypt our recipient
        byte[] bytesToEncrypt = recipientPublicKey;
        byte[] nonce = getNonce();
        for (int i = shufflingParticipants.size() - 1; i >= 0; i--) {
            ShufflingParticipant participant = shufflingParticipants.get(i);
            if (participant.getAccountId() == accountId) {
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
        return new Attachment.MonetarySystemShufflingProcessing(this.id, outputDataList.toArray(new byte[outputDataList.size()][]),
                previousDataTransactionFullHash);
    }

    public Attachment.MonetarySystemShufflingCancellation revealKeySeeds(final String secretPhrase, long cancellingAccountId) {
        Nxt.getBlockchain().readLock();
        try (DbIterator<ShufflingParticipant> participants = ShufflingParticipant.getParticipants(id)) {
            long accountId = Account.getId(Crypto.getPublicKey(secretPhrase));
            byte[][] data = null;
            byte[] dataTransactionFullHash = null;
            while (participants.hasNext()) {
                ShufflingParticipant participant = participants.next();
                if (participant.getAccountId() == accountId) {
                    data = participant.getData();
                    dataTransactionFullHash = participant.getDataTransactionFullHash();
                    break;
                }
            }
            if (!participants.hasNext()) {
                return new Attachment.MonetarySystemShufflingCancellation(this.id, Convert.EMPTY_BYTES, dataTransactionFullHash,
                        cancellingAccountId);
            }
            if (data == null) {
                throw new RuntimeException("Account " + Long.toUnsignedString(accountId) + " is not a participant");
            }
            final byte[] nonce = getNonce();
            final List<byte[]> keySeeds = new ArrayList<>();
            byte[] nextParticipantPublicKey = Account.getPublicKey(participants.next().getAccountId());
            byte[] keySeed = Crypto.getKeySeed(secretPhrase, nextParticipantPublicKey, nonce);
            keySeeds.add(keySeed);
            byte[] publicKey = Crypto.getPublicKey(keySeed);
            byte[] decryptedBytes = null;
            // find the data that we encrypted
            for (byte[] bytes : data) {
                AnonymouslyEncryptedData encryptedData = AnonymouslyEncryptedData.readEncryptedData(bytes);
                if (Arrays.equals(encryptedData.getPublicKey(), publicKey)) {
                    decryptedBytes = encryptedData.decrypt(keySeed, nextParticipantPublicKey);
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
                decryptedBytes = encryptedData.decrypt(keySeed, nextParticipantPublicKey);
            }
            return new Attachment.MonetarySystemShufflingCancellation(this.id, keySeeds.toArray(new byte[keySeeds.size()][]),
                    dataTransactionFullHash, cancellingAccountId);
        } finally {
            Nxt.getBlockchain().readUnlock();
        }
    }

    private ShufflingParticipant blame() {
        List<ShufflingParticipant> participants = new ArrayList<>();
        try (DbIterator<ShufflingParticipant> iterator = ShufflingParticipant.getParticipants(this.id)) {
            while (iterator.hasNext()) {
                participants.add(iterator.next());
            }
        }
        if (cancellingAccountId == 0) {
            // if no one submitted cancellation, blame the first one that did not submit processing data
            for (ShufflingParticipant participant : participants) {
                if (participant.getData().length == 0) {
                    return participant;
                }
            }
            // or the first one who did not submit verification
            for (ShufflingParticipant participant : participants) {
                if (participant.getState() != ShufflingParticipant.State.VERIFIED) {
                    return participant;
                }
            }
            throw new RuntimeException("All participants submitted data and verifications, blame phase should not have been entered");
        }
        // start from issuer and verify all data up
        for (ShufflingParticipant participant : participants) {
            byte[][] keySeeds = participant.getKeySeeds();
            // if participant couldn't submit key seeds because he also couldn't decrypt some of the previous data, this should have been caught before
            if (keySeeds.length == 0) {
                return participant;
            }
            byte[] publicKey = Crypto.getPublicKey(keySeeds[0]);
            AnonymouslyEncryptedData encryptedData = null;
            for (byte[] bytes : participant.getData()) {
                encryptedData = AnonymouslyEncryptedData.readEncryptedData(bytes);
                if (Arrays.equals(publicKey, encryptedData.getPublicKey())) {
                    // found the data that this participant encrypted
                    break;
                }
            }
            if (encryptedData == null || !Arrays.equals(publicKey, encryptedData.getPublicKey())) {
                // participant lied about key seeds or data
                return participant;
            }
            for (int i = participant.getIndex() + 1; i < participantCount; i++) {
                ShufflingParticipant nextParticipant = participants.get(i);
                byte[] nextParticipantPublicKey = Account.getPublicKey(nextParticipant.getAccountId());
                byte[] keySeed = keySeeds[i - participant.getIndex() - 1];
                byte[] participantBytes;
                try {
                    participantBytes = encryptedData.decrypt(keySeed, nextParticipantPublicKey);
                } catch (Exception e) {
                    // the next participant couldn't decrypt the data either, blame this one
                    return participant;
                }
                boolean found = false;
                for (byte[] bytes : nextParticipant.getData()) {
                    if (Arrays.equals(participantBytes, bytes)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    // the next participant did not include this participant's data
                    return nextParticipant;
                }
                if (i < participantCount - 1) {
                    encryptedData = AnonymouslyEncryptedData.readEncryptedData(participantBytes);
                } else {
                    if (participantBytes.length != 32) {
                        // cannot be a valid public key
                        return participant;
                    }
                    // else it is not encrypted data but plaintext recipient public key, at the last participant
                    // check for collisions and assume they are intentional (?)
                    byte[] currentPublicKey = Account.getPublicKey(Account.getId(participantBytes));
                    if (currentPublicKey != null && !Arrays.equals(currentPublicKey, participantBytes)) {
                        return participant;
                    }
                }
            }
        }
        // blame the participant that cancelled without reason
        for (ShufflingParticipant participant : participants) {
            if (participant.getAccountId() == cancellingAccountId) {
                return participant;
            }
        }
        throw new RuntimeException("Couldn't find a participant to blame"); // shouldn't happen
    }

    void addParticipant(long participantId) {
        // Update the shuffling assignee to point to the new participant and update the next pointer of the existing participant
        // to the new participant
        ShufflingParticipant lastParticipant = ShufflingParticipant.getParticipant(this.id, this.assigneeAccountId);
        lastParticipant.setNextAccountId(participantId);
        ShufflingParticipant participant = ShufflingParticipant.addParticipant(this.id, participantId, lastParticipant.getIndex() + 1);

        // Check if participant registration is complete and if so update the shuffling
        if (participant.getIndex() == this.participantCount - 1) {
            this.assigneeAccountId = this.issuerId;
            setStage(Stage.PROCESSING);
        } else {
            this.assigneeAccountId = participantId;
        }
        shufflingTable.insert(this);
    }

    void updateParticipantData(Transaction transaction, Attachment.MonetarySystemShufflingProcessing attachment) {
        long participantId = transaction.getSenderId();
        byte[][] data = attachment.getData();
        ShufflingParticipant participant = ShufflingParticipant.getParticipant(this.id, participantId);
        participant.setDataTransactionFullHash(((TransactionImpl) transaction).fullHash());
        participant.setData(data);
        if (data.length < participant.getIndex() + 1) {
            // couldn't decrypt all data from previous participants
            cancelBy(participant, Convert.EMPTY_BYTES);
            return;
        }
        long nextParticipantId = participant.getNextAccountId();
        if (nextParticipantId == 0) {
            // last participant announces all valid recipient public keys
            for (byte[] recipientPublicKey : data) {
                if (recipientPublicKey.length == 32) {
                    long recipientId = Account.getId(recipientPublicKey);
                    if (Account.setOrVerify(recipientId, recipientPublicKey)) {
                        Account.addOrGetAccount(recipientId).apply(recipientPublicKey);
                    }
                }
            }
            nextParticipantId = this.issuerId;
            setStage(Stage.VERIFICATION);
        }
        this.assigneeAccountId = nextParticipantId;
        shufflingTable.insert(this);
    }

    void verify(long accountId) {
        ShufflingParticipant.getParticipant(id, accountId).verify();
        if (ShufflingParticipant.getVerifiedCount(id) == participantCount) {
            distribute();
        }
    }

    private void distribute() {
        byte[][] recipientPublicKeys = getRecipientPublicKeys();
        for (byte[] recipientPublicKey : recipientPublicKeys) {
            byte[] publicKey = Account.getPublicKey(Account.getId(recipientPublicKey));
            if (publicKey != null && !Arrays.equals(publicKey, recipientPublicKey)) {
                // distribution not possible, do a cancellation on behalf of last participant instead
                ShufflingParticipant lastParticipant = ShufflingParticipant.getLastParticipant(this.id);
                cancelBy(lastParticipant, Convert.EMPTY_BYTES);
                return;
            }
        }
        try (DbIterator<ShufflingParticipant> participants = ShufflingParticipant.getParticipants(id)) {
            for (ShufflingParticipant participant : participants) {
                Account participantAccount = Account.getAccount(participant.getAccountId());
                if (isCurrency()) {
                    participantAccount.addToCurrencyUnits(AccountLedger.LedgerEvent.CURRENCY_SHUFFLING, this.id, currencyId, -amount);
                    participantAccount.addToBalanceNQT(AccountLedger.LedgerEvent.CURRENCY_SHUFFLING, this.id, -Constants.SHUFFLE_DEPOSIT_NQT);
                } else {
                    participantAccount.addToBalanceNQT(AccountLedger.LedgerEvent.CURRENCY_SHUFFLING, this.id, -amount);
                }
            }
        }
        for (byte[] recipientPublicKey : recipientPublicKeys) {
            long recipientId = Account.getId(recipientPublicKey);
            Account recipientAccount = Account.addOrGetAccount(recipientId);
            recipientAccount.apply(recipientPublicKey);
            if (isCurrency()) {
                recipientAccount.addToCurrencyAndUnconfirmedCurrencyUnits(AccountLedger.LedgerEvent.CURRENCY_SHUFFLING, this.id, currencyId, amount);
                recipientAccount.addToBalanceAndUnconfirmedBalanceNQT(AccountLedger.LedgerEvent.CURRENCY_SHUFFLING, this.id, Constants.SHUFFLE_DEPOSIT_NQT);
            } else {
                recipientAccount.addToBalanceAndUnconfirmedBalanceNQT(AccountLedger.LedgerEvent.CURRENCY_SHUFFLING, this.id, amount);
            }
        }
        setStage(Stage.DONE);
        shufflingTable.insert(this);
        listeners.notify(this, Event.SHUFFLING_DONE);
    }

    private void cancel(Block block) {
        AccountLedger.LedgerEvent event = AccountLedger.LedgerEvent.CURRENCY_SHUFFLING;
        long eventId = block.getId();
        ShufflingParticipant blamed = blame();
        long blamedAccountId = blamed.getAccountId();
        try (DbIterator<ShufflingParticipant> participants = ShufflingParticipant.getParticipants(id)) {
            for (ShufflingParticipant participant : participants) {
                Account participantAccount = Account.getAccount(participant.getAccountId());
                if (isCurrency()) {
                    participantAccount.addToUnconfirmedCurrencyUnits(event, eventId, currencyId, amount);
                    if (participantAccount.getId() != blamedAccountId) {
                        participantAccount.addToUnconfirmedBalanceNQT(event, eventId, Constants.SHUFFLE_DEPOSIT_NQT);
                    } else {
                        // as a penalty the deposit goes to the generator of the finish block
                        participantAccount.addToBalanceNQT(event, eventId, -Constants.SHUFFLE_DEPOSIT_NQT);
                        Account.getAccount(block.getGeneratorId()).addToBalanceAndUnconfirmedBalanceNQT(event, eventId, Constants.SHUFFLE_DEPOSIT_NQT);
                    }
                } else {
                    if (participantAccount.getId() != blamedAccountId) {
                        participantAccount.addToUnconfirmedBalanceNQT(event, eventId, amount);
                    } else {
                        participantAccount.addToUnconfirmedBalanceNQT(event, eventId, amount - Constants.SHUFFLE_DEPOSIT_NQT);
                        participantAccount.addToBalanceNQT(event, eventId, -Constants.SHUFFLE_DEPOSIT_NQT);
                        Account.getAccount(block.getGeneratorId()).addToBalanceAndUnconfirmedBalanceNQT(event, eventId, Constants.SHUFFLE_DEPOSIT_NQT);
                    }
                }
            }
        }
        setStage(Stage.CANCELLED);
        shufflingTable.insert(this);
        listeners.notify(this, Event.SHUFFLING_CANCELLED);
    }

    public ShufflingParticipant getParticipant(long accountId) {
        return ShufflingParticipant.getParticipant(id, accountId);
    }

    public ShufflingParticipant getLastParticipant() {
        return ShufflingParticipant.getLastParticipant(id);
    }

    public byte[][] getRecipientPublicKeys() {
        return ShufflingParticipant.getLastParticipant(id).getData();
    }

    void delete() {
        try (DbIterator<ShufflingParticipant> participants = ShufflingParticipant.getParticipants(id)) {
            for (ShufflingParticipant participant : participants) {
                participant.delete();
            }
        }
        shufflingTable.delete(this);
    }

}
