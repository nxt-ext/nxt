package nxt;

import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

final class TransactionImpl implements Transaction {

    private final short deadline;
    private final byte[] senderPublicKey;
    private final Long recipientId;
    private final int amount;
    private final int fee;
    private final Long referencedTransactionId;
    private final TransactionType type;

    private int height = Integer.MAX_VALUE;
    private Long blockId;
    private volatile Block block;
    private byte[] signature;
    private int timestamp;
    private int blockTimestamp = -1;
    private Attachment attachment;
    private volatile Long id;
    private volatile String stringId = null;
    private volatile Long senderId;
    private volatile String hash;

    TransactionImpl(TransactionType type, int timestamp, short deadline, byte[] senderPublicKey, Long recipientId,
                    int amount, int fee, Long referencedTransactionId, byte[] signature) throws NxtException.ValidationException {

        if ((timestamp == 0 && Arrays.equals(senderPublicKey, Genesis.CREATOR_PUBLIC_KEY)) ? (deadline != 0 || fee != 0) : (deadline < 1 || fee <= 0)
                || fee > Constants.MAX_BALANCE || amount < 0 || amount > Constants.MAX_BALANCE || type == null) {
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

    TransactionImpl(TransactionType type, int timestamp, short deadline, byte[] senderPublicKey, Long recipientId,
                    int amount, int fee, Long referencedTransactionId, byte[] signature, Long blockId, int height,
                    Long id, Long senderId, Attachment attachment, byte[] hash, int blockTimestamp)
            throws NxtException.ValidationException {
        this(type, timestamp, deadline, senderPublicKey, recipientId, amount, fee, referencedTransactionId, signature);
        this.blockId = blockId;
        this.height = height;
        this.id = id;
        this.senderId = senderId;
        this.attachment = attachment;
        this.hash = hash == null ? null : Convert.toHexString(hash);
        this.blockTimestamp = blockTimestamp;
    }

    @Override
    public short getDeadline() {
        return deadline;
    }

    @Override
    public byte[] getSenderPublicKey() {
        return senderPublicKey;
    }

    @Override
    public Long getRecipientId() {
        return recipientId;
    }

    @Override
    public int getAmount() {
        return amount;
    }

    @Override
    public int getFee() {
        return fee;
    }

    @Override
    public Long getReferencedTransactionId() {
        return referencedTransactionId;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public byte[] getSignature() {
        return signature;
    }

    @Override
    public TransactionType getType() {
        return type;
    }

    @Override
    public Long getBlockId() {
        return blockId;
    }

    @Override
    public Block getBlock() {
        if (block == null) {
            block = BlockDb.findBlock(blockId);
        }
        return block;
    }

    void setBlock(Block block) {
        this.block = block;
        this.blockId = block.getId();
        this.height = block.getHeight();
        this.blockTimestamp = block.getTimestamp();
    }

    @Override
    public int getTimestamp() {
        return timestamp;
    }

    @Override
    public int getBlockTimestamp() {
        return blockTimestamp;
    }

    @Override
    public int getExpiration() {
        return timestamp + deadline * 60;
    }

    @Override
    public Attachment getAttachment() {
        return attachment;
    }

    void setAttachment(Attachment attachment) {
        this.attachment = attachment;
    }

    @Override
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

    @Override
    public String getStringId() {
        if (stringId == null) {
            getId();
            if (stringId == null) {
                stringId = Convert.toUnsignedLong(id);
            }
        }
        return stringId;
    }

    @Override
    public Long getSenderId() {
        if (senderId == null) {
            senderId = Account.getId(senderPublicKey);
        }
        return senderId;
    }

    @Override
    public int compareTo(Transaction o) {

        if (height < o.getHeight()) {
            return -1;
        }
        if (height > o.getHeight()) {
            return 1;
        }
        // equivalent to: fee * 1048576L / getSize() > o.fee * 1048576L / o.getSize()
        if (fee * ((TransactionImpl)o).getSize() > o.getFee() * getSize()) {
            return -1;
        }
        if (fee * ((TransactionImpl)o).getSize() < o.getFee() * getSize()) {
            return 1;
        }
        if (timestamp < o.getTimestamp()) {
            return -1;
        }
        if (timestamp > o.getTimestamp()) {
            return 1;
        }
        if (getId() < o.getId()) {
            return -1;
        }
        if (getId() > o.getId()) {
            return 1;
        }
        return 0;

    }

    static final int TRANSACTION_BYTES_LENGTH = 1 + 1 + 4 + 2 + 32 + 8 + 4 + 4 + 8 + 64;

    int getSize() {
        return TRANSACTION_BYTES_LENGTH + (attachment == null ? 0 : attachment.getSize());
    }

    @Override
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
        buffer.put(signature != null ? signature : new byte[64]);
        if (attachment != null) {
            buffer.put(attachment.getBytes());
        }
        return buffer.array();

    }

    @Override
    public JSONObject getJSONObject() {

        JSONObject json = new JSONObject();

        json.put("type", type.getType());
        json.put("subtype", type.getSubtype());
        json.put("timestamp", timestamp);
        json.put("deadline", deadline);
        json.put("senderPublicKey", Convert.toHexString(senderPublicKey));
        json.put("recipient", Convert.toUnsignedLong(recipientId));
        json.put("amount", amount);
        json.put("fee", fee);
        json.put("referencedTransaction", Convert.toUnsignedLong(referencedTransactionId));
        json.put("signature", Convert.toHexString(signature));
        if (attachment != null) {
            json.put("attachment", attachment.getJSON());
        }

        return json;
    }

    @Override
    public void sign(String secretPhrase) {
        if (signature != null) {
            throw new IllegalStateException("Transaction already signed");
        }
        signature = Crypto.sign(getBytes(), secretPhrase);
    }

    @Override
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
        return o instanceof TransactionImpl && this.getId().equals(((Transaction)o).getId());
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

    // returns false iff double spending
    boolean applyUnconfirmed() {
        Account senderAccount = Account.getAccount(getSenderId());
        if (senderAccount == null) {
            return false;
        }
        synchronized(senderAccount) {
            return type.applyUnconfirmed(this, senderAccount);
        }
    }

    void apply() {
        Account senderAccount = Account.getAccount(getSenderId());
        senderAccount.apply(senderPublicKey, this.getHeight());
        Account recipientAccount = Account.getAccount(recipientId);
        if (recipientAccount == null) {
            recipientAccount = Account.addOrGetAccount(recipientId);
        }
        type.apply(this, senderAccount, recipientAccount);
    }

    void undoUnconfirmed() {
        Account senderAccount = Account.getAccount(getSenderId());
        type.undoUnconfirmed(this, senderAccount);
    }

    // NOTE: when undo is called, lastBlock has already been set to the previous block
    void undo() throws TransactionType.UndoNotSupportedException {
        Account senderAccount = Account.getAccount(senderId);
        senderAccount.undo(this.getHeight());
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

    boolean isDuplicate(Map<TransactionType, Set<String>> duplicates) {
        return type.isDuplicate(this, duplicates);
    }

}
