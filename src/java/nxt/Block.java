package nxt;

import nxt.crypto.Crypto;
import nxt.util.Convert;
import nxt.util.JSON;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;

public final class Block implements Serializable {

    static final long serialVersionUID = 0;
    static final Long[] emptyLong = new Long[0];
    static final Transaction[] emptyTransactions = new Transaction[0];

    public static final Comparator<Block> heightComparator = new Comparator<Block>() {
        @Override
        public int compare(Block o1, Block o2) {
            return o1.height < o2.height ? -1 : (o1.height > o2.height ? 1 : 0);
        }
    };

    public static Block getBlock(JSONObject blockData) {

        try {
            int version = ((Long)blockData.get("version")).intValue();
            int timestamp = ((Long)blockData.get("timestamp")).intValue();
            Long previousBlock = Convert.parseUnsignedLong((String) blockData.get("previousBlock"));
            int numberOfTransactions = ((Long)blockData.get("numberOfTransactions")).intValue();
            int totalAmount = ((Long)blockData.get("totalAmount")).intValue();
            int totalFee = ((Long)blockData.get("totalFee")).intValue();
            int payloadLength = ((Long)blockData.get("payloadLength")).intValue();
            byte[] payloadHash = Convert.convert((String) blockData.get("payloadHash"));
            byte[] generatorPublicKey = Convert.convert((String) blockData.get("generatorPublicKey"));
            byte[] generationSignature = Convert.convert((String) blockData.get("generationSignature"));
            byte[] blockSignature = Convert.convert((String) blockData.get("blockSignature"));

            byte[] previousBlockHash = version == 1 ? null : Convert.convert((String) blockData.get("previousBlockHash"));

            if (numberOfTransactions > Nxt.MAX_NUMBER_OF_TRANSACTIONS || payloadLength > Nxt.MAX_PAYLOAD_LENGTH) {

                return null;

            }
            return new Block(version, timestamp, previousBlock, numberOfTransactions, totalAmount, totalFee, payloadLength,
                    payloadHash, generatorPublicKey, generationSignature, blockSignature, previousBlockHash);
        } catch (RuntimeException e) {
            //logDebugMessage("Failed to parse JSON block data");
            //logDebugMessage(blockData.toJSONString());
            return null;
        }
    }


    public final int version;
    public final int timestamp;
    public final Long previousBlock;
    public int totalAmount;
    public int totalFee;
    public int payloadLength;
    public byte[] payloadHash;
    public final byte[] generatorPublicKey;
    public byte[] generationSignature;
    public byte[] blockSignature;

    public final byte[] previousBlockHash;

    public int index;
    public final Long[] transactions;
    public long baseTarget;
    public int height;
    public volatile Long nextBlock;
    public BigInteger cumulativeDifficulty;

    public transient Transaction[] blockTransactions;

    private transient volatile Long id;
    private transient volatile String stringId = null;
    private transient volatile Long generatorAccountId;
    private transient SoftReference<JSONStreamAware> jsonRef;

    Block(int version, int timestamp, Long previousBlock, int numberOfTransactions, int totalAmount, int totalFee,
          int payloadLength, byte[] payloadHash, byte[] generatorPublicKey, byte[] generationSignature, byte[] blockSignature) {

        this(version, timestamp, previousBlock, numberOfTransactions, totalAmount, totalFee, payloadLength, payloadHash,
                generatorPublicKey, generationSignature, blockSignature, null);

    }

    public Block(int version, int timestamp, Long previousBlock, int numberOfTransactions, int totalAmount, int totalFee, int payloadLength,
                 byte[] payloadHash, byte[] generatorPublicKey, byte[] generationSignature, byte[] blockSignature, byte[] previousBlockHash) {

        if (numberOfTransactions > Nxt.MAX_NUMBER_OF_TRANSACTIONS || numberOfTransactions < 0) {
            throw new IllegalArgumentException("attempted to create a block with " + numberOfTransactions + " transactions");
        }

        if (payloadLength > Nxt.MAX_PAYLOAD_LENGTH || payloadLength < 0) {
            throw new IllegalArgumentException("attempted to create a block with payloadLength " + payloadLength);
        }

        this.version = version;
        this.timestamp = timestamp;
        this.previousBlock = previousBlock;
        this.totalAmount = totalAmount;
        this.totalFee = totalFee;
        this.payloadLength = payloadLength;
        this.payloadHash = payloadHash;
        this.generatorPublicKey = generatorPublicKey;
        this.generationSignature = generationSignature;
        this.blockSignature = blockSignature;

        this.previousBlockHash = previousBlockHash;
        this.transactions = numberOfTransactions == 0 ? emptyLong : new Long[numberOfTransactions];
        this.blockTransactions = numberOfTransactions == 0 ? emptyTransactions : new Transaction[numberOfTransactions];

    }


    public byte[] getBytes() {

        ByteBuffer buffer = ByteBuffer.allocate(4 + 4 + 8 + 4 + 4 + 4 + 4 + 32 + 32 + (32 + 32) + 64);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(version);
        buffer.putInt(timestamp);
        buffer.putLong(Convert.nullToZero(previousBlock));
        buffer.putInt(transactions.length);
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

    public Long getId() {
        calculateIds();
        return id;
    }

    public String getStringId() {
        calculateIds();
        return stringId;
    }

    public Long getGeneratorAccountId() {
        calculateIds();
        return generatorAccountId;
    }

    public synchronized JSONStreamAware getJSONStreamAware() {
        JSONStreamAware json;
        if (jsonRef != null) {
            json = jsonRef.get();
            if (json != null) {
                return json;
            }
        }
        json = JSON.prepare(getJSONObject());
        jsonRef = new SoftReference<>(json);
        return json;
    }

    JSONObject getJSONObject() {

        JSONObject block = new JSONObject();

        block.put("version", version);
        block.put("timestamp", timestamp);
        block.put("previousBlock", Convert.convert(previousBlock));
        block.put("numberOfTransactions", transactions.length);
        block.put("totalAmount", totalAmount);
        block.put("totalFee", totalFee);
        block.put("payloadLength", payloadLength);
        block.put("payloadHash", Convert.convert(payloadHash));
        block.put("generatorPublicKey", Convert.convert(generatorPublicKey));
        block.put("generationSignature", Convert.convert(generationSignature));
        if (version > 1) {

            block.put("previousBlockHash", Convert.convert(previousBlockHash));

        }
        block.put("blockSignature", Convert.convert(blockSignature));

        JSONArray transactionsData = new JSONArray();
        for (Transaction transaction : this.blockTransactions) {

            transactionsData.add(transaction.getJSONObject());

        }
        block.put("transactions", transactionsData);

        return block;

    }

    boolean verifyBlockSignature() {

        Account account = Account.getAccount(getGeneratorAccountId());
        if (account == null) {

            return false;

        }

        byte[] data = getBytes();
        byte[] data2 = new byte[data.length - 64];
        System.arraycopy(data, 0, data2, 0, data2.length);

        return Crypto.verify(blockSignature, data2, generatorPublicKey) && account.setOrVerify(generatorPublicKey);

    }

    boolean verifyGenerationSignature() {

        try {

            Block previousBlock = Blockchain.getBlock(this.previousBlock);
            if (previousBlock == null) {

                return false;

            }

            if (version == 1 && !Crypto.verify(generationSignature, previousBlock.generationSignature, generatorPublicKey)) {

                return false;

            }

            Account account = Account.getAccount(getGeneratorAccountId());
            if (account == null || account.getEffectiveBalance() <= 0) {

                return false;

            }

            int elapsedTime = timestamp - previousBlock.timestamp;
            BigInteger target = BigInteger.valueOf(Blockchain.getLastBlock().baseTarget).multiply(BigInteger.valueOf(account.getEffectiveBalance())).multiply(BigInteger.valueOf(elapsedTime));

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

    private void calculateIds() {
        if (stringId != null) {
            return;
        }
        byte[] hash = Crypto.sha256().digest(getBytes());
        BigInteger bigInteger = new BigInteger(1, new byte[] {hash[7], hash[6], hash[5], hash[4], hash[3], hash[2], hash[1], hash[0]});
        id = bigInteger.longValue();
        stringId = bigInteger.toString();
        generatorAccountId = Account.getId(generatorPublicKey);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.blockTransactions = transactions.length == 0 ? emptyTransactions : new Transaction[transactions.length];
    }

}
