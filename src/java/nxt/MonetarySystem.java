package nxt;

import nxt.util.Convert;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.util.Map;

public abstract class MonetarySystem extends TransactionType {

    private static final byte SUBTYPE_MONETARY_SYSTEM_CURRENCY_ISSUANCE = 0;
    private static final byte SUBTYPE_MONETARY_SYSTEM_RESERVE_INCREASE = 1;
    private static final byte SUBTYPE_MONETARY_SYSTEM_RESERVE_CLAIM = 2;
    private static final byte SUBTYPE_MONETARY_SYSTEM_CURRENCY_TRANSFER = 3;
    private static final byte SUBTYPE_MONETARY_SYSTEM_PUBLISH_EXCHANGE_OFFER = 4;
    private static final byte SUBTYPE_MONETARY_SYSTEM_EXCHANGE_BUY = 5;
    private static final byte SUBTYPE_MONETARY_SYSTEM_EXCHANGE_SELL = 6;
    private static final byte SUBTYPE_MONETARY_SYSTEM_CURRENCY_MINTING = 7;
    private static final byte SUBTYPE_MONETARY_SYSTEM_CURRENCY_DELETION = 8;
    private static final byte SUBTYPE_MONETARY_SYSTEM_SHUFFLING_CREATION = 9;
    private static final byte SUBTYPE_MONETARY_SYSTEM_SHUFFLING_REGISTRATION = 10;
    private static final byte SUBTYPE_MONETARY_SYSTEM_SHUFFLING_PROCESSING = 11;
    private static final byte SUBTYPE_MONETARY_SYSTEM_SHUFFLING_VERIFICATION = 12;
    private static final byte SUBTYPE_MONETARY_SYSTEM_SHUFFLING_DISTRIBUTION = 13;
    private static final byte SUBTYPE_MONETARY_SYSTEM_SHUFFLING_CANCELLATION = 14;

    static TransactionType findTransactionType(byte subtype) {
        switch (subtype) {
            case MonetarySystem.SUBTYPE_MONETARY_SYSTEM_CURRENCY_ISSUANCE:
                return MonetarySystem.CURRENCY_ISSUANCE;
            case MonetarySystem.SUBTYPE_MONETARY_SYSTEM_RESERVE_INCREASE:
                return MonetarySystem.RESERVE_INCREASE;
            case MonetarySystem.SUBTYPE_MONETARY_SYSTEM_RESERVE_CLAIM:
                return MonetarySystem.RESERVE_CLAIM;
            case MonetarySystem.SUBTYPE_MONETARY_SYSTEM_CURRENCY_TRANSFER:
                return MonetarySystem.CURRENCY_TRANSFER;
            case MonetarySystem.SUBTYPE_MONETARY_SYSTEM_PUBLISH_EXCHANGE_OFFER:
                return MonetarySystem.PUBLISH_EXCHANGE_OFFER;
            case MonetarySystem.SUBTYPE_MONETARY_SYSTEM_EXCHANGE_BUY:
                return MonetarySystem.EXCHANGE_BUY;
            case MonetarySystem.SUBTYPE_MONETARY_SYSTEM_EXCHANGE_SELL:
                return MonetarySystem.EXCHANGE_SELL;
            case MonetarySystem.SUBTYPE_MONETARY_SYSTEM_CURRENCY_MINTING:
                return MonetarySystem.CURRENCY_MINTING;
            case MonetarySystem.SUBTYPE_MONETARY_SYSTEM_CURRENCY_DELETION:
                return MonetarySystem.CURRENCY_DELETION;
            case MonetarySystem.SUBTYPE_MONETARY_SYSTEM_SHUFFLING_CREATION:
                return MonetarySystem.SHUFFLING_CREATION;
            case MonetarySystem.SUBTYPE_MONETARY_SYSTEM_SHUFFLING_REGISTRATION:
                return MonetarySystem.SHUFFLING_REGISTRATION;
            case MonetarySystem.SUBTYPE_MONETARY_SYSTEM_SHUFFLING_PROCESSING:
                return MonetarySystem.SHUFFLING_PROCESSING;
            case MonetarySystem.SUBTYPE_MONETARY_SYSTEM_SHUFFLING_VERIFICATION:
                return MonetarySystem.SHUFFLING_VERIFICATION;
            case MonetarySystem.SUBTYPE_MONETARY_SYSTEM_SHUFFLING_DISTRIBUTION:
                return MonetarySystem.SHUFFLING_DISTRIBUTION;
            case MonetarySystem.SUBTYPE_MONETARY_SYSTEM_SHUFFLING_CANCELLATION:
                return MonetarySystem.SHUFFLING_CANCELLATION;
            default:
                return null;
        }
    }

    private MonetarySystem() {}

    @Override
    public final byte getType() {
        return TransactionType.TYPE_MONETARY_SYSTEM;
    }

    @Override
    boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Boolean>> duplicates) {
        if (!(transaction.getAttachment() instanceof Attachment.MonetarySystemAttachment)) {
            return false;
        }
        Attachment.MonetarySystemAttachment attachment = (Attachment.MonetarySystemAttachment) transaction.getAttachment();
        if (attachment.getCurrencyId() == 0) {
            // Shuffling transactions for NXT cannot duplicate a currency related transaction
            return false;
        }
        Currency currency = Currency.getCurrency(attachment.getCurrencyId());
        String nameLower = currency.getName().toLowerCase();
        String codeLower = currency.getCode().toLowerCase();
        boolean isDuplicate = TransactionType.isDuplicate(CURRENCY_ISSUANCE, nameLower, duplicates, false);
        if (! nameLower.equals(codeLower)) {
            isDuplicate = isDuplicate || TransactionType.isDuplicate(CURRENCY_ISSUANCE, codeLower, duplicates, false);
        }
        return isDuplicate;
    }

    public static final TransactionType CURRENCY_ISSUANCE = new MonetarySystem() {

        private final Fee FIVE_LETTER_CURRENCY_ISSUANCE_FEE = new Fee.ConstantFee(40 * Constants.ONE_NXT);
        private final Fee FOUR_LETTER_CURRENCY_ISSUANCE_FEE = new Fee.ConstantFee(1000 * Constants.ONE_NXT);
        private final Fee THREE_LETTER_CURRENCY_ISSUANCE_FEE = new Fee.ConstantFee(25000 * Constants.ONE_NXT);

        @Override
        public byte getSubtype() {
            return SUBTYPE_MONETARY_SYSTEM_CURRENCY_ISSUANCE;
        }

        @Override
        public String getName() {
            return "CurrencyIssuance";
        }

        @Override
        public Fee getBaselineFee(Transaction transaction) {
            Attachment.MonetarySystemCurrencyIssuance attachment = (Attachment.MonetarySystemCurrencyIssuance) transaction.getAttachment();
            if (Currency.getCurrencyByCode(attachment.getCode()) != null || Currency.getCurrencyByCode(attachment.getName().toUpperCase()) != null
                    || Currency.getCurrencyByName(attachment.getName()) != null || Currency.getCurrencyByName(attachment.getCode()) != null) {
                return FIVE_LETTER_CURRENCY_ISSUANCE_FEE;
            }
            switch (Math.min(attachment.getCode().length(), attachment.getName().length())) {
                case 3:
                    return THREE_LETTER_CURRENCY_ISSUANCE_FEE;
                case 4:
                    return FOUR_LETTER_CURRENCY_ISSUANCE_FEE;
                case 5:
                    return FIVE_LETTER_CURRENCY_ISSUANCE_FEE;
                default:
                    // never, invalid code length will be checked and caught later
                    return THREE_LETTER_CURRENCY_ISSUANCE_FEE;
            }
        }

        @Override
        Attachment.MonetarySystemCurrencyIssuance parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemCurrencyIssuance(buffer, transactionVersion);
        }

        @Override
        Attachment.MonetarySystemCurrencyIssuance parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemCurrencyIssuance(attachmentData);
        }

        @Override
        boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Boolean>> duplicates) {
            Attachment.MonetarySystemCurrencyIssuance attachment = (Attachment.MonetarySystemCurrencyIssuance) transaction.getAttachment();
            String nameLower = attachment.getName().toLowerCase();
            String codeLower = attachment.getCode().toLowerCase();
            boolean isDuplicate = TransactionType.isDuplicate(CURRENCY_ISSUANCE, nameLower, duplicates, true);
            if (! nameLower.equals(codeLower)) {
                isDuplicate = isDuplicate || TransactionType.isDuplicate(CURRENCY_ISSUANCE, codeLower, duplicates, true);
            }
            return isDuplicate;
        }

        @Override
        void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            Attachment.MonetarySystemCurrencyIssuance attachment = (Attachment.MonetarySystemCurrencyIssuance) transaction.getAttachment();
            if (attachment.getMaxSupply() > Constants.MAX_CURRENCY_TOTAL_SUPPLY
                    || attachment.getMaxSupply() <= 0
                    || attachment.getInitialSupply() < 0
                    || attachment.getInitialSupply() > attachment.getMaxSupply()
                    || attachment.getReserveSupply() < 0
                    || attachment.getReserveSupply() > attachment.getMaxSupply()
                    || attachment.getIssuanceHeight() < 0
                    || attachment.getMinReservePerUnitNQT() < 0
                    || attachment.getDecimals() < 0 || attachment.getDecimals() > 8
                    || attachment.getRuleset() != 0) {
                throw new NxtException.NotValidException("Invalid currency issuance: " + attachment.getJSONObject());
            }
            int t = 1;
            for (int i = 0; i < 32; i++) {
                if ((t & attachment.getType()) != 0 && CurrencyType.get(t) == null) {
                    throw new NxtException.NotValidException("Invalid currency type: " + attachment.getType());
                }
                t <<= 1;
            }
            CurrencyType.validate(attachment.getType(), transaction);
            CurrencyType.validateCurrencyNaming(transaction.getSenderId(), attachment);
        }


        @Override
        boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        @Override
        void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.MonetarySystemCurrencyIssuance attachment = (Attachment.MonetarySystemCurrencyIssuance) transaction.getAttachment();
            Currency.addCurrency(transaction, attachment);
            senderAccount.addToCurrencyAndUnconfirmedCurrencyUnits(transaction.getId(), attachment.getInitialSupply());
        }

        @Override
        public boolean canHaveRecipient() {
            return false;
        }

    };

    public static final TransactionType RESERVE_INCREASE = new MonetarySystem() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_MONETARY_SYSTEM_RESERVE_INCREASE;
        }

        @Override
        public String getName() {
            return "ReserveIncrease";
        }

        @Override
        Attachment.MonetarySystemReserveIncrease parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemReserveIncrease(buffer, transactionVersion);
        }

        @Override
        Attachment.MonetarySystemReserveIncrease parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemReserveIncrease(attachmentData);
        }

        @Override
        void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            Attachment.MonetarySystemReserveIncrease attachment = (Attachment.MonetarySystemReserveIncrease) transaction.getAttachment();
            if (attachment.getAmountPerUnitNQT() <= 0) {
                throw new NxtException.NotValidException("Reserve increase NXT amount must be positive: " + attachment.getAmountPerUnitNQT());
            }
            CurrencyType.validate(Currency.getCurrency(attachment.getCurrencyId()), transaction);
        }

        @Override
        boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemReserveIncrease attachment = (Attachment.MonetarySystemReserveIncrease) transaction.getAttachment();
            Currency currency = Currency.getCurrency(attachment.getCurrencyId());
            if (senderAccount.getUnconfirmedBalanceNQT() >= Math.multiplyExact(currency.getReserveSupply(), attachment.getAmountPerUnitNQT())) {
                senderAccount.addToUnconfirmedBalanceNQT(-Math.multiplyExact(currency.getReserveSupply(), attachment.getAmountPerUnitNQT()));
                return true;
            }
            return false;
        }

        @Override
        void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemReserveIncrease attachment = (Attachment.MonetarySystemReserveIncrease) transaction.getAttachment();
            senderAccount.addToUnconfirmedBalanceNQT(Math.multiplyExact(Currency.getCurrency(attachment.getCurrencyId()).getReserveSupply(), attachment.getAmountPerUnitNQT()));
        }

        @Override
        void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.MonetarySystemReserveIncrease attachment = (Attachment.MonetarySystemReserveIncrease) transaction.getAttachment();
            Currency.increaseReserve(senderAccount, attachment.getCurrencyId(), attachment.getAmountPerUnitNQT());
        }

        @Override
        public boolean canHaveRecipient() {
            return false;
        }

    };

    public static final TransactionType RESERVE_CLAIM = new MonetarySystem() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_MONETARY_SYSTEM_RESERVE_CLAIM;
        }

        @Override
        public String getName() {
            return "ReserveClaim";
        }

        @Override
        Attachment.MonetarySystemReserveClaim parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemReserveClaim(buffer, transactionVersion);
        }

        @Override
        Attachment.MonetarySystemReserveClaim parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemReserveClaim(attachmentData);
        }

        @Override
        void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            Attachment.MonetarySystemReserveClaim attachment = (Attachment.MonetarySystemReserveClaim) transaction.getAttachment();
            if (attachment.getUnits() <= 0) {
                throw new NxtException.NotValidException("Reserve claim number of units must be positive: " + attachment.getUnits());
            }
            CurrencyType.validate(Currency.getCurrency(attachment.getCurrencyId()), transaction);
        }

        @Override
        boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemReserveClaim attachment = (Attachment.MonetarySystemReserveClaim) transaction.getAttachment();
            if (senderAccount.getUnconfirmedCurrencyUnits(attachment.getCurrencyId()) >= attachment.getUnits()) {
                senderAccount.addToUnconfirmedCurrencyUnits(attachment.getCurrencyId(), -attachment.getUnits());
                return true;
            }
            return false;
        }

        @Override
        void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemReserveClaim attachment = (Attachment.MonetarySystemReserveClaim) transaction.getAttachment();
            senderAccount.addToUnconfirmedCurrencyUnits(attachment.getCurrencyId(), attachment.getUnits());
        }

        @Override
        void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.MonetarySystemReserveClaim attachment = (Attachment.MonetarySystemReserveClaim) transaction.getAttachment();
            Currency.claimReserve(senderAccount, attachment.getCurrencyId(), attachment.getUnits());
        }

        @Override
        public boolean canHaveRecipient() {
            return false;
        }

    };

    public static final TransactionType CURRENCY_TRANSFER = new MonetarySystem() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_MONETARY_SYSTEM_CURRENCY_TRANSFER;
        }

        @Override
        public String getName() {
            return "CurrencyTransfer";
        }

        @Override
        Attachment.MonetarySystemCurrencyTransfer parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemCurrencyTransfer(buffer, transactionVersion);
        }

        @Override
        Attachment.MonetarySystemCurrencyTransfer parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemCurrencyTransfer(attachmentData);
        }

        @Override
        void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            Attachment.MonetarySystemCurrencyTransfer attachment = (Attachment.MonetarySystemCurrencyTransfer) transaction.getAttachment();
            if (attachment.getUnits() <= 0) {
                throw new NxtException.NotValidException("Invalid currency transfer: " + attachment.getJSONObject());
            }
            if (transaction.getRecipientId() == Genesis.CREATOR_ID) {
                throw new NxtException.NotValidException("Currency transfer to genesis account not allowed");
            }
            Currency currency = Currency.getCurrency(attachment.getCurrencyId());
            CurrencyType.validate(currency, transaction);
            if (! currency.isActive()) {
                throw new NxtException.NotCurrentlyValidException("Currency not currently active: " + attachment.getJSONObject());
            }
        }

        @Override
        boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemCurrencyTransfer attachment = (Attachment.MonetarySystemCurrencyTransfer) transaction.getAttachment();
            if (attachment.getUnits() > senderAccount.getUnconfirmedCurrencyUnits(attachment.getCurrencyId())) {
                return false;
            }
            senderAccount.addToUnconfirmedCurrencyUnits(attachment.getCurrencyId(), -attachment.getUnits());
            return true;
        }

        @Override
        void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemCurrencyTransfer attachment = (Attachment.MonetarySystemCurrencyTransfer) transaction.getAttachment();
            senderAccount.addToUnconfirmedCurrencyUnits(attachment.getCurrencyId(), attachment.getUnits());
        }

        @Override
        void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.MonetarySystemCurrencyTransfer attachment = (Attachment.MonetarySystemCurrencyTransfer) transaction.getAttachment();
            Currency.transferCurrency(senderAccount, recipientAccount, attachment.getCurrencyId(), attachment.getUnits());
            CurrencyTransfer.addTransfer(transaction, attachment);
        }

        @Override
        public boolean canHaveRecipient() {
            return true;
        }

    };

    public static final TransactionType PUBLISH_EXCHANGE_OFFER = new MonetarySystem() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_MONETARY_SYSTEM_PUBLISH_EXCHANGE_OFFER;
        }

        @Override
        public String getName() {
            return "PublishExchangeOffer";
        }

        @Override
        Attachment.MonetarySystemPublishExchangeOffer parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemPublishExchangeOffer(buffer, transactionVersion);
        }

        @Override
        Attachment.MonetarySystemPublishExchangeOffer parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemPublishExchangeOffer(attachmentData);
        }

        @Override
        void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            Attachment.MonetarySystemPublishExchangeOffer attachment = (Attachment.MonetarySystemPublishExchangeOffer) transaction.getAttachment();
            if (attachment.getBuyRateNQT() <= 0
                    || attachment.getSellRateNQT() <= 0
                    || attachment.getBuyRateNQT() > attachment.getSellRateNQT()
                    || attachment.getTotalBuyLimit() < 0
                    || attachment.getTotalSellLimit() < 0
                    || attachment.getInitialBuySupply() < 0
                    || attachment.getInitialSellSupply() < 0
                    || attachment.getExpirationHeight() < 0) {
                throw new NxtException.NotValidException("Invalid exchange offer: " + attachment.getJSONObject());
            }
            if (attachment.getTotalBuyLimit() < attachment.getInitialBuySupply()
                || attachment.getTotalSellLimit() < attachment.getInitialSellSupply()) {
                throw new NxtException.NotValidException("Initial supplies must not exceed total limits");
            }
            if (attachment.getExpirationHeight() <= Nxt.getBlockchain().getHeight()) {
                throw new NxtException.NotCurrentlyValidException("Expiration height must be after current blockchain height");
            }
            Currency currency = Currency.getCurrency(attachment.getCurrencyId());
            CurrencyType.validate(currency, transaction);
            if (! currency.isActive()) {
                throw new NxtException.NotCurrentlyValidException("Currency not currently active: " + attachment.getJSONObject());
            }
        }

        @Override
        boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemPublishExchangeOffer attachment = (Attachment.MonetarySystemPublishExchangeOffer) transaction.getAttachment();
            if (senderAccount.getUnconfirmedBalanceNQT() >= Math.multiplyExact(attachment.getInitialBuySupply(), attachment.getBuyRateNQT())
                    && senderAccount.getUnconfirmedCurrencyUnits(attachment.getCurrencyId()) >= attachment.getInitialSellSupply()) {
                senderAccount.addToUnconfirmedBalanceNQT(-Math.multiplyExact(attachment.getInitialBuySupply(), attachment.getBuyRateNQT()));
                senderAccount.addToUnconfirmedCurrencyUnits(attachment.getCurrencyId(), -attachment.getInitialSellSupply());
                return true;
            }
            return false;
        }

        @Override
        void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemPublishExchangeOffer attachment = (Attachment.MonetarySystemPublishExchangeOffer) transaction.getAttachment();
            senderAccount.addToUnconfirmedBalanceNQT(Math.multiplyExact(attachment.getInitialBuySupply(), attachment.getBuyRateNQT()));
            senderAccount.addToUnconfirmedCurrencyUnits(attachment.getCurrencyId(), attachment.getInitialSellSupply());
        }

        @Override
        void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.MonetarySystemPublishExchangeOffer attachment = (Attachment.MonetarySystemPublishExchangeOffer) transaction.getAttachment();
            CurrencyExchangeOffer.publishOffer(transaction, attachment);
        }

        @Override
        public boolean canHaveRecipient() {
            return false;
        }

    };

    abstract static class MonetarySystemExchange extends MonetarySystem {

        @Override
        final void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            Attachment.MonetarySystemExchange attachment = (Attachment.MonetarySystemExchange) transaction.getAttachment();
            if (attachment.getRateNQT() <= 0 || attachment.getUnits() == 0) {
                throw new NxtException.NotValidException("Invalid exchange: " + attachment.getJSONObject());
            }
            Currency currency = Currency.getCurrency(attachment.getCurrencyId());
            CurrencyType.validate(currency, transaction);
            if (! currency.isActive()) {
                throw new NxtException.NotCurrentlyValidException("Currency not active: " + attachment.getJSONObject());
            }
        }

        @Override
        public final boolean canHaveRecipient() {
            return false;
        }

    }

    public static final TransactionType EXCHANGE_BUY = new MonetarySystemExchange() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_MONETARY_SYSTEM_EXCHANGE_BUY;
        }

        @Override
        public String getName() {
            return "ExchangeBuy";
        }

        @Override
        Attachment.MonetarySystemExchangeBuy parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemExchangeBuy(buffer, transactionVersion);
        }

        @Override
        Attachment.MonetarySystemExchangeBuy parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemExchangeBuy(attachmentData);
        }


        @Override
        boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemExchangeBuy attachment = (Attachment.MonetarySystemExchangeBuy) transaction.getAttachment();
            if (senderAccount.getUnconfirmedBalanceNQT() >= Math.multiplyExact(attachment.getUnits(), attachment.getRateNQT())) {
                senderAccount.addToUnconfirmedBalanceNQT(-Math.multiplyExact(attachment.getUnits(), attachment.getRateNQT()));
                return true;
            }
            return false;
        }

        @Override
        void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemExchangeBuy attachment = (Attachment.MonetarySystemExchangeBuy) transaction.getAttachment();
            senderAccount.addToUnconfirmedBalanceNQT(Math.multiplyExact(attachment.getUnits(), attachment.getRateNQT()));
        }

        @Override
        void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.MonetarySystemExchangeBuy attachment = (Attachment.MonetarySystemExchangeBuy) transaction.getAttachment();
            CurrencyExchangeOffer.exchangeNXTForCurrency(transaction, senderAccount, attachment.getCurrencyId(), attachment.getRateNQT(), attachment.getUnits());
        }

    };

    public static final TransactionType EXCHANGE_SELL = new MonetarySystemExchange() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_MONETARY_SYSTEM_EXCHANGE_SELL;
        }

        @Override
        public String getName() {
            return "ExchangeSell";
        }

        @Override
        Attachment.MonetarySystemExchangeSell parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemExchangeSell(buffer, transactionVersion);
        }

        @Override
        Attachment.MonetarySystemExchangeSell parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemExchangeSell(attachmentData);
        }

        @Override
        boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemExchangeSell attachment = (Attachment.MonetarySystemExchangeSell) transaction.getAttachment();
            if (senderAccount.getUnconfirmedCurrencyUnits(attachment.getCurrencyId()) >= attachment.getUnits()) {
                senderAccount.addToUnconfirmedCurrencyUnits(attachment.getCurrencyId(), -attachment.getUnits());
                return true;
            }
            return false;
        }

        @Override
        void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemExchangeSell attachment = (Attachment.MonetarySystemExchangeSell) transaction.getAttachment();
            senderAccount.addToUnconfirmedCurrencyUnits(attachment.getCurrencyId(), attachment.getUnits());
        }

        @Override
        void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.MonetarySystemExchangeSell attachment = (Attachment.MonetarySystemExchangeSell) transaction.getAttachment();
            CurrencyExchangeOffer.exchangeCurrencyForNXT(transaction, senderAccount, attachment.getCurrencyId(), attachment.getRateNQT(), attachment.getUnits());
        }

    };

    public static final TransactionType CURRENCY_MINTING = new MonetarySystem() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_MONETARY_SYSTEM_CURRENCY_MINTING;
        }

        @Override
        public String getName() {
            return "CurrencyMinting";
        }

        @Override
        Attachment.MonetarySystemCurrencyMinting parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemCurrencyMinting(buffer, transactionVersion);
        }

        @Override
        Attachment.MonetarySystemCurrencyMinting parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemCurrencyMinting(attachmentData);
        }

        @Override
        void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            Attachment.MonetarySystemCurrencyMinting attachment = (Attachment.MonetarySystemCurrencyMinting) transaction.getAttachment();
            Currency currency = Currency.getCurrency(attachment.getCurrencyId());
            if (!currency.isActive()) {
                throw new NxtException.NotCurrentlyValidException("Currency not currently active " + attachment.getJSONObject());
            }
            CurrencyType.validate(currency, transaction);
            if (attachment.getUnits() <= 0) {
                throw new NxtException.NotValidException("Invalid number of units: " + attachment.getUnits());
            }
            if (attachment.getUnits() > (currency.getMaxSupply() - currency.getReserveSupply()) / Constants.MAX_MINTING_RATIO) {
                throw new NxtException.NotValidException(String.format("Cannot mint more than 1/%d of the total units supply in a single request", Constants.MAX_MINTING_RATIO));
            }
            if (!currency.isActive()) {
                throw new NxtException.NotCurrentlyValidException("Currency not currently active " + attachment.getJSONObject());
            }
            long counter = CurrencyMint.getCounter(attachment.getCurrencyId(), transaction.getSenderId());
            if (attachment.getCounter() <= counter) {
                throw new NxtException.NotCurrentlyValidException(String.format("Counter %d has to be bigger than %d", attachment.getCounter(), counter));
            }
            if (!CurrencyMinting.meetsTarget(transaction.getSenderId(), currency, attachment)) {
                throw new NxtException.NotCurrentlyValidException(String.format("Hash doesn't meet target %s", attachment.getJSONObject()));
            }
        }

        @Override
        boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        @Override
        void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.MonetarySystemCurrencyMinting attachment = (Attachment.MonetarySystemCurrencyMinting) transaction.getAttachment();
            CurrencyMint.mintCurrency(senderAccount, attachment);
        }

        @Override
        boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Boolean>> duplicates) {
            Attachment.MonetarySystemCurrencyMinting attachment = (Attachment.MonetarySystemCurrencyMinting) transaction.getAttachment();
            return super.isDuplicate(transaction, duplicates) ||
                    TransactionType.isDuplicate(CURRENCY_MINTING, attachment.getCurrencyId() + ":" + transaction.getSenderId(), duplicates, true);
        }

        @Override
        boolean isUnconfirmedDuplicate(Transaction transaction, Map<TransactionType, Map<String, Boolean>> duplicates) {
            Attachment.MonetarySystemCurrencyMinting attachment = (Attachment.MonetarySystemCurrencyMinting) transaction.getAttachment();
            return TransactionType.isDuplicate(CURRENCY_MINTING, attachment.getCurrencyId() + ":" + transaction.getSenderId(), duplicates, true);
        }

        @Override
        public boolean canHaveRecipient() {
            return false;
        }

    };

    public static final TransactionType CURRENCY_DELETION = new MonetarySystem() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_MONETARY_SYSTEM_CURRENCY_DELETION;
        }

        @Override
        public String getName() {
            return "CurrencyDeletion";
        }

        @Override
        Attachment.MonetarySystemCurrencyDeletion parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemCurrencyDeletion(buffer, transactionVersion);
        }

        @Override
        Attachment.MonetarySystemCurrencyDeletion parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemCurrencyDeletion(attachmentData);
        }

        @Override
        boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Boolean>> duplicates) {
            Attachment.MonetarySystemAttachment attachment = (Attachment.MonetarySystemAttachment) transaction.getAttachment();
            Currency currency = Currency.getCurrency(attachment.getCurrencyId());
            String nameLower = currency.getName().toLowerCase();
            String codeLower = currency.getCode().toLowerCase();
            boolean isDuplicate = TransactionType.isDuplicate(CURRENCY_ISSUANCE, nameLower, duplicates, true);
            if (! nameLower.equals(codeLower)) {
                isDuplicate = isDuplicate || TransactionType.isDuplicate(CURRENCY_ISSUANCE, codeLower, duplicates, true);
            }
            return isDuplicate;
        }

        @Override
        void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            Attachment.MonetarySystemCurrencyDeletion attachment = (Attachment.MonetarySystemCurrencyDeletion) transaction.getAttachment();
            Currency currency = Currency.getCurrency(attachment.getCurrencyId());
            CurrencyType.validate(currency, transaction);
            if (!currency.canBeDeletedBy(transaction.getSenderId())) {
                throw new NxtException.NotCurrentlyValidException("Currency " + Long.toUnsignedString(currency.getId()) + " cannot be deleted by account " +
                        Long.toUnsignedString(transaction.getSenderId()));
            }
        }


        @Override
        boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        @Override
        void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.MonetarySystemCurrencyDeletion attachment = (Attachment.MonetarySystemCurrencyDeletion) transaction.getAttachment();
            Currency currency = Currency.getCurrency(attachment.getCurrencyId());
            currency.delete(transaction.getSenderId());
        }

        @Override
        public boolean canHaveRecipient() {
            return false;
        }

    };

    //TODO: shuffling transactions not yet reviewed
    public static final TransactionType SHUFFLING_CREATION = new MonetarySystem() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_MONETARY_SYSTEM_SHUFFLING_CREATION;
        }

        @Override
        public String getName() {
            return "ShufflingCreation";
        }

        @Override
        Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemShufflingCreation(buffer, transactionVersion);
        }

        @Override
        Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemShufflingCreation(attachmentData);
        }

        @Override
        void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            Attachment.MonetarySystemShufflingCreation attachment = (Attachment.MonetarySystemShufflingCreation) transaction.getAttachment();
            if (attachment.isCurrency()) {
                Currency currency = Currency.getCurrency(attachment.getCurrencyId());
                CurrencyType.validate(currency, transaction);
                if (!currency.isActive()) {
                    throw new NxtException.NotValidException("Currency is not active: " + currency.getCode());
                }
                if (attachment.getAmount() <= 0 || attachment.getAmount() > Constants.MAX_CURRENCY_TOTAL_SUPPLY) {
                    throw new NxtException.NotValidException("Invalid currency amount " + attachment.getAmount());
                }
            } else {
                if (attachment.getAmount() <= 0 || attachment.getAmount() > Constants.MAX_BALANCE_NQT) {
                    throw new NxtException.NotValidException("Invalid NQT amount " + attachment.getAmount());
                }
            }
            if (Account.getAccount(transaction.getSenderId()).getPublicKey() == null) {
                throw new NxtException.NotValidException(String.format("Account %s without public key cannot create shuffling",
                        Convert.rsAccount(transaction.getSenderId())));
            }
            if (attachment.getParticipantCount() < Constants.MIN_NUMBER_OF_SHUFFLING_PARTICIPANTS
                    || attachment.getParticipantCount() > Constants.MAX_NUMBER_OF_SHUFFLING_PARTICIPANTS) {
                throw new NxtException.NotValidException(String.format("Number of participants %d is not between %d and %d",
                        attachment.getParticipantCount(), Constants.MIN_NUMBER_OF_SHUFFLING_PARTICIPANTS, Constants.MAX_NUMBER_OF_SHUFFLING_PARTICIPANTS));
            }
            if (attachment.getCancellationHeight() <= Nxt.getBlockchain().getHeight()) {
                throw new NxtException.NotValidException(String.format("Cancellation height %d is smaller than current height %d",
                        attachment.getCancellationHeight(), Nxt.getBlockchain().getHeight()));
            }
        }

        @Override
        boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemShufflingCreation attachment = (Attachment.MonetarySystemShufflingCreation) transaction.getAttachment();
            if (attachment.isCurrency()) {
                if (senderAccount.getUnconfirmedCurrencyUnits(attachment.getCurrencyId()) >= attachment.getAmount()) {
                    senderAccount.addToUnconfirmedCurrencyUnits(attachment.getCurrencyId(), -attachment.getAmount());
                    return true;
                }
            } else {
                if (senderAccount.getUnconfirmedBalanceNQT() >= attachment.getAmount()) {
                    senderAccount.addToUnconfirmedBalanceNQT(-attachment.getAmount());
                    return true;
                }
            }
            return false;
        }

        @Override
        void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.MonetarySystemShufflingCreation attachment = (Attachment.MonetarySystemShufflingCreation) transaction.getAttachment();
            Shuffling.addShuffling(transaction, attachment);
        }

        @Override
        void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemShufflingCreation attachment = (Attachment.MonetarySystemShufflingCreation) transaction.getAttachment();
            if (attachment.isCurrency()) {
                senderAccount.addToUnconfirmedCurrencyUnits(attachment.getCurrencyId(), attachment.getAmount());
            } else {
                senderAccount.addToUnconfirmedBalanceNQT(attachment.getAmount());
            }
        }

        @Override
        public boolean canHaveRecipient() {
            return false;
        }
    };

    public static final TransactionType SHUFFLING_REGISTRATION = new MonetarySystem() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_MONETARY_SYSTEM_SHUFFLING_REGISTRATION;
        }

        @Override
        public String getName() {
            return "ShufflingRegistration";
        }

        @Override
        Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemShufflingRegistration(buffer, transactionVersion);
        }

        @Override
        Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemShufflingRegistration(attachmentData);
        }

        @Override
        void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            Attachment.MonetarySystemShufflingRegistration attachment = (Attachment.MonetarySystemShufflingRegistration) transaction.getAttachment();
            Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
            if (shuffling == null) {
                throw new NxtException.NotValidException("Shuffling not found: " + attachment.getShufflingId());
            }
            if (!shuffling.isRegistrationAllowed()) {
                throw new NxtException.NotValidException("Shuffling registration has ended");
            }
            if (Account.getAccount(transaction.getSenderId()).getPublicKey() == null) {
                throw new NxtException.NotValidException(String.format("Account %s without public key cannot register for shuffling",
                        Convert.rsAccount(transaction.getSenderId())));
            }
            if (shuffling.isParticipant(transaction.getSenderId())) {
                throw new NxtException.NotValidException(String.format("Account %s is already registered for shuffling %d",
                        Convert.rsAccount(transaction.getSenderId()), shuffling.getId()));
            }
        }

        @Override
        boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemShufflingRegistration attachment = (Attachment.MonetarySystemShufflingRegistration) transaction.getAttachment();
            Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
            if (shuffling.isCurrency()) {
                if (senderAccount.getUnconfirmedCurrencyUnits(shuffling.getCurrencyId()) >= shuffling.getAmount()) {
                    senderAccount.addToUnconfirmedCurrencyUnits(shuffling.getCurrencyId(), -shuffling.getAmount());
                    return true;
                }
            } else {
                if (senderAccount.getUnconfirmedBalanceNQT() >= shuffling.getAmount()) {
                    senderAccount.addToUnconfirmedBalanceNQT(-shuffling.getAmount());
                    return true;
                }
            }
            return false;
        }

        @Override
        void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.MonetarySystemShufflingRegistration attachment = (Attachment.MonetarySystemShufflingRegistration) transaction.getAttachment();
            Shuffling.addParticipant(transaction, attachment);
        }

        @Override
        void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemShufflingRegistration attachment = (Attachment.MonetarySystemShufflingRegistration) transaction.getAttachment();
            Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
            if (shuffling.isCurrency()) {
                senderAccount.addToUnconfirmedCurrencyUnits(shuffling.getCurrencyId(), shuffling.getAmount());
            } else {
                senderAccount.addToUnconfirmedBalanceNQT(shuffling.getAmount());
            }
        }

        @Override
        public boolean canHaveRecipient() {
            return false;
        }
    };

    public static final TransactionType SHUFFLING_PROCESSING = new MonetarySystem() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_MONETARY_SYSTEM_SHUFFLING_PROCESSING;
        }

        @Override
        public String getName() {
            return "ShufflingProcessing";
        }

        @Override
        Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemShufflingProcessing(buffer, transactionVersion);
        }

        @Override
        Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemShufflingProcessing(attachmentData);
        }

        @Override
        void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            Attachment.MonetarySystemShufflingProcessing attachment = (Attachment.MonetarySystemShufflingProcessing)transaction.getAttachment();
            Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
            if (shuffling == null) {
                throw new NxtException.NotValidException("Shuffling not found: " + attachment.getShufflingId());
            }
            if (!shuffling.isProcessingAllowed()) {
                throw new NxtException.NotValidException("Shuffling is not ready for processing");
            }
            if (!shuffling.isParticipant(transaction.getSenderId())) {
                throw new NxtException.NotValidException(String.format("Account %s is not registered for shuffling %d",
                        Convert.rsAccount(transaction.getSenderId()), shuffling.getId()));
            }
            if (shuffling.isParticipantProcessingComplete(transaction.getSenderId())) {
                throw new NxtException.NotValidException(String.format("Participant %s processing already complete",
                        Convert.rsAccount(transaction.getSenderId())));
            }
        }

        @Override
        boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.MonetarySystemShufflingProcessing attachment = (Attachment.MonetarySystemShufflingProcessing)transaction.getAttachment();
            Shuffling.updateParticipantData(transaction, attachment);
        }

        @Override
        void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {}

        @Override
        public boolean canHaveRecipient() {
            return false;
        }
    };

    public static final TransactionType SHUFFLING_VERIFICATION = new MonetarySystem() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_MONETARY_SYSTEM_SHUFFLING_VERIFICATION;
        }

        @Override
        public String getName() {
            return "ShufflingVerification";
        }

        @Override
        Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemShufflingVerification(buffer, transactionVersion);
        }

        @Override
        Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemShufflingVerification(attachmentData);
        }

        @Override
        void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            Attachment.MonetarySystemShufflingVerification attachment = (Attachment.MonetarySystemShufflingVerification) transaction.getAttachment();
            Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
            if (shuffling == null) {
                throw new NxtException.NotValidException("Shuffling not found: " + attachment.getShufflingId());
            }
            if (!shuffling.isVerificationAllowed()) {
                throw new NxtException.NotValidException("Shuffling not ready for verification: " + attachment.getShufflingId());
            }
            if (!shuffling.isParticipant(transaction.getSenderId())) {
                throw new NxtException.NotValidException(String.format("Account %s is not registered for shuffling %d",
                        Convert.rsAccount(transaction.getSenderId()), shuffling.getId()));
            }
            if (shuffling.isParticipantVerified(transaction.getSenderId())) {
                throw new NxtException.NotValidException("Shuffling participant already verified: " + attachment.getShufflingId());
            }
        }

        @Override
        boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.MonetarySystemShufflingVerification attachment = (Attachment.MonetarySystemShufflingVerification) transaction.getAttachment();
            Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
            shuffling.verify(transaction.getSenderId());
        }

        @Override
        void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        @Override
        public boolean canHaveRecipient() {
            return false;
        }
    };

    public static final TransactionType SHUFFLING_DISTRIBUTION = new MonetarySystem() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_MONETARY_SYSTEM_SHUFFLING_DISTRIBUTION;
        }

        @Override
        public String getName() {
            return "ShufflingDistribution";
        }

        @Override
        Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemShufflingDistribution(buffer, transactionVersion);
        }

        @Override
        Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemShufflingDistribution(attachmentData);
        }

        @Override
        boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Boolean>> duplicates) {
            if (super.isDuplicate(transaction, duplicates)) {
                return true;
            }
            Attachment.MonetarySystemShufflingDistribution attachment = (Attachment.MonetarySystemShufflingDistribution) transaction.getAttachment();
            String key = Long.toUnsignedString(attachment.getShufflingId()) + "." + Long.toUnsignedString(transaction.getSenderId());
            return TransactionType.isDuplicate(SHUFFLING_DISTRIBUTION, key, duplicates, true);
        }

        @Override
        void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            Attachment.MonetarySystemShufflingDistribution attachment = (Attachment.MonetarySystemShufflingDistribution) transaction.getAttachment();
            Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
            if (shuffling == null) {
                throw new NxtException.NotValidException("Shuffling not found: " + attachment.getShufflingId());
            }
            if (shuffling.getIssuerId() != transaction.getSenderId()) {
                throw new NxtException.NotValidException(String.format("Only shuffling issuer %s can trigger distribution",
                        Convert.rsAccount(transaction.getSenderId())));
            }
            if (!shuffling.isDistributionAllowed()) {
                throw new NxtException.NotValidException("Shuffling not ready for distribution: " + attachment.getShufflingId());
            }
        }

        @Override
        boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.MonetarySystemShufflingDistribution attachment = (Attachment.MonetarySystemShufflingDistribution) transaction.getAttachment();
            Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
            shuffling.distribute();
        }

        @Override
        void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        @Override
        public boolean canHaveRecipient() {
            return false;
        }
    };

    public static final TransactionType SHUFFLING_CANCELLATION = new MonetarySystem() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_MONETARY_SYSTEM_SHUFFLING_CANCELLATION;
        }

        @Override
        public String getName() {
            return "ShufflingCancellation";
        }

        @Override
        Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemShufflingCancellation(buffer, transactionVersion);
        }

        @Override
        Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemShufflingCancellation(attachmentData);
        }

        @Override
        boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Boolean>> duplicates) {
            if (super.isDuplicate(transaction, duplicates)) {
                return true;
            }
            Attachment.MonetarySystemShufflingCancellation attachment = (Attachment.MonetarySystemShufflingCancellation) transaction.getAttachment();
            String key = Long.toUnsignedString(attachment.getShufflingId()) + "." + Long.toUnsignedString(transaction.getSenderId());
            return TransactionType.isDuplicate(SHUFFLING_CANCELLATION, key, duplicates, true);
        }

        @Override
        void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            Attachment.MonetarySystemShufflingCancellation attachment = (Attachment.MonetarySystemShufflingCancellation) transaction.getAttachment();
            Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
            if (shuffling == null) {
                throw new NxtException.NotValidException("Shuffling not found: " + attachment.getShufflingId());
            }
            if (!shuffling.isCancellationAllowed(transaction.getSenderId())) {
                throw new NxtException.NotValidException(String.format("Shuffling in state %s cannot be cancelled by account %s",
                       shuffling.getStage(), Convert.rsAccount(transaction.getSenderId())));
            }
        }

        @Override
        boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.MonetarySystemShufflingCancellation attachment = (Attachment.MonetarySystemShufflingCancellation) transaction.getAttachment();
            Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
            shuffling.cancel();
        }

        @Override
        void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {}

        @Override
        public boolean canHaveRecipient() {
            return false;
        }
    };
}
