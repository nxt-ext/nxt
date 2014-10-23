package nxt;

import nxt.crypto.HashFunction;

import java.util.EnumSet;
import java.util.Set;

/**
 * Define and validate currency capabilities
 */
public enum CurrencyType {

    /**
     * Can be exchanged from/to NXT<br>
     */
    EXCHANGEABLE(0x01) {

        @Override
        void validate(Transaction transaction, Attachment attachment, Set<CurrencyType> validators) throws NxtException.NotValidException {
            if (attachment instanceof Attachment.MonetarySystemCurrencyIssuance) {
                if (validators.contains(CLAIMABLE)) {
                    throw new NxtException.NotValidException("Exchangeable currency cannot be claimed");
                }
            }
        }

        @Override
        void validateMissing(Transaction transaction, Attachment attachment, Set<CurrencyType> validators) throws NxtException.NotValidException {
            if (attachment instanceof Attachment.MonetarySystemCurrencyIssuance) {
                if (!validators.contains(CLAIMABLE)) {
                    throw new NxtException.NotValidException("Currency is not exchangeable and not claimable");
                }
            }
            if (attachment instanceof Attachment.MonetarySystemExchange ||
                    attachment instanceof Attachment.MonetarySystemPublishExchangeOffer) {
                throw new NxtException.NotValidException("Currency is not exchangeable");
            }
        }
    },
    /**
     * Transfers are only allowed from/to issuer account<br>
     * Only issuer account can publish exchange offer<br>
     */
    CONTROLLABLE(0x02) {

        @Override
        void validate(Transaction transaction, Attachment attachment, Set<CurrencyType> validators) throws NxtException.NotValidException {
            if (attachment instanceof Attachment.MonetarySystemCurrencyTransfer) {
                Attachment.MonetarySystemCurrencyTransfer transfer = (Attachment.MonetarySystemCurrencyTransfer)attachment;
                if (!Currency.isIssuer(transfer.getCurrencyId(), transaction.getSenderId()) && !Currency.isIssuer(transfer.getCurrencyId(), transfer.getRecipientId())) {
                    throw new NxtException.NotValidException("Controllable currency can only be transferred to/from issuer account");
                }
            }
            if (attachment instanceof Attachment.MonetarySystemPublishExchangeOffer) {
                Attachment.MonetarySystemPublishExchangeOffer offer = (Attachment.MonetarySystemPublishExchangeOffer)attachment;
                if (!Currency.isIssuer(offer.getCurrencyId(), transaction.getSenderId())) {
                    throw new NxtException.NotValidException("Only currency issuer can publish an exchange offer for controllable currency");
                }
            }
        }
        //TODO: no need for validateMissing?
    },
    /**
     * Can be reserved before the currency is active, reserve is distributed to founders once the currency becomes active<br>
     */
    RESERVABLE(0x04) {

        @Override
        void validate(Transaction transaction, Attachment attachment, Set<CurrencyType> validators) throws NxtException.ValidationException {
            if (attachment instanceof Attachment.MonetarySystemCurrencyIssuance) {
                int issuanceHeight = ((Attachment.MonetarySystemCurrencyIssuance) attachment).getIssuanceHeight();
                if  (issuanceHeight <= Nxt.getBlockchain().getHeight()) {
                    throw new NxtException.NotCurrentlyValidException(
                        String.format("Reservable currency activation height %d not higher than current height %d",
                                issuanceHeight, Nxt.getBlockchain().getHeight()));
                }
            }
            if (attachment instanceof Attachment.MonetarySystemReserveIncrease) {
                if (Currency.isActive(((Attachment.MonetarySystemReserveIncrease)attachment).getCurrencyId())) {
                    throw new NxtException.NotCurrentlyValidException("Cannot increase reserve for active currency");
                }
            }
        }

        @Override
        void validateMissing(Transaction transaction, Attachment attachment, Set<CurrencyType> validators) throws NxtException.NotValidException {
            if (attachment instanceof Attachment.MonetarySystemReserveIncrease) {
                throw new NxtException.NotValidException("Cannot increase reserve since currency is not reservable");
            }
        }
    },
    /**
     * Is {@link #RESERVABLE} and can be claimed after currency is active<br>
     * Cannot be {@link #EXCHANGEABLE}
     */
    CLAIMABLE(0x08) {

        @Override
        void validate(Transaction transaction, Attachment attachment, Set<CurrencyType> validators) throws NxtException.ValidationException {
            if (attachment instanceof Attachment.MonetarySystemCurrencyIssuance) {
                if (!validators.contains(RESERVABLE)) {
                    throw new NxtException.NotValidException("Claimable currency must be reservable");
                }
            }
            if (attachment instanceof Attachment.MonetarySystemReserveClaim) {
                if (!Currency.isActive(((Attachment.MonetarySystemReserveIncrease)attachment).getCurrencyId())) {
                    throw new NxtException.NotCurrentlyValidException("Cannot claim reserve since currency is not yet active");
                }
            }
        }

        @Override
        void validateMissing(Transaction transaction, Attachment attachment, Set<CurrencyType> validators) throws NxtException.NotValidException {
            if (attachment instanceof Attachment.MonetarySystemReserveClaim) {
                throw new NxtException.NotValidException("Cannot claim reserve since currency is not claimable");
            }
        }
    },
    /**
     * Can be minted using proof of work algorithm<br>
     */
    MINTABLE(0x10) {
        @Override
        void validate(Transaction transaction, Attachment attachment, Set<CurrencyType> validators) throws NxtException.NotValidException {
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
        void validateMissing(Transaction transaction, Attachment attachment, Set<CurrencyType> validators) throws NxtException.NotValidException {
            if (attachment instanceof Attachment.MonetarySystemCurrencyIssuance) {
                Attachment.MonetarySystemCurrencyIssuance issuanceAttachment = (Attachment.MonetarySystemCurrencyIssuance) attachment;
                if (issuanceAttachment.getMinDifficulty() != 0 ||
                        issuanceAttachment.getMaxDifficulty() != 0 ||
                        issuanceAttachment.getAlgorithm() != 0) {
                    throw new NxtException.NotValidException("Non mintable currency should not specify algorithm or difficulty");
                }
            }
            if (attachment instanceof Attachment.MonetarySystemCurrencyMinting) {
                throw new NxtException.NotValidException("Currency is not mintable");
            }
        }

    },
    /**
     * Support shuffling - not implemented yet<br>
     */
    SHUFFLEABLE(0x20);

    private final int code;

    CurrencyType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    //TODO: better make those abstract?
    void validate(Transaction transaction, Attachment attachment, Set<CurrencyType> validators) throws NxtException.ValidationException {}

    void validateMissing(Transaction transaction, Attachment attachment, Set<CurrencyType> validators) throws NxtException.ValidationException {}

    static void validate(Attachment attachment, int type, Transaction transaction) throws NxtException.ValidationException {
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
            throw new NxtException.NotValidException("Currency type not specified");
        }
        for (CurrencyType validator : CurrencyType.values()) {
            if ((validator.getCode() & type) != 0) {
                validator.validate(transaction, attachment, validators);
            } else {
                validator.validateMissing(transaction, attachment, validators);
            }
        }
    }

    static void validateCurrencyNaming(Attachment.MonetarySystemCurrencyIssuance attachment) throws NxtException.ValidationException {
        String name = attachment.getName();
        String code = attachment.getCode();
        String description = attachment.getDescription();
        validateCurrencyNaming(name, code, description);
    }

    private static void validateCurrencyNaming(String name, String code, String description) throws NxtException.ValidationException {
        if (name.length() < Constants.MIN_CURRENCY_NAME_LENGTH || name.length() > Constants.MAX_CURRENCY_NAME_LENGTH
                || code.length() != Constants.CURRENCY_CODE_LENGTH
                || description.length() > Constants.MAX_CURRENCY_DESCRIPTION_LENGTH) {
            throw new NxtException.NotValidException(String.format("Invalid currency name %s code %s or description %s", name, code, description));
        }
        String normalizedName = name.toLowerCase();
        for (int i = 0; i < normalizedName.length(); i++) {
            if (Constants.ALPHABET.indexOf(normalizedName.charAt(i)) < 0) {
                throw new NxtException.NotValidException("Invalid currency name: " + normalizedName);
            }
        }
        for (int i = 0; i < code.length(); i++) {
            if (Constants.ALLOWED_CURRENCY_CODE_LETTERS.indexOf(code.charAt(i)) < 0) {
                throw new NxtException.NotValidException("Invalid currency code: " + code + " code must be all upper case");
            }
        }
        if (Currency.getCurrencyByName(normalizedName) != null || Currency.getCurrencyByCode(name.toUpperCase()) != null) {
            throw new NxtException.NotCurrentlyValidException("Currency name already used: " + normalizedName);
        }
        if (Currency.getCurrencyByCode(code) != null || Currency.getCurrencyByName(code.toLowerCase()) != null) {
            throw new NxtException.NotCurrentlyValidException("Currency code already used: " + code);
        }
    }

    static int getCurrencyType(long currencyId) throws NxtException.NotValidException {
        Currency currency = Currency.getCurrency(currencyId);
        if (currency == null) {
            throw new NxtException.NotValidException("Unknown currency id: " + currencyId);
        }
        return currency.getType();
    }

}
