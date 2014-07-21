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
                        && attachment.getMinReservePerUnitNQT() == 0
                        && attachment.getMinDifficulty() == 0
                        && attachment.getMaxDifficulty() == 0;
            }

            @Override
            public boolean applyCurrencyIssuanceAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                return true;
            }

            @Override
            public void undoCurrencyIssuanceAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            }

            @Override
            public void applyCurrencyIssuanceAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MonetarySystemCurrencyIssuance attachment = (Attachment.MonetarySystemCurrencyIssuance)transaction.getAttachment();

                Currency.addCurrency(transaction.getId(), attachment.getName(), attachment.getCode(), attachment.getDescription(), attachment.getType(), attachment.getTotalSupply(), attachment.getIssuanceHeight(), attachment.getMinReservePerUnitNQT(), attachment.getMinDifficulty(), attachment.getMaxDifficulty(), attachment.getRuleset(), 0);

                senderAccount.addToCurrencyAndUnconfirmedCurrencyBalanceQNT(transaction.getId(), attachment.getTotalSupply());
            }

        });

        // This currency is issued at some height if min required amount of NXT is collected, the money is split proportionally to reserved NXT
        types.put((byte)2, new CurrencyType() {

            @Override
            public boolean isCurrencyIssuanceAttachmentValid(Transaction transaction) {
                Attachment.MonetarySystemCurrencyIssuance attachment = (Attachment.MonetarySystemCurrencyIssuance)transaction.getAttachment();

                return attachment.getIssuanceHeight() > 0
                        && attachment.getMinDifficulty() == 0
                        && attachment.getMaxDifficulty() == 0;
            }

            @Override
            public boolean applyCurrencyIssuanceAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                return true;
            }

            @Override
            public void undoCurrencyIssuanceAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            }

            @Override
            public void applyCurrencyIssuanceAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MonetarySystemCurrencyIssuance attachment = (Attachment.MonetarySystemCurrencyIssuance)transaction.getAttachment();

                Currency.addCurrency(transaction.getId(), attachment.getName(), attachment.getCode(), attachment.getDescription(), attachment.getType(), attachment.getTotalSupply(), attachment.getIssuanceHeight(), attachment.getMinReservePerUnitNQT(), attachment.getMinDifficulty(), attachment.getMaxDifficulty(), attachment.getRuleset(), 0);
            }

        });

        // This currency is issued at some height, the money is minted over time in a PoW manner
        types.put((byte)3, new CurrencyType() {

            @Override
            public boolean isCurrencyIssuanceAttachmentValid(Transaction transaction) {
                Attachment.MonetarySystemCurrencyIssuance attachment = (Attachment.MonetarySystemCurrencyIssuance)transaction.getAttachment();

                return attachment.getIssuanceHeight() > 0
                        && attachment.getMinReservePerUnitNQT() == 0;
            }

            @Override
            public boolean applyCurrencyIssuanceAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                return true;
            }

            @Override
            public void undoCurrencyIssuanceAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            }

            @Override
            public void applyCurrencyIssuanceAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MonetarySystemCurrencyIssuance attachment = (Attachment.MonetarySystemCurrencyIssuance)transaction.getAttachment();

                Currency.addCurrency(transaction.getId(), attachment.getName(), attachment.getCode(), attachment.getDescription(), attachment.getType(), attachment.getTotalSupply(), attachment.getIssuanceHeight(), attachment.getMinReservePerUnitNQT(), attachment.getMinDifficulty(), attachment.getMaxDifficulty(), attachment.getRuleset(), 0);
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
