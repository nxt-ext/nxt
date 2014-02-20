package nxt;

import nxt.util.Convert;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

final class Transactions {

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

    static Transaction getTransaction(byte[] bytes) throws NxtException.ValidationException {

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

            TransactionType transactionType = TransactionType.findTransactionType(type, subtype);
            TransactionImpl transaction = new TransactionImpl(transactionType, timestamp, deadline, senderPublicKey, recipientId, amount,
                    fee, referencedTransactionId, signature);

            transactionType.loadAttachment(transaction, buffer);

            return transaction;

        } catch (RuntimeException e) {
            throw new NxtException.ValidationException(e.toString());
        }
    }

    static TransactionImpl getTransaction(JSONObject transactionData) throws NxtException.ValidationException {

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

            TransactionType transactionType = TransactionType.findTransactionType(type, subtype);
            TransactionImpl transaction = new TransactionImpl(transactionType, timestamp, deadline, senderPublicKey, recipientId, amount, fee,
                    referencedTransactionId, signature);

            JSONObject attachmentData = (JSONObject)transactionData.get("attachment");

            transactionType.loadAttachment(transaction, attachmentData);

            return transaction;

        } catch (RuntimeException e) {
            throw new NxtException.ValidationException(e.toString());
        }
    }

    static TransactionImpl getTransaction(Connection con, ResultSet rs) throws NxtException.ValidationException {
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
            Long blockId = rs.getLong("block_id");
            int height = rs.getInt("height");
            Long id = rs.getLong("id");
            Long senderId = rs.getLong("sender_id");
            Attachment attachment = (Attachment)rs.getObject("attachment");

            TransactionType transactionType = TransactionType.findTransactionType(type, subtype);
            return new TransactionImpl(transactionType, timestamp, deadline, senderPublicKey, recipientId, amount, fee,
                    referencedTransactionId, signature, blockId, height, id, senderId, attachment);

        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    static List<TransactionImpl> findBlockTransactions(Connection con, Long blockId) {
        List<TransactionImpl> list = new ArrayList<>();
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

    static void saveTransactions(Connection con, List<TransactionImpl> transactions) {
        try {
            for (Transaction transaction : transactions) {
                try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO transaction (id, deadline, sender_public_key, recipient_id, "
                        + "amount, fee, referenced_transaction_id, height, block_id, signature, timestamp, type, subtype, sender_id, attachment) "
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                    int i = 0;
                    pstmt.setLong(++i, transaction.getId());
                    pstmt.setShort(++i, transaction.getDeadline());
                    pstmt.setBytes(++i, transaction.getSenderPublicKey());
                    pstmt.setLong(++i, transaction.getRecipientId());
                    pstmt.setInt(++i, transaction.getAmount());
                    pstmt.setInt(++i, transaction.getFee());
                    if (transaction.getReferencedTransactionId() != null) {
                        pstmt.setLong(++i, transaction.getReferencedTransactionId());
                    } else {
                        pstmt.setNull(++i, Types.BIGINT);
                    }
                    pstmt.setInt(++i, transaction.getHeight());
                    pstmt.setLong(++i, transaction.getBlock().getId());
                    pstmt.setBytes(++i, transaction.getSignature());
                    pstmt.setInt(++i, transaction.getTimestamp());
                    pstmt.setByte(++i, transaction.getType().getType());
                    pstmt.setByte(++i, transaction.getType().getSubtype());
                    pstmt.setLong(++i, transaction.getSenderId());
                    if (transaction.getAttachment() != null) {
                        pstmt.setObject(++i, transaction.getAttachment());
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
}
