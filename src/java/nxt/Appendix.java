package nxt;

import nxt.crypto.Crypto;
import nxt.crypto.EncryptedData;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;

public interface Appendix {

    int getSize();
    int getFullSize();
    void putBytes(ByteBuffer buffer);
    JSONObject getJSONObject();
    byte getVersion();
    int getBaselineFeeHeight();
    Fee getBaselineFee(Transaction transaction);
    int getNextFeeHeight();
    Fee getNextFee(Transaction transaction);


    abstract class AbstractAppendix implements Appendix {

        private final byte version;

        AbstractAppendix(JSONObject attachmentData) {
            Long l = (Long) attachmentData.get("version." + getAppendixName());
            version = (byte) (l == null ? 0 : l);
        }

        AbstractAppendix(ByteBuffer buffer, byte transactionVersion) {
            if (transactionVersion == 0) {
                version = 0;
            } else {
                version = buffer.get();
            }
        }

        AbstractAppendix(int version) {
            this.version = (byte) version;
        }

        AbstractAppendix() {
            this.version = 1;
        }

        abstract String getAppendixName();

        @Override
        public final int getSize() {
            return getMySize() + (version > 0 ? 1 : 0);
        }

        @Override
        public final int getFullSize() {
            return getMyFullSize() + (version > 0 ? 1 : 0);
        }

        abstract int getMySize();

        int getMyFullSize() {
            return getMySize();
        }

        @Override
        public final void putBytes(ByteBuffer buffer) {
            if (version > 0) {
                buffer.put(version);
            }
            putMyBytes(buffer);
        }

        abstract void putMyBytes(ByteBuffer buffer);

        @Override
        public final JSONObject getJSONObject() {
            JSONObject json = new JSONObject();
            if (version > 0) {
                json.put("version." + getAppendixName(), version);
            }
            putMyJSON(json);
            return json;
        }

        abstract void putMyJSON(JSONObject json);

        @Override
        public final byte getVersion() {
            return version;
        }

        boolean verifyVersion(byte transactionVersion) {
            return transactionVersion == 0 ? version == 0 : version > 0;
        }

        @Override
        public int getBaselineFeeHeight() {
            return 1;
        }

        @Override
        public Fee getBaselineFee(Transaction transaction) {
            return Fee.NONE;
        }

        @Override
        public int getNextFeeHeight() {
            return Integer.MAX_VALUE;
        }

        @Override
        public Fee getNextFee(Transaction transaction) {
            return getBaselineFee(transaction);
        }

        abstract void validate(Transaction transaction) throws NxtException.ValidationException;

        abstract void apply(Transaction transaction, Account senderAccount, Account recipientAccount);

    }

    class Message extends AbstractAppendix {

        private static final Fee MESSAGE_FEE = new Fee.SizeBasedFee(Constants.ONE_NXT) {
            @Override
            public int getSize(TransactionImpl transaction, Appendix appendix) {
                return ((Message)appendix).getMessage().length;
            }
        };

        static Message parse(JSONObject attachmentData) {
            if (attachmentData.get("message") == null) {
                return null;
            }
            return new Message(attachmentData);
        }

        private final byte[] message;
        private final boolean isText;

        Message(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            super(buffer, transactionVersion);
            int messageLength = buffer.getInt();
            this.isText = messageLength < 0; // ugly hack
            if (messageLength < 0) {
                messageLength &= Integer.MAX_VALUE;
            }
            if (messageLength > Constants.MAX_ARBITRARY_MESSAGE_LENGTH) {
                throw new NxtException.NotValidException("Invalid arbitrary message length: " + messageLength);
            }
            this.message = new byte[messageLength];
            buffer.get(this.message);
        }

        Message(JSONObject attachmentData) {
            super(attachmentData);
            String messageString = (String)attachmentData.get("message");
            this.isText = Boolean.TRUE.equals(attachmentData.get("messageIsText"));
            this.message = isText ? Convert.toBytes(messageString) : Convert.parseHexString(messageString);
        }

        public Message(byte[] message) {
            this.message = message;
            this.isText = false;
        }

        public Message(String string) {
            this.message = Convert.toBytes(string);
            this.isText = true;
        }

        @Override
        String getAppendixName() {
            return "Message";
        }

        @Override
        public Fee getBaselineFee(Transaction transaction) {
            return MESSAGE_FEE;
        }

        @Override
        int getMySize() {
            return 4 + message.length;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putInt(isText ? (message.length | Integer.MIN_VALUE) : message.length);
            buffer.put(message);
        }

        @Override
        void putMyJSON(JSONObject json) {
            json.put("message", this.toString());
            json.put("messageIsText", isText);
        }

        @Override
        void validate(Transaction transaction) throws NxtException.ValidationException {
            if (this.isText && transaction.getVersion() == 0) {
                throw new NxtException.NotValidException("Text messages not yet enabled");
            }
            if (transaction.getVersion() == 0 && transaction.getAttachment() != Attachment.ARBITRARY_MESSAGE) {
                throw new NxtException.NotValidException("Message attachments not enabled for version 0 transactions");
            }
            if (message.length > Constants.MAX_ARBITRARY_MESSAGE_LENGTH) {
                throw new NxtException.NotCurrentlyValidException("Invalid arbitrary message length: " + message.length);
            }
        }

        @Override
        void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {}

        public byte[] getMessage() {
            return message;
        }

        public boolean isText() {
            return isText;
        }

        @Override
        public String toString() {
            return isText ? Convert.toString(message) : Convert.toHexString(message);
        }
    }

    class PrunableMessageAppendix extends Appendix.AbstractAppendix {

        private static final Fee PRUNABLE_MESSAGE_FEE = new Fee.SizeBasedFee(Constants.ONE_NXT/10) {
            @Override
            public int getSize(TransactionImpl transaction, Appendix appendix) {
                return ((PrunableMessageAppendix)appendix).getMessageLength();
            }
        };

        static PrunableMessageAppendix parse(JSONObject attachmentData) {
            if (attachmentData.get("prunableMessage") == null && attachmentData.get("prunableMessageHash") == null) {
                return null;
            }
            return new PrunableMessageAppendix(attachmentData);
        }

        private final byte[] hash;
        private final byte[] message;
        private final boolean isText;
        private volatile PrunableMessage prunableMessage;

        PrunableMessageAppendix(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            super(buffer, transactionVersion);
            this.hash = new byte[32];
            buffer.get(this.hash);
            this.message = null;
            this.isText = false;
        }

        PrunableMessageAppendix(JSONObject attachmentData) {
            super(attachmentData);
            byte[] hash = Convert.parseHexString(Convert.emptyToNull((String) attachmentData.get("prunableMessageHash")));
            if (hash != null) {
                this.hash = hash;
                this.message = null;
                this.isText = false;
            } else {
                String messageString = Convert.nullToEmpty((String) attachmentData.get("prunableMessage"));
                this.isText = Boolean.TRUE.equals(attachmentData.get("prunableMessageIsText"));
                this.message = isText ? Convert.toBytes(messageString) : Convert.parseHexString(messageString);
                this.hash = null;
            }
        }

        public PrunableMessageAppendix(byte[] message) {
            this.message = message;
            this.isText = false;
            this.hash = null;
        }

        public PrunableMessageAppendix(String string) {
            this.message = Convert.toBytes(string);
            this.isText = true;
            this.hash = null;
        }

        @Override
        String getAppendixName() {
            return "PrunableMessage";
        }

        @Override
        public Fee getBaselineFee(Transaction transaction) {
            return PRUNABLE_MESSAGE_FEE;
        }

        @Override
        int getMySize() {
            return 32;
        }

        @Override
        int getMyFullSize() {
            return getMessageLength();
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            if (hash != null) {
                buffer.put(hash);
            } else {
                MessageDigest digest = Crypto.sha256();
                digest.update((byte)(isText ? 1 : 0));
                digest.update(message);
                buffer.put(digest.digest());
            }
        }

        @Override
        void putMyJSON(JSONObject json) {
            if (prunableMessage != null) {
                json.put("prunableMessage", prunableMessage.toString());
                json.put("prunableMessageIsText", isText);
            } else if (hash != null) {
                json.put("prunableMessageHash", Convert.toHexString(hash));
            } else {
                json.put("prunableMessage", this.toString());
                json.put("prunableMessageIsText", isText);
            }
        }

        @Override
        void validate(Transaction transaction) throws NxtException.ValidationException {
            if (Nxt.getBlockchain().getHeight() < Constants.VOTING_SYSTEM_BLOCK) {
                throw new NxtException.NotYetEnabledException("Prunable messages not yet enabled");
            }
            if (getMessageLength() > Constants.MAX_PRUNABLE_MESSAGE_LENGTH) {
                throw new NxtException.NotValidException("Invalid prunable message length: " + message.length);
            }
            if (message != null && message.length <= 28) {
                throw new NxtException.NotValidException("Prunable messages must be longer than 28 bytes");
            }
            if (message == null && Nxt.getEpochTime() - transaction.getTimestamp() < Constants.MIN_PRUNABLE_LIFETIME) {
                throw new NxtException.NotCurrentlyValidException("Message has been pruned prematurely");
            }
        }

        @Override
        void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
            nxt.PrunableMessage.add(transaction, this);
        }

        public byte[] getMessage() {
            return message;
        }

        public boolean isText() {
            return isText;
        }

        @Override
        public String toString() {
            return isText ? Convert.toString(message) : Convert.toHexString(message);
        }

        private int getMessageLength() {
            return message == null ? 0 : message.length;
        }

        void loadPrunableMessage(TransactionImpl transaction) {
            if (message == null && prunableMessage == null && Nxt.getEpochTime() - transaction.getTimestamp() < Constants.MIN_PRUNABLE_LIFETIME) {
                prunableMessage = PrunableMessage.getPrunableMessage(transaction.getId());
            }
        }

    }

    abstract class AbstractEncryptedMessage extends AbstractAppendix {

        private static final Fee ENCRYPTED_DATA_FEE = new Fee.SizeBasedFee(Constants.ONE_NXT) {
            @Override
            public int getSize(TransactionImpl transaction, Appendix appendix) {
                return ((AbstractEncryptedMessage)appendix).getEncryptedData().getData().length;
            }
        };

        private final EncryptedData encryptedData;
        private final boolean isText;

        private AbstractEncryptedMessage(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            super(buffer, transactionVersion);
            int length = buffer.getInt();
            this.isText = length < 0;
            if (length < 0) {
                length &= Integer.MAX_VALUE;
            }
            this.encryptedData = EncryptedData.readEncryptedData(buffer, length, Constants.MAX_ENCRYPTED_MESSAGE_LENGTH_2);
        }

        private AbstractEncryptedMessage(JSONObject attachmentJSON, JSONObject encryptedMessageJSON) {
            super(attachmentJSON);
            byte[] data = Convert.parseHexString((String)encryptedMessageJSON.get("data"));
            byte[] nonce = Convert.parseHexString((String)encryptedMessageJSON.get("nonce"));
            this.encryptedData = new EncryptedData(data, nonce);
            this.isText = Boolean.TRUE.equals(encryptedMessageJSON.get("isText"));
        }

        private AbstractEncryptedMessage(EncryptedData encryptedData, boolean isText) {
            this.encryptedData = encryptedData;
            this.isText = isText;
        }

        @Override
        public Fee getBaselineFee(Transaction transaction) {
            return ENCRYPTED_DATA_FEE;
        }

        @Override
        int getMySize() {
            return 4 + encryptedData.getSize();
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putInt(isText ? (encryptedData.getData().length | Integer.MIN_VALUE) : encryptedData.getData().length);
            buffer.put(encryptedData.getData());
            buffer.put(encryptedData.getNonce());
        }

        @Override
        void putMyJSON(JSONObject json) {
            json.put("data", Convert.toHexString(encryptedData.getData()));
            json.put("nonce", Convert.toHexString(encryptedData.getNonce()));
            json.put("isText", isText);
        }

        @Override
        void validate(Transaction transaction) throws NxtException.ValidationException {
            if (Nxt.getBlockchain().getHeight() < Constants.VOTING_SYSTEM_BLOCK) {
                if (encryptedData.getData().length > Constants.MAX_ENCRYPTED_MESSAGE_LENGTH) {
                    throw new NxtException.NotCurrentlyValidException("Max encrypted message length exceeded");
                }
            } else {
                if (encryptedData.getData().length > Constants.MAX_ENCRYPTED_MESSAGE_LENGTH_2) {
                    throw new NxtException.NotValidException("Max encrypted message length exceeded");
                }
            }
            if ((encryptedData.getNonce().length != 32 && encryptedData.getData().length > 0)
                    || (encryptedData.getNonce().length != 0 && encryptedData.getData().length == 0)) {
                throw new NxtException.NotValidException("Invalid nonce length " + encryptedData.getNonce().length);
            }
        }

        void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {}

        public final EncryptedData getEncryptedData() {
            return encryptedData;
        }

        public final boolean isText() {
            return isText;
        }

    }

    class EncryptedMessage extends AbstractEncryptedMessage {

        static EncryptedMessage parse(JSONObject attachmentData) {
            if (attachmentData.get("encryptedMessage") == null ) {
                return null;
            }
            return new EncryptedMessage(attachmentData);
        }

        EncryptedMessage(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            super(buffer, transactionVersion);
        }

        EncryptedMessage(JSONObject attachmentData) {
            super(attachmentData, (JSONObject)attachmentData.get("encryptedMessage"));
        }

        public EncryptedMessage(EncryptedData encryptedData, boolean isText) {
            super(encryptedData, isText);
        }

        @Override
        String getAppendixName() {
            return "EncryptedMessage";
        }

        @Override
        void putMyJSON(JSONObject json) {
            JSONObject encryptedMessageJSON = new JSONObject();
            super.putMyJSON(encryptedMessageJSON);
            json.put("encryptedMessage", encryptedMessageJSON);
        }

        @Override
        void validate(Transaction transaction) throws NxtException.ValidationException {
            super.validate(transaction);
            if (transaction.getRecipientId() == 0) {
                throw new NxtException.NotValidException("Encrypted messages cannot be attached to transactions with no recipient");
            }
            if (transaction.getVersion() == 0) {
                throw new NxtException.NotValidException("Encrypted message attachments not enabled for version 0 transactions");
            }
        }

    }

    class EncryptToSelfMessage extends AbstractEncryptedMessage {

        static EncryptToSelfMessage parse(JSONObject attachmentData) {
            if (attachmentData.get("encryptToSelfMessage") == null ) {
                return null;
            }
            return new EncryptToSelfMessage(attachmentData);
        }

        EncryptToSelfMessage(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            super(buffer, transactionVersion);
        }

        EncryptToSelfMessage(JSONObject attachmentData) {
            super(attachmentData, (JSONObject)attachmentData.get("encryptToSelfMessage"));
        }

        public EncryptToSelfMessage(EncryptedData encryptedData, boolean isText) {
            super(encryptedData, isText);
        }

        @Override
        String getAppendixName() {
            return "EncryptToSelfMessage";
        }

        @Override
        void putMyJSON(JSONObject json) {
            JSONObject encryptToSelfMessageJSON = new JSONObject();
            super.putMyJSON(encryptToSelfMessageJSON);
            json.put("encryptToSelfMessage", encryptToSelfMessageJSON);
        }

        @Override
        void validate(Transaction transaction) throws NxtException.ValidationException {
            super.validate(transaction);
            if (transaction.getVersion() == 0) {
                throw new NxtException.NotValidException("Encrypt-to-self message attachments not enabled for version 0 transactions");
            }
        }

    }

    class PublicKeyAnnouncement extends AbstractAppendix {

        static PublicKeyAnnouncement parse(JSONObject attachmentData) {
            if (attachmentData.get("recipientPublicKey") == null) {
                return null;
            }
            return new PublicKeyAnnouncement(attachmentData);
        }

        private final byte[] publicKey;

        PublicKeyAnnouncement(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.publicKey = new byte[32];
            buffer.get(this.publicKey);
        }

        PublicKeyAnnouncement(JSONObject attachmentData) {
            super(attachmentData);
            this.publicKey = Convert.parseHexString((String)attachmentData.get("recipientPublicKey"));
        }

        public PublicKeyAnnouncement(byte[] publicKey) {
            this.publicKey = publicKey;
        }

        @Override
        String getAppendixName() {
            return "PublicKeyAnnouncement";
        }

        @Override
        int getMySize() {
            return 32;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.put(publicKey);
        }

        @Override
        void putMyJSON(JSONObject json) {
            json.put("recipientPublicKey", Convert.toHexString(publicKey));
        }

        @Override
        void validate(Transaction transaction) throws NxtException.ValidationException {
            if (transaction.getRecipientId() == 0) {
                throw new NxtException.NotValidException("PublicKeyAnnouncement cannot be attached to transactions with no recipient");
            }
            if (publicKey.length != 32) {
                throw new NxtException.NotValidException("Invalid recipient public key length: " + Convert.toHexString(publicKey));
            }
            long recipientId = transaction.getRecipientId();
            if (Account.getId(this.publicKey) != recipientId) {
                throw new NxtException.NotValidException("Announced public key does not match recipient accountId");
            }
            if (transaction.getVersion() == 0) {
                throw new NxtException.NotValidException("Public key announcements not enabled for version 0 transactions");
            }
            Account recipientAccount = Account.getAccount(recipientId);
            if (recipientAccount != null && recipientAccount.getKeyHeight() > 0 && ! Arrays.equals(publicKey, recipientAccount.getPublicKey())) {
                throw new NxtException.NotCurrentlyValidException("A different public key for this account has already been announced");
            }
        }

        @Override
        void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
            if (recipientAccount.setOrVerify(publicKey)) {
                recipientAccount.apply(this.publicKey);
            }
        }

        public byte[] getPublicKey() {
            return publicKey;
        }

    }

    class Phasing extends AbstractAppendix {

        private static final Fee PHASING_FEE = new Fee.ConstantFee(20 * Constants.ONE_NXT);

        static Phasing parse(JSONObject attachmentData) {
            if (attachmentData.get("phasingFinishHeight") == null) {
                return null;
            }
            return new Phasing(attachmentData);
        }

        private final int finishHeight;
        private final long quorum;
        private final long[] whitelist;
        private final byte[][] linkedFullHashes;
        private final byte[] hashedSecret;
        private final byte algorithm;
        private final VoteWeighting voteWeighting;

        Phasing(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            finishHeight = buffer.getInt();
            byte votingModel = buffer.get();
            quorum = buffer.getLong();
            long minBalance = buffer.getLong();
            byte whitelistSize = buffer.get();
            whitelist = new long[whitelistSize];
            for (int i = 0; i < whitelistSize; i++) {
                whitelist[i] = buffer.getLong();
            }
            long holdingId = buffer.getLong();
            byte minBalanceModel = buffer.get();
            voteWeighting = new VoteWeighting(votingModel, holdingId, minBalance, minBalanceModel);
            byte linkedFullHashesSize = buffer.get();
            linkedFullHashes = new byte[linkedFullHashesSize][];
            for (int i = 0; i < linkedFullHashesSize; i++) {
                linkedFullHashes[i] = new byte[32];
                buffer.get(linkedFullHashes[i]);
            }
            byte hashedSecretLength = buffer.get();
            if (hashedSecretLength > 0) {
                hashedSecret = new byte[hashedSecretLength];
                buffer.get(hashedSecret);
            } else {
                hashedSecret = Convert.EMPTY_BYTE;
            }
            algorithm = buffer.get();
        }

        Phasing(JSONObject attachmentData) {
            super(attachmentData);
            finishHeight = ((Long) attachmentData.get("phasingFinishHeight")).intValue();
            quorum = Convert.parseLong(attachmentData.get("phasingQuorum"));
            long minBalance = Convert.parseLong(attachmentData.get("phasingMinBalance"));
            byte votingModel = ((Long) attachmentData.get("phasingVotingModel")).byteValue();
            long holdingId = Convert.parseUnsignedLong((String) attachmentData.get("phasingHolding"));
            JSONArray whitelistJson = (JSONArray) (attachmentData.get("phasingWhitelist"));
            if (whitelistJson != null && whitelistJson.size() > 0) {
                whitelist = new long[whitelistJson.size()];
                for (int i = 0; i < whitelist.length; i++) {
                    whitelist[i] = Convert.parseUnsignedLong((String) whitelistJson.get(i));
                }
            } else {
                whitelist = Convert.EMPTY_LONG;
            }
            byte minBalanceModel = ((Long) attachmentData.get("phasingMinBalanceModel")).byteValue();
            voteWeighting = new VoteWeighting(votingModel, holdingId, minBalance, minBalanceModel);
            JSONArray linkedFullHashesJson = (JSONArray) attachmentData.get("phasingLinkedFullHashes");
            if (linkedFullHashesJson != null && linkedFullHashesJson.size() > 0) {
                linkedFullHashes = new byte[linkedFullHashesJson.size()][];
                for (int i = 0; i < linkedFullHashes.length; i++) {
                    linkedFullHashes[i] = Convert.parseHexString((String) linkedFullHashesJson.get(i));
                }
            } else {
                linkedFullHashes = Convert.EMPTY_BYTES;
            }
            String hashedSecret = Convert.emptyToNull((String)attachmentData.get("phasingHashedSecret"));
            if (hashedSecret != null) {
                this.hashedSecret = Convert.parseHexString(hashedSecret);
                this.algorithm = ((Long) attachmentData.get("phasingHashedSecretAlgorithm")).byteValue();
            } else {
                this.hashedSecret = Convert.EMPTY_BYTE;
                this.algorithm = 0;
            }
        }

        public Phasing(int finishHeight, byte votingModel, long holdingId, long quorum,
                       long minBalance, byte minBalanceModel, long[] whitelist, byte[][] linkedFullHashes, byte[] hashedSecret, byte algorithm) {
            this.finishHeight = finishHeight;
            this.quorum = quorum;
            this.whitelist = Convert.nullToEmpty(whitelist);
            if (this.whitelist.length > 0) {
                Arrays.sort(this.whitelist);
            }
            voteWeighting = new VoteWeighting(votingModel, holdingId, minBalance, minBalanceModel);
            this.linkedFullHashes = Convert.nullToEmpty(linkedFullHashes);
            this.hashedSecret = hashedSecret != null ? hashedSecret : Convert.EMPTY_BYTE;
            this.algorithm = algorithm;
        }

        @Override
        String getAppendixName() {
            return "Phasing";
        }

        @Override
        int getMySize() {
            return 4 + 1 + 8 + 8 + 1 + 8 * whitelist.length + 8 + 1 + 1 + 32 * linkedFullHashes.length + 1 + hashedSecret.length + 1;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putInt(finishHeight);
            buffer.put(voteWeighting.getVotingModel().getCode());
            buffer.putLong(quorum);
            buffer.putLong(voteWeighting.getMinBalance());
            buffer.put((byte) whitelist.length);
            for (long account : whitelist) {
                buffer.putLong(account);
            }
            buffer.putLong(voteWeighting.getHoldingId());
            buffer.put(voteWeighting.getMinBalanceModel().getCode());
            buffer.put((byte) linkedFullHashes.length);
            for (byte[] hash : linkedFullHashes) {
                buffer.put(hash);
            }
            buffer.put((byte)hashedSecret.length);
            buffer.put(hashedSecret);
            buffer.put(algorithm);
        }

        @Override
        void putMyJSON(JSONObject json) {
            json.put("phasingFinishHeight", finishHeight);
            json.put("phasingQuorum", quorum);
            json.put("phasingMinBalance", voteWeighting.getMinBalance());
            json.put("phasingVotingModel", voteWeighting.getVotingModel().getCode());
            json.put("phasingHolding", Long.toUnsignedString(voteWeighting.getHoldingId()));
            json.put("phasingMinBalanceModel", voteWeighting.getMinBalanceModel().getCode());
            if (whitelist.length > 0) {
                JSONArray whitelistJson = new JSONArray();
                for (long accountId : whitelist) {
                    whitelistJson.add(Long.toUnsignedString(accountId));
                }
                json.put("phasingWhitelist", whitelistJson);
            }
            if (linkedFullHashes.length > 0) {
                JSONArray linkedFullHashesJson = new JSONArray();
                for (byte[] hash : linkedFullHashes) {
                    linkedFullHashesJson.add(Convert.toHexString(hash));
                }
                json.put("phasingLinkedFullHashes", linkedFullHashesJson);
            }
            if (hashedSecret.length > 0) {
                json.put("phasingHashedSecret", Convert.toHexString(hashedSecret));
                json.put("phasingHashedSecretAlgorithm", algorithm);
            }
        }

        @Override
        void validate(Transaction transaction) throws NxtException.ValidationException {

            if (transaction.getSignature() == null || PhasingPoll.getPoll(transaction.getId()) == null) {
                int currentHeight = Nxt.getBlockchain().getHeight();
                if (currentHeight < Constants.VOTING_SYSTEM_BLOCK) {
                    throw new NxtException.NotYetEnabledException("Voting System not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
                }

                if (whitelist.length > Constants.MAX_PHASING_WHITELIST_SIZE) {
                    throw new NxtException.NotValidException("Whitelist is too big");
                }

                long previousAccountId = 0;
                for (long accountId : whitelist) {
                    if (accountId == 0) {
                        throw new NxtException.NotValidException("Invalid accountId 0 in whitelist");
                    }
                    if (previousAccountId != 0 && accountId < previousAccountId) {
                        throw new NxtException.NotValidException("Whitelist not sorted " + Arrays.toString(whitelist));
                    }
                    if (accountId == previousAccountId) {
                        throw new NxtException.NotValidException("Duplicate accountId " + Long.toUnsignedString(accountId) + " in whitelist");
                    }
                    previousAccountId = accountId;
                }

                if (quorum <= 0 && voteWeighting.getVotingModel() != VoteWeighting.VotingModel.NONE) {
                    throw new NxtException.NotValidException("quorum <= 0");
                }

                if (voteWeighting.getVotingModel() == VoteWeighting.VotingModel.NONE) {
                    if (quorum != 0) {
                        throw new NxtException.NotValidException("Quorum must be 0 for no-voting phased transaction");
                    }
                    if (whitelist.length != 0) {
                        throw new NxtException.NotValidException("No whitelist needed for no-voting phased transaction");
                    }
                }

                if (voteWeighting.getVotingModel() == VoteWeighting.VotingModel.ACCOUNT && whitelist.length > 0 && quorum > whitelist.length) {
                    throw new NxtException.NotValidException("Quorum of " + quorum + " cannot be achieved in by-account voting with whitelist of length "
                            + whitelist.length);
                }

                if (voteWeighting.getVotingModel() == VoteWeighting.VotingModel.TRANSACTION) {
                    if (linkedFullHashes.length == 0 || linkedFullHashes.length > Constants.MAX_PHASING_LINKED_TRANSACTIONS) {
                        throw new NxtException.NotValidException("Invalid number of linkedFullHashes " + linkedFullHashes.length);
                    }
                    for (byte[] hash : linkedFullHashes) {
                        if (Convert.emptyToNull(hash) == null || hash.length != 32) {
                            throw new NxtException.NotValidException("Invalid linkedFullHash " + Convert.toHexString(hash));
                        }
                        TransactionImpl linkedTransaction = TransactionDb.findTransactionByFullHash(hash, currentHeight);
                        if (linkedTransaction != null) {
                            if (transaction.getTimestamp() - linkedTransaction.getTimestamp() > Constants.MAX_REFERENCED_TRANSACTION_TIMESPAN) {
                                throw new NxtException.NotValidException("Linked transaction cannot be more than 60 days older than the phased transaction");
                            }
                            if (linkedTransaction.getPhasing() != null) {
                                throw new NxtException.NotCurrentlyValidException("Cannot link to an already existing phased transaction");
                            }
                        }
                    }
                    if (quorum > linkedFullHashes.length) {
                        throw new NxtException.NotValidException("Quorum of " + quorum + " cannot be achieved in by-transaction voting with "
                                + linkedFullHashes.length + " linked full hashes only");
                    }
                } else {
                    if (linkedFullHashes.length != 0) {
                        throw new NxtException.NotValidException("LinkedFullHashes can only be used with VotingModel.TRANSACTION");
                    }
                }

                if (voteWeighting.getVotingModel() == VoteWeighting.VotingModel.HASH) {
                    if (quorum != 1) {
                        throw new NxtException.NotValidException("Quorum must be 1 for by-hash voting");
                    }
                    if (hashedSecret.length == 0 || hashedSecret.length > Byte.MAX_VALUE) {
                        throw new NxtException.NotValidException("Invalid hashedSecret " + Convert.toHexString(hashedSecret));
                    }
                    if (PhasingPoll.getHashFunction(algorithm) == null) {
                        throw new NxtException.NotValidException("Invalid hashedSecretAlgorithm " + algorithm);
                    }
                } else {
                    if (hashedSecret.length != 0) {
                        throw new NxtException.NotValidException("HashedSecret can only be used with VotingModel.HASH");
                    }
                    if (algorithm != 0) {
                        throw new NxtException.NotValidException("HashedSecretAlgorithm can only be used with VotingModel.HASH");
                    }
                }

                if (finishHeight <= currentHeight + (voteWeighting.acceptsVotes() ? 2 : 1)
                        || finishHeight >= currentHeight + Constants.MAX_PHASING_DURATION) {
                    throw new NxtException.NotCurrentlyValidException("Invalid finish height " + finishHeight);
                }
            }

            voteWeighting.validate();

        }

        @Override
        void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
            PhasingPoll.addPoll(transaction, this);
        }

        @Override
        public Fee getBaselineFee(Transaction transaction) {
            if (voteWeighting.isBalanceIndependent()) {
                return Fee.DEFAULT_FEE;
            }
            return PHASING_FEE;
        }

        private void release(TransactionImpl transaction) {
            Account senderAccount = Account.getAccount(transaction.getSenderId());
            Account recipientAccount = Account.getAccount(transaction.getRecipientId());
            //apply all attachments and appendixes, except the phasing itself
            for (Appendix.AbstractAppendix appendage : transaction.getAppendages()) {
                if (appendage != transaction.getPhasing()) {
                    appendage.apply(transaction, senderAccount, recipientAccount);
                }
            }
            TransactionProcessorImpl.getInstance().notifyListeners(Collections.singletonList(transaction), TransactionProcessor.Event.RELEASE_PHASED_TRANSACTION);
            Logger.logDebugMessage("Transaction " + transaction.getStringId() + " has been released");
        }

        void reject(TransactionImpl transaction) {
            Account senderAccount = Account.getAccount(transaction.getSenderId());
            transaction.getType().undoAttachmentUnconfirmed(transaction, senderAccount);
            senderAccount.addToUnconfirmedBalanceNQT(transaction.getAmountNQT());
            TransactionProcessorImpl.getInstance().notifyListeners(Collections.singletonList(transaction), TransactionProcessor.Event.REJECT_PHASED_TRANSACTION);
            Logger.logDebugMessage("Transaction " + transaction.getStringId() + " has been rejected");
        }

        void countVotes(TransactionImpl transaction) {
            PhasingPoll poll = PhasingPoll.getPoll(transaction.getId());
            long result = poll.getResult();
            poll.finish(result);
            if (result >= poll.getQuorum()) {
                try {
                    release(transaction);
                } catch (RuntimeException e) {
                    Logger.logErrorMessage("Failed to release phased transaction " + transaction.getJSONObject().toJSONString(), e);
                    reject(transaction);
                }
            } else {
                reject(transaction);
            }
        }

        public int getFinishHeight() {
            return finishHeight;
        }

        public long getQuorum() {
            return quorum;
        }

        public long[] getWhitelist() {
            return whitelist;
        }

        public VoteWeighting getVoteWeighting() {
            return voteWeighting;
        }

        public byte[][] getLinkedFullHashes() {
            return linkedFullHashes;
        }

        public byte[] getHashedSecret() {
            return hashedSecret;
        }

        public byte getAlgorithm() {
            return algorithm;
        }

    }
}
