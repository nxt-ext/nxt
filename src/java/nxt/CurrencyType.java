package nxt;

import nxt.crypto.HashFunction;

import java.util.EnumSet;
import java.util.Set;

public enum CurrencyType {

    EXCHANGEABLE((byte)0x01) {

        @Override
        public void validate(Attachment attachment, Set<CurrencyType> validators) throws NxtException.NotValidException {
            if (attachment instanceof Attachment.MonetarySystemCurrencyIssuance) {
                if (validators.contains(INFLATABLE)) {
                    throw new NxtException.NotValidException("Exchangeable currency cannot be inflated once active");
                }
            }
        }

        @Override
        public void validateMissing(Attachment attachment, Set<CurrencyType> validators) throws NxtException.NotValidException {
            if (attachment instanceof Attachment.MonetarySystemCurrencyIssuance) {
                if (!validators.contains(INFLATABLE)) {
                    throw new NxtException.NotValidException("Currency is not exchangeable and not inflatable");
                }
            }
            if (attachment instanceof Attachment.MonetarySystemExchange ||
                    attachment instanceof Attachment.MonetarySystemExchangeOfferPublication) {
                throw new NxtException.NotValidException("Currency is not exchangeable");
            }
        }
    },
    RESERVABLE((byte)0x02) {

        @Override
        public void validate(Attachment attachment, Set<CurrencyType> validators) throws NxtException.NotValidException {
            if (attachment instanceof Attachment.MonetarySystemCurrencyIssuance) {
                int issuanceHeight = ((Attachment.MonetarySystemCurrencyIssuance) attachment).getIssuanceHeight();
                if  (issuanceHeight <= Nxt.getBlockchain().getHeight()) {
                    throw new NxtException.NotValidException(
                        String.format("Reservable currency activation height %d not higher than current height %d",
                                issuanceHeight, Nxt.getBlockchain().getLastBlock().getHeight()));
                }
            }
        }

        @Override
        public void validateMissing(Attachment attachment, Set<CurrencyType> validators) throws NxtException.NotValidException {
            if (attachment instanceof Attachment.MonetarySystemReserveIncrease ||
                    attachment instanceof Attachment.MonetarySystemReserveClaim) {
                throw new NxtException.NotValidException("Cannot insrease or claim reserve since currency is not reservable");
            }
        }
    },
    INFLATABLE((byte)0x04) {

        @Override
        public void validate(Attachment attachment, Set<CurrencyType> validators) throws NxtException.NotValidException {
            if (attachment instanceof Attachment.MonetarySystemCurrencyIssuance) {
                if (!validators.contains(RESERVABLE)) {
                    throw new NxtException.NotValidException("Inflatable currency must be reservable");
                }
            }
        }

        @Override
        public void validateMissing(Attachment attachment, Set<CurrencyType> validators) throws NxtException.NotValidException {
            if (attachment instanceof Attachment.MonetarySystemReserveIncrease) {
                if (Currency.isActive(((Attachment.MonetarySystemReserveIncrease) attachment).getCurrencyId())) {
                    throw new NxtException.NotValidException("Cannot increase reserve for active currency since currency is not inflatable");
                }
            }
            if (attachment instanceof Attachment.MonetarySystemReserveClaim) {
                if (Currency.isActive(((Attachment.MonetarySystemReserveClaim)attachment).getCurrencyId())) {
                    throw new NxtException.NotValidException("Cannot claim reserve for active currency since currency is not inflatable");
                }
            }
        }
    }, MINTABLE((byte)0x08) {
        @Override
        public void validate(Attachment attachment, Set<CurrencyType> validators) throws NxtException.NotValidException {
            if (attachment instanceof Attachment.MonetarySystemCurrencyIssuance) {
                Attachment.MonetarySystemCurrencyIssuance issuanceAttachment = (Attachment.MonetarySystemCurrencyIssuance) attachment;
                try {
                    HashFunction.getHashFunction(issuanceAttachment.getAlgorithm());
                } catch(IllegalArgumentException e) {
                    throw new NxtException.NotValidException("Illegal algorithm code specified" , e);
                }
                if (issuanceAttachment.getMinDifficulty() <= 0 ||
                        issuanceAttachment.getMaxDifficulty() < issuanceAttachment.getMinDifficulty()) {
                    throw new NxtException.NotValidException(
                            String.format("Invalid minting difficulties min %d max %d",
                                    issuanceAttachment.getMinDifficulty(), issuanceAttachment.getMaxDifficulty()));
                }
            }
        }

        @Override
        public void validateMissing(Attachment attachment, Set<CurrencyType> validators) throws NxtException.NotValidException {
            if (attachment instanceof Attachment.MonetarySystemCurrencyIssuance) {
                Attachment.MonetarySystemCurrencyIssuance issuanceAttachment = (Attachment.MonetarySystemCurrencyIssuance) attachment;
                if (issuanceAttachment.getMinDifficulty() != 0 ||
                        issuanceAttachment.getMaxDifficulty() != 0 ||
                        issuanceAttachment.getAlgorithm() != 0) {
                    throw new NxtException.NotValidException("Non mintable currency should not specify algorithm or difficulty");
                }
            }
            if (attachment instanceof Attachment.MonetarySystemMoneyMinting) {
                throw new NxtException.NotValidException("Currency is not mintable");
            }
        }

    }, SHUFFLEABLE((byte)0x10);

    private final byte code;

    CurrencyType(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }

    public void validate(Attachment attachment, Set<CurrencyType> validators) throws NxtException.NotValidException {}

    public void validateMissing(Attachment attachment, Set<CurrencyType> validators) throws NxtException.NotValidException {}

    public static void validate(Attachment attachment, byte type, Transaction transaction) throws NxtException.ValidationException {
        // sanity checks for all currency types
        if (Nxt.getBlockchain().getLastBlock().getHeight() < Constants.MONETARY_SYSTEM_BLOCK) {
            throw new NxtException.NotYetEnabledException("Monetary System not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
        }
        if (transaction.getAmountNQT() != 0) {
            throw new NxtException.NotValidException("Currency issuance NXT amount must be 0");
        }

        final EnumSet<CurrencyType> validators = EnumSet.noneOf(CurrencyType.class);
        for (CurrencyType validator : CurrencyType.values()) {
            if ((validator.getCode() & type) != 0) {
                validators.add(validator);
            }
        }
        if (validators.isEmpty()) {
            throw new NxtException.NotValidException("Invalid currency type " + type);
        }
        for (CurrencyType validator : CurrencyType.values()) {
            if ((validator.getCode() & type) != 0) {
                validator.validate(attachment, validators);
            } else {
                validator.validateMissing(attachment, validators);
            }
        }
    }

    public static void validateCurrencyNaming(Attachment.MonetarySystemCurrencyIssuance attachment) throws NxtException.NotValidException {
        if (attachment.getName().length() < Constants.MIN_CURRENCY_NAME_LENGTH || attachment.getName().length() > Constants.MAX_CURRENCY_NAME_LENGTH
                || attachment.getCode().length() != Constants.CURRENCY_CODE_LENGTH
                || attachment.getDescription().length() > Constants.MAX_CURRENCY_DESCRIPTION_LENGTH) {
            throw new NxtException.NotValidException("Invalid currency name code or description: " + attachment.getJSONObject());
        }
        String normalizedName = attachment.getName().toLowerCase();
        for (int i = 0; i < normalizedName.length(); i++) {
            if (Constants.ALPHABET.indexOf(normalizedName.charAt(i)) < 0) {
                throw new NxtException.NotValidException("Invalid currency name: " + normalizedName);
            }
        }
        if (Currency.isNameUsed(normalizedName)) {
            throw new NxtException.NotValidException("Currency name already used: " + normalizedName);
        }
        for (int i = 0; i < attachment.getCode().length(); i++) {
            if (Constants.ALLOWED_CURRENCY_CODE_LETTERS.indexOf(attachment.getCode().charAt(i)) < 0) {
                throw new NxtException.NotValidException("Invalid currency code: " + attachment.getCode());
            }
        }
        if (Currency.isCodeUsed(attachment.getCode())) {
            throw new NxtException.NotValidException("Currency code already used: " + attachment.getCode());
        }
    }

    public static byte getCurrencyType(long currencyId) throws NxtException.NotValidException {
        Currency currency = Currency.getCurrency(currencyId);
        if (currency == null) {
            throw new NxtException.NotValidException("Unknown currency id: " + currencyId);
        }
        return currency.getType();
    }

}
