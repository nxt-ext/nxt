package nxt;

import nxt.util.Convert;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;

public abstract class MonetarySystem extends TransactionType {

    private MonetarySystem() {}

    @Override
    public final byte getType() {
        return TransactionType.TYPE_MONETARY_SYSTEM;
    }

    public static final TransactionType CURRENCY_ISSUANCE = new MonetarySystem() {

        @Override
        public byte getSubtype() {
            return TransactionType.SUBTYPE_MONETARY_SYSTEM_CURRENCY_ISSUANCE;
        }

        @Override
        public Fee getBaselineFee() {
            return BASELINE_CURRENCY_ISSUANCE_FEE;
        }

        @Override
        public Fee getNextFee() {
            return NEXT_CURRENCY_ISSUANCE_FEE;
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
        boolean isDuplicate(Transaction transaction, Map<TransactionType, Set<String>> duplicates) {
            Attachment.MonetarySystemCurrencyIssuance attachment = (Attachment.MonetarySystemCurrencyIssuance) transaction.getAttachment();
            String nameLower = attachment.getName().toLowerCase();
            String codeLower = attachment.getCode().toLowerCase();
            boolean isDuplicate = TransactionType.isDuplicate(CURRENCY_ISSUANCE, nameLower, duplicates);
            if (! nameLower.equals(codeLower)) {
                isDuplicate = isDuplicate || TransactionType.isDuplicate(CURRENCY_ISSUANCE, codeLower, duplicates);
            }
            return isDuplicate;
        }

        @Override
        void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            Attachment.MonetarySystemCurrencyIssuance attachment = (Attachment.MonetarySystemCurrencyIssuance) transaction.getAttachment();
            if (attachment.getTotalSupply() > Constants.MAX_CURRENCY_TOTAL_SUPPLY
                    || attachment.getIssuanceHeight() < 0
                    || attachment.getMinReservePerUnitNQT() < 0 || attachment.getMinReservePerUnitNQT() > Constants.MAX_BALANCE_NQT
                    || attachment.getRuleset() != 0) {
                throw new NxtException.NotValidException("Invalid currency issuance: " + attachment.getJSONObject());
            }
            CurrencyType.validate(attachment.getType(), transaction);
            CurrencyType.validateCurrencyNaming(attachment);
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
            senderAccount.addToCurrencyAndUnconfirmedCurrencyUnits(transaction.getId(), attachment.getCurrentSupply());
        }

        @Override
        public boolean hasRecipient() {
            return false;
        }

    };

    public static final TransactionType RESERVE_INCREASE = new MonetarySystem() {

        @Override
        public byte getSubtype() {
            return TransactionType.SUBTYPE_MONETARY_SYSTEM_RESERVE_INCREASE;
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
            if (attachment.getAmountNQT() <= 0) {
                throw new NxtException.NotValidException("Reserve increase NXT amount must be positive: " + attachment.getAmountNQT());
            }
            CurrencyType.validate(Currency.getCurrency(attachment.getCurrencyId()), transaction);
        }

        @Override
        boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemReserveIncrease attachment = (Attachment.MonetarySystemReserveIncrease) transaction.getAttachment();
            if (senderAccount.getUnconfirmedBalanceNQT() >= Convert.safeMultiply(Currency.getCurrency(attachment.getCurrencyId()).getTotalSupply(), attachment.getAmountNQT())) {
                senderAccount.addToUnconfirmedBalanceNQT(-Convert.safeMultiply(Currency.getCurrency(attachment.getCurrencyId()).getTotalSupply(), attachment.getAmountNQT()));
                return true;
            }
            return false;
        }

        @Override
        void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemReserveIncrease attachment = (Attachment.MonetarySystemReserveIncrease) transaction.getAttachment();
            senderAccount.addToUnconfirmedBalanceNQT(Convert.safeMultiply(Currency.getCurrency(attachment.getCurrencyId()).getTotalSupply(), attachment.getAmountNQT()));
        }

        @Override
        void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.MonetarySystemReserveIncrease attachment = (Attachment.MonetarySystemReserveIncrease) transaction.getAttachment();
            Currency.increaseReserve(senderAccount, attachment.getCurrencyId(), attachment.getAmountNQT());
        }

        @Override
        public boolean hasRecipient() {
            return false;
        }

    };

    public static final TransactionType RESERVE_CLAIM = new MonetarySystem() {

        @Override
        public byte getSubtype() {
            return TransactionType.SUBTYPE_MONETARY_SYSTEM_RESERVE_CLAIM;
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
        public boolean hasRecipient() {
            return false;
        }

    };

    public static final TransactionType CURRENCY_TRANSFER = new MonetarySystem() {

        @Override
        public byte getSubtype() {
            return TransactionType.SUBTYPE_MONETARY_SYSTEM_CURRENCY_TRANSFER;
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
        public boolean hasRecipient() {
            return true;
        }

    };

    public static final TransactionType PUBLISH_EXCHANGE_OFFER = new MonetarySystem() {

        @Override
        public byte getSubtype() {
            return TransactionType.SUBTYPE_MONETARY_SYSTEM_PUBLISH_EXCHANGE_OFFER;
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
                    || attachment.getTotalBuyLimit() < 0
                    || attachment.getTotalSellLimit() < 0
                    || attachment.getInitialBuySupply() < 0
                    || attachment.getInitialSellSupply() < 0
                    || attachment.getExpirationHeight() < 0) {
                throw new NxtException.NotValidException("Invalid exchange offer: " + attachment.getJSONObject());
            }
            Currency currency = Currency.getCurrency(attachment.getCurrencyId());
            CurrencyType.validate(currency, transaction);
            Account account = Account.getAccount(transaction.getSenderId());
            long requiredBalance = Convert.safeMultiply(attachment.getInitialBuySupply(), attachment.getBuyRateNQT());
            if (account.getUnconfirmedBalanceNQT() < requiredBalance) {
                throw new NxtException.NotCurrentlyValidException(String.format("Cannot publish exchange offer, account balance %d lower than offer initial balance %d",
                        account.getUnconfirmedBalanceNQT(), requiredBalance));
            }
            long requiredUnits = account.getUnconfirmedCurrencyUnits(attachment.getCurrencyId());
            if (requiredUnits < attachment.getInitialSellSupply()) {
                throw new NxtException.NotCurrentlyValidException(String.format("Cannot publish exchange offer, currency units %d lower than offer initial units %d",
                        requiredUnits, attachment.getInitialSellSupply()));
            }
            if (! currency.isActive()) {
                throw new NxtException.NotCurrentlyValidException("Currency not currently active: " + attachment.getJSONObject());
            }
        }

        @Override
        boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemPublishExchangeOffer attachment = (Attachment.MonetarySystemPublishExchangeOffer) transaction.getAttachment();
            if (senderAccount.getUnconfirmedBalanceNQT() >= Convert.safeMultiply(attachment.getInitialBuySupply(), attachment.getBuyRateNQT())
                    && senderAccount.getUnconfirmedCurrencyUnits(attachment.getCurrencyId()) >= attachment.getInitialSellSupply()) {
                senderAccount.addToUnconfirmedBalanceNQT(-Convert.safeMultiply(attachment.getInitialBuySupply(), attachment.getBuyRateNQT()));
                senderAccount.addToUnconfirmedCurrencyUnits(attachment.getCurrencyId(), -attachment.getInitialSellSupply());
                return true;
            }
            return false;

        }

        @Override
        void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemPublishExchangeOffer attachment = (Attachment.MonetarySystemPublishExchangeOffer) transaction.getAttachment();
            senderAccount.addToUnconfirmedBalanceNQT(Convert.safeMultiply(attachment.getInitialBuySupply(), attachment.getBuyRateNQT()));
            senderAccount.addToUnconfirmedCurrencyUnits(attachment.getCurrencyId(), attachment.getInitialSellSupply());
        }

        @Override
        void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.MonetarySystemPublishExchangeOffer attachment = (Attachment.MonetarySystemPublishExchangeOffer) transaction.getAttachment();
            CurrencyExchangeOffer.publishOffer(transaction, attachment);
        }

        @Override
        public boolean hasRecipient() {
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
        public final boolean hasRecipient() {
            return false;
        }

    }

    public static final TransactionType EXCHANGE_BUY = new MonetarySystemExchange() {

        @Override
        public byte getSubtype() {
            return TransactionType.SUBTYPE_MONETARY_SYSTEM_EXCHANGE_BUY;
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
            if (senderAccount.getUnconfirmedBalanceNQT() >= Convert.safeMultiply(attachment.getUnits(), attachment.getRateNQT())) {
                senderAccount.addToUnconfirmedBalanceNQT(-Convert.safeMultiply(attachment.getUnits(), attachment.getRateNQT()));
                return true;
            }
            return false;
        }

        @Override
        void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemExchangeBuy attachment = (Attachment.MonetarySystemExchangeBuy) transaction.getAttachment();
            senderAccount.addToUnconfirmedBalanceNQT(Convert.safeMultiply(attachment.getUnits(), attachment.getRateNQT()));
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
            return TransactionType.SUBTYPE_MONETARY_SYSTEM_EXCHANGE_SELL;
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
            return TransactionType.SUBTYPE_MONETARY_SYSTEM_CURRENCY_MINTING;
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
            if (attachment.getUnits() <= 0 || attachment.getUnits() > Currency.getCurrency(attachment.getCurrencyId()).getTotalSupply() / Constants.MAX_MINTING_RATIO) {
                throw new NxtException.NotValidException("Invalid currency minting: " + attachment.getJSONObject());
            }
            Currency currency = Currency.getCurrency(attachment.getCurrencyId());
            CurrencyType.validate(currency, transaction);
            if (! currency.isActive()) {
                throw new NxtException.NotCurrentlyValidException("Currency not currently active " + attachment.getJSONObject());
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
            CurrencyMint.mintCurrency(transaction, senderAccount, attachment);
        }

        @Override
        public boolean hasRecipient() {
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
            }
            if (attachment.getAmount() <= 0 || attachment.getAmount() > Constants.MAX_CURRENCY_TOTAL_SUPPLY
                    || attachment.getParticipantCount() < Constants.MIN_SHUFFLING_PARTICIPANTS
                    || attachment.getParticipantCount() > Constants.MAX_SHUFFLING_PARTICIPANTS
                    || attachment.getCancellationHeight() <= Nxt.getBlockchain().getHeight()) {
                throw new NxtException.NotValidException("Invalid shuffling creation: " + attachment.getJSONObject());
            }
        }

        @Override
        boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemShufflingCreation attachment = (Attachment.MonetarySystemShufflingCreation) transaction.getAttachment();
            if (!attachment.isCurrency()) {
                if (senderAccount.getUnconfirmedBalanceNQT() >= attachment.getAmount()) {
                    senderAccount.addToUnconfirmedBalanceNQT(-attachment.getAmount());
                    return true;
                }
            } else {
                if (senderAccount.getUnconfirmedCurrencyUnits(attachment.getCurrencyId()) >= attachment.getAmount()) {
                    senderAccount.addToUnconfirmedCurrencyUnits(attachment.getCurrencyId(), -attachment.getAmount());
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
            if (!attachment.isCurrency()) {
                senderAccount.addToUnconfirmedBalanceNQT(attachment.getAmount());
            } else {
                senderAccount.addToUnconfirmedCurrencyUnits(attachment.getCurrencyId(), attachment.getAmount());
            }
        }

        @Override
        public boolean hasRecipient() {
            return false;
        }
    };

    public static final TransactionType SHUFFLING_REGISTRATION = new MonetarySystem() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_MONETARY_SYSTEM_SHUFFLING_REGISTRATION;
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
            if (!shuffling.isRegistrationEnabled()) {
                throw new NxtException.NotValidException("Shuffling registration has ended");
            }
            if (shuffling.isParticipant(transaction.getSenderId())) {
                throw new NxtException.NotValidException(String.format("Account %s is already registered for shuffling %s",
                        Convert.rsAccount(transaction.getSenderId()), Convert.toUnsignedLong(shuffling.getId())));
            }
        }

        @Override
        boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemShufflingRegistration attachment = (Attachment.MonetarySystemShufflingRegistration) transaction.getAttachment();
            Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
            if (!shuffling.isCurrency()) {
                if (senderAccount.getUnconfirmedBalanceNQT() >= shuffling.getAmount()) {
                    senderAccount.addToUnconfirmedBalanceNQT(-shuffling.getAmount());
                    return true;
                }
            } else {
                if (senderAccount.getUnconfirmedCurrencyUnits(shuffling.getCurrencyId()) >= shuffling.getAmount()) {
                    senderAccount.addToUnconfirmedCurrencyUnits(shuffling.getCurrencyId(), -shuffling.getAmount());
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
        }

        @Override
        public boolean hasRecipient() {
            return false;
        }
    };

    public static final TransactionType SHUFFLING_VERIFICATION = new MonetarySystem() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_MONETARY_SYSTEM_SHUFFLING_DISTRIBUTION;
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
            if (!shuffling.isVerificationEnabled()) {
                throw new NxtException.NotValidException("Shuffling not ready for verification: " + attachment.getShufflingId());
            }
            if (shuffling.isParticipant(transaction.getSenderId())) {
                throw new NxtException.NotValidException("Only participant can verify: " + attachment.getShufflingId());
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
        public boolean hasRecipient() {
            return false;
        }
    };

    public static final TransactionType SHUFFLING_DISTRIBUTION = new MonetarySystem() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_MONETARY_SYSTEM_SHUFFLING_DISTRIBUTION;
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
        void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            Attachment.MonetarySystemShufflingDistribution attachment = (Attachment.MonetarySystemShufflingDistribution) transaction.getAttachment();
            Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
            if (shuffling == null) {
                throw new NxtException.NotValidException("Shuffling not found: " + attachment.getShufflingId());
            }
            if (!shuffling.isDistributionEnabled()) {
                throw new NxtException.NotValidException("Shuffling not ready for distribution: " + attachment.getShufflingId());
            }
            if (shuffling.getIssuerId() != transaction.getSenderId()) {
                throw new NxtException.NotValidException("Only shuffling issuer can trigger distribution: " + attachment.getShufflingId());
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
        public boolean hasRecipient() {
            return false;
        }
    };

    public static final TransactionType SHUFFLING_CANCELLATION = new MonetarySystem() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_MONETARY_SYSTEM_SHUFFLING_CANCELLATION;
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
        void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            Attachment.MonetarySystemShufflingCancellation attachment = (Attachment.MonetarySystemShufflingCancellation) transaction.getAttachment();
            Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
            if (shuffling == null) {
                throw new NxtException.NotValidException("Shuffling not found: " + attachment.getShufflingId());
            }
            if (!shuffling.isCancelingEnabled()) {
                throw new NxtException.NotValidException("Shuffling cannot be cancelled: " + attachment.getShufflingId());
            }
            if (shuffling.getIssuerId() != transaction.getSenderId()) {
                throw new NxtException.NotValidException("Only shuffling issuer can cancel shuffling: " + attachment.getShufflingId());
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
            shuffling.distribute();
        }

        @Override
        void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        @Override
        public boolean hasRecipient() {
            return false;
        }
    };

}

