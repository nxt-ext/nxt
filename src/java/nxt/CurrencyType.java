package nxt;

import java.util.HashMap;
import java.util.Map;

public abstract class CurrencyType {

    private static final Map<Byte, CurrencyType> types = new HashMap<>();

    static {

        // This currency is issued by a single entity immediately, all the money belongs to this entity
        types.put((byte)1, new CurrencyType() {

            @Override
            public boolean isCurrencyIssuanceAttachmentValid(Transaction transaction) {

                Attachment.MonetarySystemCurrencyIssuance attachment = (Attachment.MonetarySystemCurrencyIssuance)transaction.getAttachment();

                return attachment.getIssuanceHeight() == 0
                        && attachment.getMinDifficulty() == 0
                        && attachment.getMaxDifficulty() == 0;

            }

            @Override
            public boolean applyCurrencyIssuanceAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                return false;
            }

            @Override
            public void undoCurrencyIssuanceAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {

            }

            @Override
            public void applyCurrencyIssuanceAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {

            }

        });

        // This currency is issued at some height if min required amount of NXT is collected, the money is split proportionally to reserved NXT
        types.put((byte)2, new CurrencyType() {

            @Override
            public boolean isCurrencyIssuanceAttachmentValid(Transaction transaction) {

                Attachment.MonetarySystemCurrencyIssuance attachment = (Attachment.MonetarySystemCurrencyIssuance)transaction.getAttachment();

                return attachment.getIssuanceHeight() != 0
                        && attachment.getMinDifficulty() == 0
                        && attachment.getMaxDifficulty() == 0;

            }

            @Override
            public boolean applyCurrencyIssuanceAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                return false;
            }

            @Override
            public void undoCurrencyIssuanceAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {

            }

            @Override
            public void applyCurrencyIssuanceAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {

            }

        });

        // This currency is issued at some height, the money is minted over time in a PoW manner
        types.put((byte)3, new CurrencyType() {

            @Override
            public boolean isCurrencyIssuanceAttachmentValid(Transaction transaction) {

                Attachment.MonetarySystemCurrencyIssuance attachment = (Attachment.MonetarySystemCurrencyIssuance)transaction.getAttachment();

                return attachment.getIssuanceHeight() != 0;

            }

            @Override
            public boolean applyCurrencyIssuanceAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                return false;
            }

            @Override
            public void undoCurrencyIssuanceAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {

            }

            @Override
            public void applyCurrencyIssuanceAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {

            }

        });

    }

    public static CurrencyType getCurrencyType(byte type) {
        return types.get(type);
    }

    public abstract boolean isCurrencyIssuanceAttachmentValid(Transaction transaction);

    public abstract boolean applyCurrencyIssuanceAttachmentUnconfirmed(Transaction transaction, Account senderAccount);

    public abstract void undoCurrencyIssuanceAttachmentUnconfirmed(Transaction transaction, Account senderAccount);

    public abstract void applyCurrencyIssuanceAttachment(Transaction transaction, Account senderAccount, Account recipientAccount);

    public void undoCurrencyIssuanceAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) throws TransactionType.UndoNotSupportedException {
        throw new TransactionType.UndoNotSupportedException("Reversal of currency issuance not supported");
    }

}
