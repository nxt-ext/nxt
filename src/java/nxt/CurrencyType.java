package nxt;

import java.util.HashMap;
import java.util.Map;

public class CurrencyType {

    static final CurrencyType instance = new CurrencyType();

    private CurrencyType() {

        // This currency is issued by a single entity immediately, all the money belongs to this entity
        types.put((byte)1, new CurrencyType() {

            @Override
            public boolean isCurrencyIssuanceAttachmentValid(Transaction transaction) {

                Attachment.MonetarySystemCurrencyIssuance attachment = (Attachment.MonetarySystemCurrencyIssuance)transaction.getAttachment();

                return attachment.getIssuanceHeight() == 0
                        && attachment.getMinDifficulty() == 0
                        && attachment.getMaxDifficulty() == 0;

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

        });

        // This currency is issued at some height, the money is minted over time in a PoW manner
        types.put((byte)3, new CurrencyType() {

            @Override
            public boolean isCurrencyIssuanceAttachmentValid(Transaction transaction) {

                Attachment.MonetarySystemCurrencyIssuance attachment = (Attachment.MonetarySystemCurrencyIssuance)transaction.getAttachment();

                return attachment.getIssuanceHeight() != 0;

            }

        });

    }

    private static final Map<Byte, CurrencyType> types = new HashMap<>();

    public static CurrencyType getCurrencyType(byte type) {
        return types.get(type) == null ? instance : types.get(type);
    }

    public boolean isCurrencyIssuanceAttachmentValid(Transaction transaction) {
        return false;
    }

}
