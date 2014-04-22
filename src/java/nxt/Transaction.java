package nxt;

import org.json.simple.JSONObject;

public interface Transaction extends Comparable<Transaction> {

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

    // remove after NQT_BLOCK
    Long getReferencedTransactionId();

    byte[] getSignature();

    String getHash();

    String getFullHash();

    TransactionType getType();

    Attachment getAttachment();

    void sign(String secretPhrase);

    boolean verify();

    byte[] getBytes();

    byte[] getUnsignedBytes();

    JSONObject getJSONObject();

    /*
    Collection<TransactionType> getPhasingTransactionTypes();

    Collection<TransactionType> getPhasedTransactionTypes();
    */

}
