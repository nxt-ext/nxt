package nxt;

import nxt.crypto.HashFunction;

import java.util.Set;

public enum CurrencyValidator {

    EXCHANGEABLE(0x01) {

        @Override
        public void validate(Attachment attachment, Set<CurrencyValidator> validators) throws NxtException.NotValidException {
            if (attachment instanceof Attachment.MonetarySystemCurrencyIssuance) {
                if (validators.contains(INFLATABLE)) {
                    throw new NxtException.NotValidException("Exchangeable currency cannot be inflated once active");
                }
            }
        }

        @Override
        public void validateMissing(Attachment attachment, Set<CurrencyValidator> validators) throws NxtException.NotValidException {
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
    RESERVABLE(0x02) {

        @Override
        public void validate(Attachment attachment, Set<CurrencyValidator> validators) throws NxtException.NotValidException {
            if (attachment instanceof Attachment.MonetarySystemCurrencyIssuance) {
                int issuanceHeight = ((Attachment.MonetarySystemCurrencyIssuance) attachment).getIssuanceHeight();
                if  (issuanceHeight > Nxt.getBlockchain().getLastBlock().getHeight()) {
                    throw new NxtException.NotValidException(
                        String.format("Reservable currency activation height %d lower than current height %d",
                                issuanceHeight, Nxt.getBlockchain().getLastBlock().getHeight()));
                }
            }
        }

        @Override
        public void validateMissing(Attachment attachment, Set<CurrencyValidator> validators) throws NxtException.NotValidException {
            if (attachment instanceof Attachment.MonetarySystemReserveIncrease ||
                    attachment instanceof Attachment.MonetarySystemReserveClaim) {
                throw new NxtException.NotValidException("Cannot insrease or claim reserve since currency is not reservable");
            }
        }
    },
    INFLATABLE(0x04) {

        @Override
        public void validate(Attachment attachment, Set<CurrencyValidator> validators) throws NxtException.NotValidException {
            if (attachment instanceof Attachment.MonetarySystemCurrencyIssuance) {
                if (!validators.contains(RESERVABLE)) {
                    throw new NxtException.NotValidException("Inflatable currency must be reservable");
                }
            }
        }

        @Override
        public void validateMissing(Attachment attachment, Set<CurrencyValidator> validators) throws NxtException.NotValidException {
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
    }, MINTABLE(0x08) {
        @Override
        public void validate(Attachment attachment, Set<CurrencyValidator> validators) throws NxtException.NotValidException {
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
        public void validateMissing(Attachment attachment, Set<CurrencyValidator> validators) throws NxtException.NotValidException {
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

    }, SHUFFLEABLE(0x10);

    private final int code;

    CurrencyValidator(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public void validate(Attachment attachment, Set<CurrencyValidator> validators) throws NxtException.NotValidException {}

    public void validateMissing(Attachment attachment, Set<CurrencyValidator> validators) throws NxtException.NotValidException {}
}
