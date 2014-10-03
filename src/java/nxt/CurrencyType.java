package nxt;

import java.util.EnumSet;
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

    public static void validate(Attachment attachment, byte type, Transaction transaction) throws NxtException.ValidationException {
        // sanity checks for all currency types
        if (Nxt.getBlockchain().getLastBlock().getHeight() < Constants.MONETARY_SYSTEM_BLOCK) {
            throw new NxtException.NotYetEnabledException("Monetary System not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
        }
        if (transaction.getAmountNQT() != 0) {
            throw new NxtException.NotValidException("Currency issuance NXT amount must be 0");
        }

        final EnumSet<CurrencyValidator> validators = EnumSet.noneOf(CurrencyValidator.class);
        for (CurrencyValidator validator : CurrencyValidator.values()) {
            if ((validator.getCode() & type) != 0) {
                validators.add(validator);
            }
        }
        if (validators.isEmpty()) {
            throw new NxtException.NotValidException("Invalid currency type " + type);
        }
        for (CurrencyValidator validator : CurrencyValidator.values()) {
            if ((validator.getCode() & type) != 0) {
                validator.validate(attachment, validators);
            } else {
                validator.validateMissing(attachment, validators);
            }
        }
    }

}
