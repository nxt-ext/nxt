package nxt;

import org.json.simple.JSONObject;

public interface Transaction extends Comparable<Transaction> {

    Long getId();

    String getStringId();

    Long getSenderId();

    byte[] getSenderPublicKey();

    Long getRecipientId();

    int getHeight();

    Block getBlock();

    int getTimestamp();

    short getDeadline();

    int getExpiration();

    int getAmount();

    int getFee();

    Long getReferencedTransactionId();

    byte[] getSignature();

    String getHash();

    TransactionType getType();

    Attachment getAttachment();

    void sign(String secretPhrase);

    JSONObject getJSONObject();

    byte[] getBytes();

}
