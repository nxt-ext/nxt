package nxt;

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

    Long getReferencedTransactionId();

    byte[] getSignature();

    String getHash();

    TransactionType getType();

    Attachment getAttachment();

    void sign(String secretPhrase);

    byte[] getBytes();

}
