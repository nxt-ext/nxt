package nxt;

import nxt.crypto.Crypto;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.Writer;
import java.lang.ref.SoftReference;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

public class Block implements Serializable {

    static final long serialVersionUID = 0;
    static final long[] emptyLong = new long[0];
    static final Transaction[] emptyTransactions = new Transaction[0];

    final int version;
    final int timestamp;
    final long previousBlock;
    int totalAmount, totalFee;
    int payloadLength;
    byte[] payloadHash;
    final byte[] generatorPublicKey;
    byte[] generationSignature;
    byte[] blockSignature;

    final byte[] previousBlockHash;

    public int index;
    public final long[] transactions;
    public long baseTarget;
    public int height;
    public volatile long nextBlock;
    public BigInteger cumulativeDifficulty;

    transient Transaction[] blockTransactions;

    Block(int version, int timestamp, long previousBlock, int numberOfTransactions, int totalAmount, int totalFee,
          int payloadLength, byte[] payloadHash, byte[] generatorPublicKey, byte[] generationSignature, byte[] blockSignature) {

        this(version, timestamp, previousBlock, numberOfTransactions, totalAmount, totalFee, payloadLength, payloadHash,
                generatorPublicKey, generationSignature, blockSignature, null);

    }

    public Block(int version, int timestamp, long previousBlock, int numberOfTransactions, int totalAmount, int totalFee, int payloadLength,
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
        this.transactions = numberOfTransactions == 0 ? emptyLong : new long[numberOfTransactions];
        this.blockTransactions = numberOfTransactions == 0 ? emptyTransactions : new Transaction[numberOfTransactions];

    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.blockTransactions = transactions.length == 0 ? emptyTransactions : new Transaction[transactions.length];
    }

    static Block getBlock(JSONObject blockData) {

        try {
            int version = ((Long)blockData.get("version")).intValue();
            int timestamp = ((Long)blockData.get("timestamp")).intValue();
            long previousBlock = Convert.parseUnsignedLong((String) blockData.get("previousBlock"));
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

    byte[] getBytes() {

        ByteBuffer buffer = ByteBuffer.allocate(4 + 4 + 8 + 4 + 4 + 4 + 4 + 32 + 32 + (32 + 32) + 64);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(version);
        buffer.putInt(timestamp);
        buffer.putLong(previousBlock);
        buffer.putInt(this.transactions.length);
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

    transient volatile long id;
    transient volatile String stringId = null;
    transient volatile long generatorAccountId;

    long getId() {
        calculateIds();
        return id;
    }


    String getStringId() {
        calculateIds();
        return stringId;
    }

    long getGeneratorAccountId() {
        calculateIds();
        return generatorAccountId;
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

    JSONObject getJSONObject() {

        JSONObject block = new JSONObject();

        block.put("version", version);
        block.put("timestamp", timestamp);
        block.put("previousBlock", Convert.convert(previousBlock));
        block.put("numberOfTransactions", this.transactions.length);
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

    private transient SoftReference<JSONStreamAware> jsonRef;

    synchronized JSONStreamAware getJSONStreamAware() {
        JSONStreamAware json;
        if (jsonRef != null) {
            json = jsonRef.get();
            if (json != null) {
                return json;
            }
        }
        json = new JSONStreamAware() {
            private char[] jsonChars = getJSONObject().toJSONString().toCharArray();
            @Override
            public void writeJSONString(Writer out) throws IOException {
                out.write(jsonChars);
            }
        };
        jsonRef = new SoftReference<>(json);
        return json;
    }

    public static final Comparator<Block> heightComparator = new Comparator<Block>() {
        @Override
        public int compare(Block o1, Block o2) {
            return o1.height < o2.height ? -1 : (o1.height > o2.height ? 1 : 0);
        }
    };

    static void loadBlocks(String fileName) throws FileNotFoundException {

        try (FileInputStream fileInputStream = new FileInputStream(fileName);
             ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)
        ) {
            Nxt.blockCounter.set(objectInputStream.readInt());
            Nxt.blocks.clear();
            Nxt.blocks.putAll((HashMap<Long, Block>) objectInputStream.readObject());
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException|ClassNotFoundException e) {
            Logger.logMessage("Error loading blocks from " + fileName, e);
            System.exit(1);
        }

    }

    static void saveBlocks(String fileName, boolean flag) {

        try (FileOutputStream fileOutputStream = new FileOutputStream(fileName);
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)
        ) {
            objectOutputStream.writeInt(Nxt.blockCounter.get());
            objectOutputStream.writeObject(new HashMap<>(Nxt.blocks));
        } catch (IOException e) {
            Logger.logMessage("Error saving blocks to " + fileName, e);
            throw new RuntimeException(e);
        }

            /*if (flag) {

                ByteBuffer buffer = ByteBuffer.allocate(BLOCK_HEADER_LENGTH + MAX_PAYLOAD_LENGTH);
                buffer.order(ByteOrder.LITTLE_ENDIAN);

                long curBlockId = GENESIS_BLOCK_ID;
                long prevBlockPtr = -1, curBlockPtr = 0;
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                do {

                    Block block = blocks.get(curBlockId);
                    buffer.clear();
                    buffer.putLong(prevBlockPtr);
                    buffer.put(block.getBytes());
                    for (int i = 0; i < block.numberOfTransactions; i++) {

                        buffer.put(Nxt.transactions.get(block.transactions[i]).getBytes());

                    }
                    buffer.flip();
                    byte[] rawBytes = new byte[buffer.limit()];
                    buffer.get(rawBytes);

                    MappedByteBuffer window = blockchainChannel.map(FileChannel.MapMode.READ_WRITE, curBlockPtr, 32 + rawBytes.length);
                    window.put(digest.digest(rawBytes));
                    window.put(rawBytes);

                    prevBlockPtr = curBlockPtr;
                    curBlockPtr += 32 + rawBytes.length;
                    curBlockId = block.nextBlock;

                } while (curBlockId != 0);

            }*/

    }

    boolean verifyBlockSignature() {

        Account account = Nxt.accounts.get(getGeneratorAccountId());
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

            Block previousBlock = Nxt.blocks.get(this.previousBlock);
            if (previousBlock == null) {

                return false;

            }

            if (version == 1 && !Crypto.verify(generationSignature, previousBlock.generationSignature, generatorPublicKey)) {

                return false;

            }

            Account account = Nxt.accounts.get(getGeneratorAccountId());
            if (account == null || account.getEffectiveBalance() <= 0) {

                return false;

            }

            int elapsedTime = timestamp - previousBlock.timestamp;
            BigInteger target = BigInteger.valueOf(Nxt.lastBlock.get().baseTarget).multiply(BigInteger.valueOf(account.getEffectiveBalance())).multiply(BigInteger.valueOf(elapsedTime));

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

}
