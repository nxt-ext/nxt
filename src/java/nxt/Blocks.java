package nxt;

import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

final class Blocks {

    static BlockImpl findBlock(Long blockId) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block WHERE id = ?")) {
            pstmt.setLong(1, blockId);
            ResultSet rs = pstmt.executeQuery();
            BlockImpl block = null;
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
            pstmt.setInt(1, height);
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

    static BlockImpl getBlock(JSONObject blockData) throws NxtException.ValidationException {

        try {

            int version = ((Long)blockData.get("version")).intValue();
            int timestamp = ((Long)blockData.get("timestamp")).intValue();
            Long previousBlock = Convert.parseUnsignedLong((String) blockData.get("previousBlock"));
            int totalAmount = ((Long)blockData.get("totalAmount")).intValue();
            int totalFee = ((Long)blockData.get("totalFee")).intValue();
            int payloadLength = ((Long)blockData.get("payloadLength")).intValue();
            byte[] payloadHash = Convert.parseHexString((String) blockData.get("payloadHash"));
            byte[] generatorPublicKey = Convert.parseHexString((String) blockData.get("generatorPublicKey"));
            byte[] generationSignature = Convert.parseHexString((String) blockData.get("generationSignature"));
            byte[] blockSignature = Convert.parseHexString((String) blockData.get("blockSignature"));
            byte[] previousBlockHash = version == 1 ? null : Convert.parseHexString((String) blockData.get("previousBlockHash"));

            SortedMap<Long, TransactionImpl> blockTransactions = new TreeMap<>();
            JSONArray transactionsData = (JSONArray)blockData.get("transactions");
            for (Object transactionData : transactionsData) {
                TransactionImpl transaction = Transactions.getTransaction((JSONObject) transactionData);
                if (blockTransactions.put(transaction.getId(), transaction) != null) {
                    throw new NxtException.ValidationException("Block contains duplicate transactions: " + transaction.getStringId());
                }
            }

            return new BlockImpl(version, timestamp, previousBlock, totalAmount, totalFee, payloadLength, payloadHash, generatorPublicKey,
                    generationSignature, blockSignature, previousBlockHash, new ArrayList<>(blockTransactions.values()));

        } catch (RuntimeException e) {
            throw new NxtException.ValidationException(e.toString(), e);
        }

    }

    static BlockImpl getBlock(Connection con, ResultSet rs) throws NxtException.ValidationException {
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
            int height = rs.getInt("height");
            byte[] generationSignature = rs.getBytes("generation_signature");
            byte[] blockSignature = rs.getBytes("block_signature");
            byte[] payloadHash = rs.getBytes("payload_hash");

            Long id = rs.getLong("id");
            List<TransactionImpl> transactions = Transactions.findBlockTransactions(con, id);

            BlockImpl block = new BlockImpl(version, timestamp, previousBlockId, totalAmount, totalFee, payloadLength, payloadHash,
                    generatorPublicKey, generationSignature, blockSignature, previousBlockHash, transactions,
                    cumulativeDifficulty, baseTarget, nextBlockId, height, id);

            for (TransactionImpl transaction : transactions) {
                transaction.setBlock(block);
            }

            return block;

        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    static void saveBlock(Connection con, BlockImpl block) {
        try {
            try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO block (id, version, timestamp, previous_block_id, "
                    + "total_amount, total_fee, payload_length, generator_public_key, previous_block_hash, cumulative_difficulty, "
                    + "base_target, next_block_id, height, generation_signature, block_signature, payload_hash, generator_id) "
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                int i = 0;
                pstmt.setLong(++i, block.getId());
                pstmt.setInt(++i, block.getVersion());
                pstmt.setInt(++i, block.getTimestamp());
                if (block.getPreviousBlockId() != null) {
                    pstmt.setLong(++i, block.getPreviousBlockId());
                } else {
                    pstmt.setNull(++i, Types.BIGINT);
                }
                pstmt.setInt(++i, block.getTotalAmount());
                pstmt.setInt(++i, block.getTotalFee());
                pstmt.setInt(++i, block.getPayloadLength());
                pstmt.setBytes(++i, block.getGeneratorPublicKey());
                pstmt.setBytes(++i, block.getPreviousBlockHash());
                pstmt.setBytes(++i, block.getCumulativeDifficulty().toByteArray());
                pstmt.setLong(++i, block.getBaseTarget());
                if (block.getNextBlockId()!= null) {
                    pstmt.setLong(++i, block.getNextBlockId());
                } else {
                    pstmt.setNull(++i, Types.BIGINT);
                }
                pstmt.setInt(++i, block.getHeight());
                pstmt.setBytes(++i, block.getGenerationSignature());
                pstmt.setBytes(++i, block.getBlockSignature());
                pstmt.setBytes(++i, block.getPayloadHash());
                pstmt.setLong(++i, block.getGeneratorId());
                pstmt.executeUpdate();
                Transactions.saveTransactions(con, block.getTransactions());
            }
            if (block.getPreviousBlockId() != null) {
                try (PreparedStatement pstmt = con.prepareStatement("UPDATE block SET next_block_id = ? WHERE id = ?")) {
                    pstmt.setLong(1, block.getId());
                    pstmt.setLong(2, block.getPreviousBlockId());
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
}
