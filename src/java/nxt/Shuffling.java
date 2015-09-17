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

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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

    private static final Listeners<Shuffling, Event> listeners = new Listeners<>();

    private static final DbKey.LongKeyFactory<Shuffling> shufflingDbKeyFactory = new DbKey.LongKeyFactory<Shuffling>("id") {

        @Override
        public DbKey newKey(Shuffling transfer) {
            return transfer.dbKey;
        }

    };

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

    static {
        Nxt.getBlockchainProcessor().addListener(block -> {
            if (block.getTransactions().size() == Constants.MAX_NUMBER_OF_TRANSACTIONS) {
                return;
            }
            List<Shuffling> shufflings = new ArrayList<>();
            try (DbIterator<Shuffling> iterator = shufflingTable.getAll(0, -1)) {
                for (Shuffling shuffling : iterator) {
                    if (!shuffling.isFull(block)) {
                        shufflings.add(shuffling);
                    }
                }
            }
            shufflings.forEach(shuffling -> {
                if (--shuffling.blocksRemaining <= 0) {
                    shuffling.cancel(block);
                } else {
                    shufflingTable.insert(shuffling);
                }
            });
        }, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
    }

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

    public static int getHoldingShufflingCount(long holdingId) {
        return shufflingTable.getCount(new DbClause.LongClause("holding_id", holdingId));
    }

    static void addShuffling(Transaction transaction, Attachment.ShufflingCreation attachment) {
        Shuffling shuffling = new Shuffling(transaction, attachment);
        shufflingTable.insert(shuffling);
        ShufflingParticipant.addParticipant(shuffling.getId(), transaction.getSenderId(), 0);
        listeners.notify(shuffling, Event.SHUFFLING_CREATED);
    }

    static void init() {}

    private final long id;
    private final DbKey dbKey;
    private final long holdingId;
    private final HoldingType holdingType;
    private final long issuerId;
    private final long amount;
    private final byte participantCount;
    private short blocksRemaining;

    private Stage stage;
    private long assigneeAccountId;
    private long cancellingAccountId;
    private byte[][] recipientPublicKeys;

    private Shuffling(Transaction transaction, Attachment.ShufflingCreation attachment) {
        this.id = transaction.getId();
        this.dbKey = shufflingDbKeyFactory.newKey(this.id);
        this.holdingId = attachment.getHoldingId();
        this.holdingType = attachment.getHoldingType();
        this.issuerId = transaction.getSenderId();
        this.amount = attachment.getAmount();
        this.participantCount = attachment.getParticipantCount();
        this.blocksRemaining = attachment.getRegistrationPeriod();
        this.stage = Stage.REGISTRATION;
        this.assigneeAccountId = issuerId;
        this.recipientPublicKeys = Convert.EMPTY_BYTES;
    }

    private Shuffling(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.dbKey = shufflingDbKeyFactory.newKey(this.id);
        this.holdingId = rs.getLong("holding_id");
        this.holdingType = HoldingType.get(rs.getByte("holding_type"));
        this.issuerId = rs.getLong("issuer_id");
        this.amount = rs.getLong("amount");
        this.participantCount = rs.getByte("participant_count");
        this.blocksRemaining = rs.getShort("blocks_remaining");
        this.stage = Stage.get(rs.getByte("stage"));
        this.assigneeAccountId = rs.getLong("assignee_account_id");
        this.cancellingAccountId = rs.getLong("cancelling_account_id");
        Array array = rs.getArray("recipient_public_keys");
        if (array != null) {
            Object[] recipientPublicKeys = (Object[]) array.getArray();
            this.recipientPublicKeys = Arrays.copyOf(recipientPublicKeys, recipientPublicKeys.length, byte[][].class);
        } else {
            this.recipientPublicKeys = Convert.EMPTY_BYTES;
        }
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO shuffling (id, holding_id, holding_type, "
                + "issuer_id, amount, participant_count, blocks_remaining, stage, assignee_account_id, "
                + "cancelling_account_id, recipient_public_keys, height, latest) "
                + "KEY (id, height) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, this.id);
            DbUtils.setLongZeroToNull(pstmt, ++i, this.holdingId);
            pstmt.setByte(++i, this.holdingType.getCode());
            pstmt.setLong(++i, this.issuerId);
            pstmt.setLong(++i, this.amount);
            pstmt.setByte(++i, this.participantCount);
            pstmt.setShort(++i, this.blocksRemaining);
            pstmt.setByte(++i, this.getStage().getCode());
            pstmt.setLong(++i, this.assigneeAccountId);
            pstmt.setLong(++i, this.cancellingAccountId);
            if (recipientPublicKeys.length > 0) {
                pstmt.setObject(++i, recipientPublicKeys);
            } else {
                pstmt.setNull(++i, Types.ARRAY);
            }
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }

    public long getId() {
        return id;
    }

    public long getHoldingId() {
        return holdingId;
    }

    public HoldingType getHoldingType() {
        return holdingType;
    }

    public long getIssuerId() {
        return issuerId;
    }

    public long getAmount() {
        return amount;
    }

    public byte getParticipantCount() {
        return participantCount;
    }

    public short getBlocksRemaining() {
        return blocksRemaining;
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

    public byte[][] getRecipientPublicKeys() {
        return recipientPublicKeys;
    }

    void cancelBy(ShufflingParticipant participant, byte[][] blameData, byte[][] keySeeds) {
        participant.cancel(blameData, keySeeds);
        this.blocksRemaining = (short)(100 + participantCount);
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

    public Attachment.ShufflingAttachment process(final long accountId, final String secretPhrase, final byte[] recipientPublicKey) {
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
        boolean isLast = shufflingParticipants.get(shufflingParticipants.size() - 1).getAccountId() == accountId;
        // decrypt the tokens bundled in the current data
        List<byte[]> outputDataList = new ArrayList<>();
        for (byte[] bytes : data) {
            AnonymouslyEncryptedData encryptedData = AnonymouslyEncryptedData.readEncryptedData(bytes);
            try {
                byte[] decrypted = encryptedData.decrypt(secretPhrase);
                if (isLast && decrypted.length != 32) {
                    continue;
                }
                outputDataList.add(decrypted);
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
        if (isLast) {
            Set<Long> recipientAccounts = new HashSet<>();
            Iterator<byte[]> iterator = outputDataList.iterator();
            while (iterator.hasNext()) {
                if (!recipientAccounts.add(Account.getId(iterator.next()))) {
                    iterator.remove();
                }
            }
            return new Attachment.ShufflingRecipients(this.id, outputDataList.toArray(new byte[outputDataList.size()][]),
                    previousDataTransactionFullHash);
        } else {
            return new Attachment.ShufflingProcessing(this.id, outputDataList.toArray(new byte[outputDataList.size()][]),
                    previousDataTransactionFullHash);
        }
    }

    public Attachment.ShufflingCancellation revealKeySeeds(final String secretPhrase, long cancellingAccountId) {
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
            if (data == null) {
                throw new RuntimeException("Account " + Long.toUnsignedString(accountId) + " is not a participant");
            }
            if (!participants.hasNext()) {
                return new Attachment.ShufflingCancellation(this.id, data, Convert.EMPTY_BYTES, dataTransactionFullHash,
                        cancellingAccountId);
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
            return new Attachment.ShufflingCancellation(this.id, data, keySeeds.toArray(new byte[keySeeds.size()][]),
                    dataTransactionFullHash, cancellingAccountId);
        } finally {
            Nxt.getBlockchain().readUnlock();
        }
    }

    private long blame() {
        if (stage == Stage.REGISTRATION) {
            return 0;
        }
        List<ShufflingParticipant> participants = new ArrayList<>();
        try (DbIterator<ShufflingParticipant> iterator = ShufflingParticipant.getParticipants(this.id)) {
            while (iterator.hasNext()) {
                participants.add(iterator.next());
            }
        }
        if (cancellingAccountId == 0) {
            // if no one submitted cancellation, blame the first one that did not submit processing data
            for (ShufflingParticipant participant : participants) {
                if (participant.getState() == ShufflingParticipant.State.REGISTERED) {
                    return participant.getAccountId();
                }
            }
            // or the first one who did not submit verification
            for (ShufflingParticipant participant : participants) {
                if (participant.getState() != ShufflingParticipant.State.VERIFIED) {
                    return participant.getAccountId();
                }
            }
            throw new RuntimeException("All participants submitted data and verifications, blame phase should not have been entered");
        }
        Set<Long> recipientAccounts = new HashSet<>();
        // start from issuer and verify all data up
        for (ShufflingParticipant participant : participants) {
            byte[][] keySeeds = participant.getKeySeeds();
            // if participant couldn't submit key seeds because he also couldn't decrypt some of the previous data, this should have been caught before
            if (keySeeds.length == 0) {
                return participant.getAccountId();
            }
            byte[] publicKey = Crypto.getPublicKey(keySeeds[0]);
            AnonymouslyEncryptedData encryptedData = null;
            for (byte[] bytes : participant.getBlameData()) {
                encryptedData = AnonymouslyEncryptedData.readEncryptedData(bytes);
                if (Arrays.equals(publicKey, encryptedData.getPublicKey())) {
                    // found the data that this participant encrypted
                    break;
                }
            }
            if (encryptedData == null || !Arrays.equals(publicKey, encryptedData.getPublicKey())) {
                // participant lied about key seeds or data
                return participant.getAccountId();
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
                    return participant.getAccountId();
                }
                if (i < participantCount - 1) {
                    encryptedData = AnonymouslyEncryptedData.readEncryptedData(participantBytes);
                } else {
                    // else it is not encrypted data but plaintext recipient public key, at the last participant
                    if (participantBytes.length != 32) {
                        // cannot be a valid public key
                        return participant.getAccountId();
                    }
                    // check for collisions and assume they are intentional
                    byte[] currentPublicKey = Account.getPublicKey(Account.getId(participantBytes));
                    if (currentPublicKey != null && !Arrays.equals(currentPublicKey, participantBytes)) {
                        return participant.getAccountId();
                    }
                    if (!recipientAccounts.add(Account.getId(currentPublicKey))) {
                        return participant.getAccountId();
                    }
                }
                boolean found = false;
                for (byte[] bytes : nextParticipant.getBlameData()) {
                    if (Arrays.equals(participantBytes, bytes)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    // the next participant did not include this participant's data
                    return nextParticipant.getAccountId();
                }
            }
        }
        return cancellingAccountId;
    }

    void addParticipant(long participantId) {
        // Update the shuffling assignee to point to the new participant and update the next pointer of the existing participant
        // to the new participant
        ShufflingParticipant lastParticipant = ShufflingParticipant.getParticipant(this.id, this.assigneeAccountId);
        lastParticipant.setNextAccountId(participantId);
        int index = lastParticipant.getIndex() + 1;
        ShufflingParticipant.addParticipant(this.id, participantId, index);
        // Check if participant registration is complete and if so update the shuffling
        if (index == this.participantCount - 1) {
            this.assigneeAccountId = this.issuerId;
            this.blocksRemaining = 100;
            setStage(Stage.PROCESSING);
        } else {
            this.assigneeAccountId = participantId;
        }
        shufflingTable.insert(this);
    }

    void updateParticipantData(Transaction transaction, Attachment.ShufflingProcessing attachment) {
        long participantId = transaction.getSenderId();
        byte[][] data = attachment.getData();
        ShufflingParticipant participant = ShufflingParticipant.getParticipant(this.id, participantId);
        participant.setDataTransactionFullHash(((TransactionImpl) transaction).fullHash());
        participant.setData(data, transaction.getTimestamp());
        if (data.length < participant.getIndex() + 1) {
            // couldn't decrypt all data from previous participants
            cancelBy(participant, data, Convert.EMPTY_BYTES);
            return;
        }
        this.assigneeAccountId = participant.getNextAccountId();
        this.blocksRemaining = 100;
        shufflingTable.insert(this);
    }

    void updateRecipients(Transaction transaction, Attachment.ShufflingRecipients attachment) {
        long participantId = transaction.getSenderId();
        this.recipientPublicKeys = attachment.getRecipientPublicKeys();
        ShufflingParticipant participant = ShufflingParticipant.getParticipant(this.id, participantId);
        participant.setDataTransactionFullHash(((TransactionImpl) transaction).fullHash());
        if (recipientPublicKeys.length < participantCount) {
            // couldn't decrypt all data from previous participants
            cancelBy(participant, recipientPublicKeys, Convert.EMPTY_BYTES);
            return;
        }
        participant.verify();
        // last participant announces all valid recipient public keys
        for (byte[] recipientPublicKey : recipientPublicKeys) {
            long recipientId = Account.getId(recipientPublicKey);
            if (Account.setOrVerify(recipientId, recipientPublicKey)) {
                Account.addOrGetAccount(recipientId).apply(recipientPublicKey);
            }
        }
        this.blocksRemaining = (short)(100 + participantCount);
        setStage(Stage.VERIFICATION);
        this.assigneeAccountId = this.issuerId;
        shufflingTable.insert(this);
    }

    void verify(long accountId) {
        ShufflingParticipant.getParticipant(id, accountId).verify();
        if (ShufflingParticipant.getVerifiedCount(id) == participantCount) {
            distribute();
        }
    }

    private void distribute() {
        for (byte[] recipientPublicKey : recipientPublicKeys) {
            byte[] publicKey = Account.getPublicKey(Account.getId(recipientPublicKey));
            if (publicKey != null && !Arrays.equals(publicKey, recipientPublicKey)) {
                // distribution not possible, do a cancellation on behalf of last participant instead
                ShufflingParticipant lastParticipant = ShufflingParticipant.getLastParticipant(this.id);
                cancelBy(lastParticipant, recipientPublicKeys, Convert.EMPTY_BYTES);
                return;
            }
        }
        try (DbIterator<ShufflingParticipant> participants = ShufflingParticipant.getParticipants(id)) {
            for (ShufflingParticipant participant : participants) {
                Account participantAccount = Account.getAccount(participant.getAccountId());
                holdingType.addToBalance(participantAccount, AccountLedger.LedgerEvent.SHUFFLING, this.id, this.holdingId, -amount);
                if (holdingType != HoldingType.NXT) {
                    participantAccount.addToBalanceNQT(AccountLedger.LedgerEvent.SHUFFLING, this.id, -Constants.SHUFFLE_DEPOSIT_NQT);
                }
            }
        }
        for (byte[] recipientPublicKey : recipientPublicKeys) {
            long recipientId = Account.getId(recipientPublicKey);
            Account recipientAccount = Account.addOrGetAccount(recipientId);
            recipientAccount.apply(recipientPublicKey);
            holdingType.addToBalanceAndUnconfirmedBalance(recipientAccount, AccountLedger.LedgerEvent.SHUFFLING, this.id, this.holdingId, amount);
            if (holdingType != HoldingType.NXT) {
                recipientAccount.addToBalanceAndUnconfirmedBalanceNQT(AccountLedger.LedgerEvent.SHUFFLING, this.id, Constants.SHUFFLE_DEPOSIT_NQT);
            }
        }
        setStage(Stage.DONE);
        shufflingTable.insert(this);
        listeners.notify(this, Event.SHUFFLING_DONE);
        delete();
    }

    private void cancel(Block block) {
        AccountLedger.LedgerEvent event = AccountLedger.LedgerEvent.SHUFFLING;
        long eventId = block.getId();
        long blamedAccountId = blame();
        try (DbIterator<ShufflingParticipant> participants = ShufflingParticipant.getParticipants(id)) {
            for (ShufflingParticipant participant : participants) {
                Account participantAccount = Account.getAccount(participant.getAccountId());
                if (blamedAccountId != 0) {
                    // as a penalty the deposit goes to the generator of the finish block
                    Account blockGeneratorAccount = Account.getAccount(block.getGeneratorId());
                    blockGeneratorAccount.addToBalanceAndUnconfirmedBalanceNQT(event, eventId, Constants.SHUFFLE_DEPOSIT_NQT);
                    blockGeneratorAccount.addToForgedBalanceNQT(Constants.SHUFFLE_DEPOSIT_NQT);
                }
                holdingType.addToUnconfirmedBalance(participantAccount, event, eventId, this.holdingId, this.amount);
                if (participantAccount.getId() != blamedAccountId) {
                    if (holdingType != HoldingType.NXT) {
                        participantAccount.addToUnconfirmedBalanceNQT(event, eventId, Constants.SHUFFLE_DEPOSIT_NQT);
                    }
                } else {
                    if (holdingType == HoldingType.NXT) {
                        participantAccount.addToUnconfirmedBalanceNQT(event, eventId, -Constants.SHUFFLE_DEPOSIT_NQT);
                    }
                    participantAccount.addToBalanceNQT(event, eventId, -Constants.SHUFFLE_DEPOSIT_NQT);
                }
            }
        }
        setStage(Stage.CANCELLED);
        shufflingTable.insert(this);
        listeners.notify(this, Event.SHUFFLING_CANCELLED);
        delete();
    }

    public ShufflingParticipant getParticipant(long accountId) {
        return ShufflingParticipant.getParticipant(id, accountId);
    }

    public ShufflingParticipant getLastParticipant() {
        return ShufflingParticipant.getLastParticipant(id);
    }

    private void delete() {
        try (DbIterator<ShufflingParticipant> participants = ShufflingParticipant.getParticipants(id)) {
            for (ShufflingParticipant participant : participants) {
                participant.delete();
            }
        }
        shufflingTable.delete(this);
    }

    private boolean isFull(Block block) {
        int transactionSize = 176; // min transaction size with no attachment
        if (stage == Stage.REGISTRATION) {
            transactionSize += 1 + 8;
        } else { // must use same for PROCESSING/VERIFICATION/BLAME
            transactionSize += 1 + 8; // TODO: determine max processing attachment size
        }
        return block.getPayloadLength() + transactionSize > Constants.MAX_PAYLOAD_LENGTH;
    }

}
