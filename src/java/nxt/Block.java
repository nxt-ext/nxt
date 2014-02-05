package nxt;

import nxt.crypto.Crypto;
import nxt.util.Convert;
import nxt.util.JSON;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.lang.ref.SoftReference;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public final class Block {

    static final Long[] emptyLong = new Long[0];
    static final Transaction[] emptyTransactions = new Transaction[0];

    public static final Comparator<Block> heightComparator = new Comparator<Block>() {
        @Override
        public int compare(Block o1, Block o2) {
            return o1.height < o2.height ? -1 : (o1.height > o2.height ? 1 : 0);
        }
    };

    static Block findBlock(Long blockId) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block WHERE id = ?")) {
            pstmt.setLong(1, blockId);
            ResultSet rs = pstmt.executeQuery();
            Block block = null;
            if (rs.next()) {
                block = getBlock(con, rs);
            }
            rs.close();
            return block;
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (NxtException.ValidationException e) {
            throw new RuntimeException("Block already in database, id = " + blockId + ", does not pass validation!");
        }
    }

    static boolean hasBlock(Long blockId) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT 1 FROM block WHERE id = ?")) {
            pstmt.setLong(1, blockId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    static long findBlockIdAtHeight(int height) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT id FROM block WHERE height = ?")) {
            pstmt.setLong(1, height);
            ResultSet rs = pstmt.executeQuery();
            if (! rs.next()) {
                rs.close();
                throw new RuntimeException("Block at height " + height + " not found in database!");
            }
            long id = rs.getLong("id");
            rs.close();
            return id;
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    static Block getBlock(JSONObject blockData) throws NxtException.ValidationException {

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
                throw new NxtException.ValidationException("Invalid number of transactions or payload length");
            }

            return new Block(version, timestamp, previousBlock, numberOfTransactions, totalAmount, totalFee, payloadLength,
                    payloadHash, generatorPublicKey, generationSignature, blockSignature, previousBlockHash);

        } catch (RuntimeException e) {
            throw new NxtException.ValidationException(e.toString(), e);
        }

    }

    static Block getBlock(Connection con, ResultSet rs) throws NxtException.ValidationException {
        try {
            int version = rs.getInt("version");
            int timestamp = rs.getInt("timestamp");
            Long previousBlockId = rs.getLong("previous_block_id");
            if (rs.wasNull()) {
                previousBlockId = null;
            }
            int totalAmount = rs.getInt("total_amount");
            int totalFee = rs.getInt("total_fee");
            int payloadLength = rs.getInt("payload_length");
            byte[] generatorPublicKey = rs.getBytes("generator_public_key");
            byte[] previousBlockHash = rs.getBytes("previous_block_hash");
            BigInteger cumulativeDifficulty = new BigInteger(rs.getBytes("cumulative_difficulty"));
            long baseTarget = rs.getLong("base_target");
            Long nextBlockId = rs.getLong("next_block_id");
            if (rs.wasNull()) {
                nextBlockId = null;
            }
            int index = rs.getInt("index");
            int height = rs.getInt("height");
            byte[] generationSignature = rs.getBytes("generation_signature");
            byte[] blockSignature = rs.getBytes("block_signature");
            byte[] payloadHash = rs.getBytes("payload_hash");

            Long id = rs.getLong("id");
            List<Transaction> transactions = Transaction.findBlockTransactions(con, id);

            Block block = new Block(version, timestamp, previousBlockId, transactions.size(), totalAmount, totalFee, payloadLength,
                    payloadHash, generatorPublicKey, generationSignature, blockSignature, previousBlockHash);
            for (int i = 0; i < transactions.size(); i++) {
                Transaction transaction = transactions.get(i);
                block.transactionIds[i] = transaction.getId();
                block.blockTransactions[i] = transaction;
            }

            block.cumulativeDifficulty = cumulativeDifficulty;
            block.baseTarget = baseTarget;
            block.nextBlockId = nextBlockId;
            block.index = index;
            block.height = height;
            block.id = id;

            return block;

        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    static void saveBlock(Connection con, Block block) {
        try {
            try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO block (id, version, timestamp, previous_block_id, "
                    + "total_amount, total_fee, payload_length, generator_public_key, previous_block_hash, cumulative_difficulty, "
                    + "base_target, next_block_id, index, height, generation_signature, block_signature, payload_hash, generator_account_id) "
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                pstmt.setLong(1, block.getId());
                pstmt.setInt(2, block.version);
                pstmt.setInt(3, block.timestamp);
                if (block.previousBlockId != null) {
                    pstmt.setLong(4, block.previousBlockId);
                } else {
                    pstmt.setNull(4, Types.BIGINT);
                }
                pstmt.setInt(5, block.totalAmount);
                pstmt.setInt(6, block.totalFee);
                pstmt.setInt(7, block.payloadLength);
                pstmt.setBytes(8, block.generatorPublicKey);
                pstmt.setBytes(9, block.previousBlockHash);
                pstmt.setBytes(10, block.cumulativeDifficulty.toByteArray());
                pstmt.setLong(11, block.baseTarget);
                if (block.nextBlockId != null) {
                    pstmt.setLong(12, block.nextBlockId);
                } else {
                    pstmt.setNull(12, Types.BIGINT);
                }
                pstmt.setInt(13, block.index);
                pstmt.setInt(14, block.height);
                pstmt.setBytes(15, block.generationSignature);
                pstmt.setBytes(16, block.blockSignature);
                pstmt.setBytes(17, block.payloadHash);
                pstmt.setLong(18, block.getGeneratorAccountId());
                pstmt.executeUpdate();
                Transaction.saveTransactions(con, block.blockTransactions);
            }
            if (block.previousBlockId != null) {
                try (PreparedStatement pstmt = con.prepareStatement("UPDATE block SET next_block_id = ? WHERE id = ?")) {
                    pstmt.setLong(1, block.getId());
                    pstmt.setLong(2, block.previousBlockId);
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    // relying on cascade triggers in the database to delete also all subsequent blocks, and the transactions for all deleted blocks
    static void deleteBlock(Long blockId) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("DELETE FROM block WHERE id = ?")) {
            try {
                pstmt.setLong(1, blockId);
                pstmt.executeUpdate();
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    private final int version;
    private final int timestamp;
    private final Long previousBlockId;
    private final byte[] generatorPublicKey;
    private final byte[] previousBlockHash;
    private final int totalAmount;
    private final int totalFee;
    private final int payloadLength;
    final Long[] transactionIds;
    final Transaction[] blockTransactions;

    private BigInteger cumulativeDifficulty = BigInteger.ZERO;
    private long baseTarget = Nxt.initialBaseTarget;
    private volatile Long nextBlockId;
    private int index;
    private int height;
    private byte[] generationSignature;
    private byte[] blockSignature;
    private byte[] payloadHash;
    private volatile Long id;
    private volatile String stringId = null;
    private volatile Long generatorAccountId;
    private SoftReference<JSONStreamAware> jsonRef;

    Block(int version, int timestamp, Long previousBlockId, int numberOfTransactions, int totalAmount, int totalFee,
          int payloadLength, byte[] payloadHash, byte[] generatorPublicKey, byte[] generationSignature, byte[] blockSignature)
            throws NxtException.ValidationException {

        this(version, timestamp, previousBlockId, numberOfTransactions, totalAmount, totalFee, payloadLength, payloadHash,
                generatorPublicKey, generationSignature, blockSignature, null);

    }

    Block(int version, int timestamp, Long previousBlockId, int numberOfTransactions, int totalAmount, int totalFee, int payloadLength,
                 byte[] payloadHash, byte[] generatorPublicKey, byte[] generationSignature, byte[] blockSignature, byte[] previousBlockHash)
            throws NxtException.ValidationException {

        if (numberOfTransactions > Nxt.MAX_NUMBER_OF_TRANSACTIONS || numberOfTransactions < 0) {
            throw new NxtException.ValidationException("attempted to create a block with " + numberOfTransactions + " transactions");
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
        this.transactionIds = numberOfTransactions == 0 ? emptyLong : new Long[numberOfTransactions];
        this.blockTransactions = numberOfTransactions == 0 ? emptyTransactions : new Transaction[numberOfTransactions];

    }

    public int getVersion() {
        return version;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public Long getPreviousBlockId() {
        return previousBlockId;
    }

    public byte[] getGeneratorPublicKey() {
        return generatorPublicKey;
    }

    public byte[] getPreviousBlockHash() {
        return previousBlockHash;
    }

    public int getTotalAmount() {
        return totalAmount;
    }

    public int getTotalFee() {
        return totalFee;
    }

    public int getPayloadLength() {
        return payloadLength;
    }

    public Long[] getTransactionIds() {
        return transactionIds;
    }

    public byte[] getPayloadHash() {
        return payloadHash;
    }

    void setPayloadHash(byte[] payloadHash) {
        this.payloadHash = payloadHash;
    }

    public byte[] getGenerationSignature() {
        return generationSignature;
    }

    void setGenerationSignature(byte[] generationSignature) {
        this.generationSignature = generationSignature;
    }

    public byte[] getBlockSignature() {
        return blockSignature;
    }

    void setBlockSignature(byte[] blockSignature) {
        this.blockSignature = blockSignature;
    }

    public Transaction[] getTransactions() {
        return blockTransactions;
    }

    public long getBaseTarget() {
        return baseTarget;
    }

    public BigInteger getCumulativeDifficulty() {
        return cumulativeDifficulty;
    }

    public Long getNextBlockId() {
        return nextBlockId;
    }

    public int getIndex() {
        return index;
    }

    void setIndex(int index) {
        this.index = index;
    }

    public int getHeight() {
        return height;
    }

    void setHeight(int height) {
        this.height = height;
    }

    public Long getId() {
        if (id == null) {
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
                stringId = Convert.convert(id);
            }
        }
        return stringId;
    }

    public Long getGeneratorAccountId() {
        if (generatorAccountId == null) {
            generatorAccountId = Account.getId(generatorPublicKey);
        }
        return generatorAccountId;
    }

    public synchronized JSONStreamAware getJSON() {
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

    @Override
    public boolean equals(Object o) {
        return o instanceof Block && this.getId().equals(((Block)o).getId());
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
        buffer.putInt(transactionIds.length);
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

    JSONObject getJSONObject() {

        JSONObject block = new JSONObject();

        block.put("version", version);
        block.put("timestamp", timestamp);
        block.put("previousBlock", Convert.convert(previousBlockId));
        block.put("numberOfTransactions", transactionIds.length);
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

            Block previousBlock = Blockchain.getBlock(this.previousBlockId);
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

    void apply() {

        Account generatorAccount = Account.addOrGetAccount(getGeneratorAccountId());
        if (! generatorAccount.setOrVerify(generatorPublicKey)) {
            throw new IllegalStateException("Generator public key mismatch");
        }
        generatorAccount.addToBalanceAndUnconfirmedBalance(totalFee * 100L);

        for (Transaction transaction : blockTransactions) {
            transaction.apply();
        }

    }

    void calculateBaseTarget() {

        if (this.getId().equals(Genesis.GENESIS_BLOCK_ID) && previousBlockId == null) {
            baseTarget = Nxt.initialBaseTarget;
            cumulativeDifficulty = BigInteger.ZERO;
        } else {
            Block previousBlock = Blockchain.getBlock(this.previousBlockId);
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
