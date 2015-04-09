package nxt;

import nxt.db.DbUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

class UnconfirmedTransaction implements Transaction {

    private final TransactionImpl transaction;
    private final long arrivalTimestamp;

    UnconfirmedTransaction(TransactionImpl transaction, long arrivalTimestamp) {
        this.transaction = transaction;
        this.arrivalTimestamp = arrivalTimestamp;
    }

    UnconfirmedTransaction(ResultSet rs) throws SQLException {
        try {
            byte[] transactionBytes = rs.getBytes("transaction_bytes");
            TransactionImpl.BuilderImpl builder = TransactionImpl.newTransactionBuilder(transactionBytes);
            String prunableJSON = rs.getString("prunable_json");
            if (prunableJSON != null) {
                JSONObject attachmentData = (JSONObject)JSONValue.parse(prunableJSON);
                builder.appendix(Appendix.PrunablePlainMessage.parse(attachmentData));
                builder.appendix(Appendix.PrunableEncryptedMessage.parse(attachmentData));
            }
            this.transaction = builder.build();
            this.transaction.setHeight(rs.getInt("transaction_height"));
            this.arrivalTimestamp = rs.getLong("arrival_timestamp");
        } catch (NxtException.ValidationException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO unconfirmed_transaction (id, transaction_height, "
                + "fee_per_byte, expiration, transaction_bytes, prunable_json, arrival_timestamp, height) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, transaction.getId());
            pstmt.setInt(++i, transaction.getHeight());
            pstmt.setLong(++i, transaction.getFeeNQT() / transaction.getFullSize());
            pstmt.setInt(++i, transaction.getExpiration());
            pstmt.setBytes(++i, transaction.bytes());
            String prunableJSON = null;
            Appendix.PrunablePlainMessage prunablePlainMessage = transaction.getPrunablePlainMessage();
            Appendix.PrunableEncryptedMessage prunableEncryptedMessage = transaction.getPrunableEncryptedMessage();
            if (prunablePlainMessage != null || prunableEncryptedMessage != null) {
                JSONObject json = new JSONObject();
                if (prunablePlainMessage != null) {
                    json.putAll(prunablePlainMessage.getJSONObject());
                }
                if (prunableEncryptedMessage != null) {
                    json.putAll(prunableEncryptedMessage.getJSONObject());
                }
                prunableJSON = json.toJSONString();
            }
            DbUtils.setString(pstmt, ++i, prunableJSON);
            pstmt.setLong(++i, arrivalTimestamp);
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }

    TransactionImpl getTransaction() {
        return transaction;
    }

    long getArrivalTimestamp() {
        return arrivalTimestamp;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof UnconfirmedTransaction && transaction.equals(((UnconfirmedTransaction)o).getTransaction());
    }

    @Override
    public int hashCode() {
        return transaction.hashCode();
    }

    @Override
    public long getId() {
        return transaction.getId();
    }

    @Override
    public String getStringId() {
        return transaction.getStringId();
    }

    @Override
    public long getSenderId() {
        return transaction.getSenderId();
    }

    @Override
    public byte[] getSenderPublicKey() {
        return transaction.getSenderPublicKey();
    }

    @Override
    public long getRecipientId() {
        return transaction.getRecipientId();
    }

    @Override
    public int getHeight() {
        return transaction.getHeight();
    }

    @Override
    public int getValidationHeight() {
        return transaction.getValidationHeight();
    }
    
    @Override
    public long getBlockId() {
        return transaction.getBlockId();
    }

    @Override
    public Block getBlock() {
        return transaction.getBlock();
    }

    @Override
    public int getTimestamp() {
        return transaction.getTimestamp();
    }

    @Override
    public int getBlockTimestamp() {
        return transaction.getBlockTimestamp();
    }

    @Override
    public short getDeadline() {
        return transaction.getDeadline();
    }

    @Override
    public int getExpiration() {
        return transaction.getExpiration();
    }

    @Override
    public long getAmountNQT() {
        return transaction.getAmountNQT();
    }

    @Override
    public long getFeeNQT() {
        return transaction.getFeeNQT();
    }

    @Override
    public String getReferencedTransactionFullHash() {
        return transaction.getReferencedTransactionFullHash();
    }

    @Override
    public byte[] getSignature() {
        return transaction.getSignature();
    }

    @Override
    public String getFullHash() {
        return transaction.getFullHash();
    }

    @Override
    public TransactionType getType() {
        return transaction.getType();
    }

    @Override
    public Attachment getAttachment() {
        return transaction.getAttachment();
    }

    @Override
    public boolean verifySignature() {
        return transaction.verifySignature();
    }

    @Override
    public void validate() throws NxtException.ValidationException {
        transaction.validate();
    }

    @Override
    public byte[] getBytes() {
        return transaction.getBytes();
    }

    @Override
    public byte[] getUnsignedBytes() {
        return transaction.getUnsignedBytes();
    }

    @Override
    public JSONObject getJSONObject() {
        return transaction.getJSONObject();
    }

    @Override
    public byte getVersion() {
        return transaction.getVersion();
    }

    @Override
    public Appendix.Message getMessage() {
        return transaction.getMessage();
    }

    @Override
    public Appendix.PrunablePlainMessage getPrunablePlainMessage() {
        return transaction.getPrunablePlainMessage();
    }

    @Override
    public Appendix.EncryptedMessage getEncryptedMessage() {
        return transaction.getEncryptedMessage();
    }

    @Override
    public Appendix.PrunableEncryptedMessage getPrunableEncryptedMessage() {
        return transaction.getPrunableEncryptedMessage();
    }

    public Appendix.EncryptToSelfMessage getEncryptToSelfMessage() {
        return transaction.getEncryptToSelfMessage();
    }

    @Override
    public Appendix.Phasing getPhasing() {
        return transaction.getPhasing();
    }

    @Override
    public List<? extends Appendix> getAppendages() {
        return transaction.getAppendages();
    }

    @Override
    public int getECBlockHeight() {
        return transaction.getECBlockHeight();
    }

    @Override
    public long getECBlockId() {
        return transaction.getECBlockId();
    }

    @Override
    public short getIndex() {
        return transaction.getIndex();
    }
}
