package nxt;

import nxt.crypto.Crypto;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

final class BlockImpl implements Block {

    private final int version;
    private final int timestamp;
    private final Long previousBlockId;
    private final byte[] generatorPublicKey;
    private final byte[] previousBlockHash;
    private final int totalAmount;
    private final int totalFee;
    private final int payloadLength;
    private final byte[] generationSignature;
    private final byte[] payloadHash;
    private final List<Long> transactionIds;
    private final List<TransactionImpl> blockTransactions;

    private byte[] blockSignature;
    private BigInteger cumulativeDifficulty = BigInteger.ZERO;
    private long baseTarget = Nxt.initialBaseTarget;
    private volatile Long nextBlockId;
    private int height = -1;
    private volatile Long id;
    private volatile String stringId = null;
    private volatile Long generatorId;


    BlockImpl(int version, int timestamp, Long previousBlockId, int totalAmount, int totalFee, int payloadLength, byte[] payloadHash,
              byte[] generatorPublicKey, byte[] generationSignature, byte[] blockSignature, byte[] previousBlockHash, List<TransactionImpl> transactions)
            throws NxtException.ValidationException {

        if (transactions.size() > Nxt.MAX_NUMBER_OF_TRANSACTIONS) {
            throw new NxtException.ValidationException("attempted to create a block with " + transactions.size() + " transactions");
        }

        if (payloadLength > Nxt.MAX_PAYLOAD_LENGTH || payloadLength < 0) {
            throw new NxtException.ValidationException("attempted to create a block with payloadLength " + payloadLength);
        }

        this.version = version;
        this.timestamp = timestamp;
        this.previousBlockId = previousBlockId;
        this.totalAmount = totalAmount;
        this.totalFee = totalFee;
        this.payloadLength = payloadLength;
        this.payloadHash = payloadHash;
        this.generatorPublicKey = generatorPublicKey;
        this.generationSignature = generationSignature;
        this.blockSignature = blockSignature;

        this.previousBlockHash = previousBlockHash;
        this.blockTransactions = Collections.unmodifiableList(transactions);
        List<Long> transactionIds = new ArrayList<>(this.blockTransactions.size());
        Long previousId = Long.MIN_VALUE;
        for (Transaction transaction : this.blockTransactions) {
            if (transaction.getId() < previousId) {
                throw new NxtException.ValidationException("Block transactions are not sorted!");
            }
            transactionIds.add(transaction.getId());
            previousId = transaction.getId();
        }
        this.transactionIds = Collections.unmodifiableList(transactionIds);

    }

    BlockImpl(int version, int timestamp, Long previousBlockId, int totalAmount, int totalFee, int payloadLength, byte[] payloadHash,
              byte[] generatorPublicKey, byte[] generationSignature, byte[] blockSignature, byte[] previousBlockHash, List<TransactionImpl> transactions,
              BigInteger cumulativeDifficulty, long baseTarget, Long nextBlockId, int height, Long id)
            throws NxtException.ValidationException {
        this(version, timestamp, previousBlockId, totalAmount, totalFee, payloadLength, payloadHash,
                generatorPublicKey, generationSignature, blockSignature, previousBlockHash, transactions);
        this.cumulativeDifficulty = cumulativeDifficulty;
        this.baseTarget = baseTarget;
        this.nextBlockId = nextBlockId;
        this.height = height;
        this.id = id;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public int getTimestamp() {
        return timestamp;
    }

    @Override
    public Long getPreviousBlockId() {
        return previousBlockId;
    }

    @Override
    public byte[] getGeneratorPublicKey() {
        return generatorPublicKey;
    }

    @Override
    public byte[] getPreviousBlockHash() {
        return previousBlockHash;
    }

    @Override
    public int getTotalAmount() {
        return totalAmount;
    }

    @Override
    public int getTotalFee() {
        return totalFee;
    }

    @Override
    public int getPayloadLength() {
        return payloadLength;
    }

    @Override
    public List<Long> getTransactionIds() {
        return transactionIds;
    }

    @Override
    public byte[] getPayloadHash() {
        return payloadHash;
    }

    @Override
    public byte[] getGenerationSignature() {
        return generationSignature;
    }

    @Override
    public byte[] getBlockSignature() {
        return blockSignature;
    }

    @Override
    public List<TransactionImpl> getTransactions() {
        return blockTransactions;
    }

    @Override
    public long getBaseTarget() {
        return baseTarget;
    }

    @Override
    public BigInteger getCumulativeDifficulty() {
        return cumulativeDifficulty;
    }

    @Override
    public Long getNextBlockId() {
        return nextBlockId;
    }

    @Override
    public int getHeight() {
        if (height == -1) {
            throw new IllegalStateException("Block height not yet set");
        }
        return height;
    }

    @Override
    public Long getId() {
        if (id == null) {
            if (blockSignature == null) {
                throw new IllegalStateException("Block is not signed yet");
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
    public Long getGeneratorId() {
        if (generatorId == null) {
            generatorId = Account.getId(generatorPublicKey);
        }
        return generatorId;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BlockImpl && this.getId().equals(((BlockImpl)o).getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    byte[] getBytes() {

        ByteBuffer buffer = ByteBuffer.allocate(4 + 4 + 8 + 4 + 4 + 4 + 4 + 32 + 32 + (32 + 32) + 64);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(version);
        buffer.putInt(timestamp);
        buffer.putLong(Convert.nullToZero(previousBlockId));
        buffer.putInt(blockTransactions.size());
        buffer.putInt(totalAmount);
        buffer.putInt(totalFee);
        buffer.putInt(payloadLength);
        buffer.put(payloadHash);
        buffer.put(generatorPublicKey);
        buffer.put(generationSignature);
        if (version > 1) {
            buffer.put(previousBlockHash);
        }
        buffer.put(blockSignature);
        return buffer.array();
    }

    @Override
    public JSONObject getJSONObject() {

        JSONObject block = new JSONObject();

        block.put("version", version);
        block.put("timestamp", timestamp);
        block.put("previousBlock", Convert.toUnsignedLong(previousBlockId));
        block.put("numberOfTransactions", blockTransactions.size()); //TODO: not used anymore, remove after a few releases
        block.put("totalAmount", totalAmount);
        block.put("totalFee", totalFee);
        block.put("payloadLength", payloadLength);
        block.put("payloadHash", Convert.toHexString(payloadHash));
        block.put("generatorPublicKey", Convert.toHexString(generatorPublicKey));
        block.put("generationSignature", Convert.toHexString(generationSignature));
        if (version > 1) {
            block.put("previousBlockHash", Convert.toHexString(previousBlockHash));
        }
        block.put("blockSignature", Convert.toHexString(blockSignature));

        JSONArray transactionsData = new JSONArray();
        for (Transaction transaction : this.blockTransactions) {
            transactionsData.add(transaction.getJSONObject());
        }
        block.put("transactions", transactionsData);

        return block;

    }

    void sign(String secretPhrase) {
        if (blockSignature != null) {
            throw new IllegalStateException("Block already singed");
        }
        blockSignature = new byte[64];
        byte[] data = getBytes();
        byte[] data2 = new byte[data.length - 64];
        System.arraycopy(data, 0, data2, 0, data2.length);
        blockSignature = Crypto.sign(data2, secretPhrase);
    }

    boolean verifyBlockSignature() {

        Account account = Account.getAccount(getGeneratorId());
        if (account == null) {
            return false;
        }

        byte[] data = getBytes();
        byte[] data2 = new byte[data.length - 64];
        System.arraycopy(data, 0, data2, 0, data2.length);

        return Crypto.verify(blockSignature, data2, generatorPublicKey) && account.setOrVerify(generatorPublicKey, this.height);

    }

    boolean verifyGenerationSignature() throws Blockchain.BlockOutOfOrderException {

        try {

            BlockImpl previousBlock = (BlockImpl)Blockchain.getBlock(this.previousBlockId);
            if (previousBlock == null) {
                throw new Blockchain.BlockOutOfOrderException("Can't verify signature because previous block is missing");
            }

            if (version == 1 && !Crypto.verify(generationSignature, previousBlock.generationSignature, generatorPublicKey)) {
                return false;
            }

            Account account = Account.getAccount(getGeneratorId());
            if (account == null || account.getEffectiveBalance() <= 0) {
                return false;
            }

            int elapsedTime = timestamp - previousBlock.timestamp;
            BigInteger target = BigInteger.valueOf(Blockchain.getLastBlock().getBaseTarget()).multiply(BigInteger.valueOf(account.getEffectiveBalance())).multiply(BigInteger.valueOf(elapsedTime));

            MessageDigest digest = Crypto.sha256();
            byte[] generationSignatureHash;
            if (version == 1) {
                generationSignatureHash = digest.digest(generationSignature);
            } else {
                digest.update(previousBlock.generationSignature);
                generationSignatureHash = digest.digest(generatorPublicKey);
                if (!Arrays.equals(generationSignature, generationSignatureHash)) {
                    return false;
                }
            }

            BigInteger hit = new BigInteger(1, new byte[] {generationSignatureHash[7], generationSignatureHash[6], generationSignatureHash[5], generationSignatureHash[4], generationSignatureHash[3], generationSignatureHash[2], generationSignatureHash[1], generationSignatureHash[0]});

            return hit.compareTo(target) < 0;

        } catch (RuntimeException e) {

            Logger.logMessage("Error verifying block generation signature", e);
            return false;

        }

    }

    void apply() {

        Account generatorAccount = Account.addOrGetAccount(getGeneratorId());
        if (! generatorAccount.setOrVerify(generatorPublicKey, this.height)) {
            throw new IllegalStateException("Generator public key mismatch");
        }
        generatorAccount.apply(this.height);
        generatorAccount.addToBalanceAndUnconfirmedBalance(totalFee * 100L);

        for (TransactionImpl transaction : blockTransactions) {
            transaction.apply();
        }

        TransactionProcessor.purgeExpiredHashes(this.timestamp);

        if (getHeight() % 5000 == 0) {
            Logger.logDebugMessage("processed block " + getHeight());
        }

    }

    void setPrevious(BlockImpl previousBlock) {
        if (previousBlock != null) {
            if (! previousBlock.getId().equals(getPreviousBlockId())) {
                // shouldn't happen as previous id is already verified, but just in case
                throw new IllegalStateException("Previous block id doesn't match");
            }
            this.height = previousBlock.getHeight() + 1;
            this.calculateBaseTarget(previousBlock);
        } else {
            this.height = 0;
        }
        for (TransactionImpl transaction : blockTransactions) {
            transaction.setBlock(this);
        }
    }

    private void calculateBaseTarget(BlockImpl previousBlock) {

        if (this.getId().equals(Genesis.GENESIS_BLOCK_ID) && previousBlockId == null) {
            baseTarget = Nxt.initialBaseTarget;
            cumulativeDifficulty = BigInteger.ZERO;
        } else {
            long curBaseTarget = previousBlock.baseTarget;
            long newBaseTarget = BigInteger.valueOf(curBaseTarget)
                    .multiply(BigInteger.valueOf(this.timestamp - previousBlock.timestamp))
                    .divide(BigInteger.valueOf(60)).longValue();
            if (newBaseTarget < 0 || newBaseTarget > Nxt.maxBaseTarget) {
                newBaseTarget = Nxt.maxBaseTarget;
            }
            if (newBaseTarget < curBaseTarget / 2) {
                newBaseTarget = curBaseTarget / 2;
            }
            if (newBaseTarget == 0) {
                newBaseTarget = 1;
            }
            long twofoldCurBaseTarget = curBaseTarget * 2;
            if (twofoldCurBaseTarget < 0) {
                twofoldCurBaseTarget = Nxt.maxBaseTarget;
            }
            if (newBaseTarget > twofoldCurBaseTarget) {
                newBaseTarget = twofoldCurBaseTarget;
            }
            baseTarget = newBaseTarget;
            cumulativeDifficulty = previousBlock.cumulativeDifficulty.add(Convert.two64.divide(BigInteger.valueOf(baseTarget)));
        }
    }

}
