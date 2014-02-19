package nxt;

import nxt.crypto.Crypto;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Transaction implements Comparable<Transaction> {

    private static final byte TYPE_PAYMENT = 0;
    private static final byte TYPE_MESSAGING = 1;
    private static final byte TYPE_COLORED_COINS = 2;

    private static final byte SUBTYPE_PAYMENT_ORDINARY_PAYMENT = 0;

    private static final byte SUBTYPE_MESSAGING_ARBITRARY_MESSAGE = 0;
    private static final byte SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT = 1;
    private static final byte SUBTYPE_MESSAGING_POLL_CREATION = 2;
    private static final byte SUBTYPE_MESSAGING_VOTE_CASTING = 3;

    private static final byte SUBTYPE_COLORED_COINS_ASSET_ISSUANCE = 0;
    private static final byte SUBTYPE_COLORED_COINS_ASSET_TRANSFER = 1;
    private static final byte SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT = 2;
    private static final byte SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT = 3;
    private static final byte SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION = 4;
    private static final byte SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION = 5;


    public static Transaction newTransaction(int timestamp, short deadline, byte[] senderPublicKey, Long recipientId,
                                             int amount, int fee, Long referencedTransactionId) throws NxtException.ValidationException {
        return new Transaction(Type.Payment.ORDINARY, timestamp, deadline, senderPublicKey, recipientId, amount, fee, referencedTransactionId, null);
    }

    public static Transaction newTransaction(int timestamp, short deadline, byte[] senderPublicKey, Long recipientId,
                                             int amount, int fee, Long referencedTransactionId, Attachment attachment)
            throws NxtException.ValidationException {
        Transaction transaction = new Transaction(attachment.getTransactionType(), timestamp, deadline, senderPublicKey, recipientId, amount, fee,
                referencedTransactionId, null);
        transaction.attachment = attachment;
        return transaction;
    }

    static Transaction newTransaction(int timestamp, short deadline, byte[] senderPublicKey, Long recipientId,
                                      int amount, int fee, Long referencedTransactionId, byte[] signature) throws NxtException.ValidationException {
        return new Transaction(Type.Payment.ORDINARY, timestamp, deadline, senderPublicKey, recipientId, amount, fee, referencedTransactionId, signature);
    }

    static Transaction findTransaction(Long transactionId) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM transaction WHERE id = ?")) {
            pstmt.setLong(1, transactionId);
            ResultSet rs = pstmt.executeQuery();
            Transaction transaction = null;
            if (rs.next()) {
                transaction = getTransaction(con, rs);
            }
            rs.close();
            return transaction;
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (NxtException.ValidationException e) {
            throw new RuntimeException("Block already in database, id = " + transactionId + ", does not pass validation!");
        }
    }

    static boolean hasTransaction(Long transactionId) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT 1 FROM transaction WHERE id = ?")) {
            pstmt.setLong(1, transactionId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static Transaction getTransaction(byte[] bytes) throws NxtException.ValidationException {

        try {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            byte type = buffer.get();
            byte subtype = buffer.get();
            int timestamp = buffer.getInt();
            short deadline = buffer.getShort();
            byte[] senderPublicKey = new byte[32];
            buffer.get(senderPublicKey);
            Long recipientId = buffer.getLong();
            int amount = buffer.getInt();
            int fee = buffer.getInt();
            Long referencedTransactionId = Convert.zeroToNull(buffer.getLong());
            byte[] signature = new byte[64];
            buffer.get(signature);

            Type transactionType = findTransactionType(type, subtype);
            Transaction transaction = new Transaction(transactionType, timestamp, deadline, senderPublicKey, recipientId, amount,
                    fee, referencedTransactionId, signature);

            try {
                transactionType.loadAttachment(transaction, buffer);
            } catch (NxtException.ValidationException e) {
                if (transaction.attachment != null && ! (e instanceof NotYetEnabledException)) {
                    Logger.logDebugMessage("Invalid transaction attachment:\n" + transaction.attachment.getJSON());
                }
                throw e;
            }

            return transaction;

        } catch (RuntimeException e) {
            throw new NxtException.ValidationException(e.toString());
        }
    }

    static Transaction getTransaction(JSONObject transactionData) throws NxtException.ValidationException {

        try {

            byte type = ((Long)transactionData.get("type")).byteValue();
            byte subtype = ((Long)transactionData.get("subtype")).byteValue();
            int timestamp = ((Long)transactionData.get("timestamp")).intValue();
            short deadline = ((Long)transactionData.get("deadline")).shortValue();
            byte[] senderPublicKey = Convert.parseHexString((String) transactionData.get("senderPublicKey"));
            Long recipientId = Convert.parseUnsignedLong((String) transactionData.get("recipient"));
            if (recipientId == null) recipientId = 0L; // ugly
            int amount = ((Long)transactionData.get("amount")).intValue();
            int fee = ((Long)transactionData.get("fee")).intValue();
            Long referencedTransactionId = Convert.parseUnsignedLong((String) transactionData.get("referencedTransaction"));
            byte[] signature = Convert.parseHexString((String) transactionData.get("signature"));

            Type transactionType = findTransactionType(type, subtype);
            Transaction transaction = new Transaction(transactionType, timestamp, deadline, senderPublicKey, recipientId, amount, fee,
                    referencedTransactionId, signature);

            JSONObject attachmentData = (JSONObject)transactionData.get("attachment");

            try {
                transactionType.loadAttachment(transaction, attachmentData);
            } catch (NxtException.ValidationException e) {
                if (! (e instanceof NotYetEnabledException)) {
                    Logger.logDebugMessage("Invalid transaction attachment:\n" + attachmentData.toJSONString());
                }
                throw e;
            }

            return transaction;

        } catch (RuntimeException e) {
            throw new NxtException.ValidationException(e.toString());
        }
    }

    static Transaction getTransaction(Connection con, ResultSet rs) throws NxtException.ValidationException {
        try {

            byte type = rs.getByte("type");
            byte subtype = rs.getByte("subtype");
            int timestamp = rs.getInt("timestamp");
            short deadline = rs.getShort("deadline");
            byte[] senderPublicKey = rs.getBytes("sender_public_key");
            Long recipientId = rs.getLong("recipient_id");
            int amount = rs.getInt("amount");
            int fee = rs.getInt("fee");
            Long referencedTransactionId = rs.getLong("referenced_transaction_id");
            if (rs.wasNull()) {
                referencedTransactionId = null;
            }
            byte[] signature = rs.getBytes("signature");

            Type transactionType = findTransactionType(type, subtype);
            Transaction transaction = new Transaction(transactionType, timestamp, deadline, senderPublicKey, recipientId, amount, fee,
                    referencedTransactionId, signature);
            transaction.blockId = rs.getLong("block_id");
            transaction.height = rs.getInt("height");
            transaction.id = rs.getLong("id");
            transaction.senderId = rs.getLong("sender_id");

            transaction.attachment = (Attachment)rs.getObject("attachment");

            return transaction;

        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    static List<Transaction> findBlockTransactions(Connection con, Long blockId) {
        List<Transaction> list = new ArrayList<>();
        try (PreparedStatement pstmt = con.prepareStatement("SELECT * FROM transaction WHERE block_id = ? ORDER BY id")) {
            pstmt.setLong(1, blockId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(getTransaction(con, rs));
            }
            rs.close();
            return list;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } catch (NxtException.ValidationException e) {
            throw new RuntimeException("Transaction already in database for block_id = " + blockId + " does not pass validation!");
        }
    }

    static void saveTransactions(Connection con, List<Transaction> transactions) {
        try {
            for (Transaction transaction : transactions) {
                try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO transaction (id, deadline, sender_public_key, recipient_id, "
                        + "amount, fee, referenced_transaction_id, height, block_id, signature, timestamp, type, subtype, sender_id, attachment) "
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                    int i = 0;
                    pstmt.setLong(++i, transaction.getId());
                    pstmt.setShort(++i, transaction.deadline);
                    pstmt.setBytes(++i, transaction.senderPublicKey);
                    pstmt.setLong(++i, transaction.recipientId);
                    pstmt.setInt(++i, transaction.amount);
                    pstmt.setInt(++i, transaction.fee);
                    if (transaction.referencedTransactionId != null) {
                        pstmt.setLong(++i, transaction.referencedTransactionId);
                    } else {
                        pstmt.setNull(++i, Types.BIGINT);
                    }
                    pstmt.setInt(++i, transaction.height);
                    pstmt.setLong(++i, transaction.blockId);
                    pstmt.setBytes(++i, transaction.signature);
                    pstmt.setInt(++i, transaction.timestamp);
                    pstmt.setByte(++i, transaction.type.getType());
                    pstmt.setByte(++i, transaction.type.getSubtype());
                    pstmt.setLong(++i, transaction.getSenderId());
                    if (transaction.attachment != null) {
                        pstmt.setObject(++i, transaction.attachment);
                    } else {
                        pstmt.setNull(++i, Types.JAVA_OBJECT);
                    }
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }


    private final short deadline;
    private final byte[] senderPublicKey;
    private final Long recipientId;
    private final int amount;
    private final int fee;
    private final Long referencedTransactionId;
    private final Type type;

    private int height = Integer.MAX_VALUE;
    private Long blockId;
    private volatile Block block;
    private byte[] signature;
    private int timestamp;
    private Attachment attachment;
    private volatile Long id;
    private volatile String stringId = null;
    private volatile Long senderId;
    private volatile String hash;

    private Transaction(Type type, int timestamp, short deadline, byte[] senderPublicKey, Long recipientId,
                        int amount, int fee, Long referencedTransactionId, byte[] signature) throws NxtException.ValidationException {

        if ((timestamp == 0 && Arrays.equals(senderPublicKey, Genesis.CREATOR_PUBLIC_KEY)) ? (deadline != 0 || fee != 0) : (deadline < 1 || fee <= 0)
                || fee > Nxt.MAX_BALANCE || amount < 0 || amount > Nxt.MAX_BALANCE || type == null) {
            throw new NxtException.ValidationException("Invalid transaction parameters:\n type: " + type + ", timestamp: " + timestamp
                    + ", deadline: " + deadline + ", fee: " + fee + ", amount: " + amount);
        }

        this.timestamp = timestamp;
        this.deadline = deadline;
        this.senderPublicKey = senderPublicKey;
        this.recipientId = recipientId;
        this.amount = amount;
        this.fee = fee;
        this.referencedTransactionId = referencedTransactionId;
        this.signature = signature;
        this.type = type;

    }

    public short getDeadline() {
        return deadline;
    }

    public byte[] getSenderPublicKey() {
        return senderPublicKey;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public int getAmount() {
        return amount;
    }

    public int getFee() {
        return fee;
    }

    public Long getReferencedTransactionId() {
        return referencedTransactionId;
    }

    public int getHeight() {
        return height;
    }

    public byte[] getSignature() {
        return signature;
    }

    public Type getType() {
        return type;
    }

    public Block getBlock() {
        if (block == null) {
            block = Block.findBlock(blockId);
        }
        return block;
    }

    void setBlock(Block block) {
        this.block = block;
        this.blockId = block.getId();
        this.height = block.getHeight();
    }

    public int getTimestamp() {
        return timestamp;
    }

    public int getExpiration() {
        return timestamp + deadline * 60;
    }

    public Attachment getAttachment() {
        return attachment;
    }

    public Long getId() {
        if (id == null) {
            if (signature == null) {
                throw new IllegalStateException("Transaction is not signed yet");
            }
            byte[] hash = Crypto.sha256().digest(getBytes());
            BigInteger bigInteger = new BigInteger(1, new byte[] {hash[7], hash[6], hash[5], hash[4], hash[3], hash[2], hash[1], hash[0]});
            id = bigInteger.longValue();
            stringId = bigInteger.toString();
        }
        return id;
    }

    public String getStringId() {
        if (stringId == null) {
            getId();
            if (stringId == null) {
                stringId = Convert.toUnsignedLong(id);
            }
        }
        return stringId;
    }

    public Long getSenderId() {
        if (senderId == null) {
            senderId = Account.getId(senderPublicKey);
        }
        return senderId;
    }

    @Override
    public int compareTo(Transaction o) {

        if (height < o.height) {
            return -1;
        }
        if (height > o.height) {
            return 1;
        }
        // equivalent to: fee * 1048576L / getSize() > o.fee * 1048576L / o.getSize()
        if (fee * o.getSize() > o.fee * getSize()) {
            return -1;
        }
        if (fee * o.getSize() < o.fee * getSize()) {
            return 1;
        }
        if (timestamp < o.timestamp) {
            return -1;
        }
        if (timestamp > o.timestamp) {
            return 1;
        }
        return 0;
    }

    public byte[] getBytes() {

        ByteBuffer buffer = ByteBuffer.allocate(getSize());
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(type.getType());
        buffer.put(type.getSubtype());
        buffer.putInt(timestamp);
        buffer.putShort(deadline);
        buffer.put(senderPublicKey);
        buffer.putLong(Convert.nullToZero(recipientId));
        buffer.putInt(amount);
        buffer.putInt(fee);
        buffer.putLong(Convert.nullToZero(referencedTransactionId));
        buffer.put(signature);
        if (attachment != null) {
            buffer.put(attachment.getBytes());
        }
        return buffer.array();

    }

    public JSONObject getJSONObject() {

        JSONObject transaction = new JSONObject();

        transaction.put("type", type.getType());
        transaction.put("subtype", type.getSubtype());
        transaction.put("timestamp", timestamp);
        transaction.put("deadline", deadline);
        transaction.put("senderPublicKey", Convert.toHexString(senderPublicKey));
        transaction.put("recipient", Convert.toUnsignedLong(recipientId));
        transaction.put("amount", amount);
        transaction.put("fee", fee);
        transaction.put("referencedTransaction", Convert.toUnsignedLong(referencedTransactionId));
        transaction.put("signature", Convert.toHexString(signature));
        if (attachment != null) {
            transaction.put("attachment", attachment.getJSON());
        }

        return transaction;
    }

    public void sign(String secretPhrase) {

        if (signature != null) {
            throw new IllegalStateException("Transaction already signed");
        }

        signature = new byte[64]; // ugly but signature is needed by getBytes()
        signature = Crypto.sign(getBytes(), secretPhrase);

        try {
            while (!verify()) {
                timestamp++;
                // cfb: Sometimes EC-KCDSA generates unverifiable signatures (X*0 == Y*0 case), Crypto.sign() will be rewritten later
                signature = new byte[64];
                signature = Crypto.sign(getBytes(), secretPhrase);
            }
        } catch (RuntimeException e) {
            Logger.logMessage("Error signing transaction", e);
        }

    }

    public String getHash() {
        if (hash == null) {
            byte[] data = getBytes();
            for (int i = 64; i < 128; i++) {
                data[i] = 0;
            }
            hash = Convert.toHexString(Crypto.sha256().digest(data));
        }
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Transaction && this.getId().equals(((Transaction)o).getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    boolean verify() {
        Account account = Account.getAccount(getSenderId());
        if (account == null) {
            return false;
        }
        byte[] data = getBytes();
        for (int i = 64; i < 128; i++) {
            data[i] = 0;
        }
        return Crypto.verify(signature, data, senderPublicKey) && account.setOrVerify(senderPublicKey, this.getHeight());
    }

    void validateAttachment() throws NxtException.ValidationException {
        type.validateAttachment(this);
    }

    // returns true iff double spending
    boolean isDoubleSpending() {
        Account senderAccount = Account.getAccount(getSenderId());
        if (senderAccount == null) {
            return true;
        }
        synchronized(senderAccount) {
            return type.isDoubleSpending(this, senderAccount, this.amount + this.fee);
        }
    }

    void apply() {
        Account senderAccount = Account.getAccount(getSenderId());
        if (! senderAccount.setOrVerify(senderPublicKey, this.getHeight())) {
            throw new RuntimeException("sender public key mismatch");
            // shouldn't happen, because transactions are already verified somewhere higher in pushBlock...
        }
        senderAccount.apply(this.getHeight());
        Blockchain.transactionHashes.put(getHash(), this);
        Account recipientAccount = Account.getAccount(recipientId);
        if (recipientAccount == null) {
            recipientAccount = Account.addOrGetAccount(recipientId);
        }
        senderAccount.addToBalanceAndUnconfirmedBalance(- (amount + fee) * 100L);
        type.apply(this, senderAccount, recipientAccount);
    }

    // NOTE: when undo is called, lastBlock has already been set to the previous block
    void undo() throws UndoNotSupportedException {
        Account senderAccount = Account.getAccount(senderId);
        senderAccount.undo(this.getHeight());
        senderAccount.addToBalance((amount + fee) * 100L);
        Account recipientAccount = Account.getAccount(recipientId);
        type.undo(this, senderAccount, recipientAccount);
    }

    void updateTotals(Map<Long,Long> accumulatedAmounts, Map<Long,Map<Long,Long>> accumulatedAssetQuantities) {
        Long senderId = getSenderId();
        Long accumulatedAmount = accumulatedAmounts.get(senderId);
        if (accumulatedAmount == null) {
            accumulatedAmount = 0L;
        }
        accumulatedAmounts.put(senderId, accumulatedAmount + (amount + fee) * 100L);
        type.updateTotals(this, accumulatedAmounts, accumulatedAssetQuantities, accumulatedAmount);
    }

    boolean isDuplicate(Map<Type, Set<String>> duplicates) {
        return type.isDuplicate(this, duplicates);
    }

    private static final int TRANSACTION_BYTES_LENGTH = 1 + 1 + 4 + 2 + 32 + 8 + 4 + 4 + 8 + 64;

    int getSize() {
        return TRANSACTION_BYTES_LENGTH + (attachment == null ? 0 : attachment.getSize());
    }

    public static Type findTransactionType(byte type, byte subtype) {
        switch (type) {
            case TYPE_PAYMENT:
                switch (subtype) {
                    case SUBTYPE_PAYMENT_ORDINARY_PAYMENT:
                        return Type.Payment.ORDINARY;
                    default:
                        return null;
                }
            case TYPE_MESSAGING:
                switch (subtype) {
                    case SUBTYPE_MESSAGING_ARBITRARY_MESSAGE:
                        return Type.Messaging.ARBITRARY_MESSAGE;
                    case SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT:
                        return Type.Messaging.ALIAS_ASSIGNMENT;
                    case SUBTYPE_MESSAGING_POLL_CREATION:
                        return Type.Messaging.POLL_CREATION;
                    case SUBTYPE_MESSAGING_VOTE_CASTING:
                        return Type.Messaging.VOTE_CASTING;
                    default:
                        return null;
                }
            case TYPE_COLORED_COINS:
                switch (subtype) {
                    case SUBTYPE_COLORED_COINS_ASSET_ISSUANCE:
                        return Type.ColoredCoins.ASSET_ISSUANCE;
                    case SUBTYPE_COLORED_COINS_ASSET_TRANSFER:
                        return Type.ColoredCoins.ASSET_TRANSFER;
                    case SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT:
                        return Type.ColoredCoins.ASK_ORDER_PLACEMENT;
                    case SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT:
                        return Type.ColoredCoins.BID_ORDER_PLACEMENT;
                    case SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION:
                        return Type.ColoredCoins.ASK_ORDER_CANCELLATION;
                    case SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION:
                        return Type.ColoredCoins.BID_ORDER_CANCELLATION;
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    public static abstract class Type {

        public abstract byte getType();

        public abstract byte getSubtype();

        abstract void loadAttachment(Transaction transaction, ByteBuffer buffer) throws NxtException.ValidationException;

        abstract void loadAttachment(Transaction transaction, JSONObject attachmentData) throws NxtException.ValidationException;

        abstract void validateAttachment(Transaction transaction) throws NxtException.ValidationException;

        // return true iff double spending
        final boolean isDoubleSpending(Transaction transaction, Account senderAccount, int totalAmount) {
            if (senderAccount.getUnconfirmedBalance() < totalAmount * 100L) {
                return true;
            }
            senderAccount.addToUnconfirmedBalance(- totalAmount * 100L);
            return checkDoubleSpending(transaction, senderAccount, totalAmount);
        }

        abstract boolean checkDoubleSpending(Transaction transaction, Account senderAccount, int totalAmount);

        abstract void apply(Transaction transaction, Account senderAccount, Account recipientAccount);

        abstract void undo(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException;

        abstract void updateTotals(Transaction transaction, Map<Long, Long> accumulatedAmounts,
                                   Map<Long, Map<Long, Long>> accumulatedAssetQuantities, Long accumulatedAmount);

        boolean isDuplicate(Transaction transaction, Map<Type, Set<String>> duplicates) {
            return false;
        }

        public static abstract class Payment extends Type {

            @Override
            public final byte getType() {
                return TYPE_PAYMENT;
            }

            public static final Type ORDINARY = new Payment() {

                @Override
                public final byte getSubtype() {
                    return SUBTYPE_PAYMENT_ORDINARY_PAYMENT;
                }

                @Override
                void loadAttachment(Transaction transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                    validateAttachment(transaction);
                }

                @Override
                void loadAttachment(Transaction transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                    validateAttachment(transaction);
                }

                @Override
                void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
                    recipientAccount.addToBalanceAndUnconfirmedBalance(transaction.amount * 100L);
                }

                @Override
                void undo(Transaction transaction, Account senderAccount, Account recipientAccount) {
                    recipientAccount.addToBalanceAndUnconfirmedBalance(-transaction.amount * 100L);
                }

                @Override
                void updateTotals(Transaction transaction, Map<Long, Long> accumulatedAmounts,
                                  Map<Long, Map<Long, Long>> accumulatedAssetQuantities, Long accumulatedAmount) {}

                @Override
                boolean checkDoubleSpending(Transaction transaction, Account senderAccount, int totalAmount) {
                    return false;
                }

                @Override
                void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                    if (transaction.amount <= 0 || transaction.amount >= Nxt.MAX_BALANCE) {
                        throw new NxtException.ValidationException("Invalid payment transaction amount: " + transaction.amount);
                    }
                }

            };
        }

        public static abstract class Messaging extends Type {

            @Override
            public final byte getType() {
                return TYPE_MESSAGING;
            }

            @Override
            boolean checkDoubleSpending(Transaction transaction, Account senderAccount, int totalAmount) {
                return false;
            }

            @Override
            void updateTotals(Transaction transaction, Map<Long, Long> accumulatedAmounts,
                              Map<Long, Map<Long, Long>> accumulatedAssetQuantities, Long accumulatedAmount) {}

            public final static Type ARBITRARY_MESSAGE = new Messaging() {

                @Override
                public final byte getSubtype() {
                    return SUBTYPE_MESSAGING_ARBITRARY_MESSAGE;
                }

                @Override
                void loadAttachment(Transaction transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                    int messageLength = buffer.getInt();
                    if (messageLength > Nxt.MAX_ARBITRARY_MESSAGE_LENGTH) {
                        throw new NxtException.ValidationException("Invalid message length: " + messageLength);
                    }
                    byte[] message = new byte[messageLength];
                    buffer.get(message);
                    transaction.attachment = new Attachment.MessagingArbitraryMessage(message);
                    validateAttachment(transaction);
                }

                @Override
                void loadAttachment(Transaction transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                    String message = (String)attachmentData.get("message");
                    transaction.attachment = new Attachment.MessagingArbitraryMessage(Convert.parseHexString(message));
                    validateAttachment(transaction);
                }

                @Override
                void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {}

                @Override
                void undo(Transaction transaction, Account senderAccount, Account recipientAccount) {}

                @Override
                void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                    if (Blockchain.getLastBlock().getHeight() < Nxt.ARBITRARY_MESSAGES_BLOCK) {
                        throw new NotYetEnabledException("Arbitrary messages not yet enabled at height " + Blockchain.getLastBlock().getHeight());
                    }
                    Attachment.MessagingArbitraryMessage attachment = (Attachment.MessagingArbitraryMessage)transaction.attachment;
                    if (transaction.amount != 0 || attachment.getMessage().length > Nxt.MAX_ARBITRARY_MESSAGE_LENGTH) {
                        throw new NxtException.ValidationException("Invalid transaction amount or message length");
                    }
                }

            };

            public static final Type ALIAS_ASSIGNMENT = new Messaging() {

                @Override
                public final byte getSubtype() {
                    return SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT;
                }

                @Override
                void loadAttachment(Transaction transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                    int aliasLength = buffer.get();
                    if (aliasLength > Nxt.MAX_ALIAS_LENGTH * 3) {
                        throw new NxtException.ValidationException("Max alias length exceeded");
                    }
                    byte[] alias = new byte[aliasLength];
                    buffer.get(alias);
                    int uriLength = buffer.getShort();
                    if (uriLength > Nxt.MAX_ALIAS_URI_LENGTH * 3) {
                        throw new NxtException.ValidationException("Max alias URI length exceeded");
                    }
                    byte[] uri = new byte[uriLength];
                    buffer.get(uri);
                    try {
                        transaction.attachment = new Attachment.MessagingAliasAssignment(new String(alias, "UTF-8"),
                                new String(uri, "UTF-8"));
                        validateAttachment(transaction);
                    } catch (RuntimeException|UnsupportedEncodingException e) {
                        Logger.logDebugMessage("Error parsing alias assignment", e);
                        throw new NxtException.ValidationException(e.toString());
                    }
                }

                @Override
                void loadAttachment(Transaction transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                    String alias = (String)attachmentData.get("alias");
                    String uri = (String)attachmentData.get("uri");
                    transaction.attachment = new Attachment.MessagingAliasAssignment(alias, uri);
                    validateAttachment(transaction);
                }

                @Override
                void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
                    Attachment.MessagingAliasAssignment attachment = (Attachment.MessagingAliasAssignment)transaction.attachment;
                    Block block = transaction.getBlock();
                    Alias.addOrUpdateAlias(senderAccount, transaction.getId(), attachment.getAliasName(), attachment.getAliasURI(), block.getTimestamp());
                }

                @Override
                void undo(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                    // can't tell whether Alias existed before and what was its previous uri
                    throw new UndoNotSupportedException(transaction, "Reversal of alias assignment not supported");
                }

                @Override
                boolean isDuplicate(Transaction transaction, Map<Type, Set<String>> duplicates) {
                    Set<String> myDuplicates = duplicates.get(this);
                    if (myDuplicates == null) {
                        myDuplicates = new HashSet<>();
                        duplicates.put(this, myDuplicates);
                    }
                    Attachment.MessagingAliasAssignment attachment = (Attachment.MessagingAliasAssignment)transaction.attachment;
                    return ! myDuplicates.add(attachment.getAliasName().toLowerCase());
                }

                @Override
                void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                    if (Blockchain.getLastBlock().getHeight() < Nxt.ALIAS_SYSTEM_BLOCK) {
                        throw new NotYetEnabledException("Aliases not yet enabled at height " + Blockchain.getLastBlock().getHeight());
                    }
                    Attachment.MessagingAliasAssignment attachment = (Attachment.MessagingAliasAssignment)transaction.attachment;
                    if (! Genesis.CREATOR_ID.equals(transaction.recipientId) || transaction.amount != 0 || attachment.getAliasName().length() == 0
                            || attachment.getAliasName().length() > Nxt.MAX_ALIAS_LENGTH || attachment.getAliasURI().length() > Nxt.MAX_ALIAS_URI_LENGTH) {
                        throw new NxtException.ValidationException("Invalid alias assignment transaction");
                    }
                    String normalizedAlias = attachment.getAliasName().toLowerCase();
                    for (int i = 0; i < normalizedAlias.length(); i++) {
                        if (Convert.alphabet.indexOf(normalizedAlias.charAt(i)) < 0) {
                            throw new NxtException.ValidationException("Invalid characters in alias name: " + normalizedAlias);
                        }
                    }
                    Alias alias = Alias.getAlias(normalizedAlias);
                    if (alias != null && ! Arrays.equals(alias.getAccount().getPublicKey(), transaction.senderPublicKey)) {
                        throw new NxtException.ValidationException("Alias " + normalizedAlias + " already owned by another account");
                    }
                }

            };

            public final static Type POLL_CREATION = new Messaging() {

                @Override
                public final byte getSubtype() {
                    return SUBTYPE_MESSAGING_POLL_CREATION;
                }

                @Override
                void loadAttachment(Transaction transaction, ByteBuffer buffer) throws NxtException.ValidationException {

                    String pollName, pollDescription;
                    String[] pollOptions;
                    byte minNumberOfOptions, maxNumberOfOptions;
                    boolean optionsAreBinary;

                    try {
                        int pollNameBytesLength = buffer.getShort();
                        if (pollNameBytesLength > 4 * Nxt.MAX_POLL_NAME_LENGTH) {
                            throw new NxtException.ValidationException("Error parsing poll name");
                        }
                        byte[] pollNameBytes = new byte[pollNameBytesLength];
                        buffer.get(pollNameBytes);
                        pollName = (new String(pollNameBytes, "UTF-8")).trim();
                    } catch (RuntimeException | UnsupportedEncodingException e) {
                        Logger.logDebugMessage("Error parsing poll name", e);
                        throw new NxtException.ValidationException(e.toString());
                    }

                    try {
                        int pollDescriptionBytesLength = buffer.getShort();
                        if (pollDescriptionBytesLength > 4 * Nxt.MAX_POLL_DESCRIPTION_LENGTH) {
                            throw new NxtException.ValidationException("Error parsing poll description");
                        }
                        byte[] pollDescriptionBytes = new byte[pollDescriptionBytesLength];
                        buffer.get(pollDescriptionBytes);
                        pollDescription = (new String(pollDescriptionBytes, "UTF-8")).trim();
                    } catch (RuntimeException | UnsupportedEncodingException e) {
                        Logger.logDebugMessage("Error parsing poll description", e);
                        throw new NxtException.ValidationException(e.toString());
                    }

                    try {
                        int numberOfOptions = buffer.get();
                        pollOptions = new String[numberOfOptions];
                        for (int i = 0; i < numberOfOptions; i++) {
                            int pollOptionBytesLength = buffer.getShort();
                            if (pollOptionBytesLength > 4 * Nxt.MAX_POLL_OPTION_LENGTH) {
                                throw new NxtException.ValidationException("Error parsing poll options");
                            }
                            byte[] pollOptionBytes = new byte[pollOptionBytesLength];
                            buffer.get(pollOptionBytes);
                            pollOptions[i] = (new String(pollOptionBytes, "UTF-8")).trim();
                        }
                    } catch (RuntimeException | UnsupportedEncodingException e) {
                        Logger.logDebugMessage("Error parsing poll options", e);
                        throw new NxtException.ValidationException(e.toString());
                    }

                    try {
                        minNumberOfOptions = buffer.get();
                        maxNumberOfOptions = buffer.get();
                        optionsAreBinary = buffer.get() != 0;
                    } catch (RuntimeException e) {
                        Logger.logDebugMessage("Error parsing poll creation parameters", e);
                        throw new NxtException.ValidationException(e.toString());
                    }

                    transaction.attachment = new Attachment.MessagingPollCreation(pollName, pollDescription, pollOptions,
                            minNumberOfOptions, maxNumberOfOptions, optionsAreBinary);
                    validateAttachment(transaction);

                }

                @Override
                void loadAttachment(Transaction transaction, JSONObject attachmentData) throws NxtException.ValidationException {

                    String pollName = ((String)attachmentData.get("name")).trim();
                    String pollDescription = ((String)attachmentData.get("description")).trim();
                    JSONArray options = (JSONArray)attachmentData.get("options");
                    String[] pollOptions = new String[options.size()];
                    for (int i = 0; i < pollOptions.length; i++) {
                        pollOptions[i] = ((String)options.get(i)).trim();
                    }
                    byte minNumberOfOptions = ((Long)attachmentData.get("minNumberOfOptions")).byteValue();
                    byte maxNumberOfOptions = ((Long)attachmentData.get("maxNumberOfOptions")).byteValue();
                    boolean optionsAreBinary = (Boolean)attachmentData.get("optionsAreBinary");

                    transaction.attachment = new Attachment.MessagingPollCreation(pollName, pollDescription, pollOptions,
                            minNumberOfOptions, maxNumberOfOptions, optionsAreBinary);
                    validateAttachment(transaction);

                }

                @Override
                void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
                    Attachment.MessagingPollCreation attachment = (Attachment.MessagingPollCreation)transaction.attachment;
                    Poll.addPoll(transaction.getId(), attachment.getPollName(), attachment.getPollDescription(), attachment.getPollOptions(),
                            attachment.getMinNumberOfOptions(), attachment.getMaxNumberOfOptions(), attachment.isOptionsAreBinary());
                }

                @Override
                void undo(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                    throw new UndoNotSupportedException(transaction, "Reversal of poll creation not supported");
                }

                @Override
                void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                    if (Blockchain.getLastBlock().getHeight() < Nxt.VOTING_SYSTEM_BLOCK) {
                        throw new NotYetEnabledException("Voting System not yet enabled at height " + Blockchain.getLastBlock().getHeight());
                    }
                    Attachment.MessagingPollCreation attachment = (Attachment.MessagingPollCreation)transaction.attachment;
                    for (int i = 0; i < attachment.getPollOptions().length; i++) {
                        if (attachment.getPollOptions()[i].length() > Nxt.MAX_POLL_OPTION_LENGTH) {
                            throw new NxtException.ValidationException("Invalid poll options length");
                        }
                    }
                    if (attachment.getPollName().length() > Nxt.MAX_POLL_NAME_LENGTH || attachment.getPollDescription().length() > Nxt.MAX_POLL_DESCRIPTION_LENGTH
                            || attachment.getPollOptions().length > 100 || transaction.amount != 0 || ! Genesis.CREATOR_ID.equals(transaction.recipientId)) {
                        throw new NxtException.ValidationException("Invalid poll creation transaction");
                    }
                }

            };

            public final static Type VOTE_CASTING = new Messaging() {

                @Override
                public final byte getSubtype() {
                    return SUBTYPE_MESSAGING_VOTE_CASTING;
                }

                @Override
                void loadAttachment(Transaction transaction, ByteBuffer buffer) throws NxtException.ValidationException {

                    Long pollId;
                    byte[] pollVote;

                    try {
                        pollId = buffer.getLong();
                        int numberOfOptions = buffer.get();
                        if (numberOfOptions > 100) {
                            throw new NxtException.ValidationException("Error parsing vote casting parameters");
                        }
                        pollVote = new byte[numberOfOptions];
                        buffer.get(pollVote);
                    } catch (RuntimeException e) {
                        throw new NxtException.ValidationException(e.toString());
                    }

                    transaction.attachment = new Attachment.MessagingVoteCasting(pollId, pollVote);
                    validateAttachment(transaction);

                }

                @Override
                void loadAttachment(Transaction transaction, JSONObject attachmentData) throws NxtException.ValidationException {

                    Long pollId = (Long)attachmentData.get("pollId");
                    JSONArray vote = (JSONArray)attachmentData.get("vote");
                    byte[] pollVote = new byte[vote.size()];
                    for (int i = 0; i < pollVote.length; i++) {
                        pollVote[i] = ((Long)vote.get(i)).byteValue();
                    }

                    transaction.attachment = new Attachment.MessagingVoteCasting(pollId, pollVote);
                    validateAttachment(transaction);

                }

                @Override
                void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
                    Attachment.MessagingVoteCasting attachment = (Attachment.MessagingVoteCasting)transaction.attachment;
                    Poll poll = Poll.getPoll(attachment.getPollId());
                    if (poll != null) {
                        Vote vote = Vote.addVote(transaction.getId(), attachment.getPollId(), transaction.getSenderId(), attachment.getPollVote());
                        poll.addVoter(transaction.getSenderId(), vote.getId());
                    }
                }

                @Override
                void undo(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                    throw new UndoNotSupportedException(transaction, "Reversal of vote casting not supported");
                }

                @Override
                void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                    if (Blockchain.getLastBlock().getHeight() < Nxt.VOTING_SYSTEM_BLOCK) {
                        throw new NotYetEnabledException("Voting System not yet enabled at height " + Blockchain.getLastBlock().getHeight());
                    }
                    if (transaction.amount != 0 || ! Genesis.CREATOR_ID.equals(transaction.recipientId)) {
                        throw new NxtException.ValidationException("Invalid voting transaction");
                    }
                }

            };
        }

        public static abstract class ColoredCoins extends Type {

            @Override
            public final byte getType() {
                return TYPE_COLORED_COINS;
            }

            public static final Type ASSET_ISSUANCE = new ColoredCoins() {

                @Override
                public final byte getSubtype() {
                    return SUBTYPE_COLORED_COINS_ASSET_ISSUANCE;
                }

                @Override
                void loadAttachment(Transaction transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                    int nameLength = buffer.get();
                    if (nameLength > 30) {
                        throw new NxtException.ValidationException("Max asset name length exceeded");
                    }
                    byte[] name = new byte[nameLength];
                    buffer.get(name);
                    int descriptionLength = buffer.getShort();
                    if (descriptionLength > 300) {
                        throw new NxtException.ValidationException("Max asset description length exceeded");
                    }
                    byte[] description = new byte[descriptionLength];
                    buffer.get(description);
                    int quantity = buffer.getInt();
                    try {
                        transaction.attachment = new Attachment.ColoredCoinsAssetIssuance(new String(name, "UTF-8").intern(),
                                new String(description, "UTF-8").intern(), quantity);
                        validateAttachment(transaction);
                    } catch (RuntimeException|UnsupportedEncodingException e) {
                        throw new NxtException.ValidationException(e.toString());
                    }
                }

                @Override
                void loadAttachment(Transaction transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                    String name = (String)attachmentData.get("name");
                    String description = (String)attachmentData.get("description");
                    int quantity = ((Long)attachmentData.get("quantity")).intValue();
                    transaction.attachment = new Attachment.ColoredCoinsAssetIssuance(name.trim(), description.trim(), quantity);
                    validateAttachment(transaction);
                }

                @Override
                boolean checkDoubleSpending(Transaction transaction, Account senderAccount, int totalAmount) {
                    return false;
                }

                @Override
                void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
                    Attachment.ColoredCoinsAssetIssuance attachment = (Attachment.ColoredCoinsAssetIssuance)transaction.attachment;
                    Long assetId = transaction.getId();
                    Asset.addAsset(assetId, transaction.getSenderId(), attachment.getName(), attachment.getDescription(), attachment.getQuantity());
                    senderAccount.addToAssetAndUnconfirmedAssetBalance(assetId, attachment.getQuantity());
                }

                @Override
                void undo(Transaction transaction, Account senderAccount, Account recipientAccount) {
                    Attachment.ColoredCoinsAssetIssuance attachment = (Attachment.ColoredCoinsAssetIssuance)transaction.attachment;
                    Long assetId = transaction.getId();
                    senderAccount.addToAssetAndUnconfirmedAssetBalance(assetId, -attachment.getQuantity());
                    Asset.removeAsset(assetId);
                }

                @Override
                void updateTotals(Transaction transaction, Map<Long, Long> accumulatedAmounts,
                                 Map<Long, Map<Long, Long>> accumulatedAssetQuantities, Long accumulatedAmount) {}

                @Override
                boolean isDuplicate(Transaction transaction, Map<Type, Set<String>> duplicates) {
                    Set<String> myDuplicates = duplicates.get(this);
                    if (myDuplicates == null) {
                        myDuplicates = new HashSet<>();
                        duplicates.put(this, myDuplicates);
                    }
                    Attachment.ColoredCoinsAssetIssuance attachment = (Attachment.ColoredCoinsAssetIssuance)transaction.attachment;
                    return ! myDuplicates.add(attachment.getName().toLowerCase());
                }

                @Override
                void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                    if (Blockchain.getLastBlock().getHeight() < Nxt.ASSET_EXCHANGE_BLOCK) {
                        throw new NotYetEnabledException("Asset Exchange not yet enabled at height " + Blockchain.getLastBlock().getHeight());
                    }
                    Attachment.ColoredCoinsAssetIssuance attachment = (Attachment.ColoredCoinsAssetIssuance)transaction.attachment;
                    if (!Genesis.CREATOR_ID.equals(transaction.recipientId) || transaction.amount != 0 || transaction.fee < Nxt.ASSET_ISSUANCE_FEE
                            || attachment.getName().length() < 3 || attachment.getName().length() > 10 || attachment.getDescription().length() > 1000
                            || attachment.getQuantity() <= 0 || attachment.getQuantity() > Nxt.MAX_ASSET_QUANTITY) {
                        throw new NxtException.ValidationException("Invalid asset issue transaction");
                    }
                    String normalizedName = attachment.getName().toLowerCase();
                    for (int i = 0; i < normalizedName.length(); i++) {
                        if (Convert.alphabet.indexOf(normalizedName.charAt(i)) < 0) {
                            throw new NxtException.ValidationException("Invalid asset name " + normalizedName);
                        }
                    }
                    if (Asset.getAsset(normalizedName) != null) {
                        throw new NxtException.ValidationException("Asset " + normalizedName + " already exists");
                    }
                }

            };

            public static final Type ASSET_TRANSFER = new ColoredCoins() {

                @Override
                public final byte getSubtype() {
                    return SUBTYPE_COLORED_COINS_ASSET_TRANSFER;
                }

                @Override
                void loadAttachment(Transaction transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                    Long assetId = Convert.zeroToNull(buffer.getLong());
                    int quantity = buffer.getInt();
                    transaction.attachment = new Attachment.ColoredCoinsAssetTransfer(assetId, quantity);
                    validateAttachment(transaction);
                }

                @Override
                void loadAttachment(Transaction transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                    Long assetId = Convert.parseUnsignedLong((String) attachmentData.get("asset"));
                    int quantity = ((Long)attachmentData.get("quantity")).intValue();
                    transaction.attachment = new Attachment.ColoredCoinsAssetTransfer(assetId, quantity);
                    validateAttachment(transaction);
                }

                @Override
                boolean checkDoubleSpending(Transaction transaction, Account account, int totalAmount) {
                    Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer)transaction.attachment;
                    Integer unconfirmedAssetBalance = account.getUnconfirmedAssetBalance(attachment.getAssetId());
                    if (unconfirmedAssetBalance == null || unconfirmedAssetBalance < attachment.getQuantity()) {
                        account.addToUnconfirmedBalance(totalAmount * 100L);
                        return true;
                    } else {
                        account.addToUnconfirmedAssetBalance(attachment.getAssetId(), -attachment.getQuantity());
                        return false;
                    }
                }

                @Override
                void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
                    Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer)transaction.attachment;
                    senderAccount.addToAssetAndUnconfirmedAssetBalance(attachment.getAssetId(), -attachment.getQuantity());
                    recipientAccount.addToAssetAndUnconfirmedAssetBalance(attachment.getAssetId(), attachment.getQuantity());
                }

                @Override
                void undo(Transaction transaction, Account senderAccount, Account recipientAccount) {
                    Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer)transaction.attachment;
                    senderAccount.addToAssetAndUnconfirmedAssetBalance(attachment.getAssetId(), attachment.getQuantity());
                    recipientAccount.addToAssetAndUnconfirmedAssetBalance(attachment.getAssetId(), -attachment.getQuantity());
                }

                @Override
                void updateTotals(Transaction transaction, Map<Long, Long> accumulatedAmounts,
                                  Map<Long, Map<Long, Long>> accumulatedAssetQuantities, Long accumulatedAmount) {
                    Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer) transaction.attachment;
                    Map<Long, Long> accountAccumulatedAssetQuantities = accumulatedAssetQuantities.get(transaction.getSenderId());
                    if (accountAccumulatedAssetQuantities == null) {
                        accountAccumulatedAssetQuantities = new HashMap<>();
                        accumulatedAssetQuantities.put(transaction.getSenderId(), accountAccumulatedAssetQuantities);
                    }
                    Long assetAccumulatedAssetQuantities = accountAccumulatedAssetQuantities.get(attachment.getAssetId());
                    if (assetAccumulatedAssetQuantities == null) {
                        assetAccumulatedAssetQuantities = 0L;
                    }
                    accountAccumulatedAssetQuantities.put(attachment.getAssetId(), assetAccumulatedAssetQuantities + attachment.getQuantity());
                }

                @Override
                void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                    if (Blockchain.getLastBlock().getHeight() < Nxt.ASSET_EXCHANGE_BLOCK) {
                        throw new NotYetEnabledException("Asset Exchange not yet enabled at height " + Blockchain.getLastBlock().getHeight());
                    }
                    Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer)transaction.attachment;
                    if (transaction.amount != 0 || attachment.getQuantity() <= 0 || attachment.getQuantity() > Nxt.MAX_ASSET_QUANTITY) {
                        throw new NxtException.ValidationException("Invalid asset quantity or transaction amount");
                    }
                }

            };

            abstract static class ColoredCoinsOrderPlacement extends ColoredCoins {

                abstract Attachment.ColoredCoinsOrderPlacement makeAttachment(Long asset, int quantity, long price);

                @Override
                final void loadAttachment(Transaction transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                    Long assetId = Convert.zeroToNull(buffer.getLong());
                    int quantity = buffer.getInt();
                    long price = buffer.getLong();
                    transaction.attachment = makeAttachment(assetId, quantity, price);
                    validateAttachment(transaction);
                }

                @Override
                final void loadAttachment(Transaction transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                    Long assetId = Convert.parseUnsignedLong((String) attachmentData.get("asset"));
                    int quantity = ((Long)attachmentData.get("quantity")).intValue();
                    long price = (Long)attachmentData.get("price");
                    transaction.attachment = makeAttachment(assetId, quantity, price);
                    validateAttachment(transaction);
                }

                @Override
                final void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                    if (Blockchain.getLastBlock().getHeight() < Nxt.ASSET_EXCHANGE_BLOCK) {
                        throw new NotYetEnabledException("Asset Exchange not yet enabled at height " + Blockchain.getLastBlock().getHeight());
                    }
                    Attachment.ColoredCoinsOrderPlacement attachment = (Attachment.ColoredCoinsOrderPlacement)transaction.attachment;
                    if (! Genesis.CREATOR_ID.equals(transaction.recipientId) || transaction.amount != 0
                            || attachment.getQuantity() <= 0 || attachment.getQuantity() > Nxt.MAX_ASSET_QUANTITY
                            || attachment.getPrice() <= 0 || attachment.getPrice() > Nxt.MAX_BALANCE * 100L) {
                        throw new NxtException.ValidationException("Invalid order quantity or price");
                    }
                }

            }

            public static final Type ASK_ORDER_PLACEMENT = new ColoredCoinsOrderPlacement() {

                @Override
                public final byte getSubtype() {
                    return SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT;
                }

                final Attachment.ColoredCoinsOrderPlacement makeAttachment(Long assetId, int quantity, long price) {
                    return new Attachment.ColoredCoinsAskOrderPlacement(assetId, quantity, price);
                }

                @Override
                boolean checkDoubleSpending(Transaction transaction, Account account, int totalAmount) {
                    Attachment.ColoredCoinsAskOrderPlacement attachment = (Attachment.ColoredCoinsAskOrderPlacement)transaction.attachment;
                    Integer unconfirmedAssetBalance = account.getUnconfirmedAssetBalance(attachment.getAssetId());
                    if (unconfirmedAssetBalance == null || unconfirmedAssetBalance < attachment.getQuantity()) {
                        account.addToUnconfirmedBalance(totalAmount * 100L);
                        return true;
                    } else {
                        account.addToUnconfirmedAssetBalance(attachment.getAssetId(), -attachment.getQuantity());
                        return false;
                    }
                }

                @Override
                void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
                    Attachment.ColoredCoinsAskOrderPlacement attachment = (Attachment.ColoredCoinsAskOrderPlacement)transaction.attachment;
                    if (Asset.getAsset(attachment.getAssetId()) != null) {
                        Order.Ask.addOrder(transaction.getId(), senderAccount, attachment.getAssetId(), attachment.getQuantity(), attachment.getPrice());
                    }
                }

                @Override
                void undo(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                    Attachment.ColoredCoinsAskOrderPlacement attachment = (Attachment.ColoredCoinsAskOrderPlacement)transaction.attachment;
                    Order.Ask askOrder = Order.Ask.removeOrder(transaction.getId());
                    if (askOrder == null || askOrder.getQuantity() != attachment.getQuantity() || ! askOrder.getAssetId().equals(attachment.getAssetId())) {
                        //undoing of partially filled orders not supported yet
                        throw new UndoNotSupportedException(transaction, "Ask order already filled");
                    }
                    senderAccount.addToAssetAndUnconfirmedAssetBalance(attachment.getAssetId(), attachment.getQuantity());
                }

                @Override
                void updateTotals(Transaction transaction, Map<Long, Long> accumulatedAmounts,
                                 Map<Long, Map<Long, Long>> accumulatedAssetQuantities, Long accumulatedAmount) {
                    Attachment.ColoredCoinsAskOrderPlacement attachment = (Attachment.ColoredCoinsAskOrderPlacement) transaction.attachment;
                    Map<Long, Long> accountAccumulatedAssetQuantities = accumulatedAssetQuantities.get(transaction.getSenderId());
                    if (accountAccumulatedAssetQuantities == null) {
                        accountAccumulatedAssetQuantities = new HashMap<>();
                        accumulatedAssetQuantities.put(transaction.getSenderId(), accountAccumulatedAssetQuantities);
                    }
                    Long assetAccumulatedAssetQuantities = accountAccumulatedAssetQuantities.get(attachment.getAssetId());
                    if (assetAccumulatedAssetQuantities == null) {
                        assetAccumulatedAssetQuantities = 0L;
                    }
                    accountAccumulatedAssetQuantities.put(attachment.getAssetId(), assetAccumulatedAssetQuantities + attachment.getQuantity());
                }

            };

            public final static Type BID_ORDER_PLACEMENT = new ColoredCoinsOrderPlacement() {

                @Override
                public final byte getSubtype() {
                    return SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT;
                }

                final Attachment.ColoredCoinsOrderPlacement makeAttachment(Long asset, int quantity, long price) {
                    return new Attachment.ColoredCoinsBidOrderPlacement(asset, quantity, price);
                }

                @Override
                boolean checkDoubleSpending(Transaction transaction, Account account, int totalAmount) {
                    Attachment.ColoredCoinsBidOrderPlacement attachment = (Attachment.ColoredCoinsBidOrderPlacement) transaction.attachment;
                    if (account.getUnconfirmedBalance() < attachment.getQuantity() * attachment.getPrice()) {
                        account.addToUnconfirmedBalance(totalAmount * 100L);
                        return true;
                    } else {
                        account.addToUnconfirmedBalance(-attachment.getQuantity() * attachment.getPrice());
                        return false;
                    }
                }

                @Override
                void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
                    Attachment.ColoredCoinsBidOrderPlacement attachment = (Attachment.ColoredCoinsBidOrderPlacement)transaction.attachment;
                    if (Asset.getAsset(attachment.getAssetId()) != null) {
                        Order.Bid.addOrder(transaction.getId(), senderAccount, attachment.getAssetId(), attachment.getQuantity(), attachment.getPrice());
                    }
                }

                @Override
                void undo(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                    Attachment.ColoredCoinsBidOrderPlacement attachment = (Attachment.ColoredCoinsBidOrderPlacement)transaction.attachment;
                    Order.Bid bidOrder = Order.Bid.removeOrder(transaction.getId());
                    if (bidOrder == null || bidOrder.getQuantity() != attachment.getQuantity() || ! bidOrder.getAssetId().equals(attachment.getAssetId())) {
                        //undoing of partially filled orders not supported yet
                        throw new UndoNotSupportedException(transaction, "Bid order already filled");
                    }
                    senderAccount.addToBalanceAndUnconfirmedBalance(attachment.getQuantity() * attachment.getPrice());
                }

                @Override
                void updateTotals(Transaction transaction, Map<Long, Long> accumulatedAmounts,
                                 Map<Long, Map<Long, Long>> accumulatedAssetQuantities, Long accumulatedAmount) {
                    Attachment.ColoredCoinsBidOrderPlacement attachment = (Attachment.ColoredCoinsBidOrderPlacement) transaction.attachment;
                    accumulatedAmounts.put(transaction.getSenderId(), accumulatedAmount + attachment.getQuantity() * attachment.getPrice());
                }

            };

            abstract static class ColoredCoinsOrderCancellation extends ColoredCoins {

                @Override
                final void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                    if (Blockchain.getLastBlock().getHeight() < Nxt.ASSET_EXCHANGE_BLOCK) {
                        throw new NotYetEnabledException("Asset Exchange not yet enabled at height " + Blockchain.getLastBlock().getHeight());
                    }
                    if (! Genesis.CREATOR_ID.equals(transaction.recipientId) || transaction.amount != 0) {
                        throw new NxtException.ValidationException("Invalid transaction amount or recipient");
                    }
                }

                @Override
                final boolean checkDoubleSpending(Transaction transaction, Account senderAccount, int totalAmount) {
                    return false;
                }

                @Override
                final void updateTotals(Transaction transaction, Map<Long, Long> accumulatedAmounts,
                                  Map<Long, Map<Long, Long>> accumulatedAssetQuantities, Long accumulatedAmount) {}

                @Override
                final void undo(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                    throw new UndoNotSupportedException(transaction, "Reversal of order cancellation not supported");
                }

            }

            public static final Type ASK_ORDER_CANCELLATION = new ColoredCoinsOrderCancellation() {

                @Override
                public final byte getSubtype() {
                    return SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION;
                }

                @Override
                void loadAttachment(Transaction transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                    transaction.attachment = new Attachment.ColoredCoinsAskOrderCancellation(Convert.zeroToNull(buffer.getLong()));
                    validateAttachment(transaction);
                }

                @Override
                void loadAttachment(Transaction transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                    transaction.attachment = new Attachment.ColoredCoinsAskOrderCancellation(Convert.parseUnsignedLong((String) attachmentData.get("order")));
                    validateAttachment(transaction);
                }

                @Override
                void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
                    Attachment.ColoredCoinsAskOrderCancellation attachment = (Attachment.ColoredCoinsAskOrderCancellation)transaction.attachment;
                    Order order = Order.Ask.removeOrder(attachment.getOrderId());
                    if (order != null) {
                        senderAccount.addToAssetAndUnconfirmedAssetBalance(order.getAssetId(), order.getQuantity());
                    }
                }

            };

            public static final Type BID_ORDER_CANCELLATION = new ColoredCoinsOrderCancellation() {

                @Override
                public final byte getSubtype() {
                    return SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION;
                }

                @Override
                void loadAttachment(Transaction transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                    transaction.attachment = new Attachment.ColoredCoinsBidOrderCancellation(Convert.zeroToNull(buffer.getLong()));
                    validateAttachment(transaction);
                }

                @Override
                void loadAttachment(Transaction transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                    transaction.attachment = new Attachment.ColoredCoinsBidOrderCancellation(Convert.parseUnsignedLong((String) attachmentData.get("order")));
                    validateAttachment(transaction);
                }

                @Override
                void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
                    Attachment.ColoredCoinsBidOrderCancellation attachment = (Attachment.ColoredCoinsBidOrderCancellation)transaction.attachment;
                    Order order = Order.Bid.removeOrder(attachment.getOrderId());
                    if (order != null) {
                        senderAccount.addToBalanceAndUnconfirmedBalance(order.getQuantity() * order.getPrice());
                    }
                }

            };
        }
    }

    public static final class UndoNotSupportedException extends NxtException {

        private final Transaction transaction;

        public UndoNotSupportedException(Transaction transaction, String message) {
            super(message);
            this.transaction = transaction;
        }

        public Transaction getTransaction() {
            return transaction;
        }

    }

    public static final class NotYetEnabledException extends NxtException.ValidationException {

        public NotYetEnabledException(String message) {
            super(message);
        }

        public NotYetEnabledException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
