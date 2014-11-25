package nxt;

import org.json.simple.JSONObject;

import java.util.List;

public interface Transaction extends Comparable<Transaction> {

    public static interface Builder {

        Builder recipientId(long recipientId);

        Builder referencedTransactionFullHash(String referencedTransactionFullHash);

        Builder message(Appendix.Message message);

        Builder encryptedMessage(Appendix.EncryptedMessage encryptedMessage);

        Builder encryptToSelfMessage(Appendix.EncryptToSelfMessage encryptToSelfMessage);

        Builder publicKeyAnnouncement(Appendix.PublicKeyAnnouncement publicKeyAnnouncement);

        Builder twoPhased(Appendix.TwoPhased twoPhased);

        Transaction build() throws NxtException.NotValidException;
    }

    long getId();

    String getStringId();

    long getSenderId();

    byte[] getSenderPublicKey();

    long getRecipientId();

    int getHeight();

    long getBlockId();

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

    boolean verifySignature();

    void validate() throws NxtException.ValidationException;

    byte[] getBytes();

    byte[] getUnsignedBytes();

    JSONObject getJSONObject();

    byte getVersion();

    Appendix.Message getMessage();

    Appendix.EncryptedMessage getEncryptedMessage();

    Appendix.EncryptToSelfMessage getEncryptToSelfMessage();

    Appendix.TwoPhased getTwoPhased();

    List<? extends Appendix> getAppendages();

    int getECBlockHeight();

    long getECBlockId();

    //TODO: these two methods should not be exposed in the Transaction interface, they should not even be public
    void release(); // release pending transaction (on max height)
    void refuse(); // refuse pending transaction (on max height)
}
