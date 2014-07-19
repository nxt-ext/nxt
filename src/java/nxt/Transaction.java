package nxt;

import org.json.simple.JSONObject;

public interface Transaction extends Comparable<Transaction> {

    public static interface Builder {

        public Builder referencedTransactionFullHash(String referencedTransactionFullHash);

        public Builder message(Appendix.Message message);

        public Builder encryptedMessage(Appendix.EncryptedMessage encryptedData);

        public Transaction build() throws NxtException.ValidationException;

    }

    Long getId();

    String getStringId();

    Long getSenderId();

    byte[] getSenderPublicKey();

    Long getRecipientId();

    int getHeight();

    Long getBlockId();

    Block getBlock();

    int getTimestamp();

    int getBlockTimestamp();

    short getDeadline();

    int getExpiration();

    long getAmountNQT();

    long getFeeNQT();

    String getReferencedTransactionFullHash();

    byte[] getSignature();

    String getFullHash();

    TransactionType getType();

    Attachment getAttachment();

    void sign(String secretPhrase);

    boolean verify();

    void validateAttachment() throws NxtException.ValidationException;

    byte[] getBytes();

    byte[] getUnsignedBytes();

    JSONObject getJSONObject();

    byte getVersion();

    Appendix.Message getMessage();

    Appendix.EncryptedMessage getEncryptedMessage();

    /*
    Collection<TransactionType> getPhasingTransactionTypes();

    Collection<TransactionType> getPhasedTransactionTypes();
    */

}
