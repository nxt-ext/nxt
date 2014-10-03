package nxt;

import java.util.HashMap;
import java.util.Map;

public abstract class CurrencyType {

    private static final Map<Byte, CurrencyType> types = new HashMap<>();

    public static final byte SIMPLE = 1;
    public static final byte CROWD_FUNDING = 2;
    public static final byte MINTABLE = 3;

    static {

        // This currency is issued by a single entity immediately, all the money belongs to this entity
        types.put(SIMPLE, new CurrencyType() {

            @Override
            public boolean isCurrencyIssuanceAttachmentValid(Transaction transaction) {
                Attachment.MonetarySystemCurrencyIssuance attachment = (Attachment.MonetarySystemCurrencyIssuance)transaction.getAttachment();

                return attachment.getIssuanceHeight() == 0
                        && attachment.getMinReservePerUnitNQT() == 0
                        && attachment.getMinDifficulty() == 0
                        && attachment.getMaxDifficulty() == 0;
            }

        });

        // This currency is issued at some height if min required amount of NXT is collected, the money is split proportionally to reserved NXT
        types.put(CROWD_FUNDING, new CurrencyType() {

            @Override
            public boolean isCurrencyIssuanceAttachmentValid(Transaction transaction) {
                Attachment.MonetarySystemCurrencyIssuance attachment = (Attachment.MonetarySystemCurrencyIssuance)transaction.getAttachment();

                return attachment.getIssuanceHeight() > 0
                        && attachment.getMinDifficulty() == 0
                        && attachment.getMaxDifficulty() == 0;
            }

        });

        // This currency is issued at some height, the money is minted over time in a PoW manner
        types.put(MINTABLE, new CurrencyType() {

            @Override
            public boolean isCurrencyIssuanceAttachmentValid(Transaction transaction) {
                Attachment.MonetarySystemCurrencyIssuance attachment = (Attachment.MonetarySystemCurrencyIssuance)transaction.getAttachment();

                return true;
            }

            @Override
            public boolean isMintable() {
                return true;
            }

        });

    }

    public static CurrencyType getCurrencyType(byte type) {
        return types.get(type);
    }

    public abstract boolean isCurrencyIssuanceAttachmentValid(Transaction transaction);

    public boolean isMintable() {
        return false;
    }

}
