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
            boolean isDuplicate = TransactionType.isDuplicate(CURRENCY_ISSUANCE, attachment.getName(), duplicates);
            isDuplicate = isDuplicate || TransactionType.isDuplicate(CURRENCY_ISSUANCE, attachment.getCode(), duplicates);
            return isDuplicate;
        }

        @Override
        void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            if (Nxt.getBlockchain().getLastBlock().getHeight() < Constants.MONETARY_SYSTEM_BLOCK) {
                throw new NxtException.NotYetEnabledException("Monetary System not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
            }

            Attachment.MonetarySystemCurrencyIssuance attachment = (Attachment.MonetarySystemCurrencyIssuance) transaction.getAttachment();
            if (transaction.getAmountNQT() != 0) {
                throw new NxtException.NotValidException("Currency issuance NXT amount must be 0");
            }
            validateCurrencyNaming(attachment);
            if (!CurrencyType.getCurrencyType(attachment.getType()).isCurrencyIssuanceAttachmentValid(transaction)) {
                throw new NxtException.NotValidException("Invalid currency issuance, type specific problem: " + attachment.getJSONObject());
            }
            if (attachment.getTotalSupply() > Constants.MAX_CURRENCY_TOTAL_SUPPLY
                    || attachment.getIssuanceHeight() < 0
                    || attachment.getMinReservePerUnitNQT() < 0 || attachment.getMinReservePerUnitNQT() > Constants.MAX_BALANCE_NQT
                    || attachment.getRuleset() != 0) {
                throw new NxtException.NotValidException("Invalid currency issuance: " + attachment.getJSONObject());
            }
        }

        private void validateCurrencyNaming(Attachment.MonetarySystemCurrencyIssuance attachment) throws NxtException.NotValidException {
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
            Currency.addCurrency(transaction.getId(), transaction.getSenderId(), attachment.getName(), attachment.getCode(), attachment.getDescription(),
                    attachment.getType(), attachment.getTotalSupply(), attachment.getCurrentSupply(), attachment.getIssuanceHeight(), attachment.getMinReservePerUnitNQT(),
                    attachment.getMinDifficulty(), attachment.getMaxDifficulty(), attachment.getRuleset(), attachment.getAlgorithm());
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
            if (Nxt.getBlockchain().getLastBlock().getHeight() < Constants.MONETARY_SYSTEM_BLOCK) {
                throw new NxtException.NotYetEnabledException("Monetary System not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
            }
            Attachment.MonetarySystemReserveIncrease attachment = (Attachment.MonetarySystemReserveIncrease) transaction.getAttachment();
            if (Currency.isActive(attachment.getCurrencyId())) {
                throw new NxtException.NotValidException("Cannot increase reserve, currency already issued at height: " + Currency.getCurrency(attachment.getCurrencyId()).getIssuanceHeight());
            }
            if (transaction.getAmountNQT() != 0 || attachment.getAmountNQT() <= 0) {
                throw new NxtException.NotValidException("Invalid reserve increase: " + attachment.getJSONObject());
            }
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

        //TODO: exceptions
        @Override
        void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            if (Nxt.getBlockchain().getLastBlock().getHeight() < Constants.MONETARY_SYSTEM_BLOCK) {
                throw new NxtException.NotYetEnabledException("Monetary System not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
            }
            Attachment.MonetarySystemReserveClaim attachment = (Attachment.MonetarySystemReserveClaim) transaction.getAttachment();
            if (transaction.getAmountNQT() != 0
                    || !Currency.isActive(attachment.getCurrencyId())
                    || attachment.getUnits() <= 0) {
                throw new NxtException.NotValidException("Invalid reserve claim: " + attachment.getJSONObject());
            }
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

    public static final TransactionType MONEY_TRANSFER = new MonetarySystem() {

        @Override
        public byte getSubtype() {
            return TransactionType.SUBTYPE_MONETARY_SYSTEM_MONEY_TRANSFER;
        }

        @Override
        Attachment.MonetarySystemMoneyTransfer parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemMoneyTransfer(buffer, transactionVersion);
        }

        @Override
        Attachment.MonetarySystemMoneyTransfer parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemMoneyTransfer(attachmentData);
        }

        @Override
        void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            if (Nxt.getBlockchain().getLastBlock().getHeight() < Constants.MONETARY_SYSTEM_BLOCK) {
                throw new NxtException.NotYetEnabledException("Monetary System not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
            }
            Attachment.MonetarySystemMoneyTransfer attachment = (Attachment.MonetarySystemMoneyTransfer) transaction.getAttachment();
            if (!Currency.isActive(attachment.getCurrencyId()) || attachment.getUnits() <= 0) {
                throw new NxtException.NotValidException("Invalid money transfer: " + attachment.getJSONObject());
            }
            if (transaction.getAmountNQT() != 0) {
                throw new NxtException.NotValidException("Invalid money transfer: " + attachment.getJSONObject());
            }
        }

        @Override
        boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemMoneyTransfer attachment = (Attachment.MonetarySystemMoneyTransfer) transaction.getAttachment();
            if (attachment.getUnits() > senderAccount.getUnconfirmedCurrencyUnits(attachment.getCurrencyId())) {
                return false;
            }
            senderAccount.addToUnconfirmedCurrencyUnits(attachment.getCurrencyId(), -attachment.getUnits());
            return true;
        }

        @Override
        void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemMoneyTransfer attachment = (Attachment.MonetarySystemMoneyTransfer) transaction.getAttachment();
            senderAccount.addToUnconfirmedCurrencyUnits(attachment.getCurrencyId(), attachment.getUnits());
        }

        @Override
        void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.MonetarySystemMoneyTransfer attachment = (Attachment.MonetarySystemMoneyTransfer) transaction.getAttachment();
            Currency.transferMoney(senderAccount, attachment.getRecipientId(), attachment.getCurrencyId(), attachment.getUnits());
        }

        @Override
        public boolean hasRecipient() {
            return false;
        }

    };

    public static final TransactionType EXCHANGE_OFFER_PUBLICATION = new MonetarySystem() {

        @Override
        public byte getSubtype() {
            return TransactionType.SUBTYPE_MONETARY_SYSTEM_EXCHANGE_OFFER_PUBLICATION;
        }

        @Override
        Attachment.MonetarySystemExchangeOfferPublication parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemExchangeOfferPublication(buffer, transactionVersion);
        }

        @Override
        Attachment.MonetarySystemExchangeOfferPublication parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemExchangeOfferPublication(attachmentData);
        }

        //TODO: exceptions
        @Override
        void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            if (Nxt.getBlockchain().getLastBlock().getHeight() < Constants.MONETARY_SYSTEM_BLOCK) {
                throw new NxtException.NotYetEnabledException("Monetary System not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
            }
            Attachment.MonetarySystemExchangeOfferPublication attachment = (Attachment.MonetarySystemExchangeOfferPublication) transaction.getAttachment();
            if (transaction.getAmountNQT() != 0
                    || !Currency.isActive(attachment.getCurrencyId())
                    || attachment.getBuyRateNQT() <= 0
                    || attachment.getSellRateNQT() <= 0
                    || attachment.getTotalBuyLimit() < 0
                    || attachment.getTotalSellLimit() < 0
                    || attachment.getInitialBuySupply() < 0
                    || attachment.getInitialSellSupply() < 0
                    || attachment.getExpirationHeight() < 0) {
                throw new NxtException.NotValidException("Invalid exchange offer publication: " + attachment.getJSONObject());
            }
        }

        @Override
        boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemExchangeOfferPublication attachment = (Attachment.MonetarySystemExchangeOfferPublication) transaction.getAttachment();
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
            Attachment.MonetarySystemExchangeOfferPublication attachment = (Attachment.MonetarySystemExchangeOfferPublication) transaction.getAttachment();
            senderAccount.addToUnconfirmedBalanceNQT(Convert.safeMultiply(attachment.getInitialBuySupply(), attachment.getBuyRateNQT()));
            senderAccount.addToUnconfirmedCurrencyUnits(attachment.getCurrencyId(), attachment.getInitialSellSupply());
        }

        @Override
        void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.MonetarySystemExchangeOfferPublication attachment = (Attachment.MonetarySystemExchangeOfferPublication) transaction.getAttachment();
            CurrencyExchange.publishOffer(transaction.getId(), senderAccount, attachment.getCurrencyId(), attachment.getBuyRateNQT(), attachment.getSellRateNQT(), attachment.getTotalBuyLimit(), attachment.getTotalSellLimit(), attachment.getInitialBuySupply(), attachment.getInitialSellSupply(), attachment.getExpirationHeight());
        }

        @Override
        public boolean hasRecipient() {
            return false;
        }

    };

    public static final TransactionType EXCHANGE = new MonetarySystem() {

        @Override
        public byte getSubtype() {
            return TransactionType.SUBTYPE_MONETARY_SYSTEM_EXCHANGE;
        }

        @Override
        Attachment.MonetarySystemExchange parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemExchange(buffer, transactionVersion);
        }

        @Override
        Attachment.MonetarySystemExchange parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemExchange(attachmentData);
        }

        @Override
        void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            if (Nxt.getBlockchain().getLastBlock().getHeight() < Constants.MONETARY_SYSTEM_BLOCK) {
                throw new NxtException.NotYetEnabledException("Monetary System not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
            }
            Attachment.MonetarySystemExchange attachment = (Attachment.MonetarySystemExchange) transaction.getAttachment();
            if (transaction.getAmountNQT() != 0
                    || !Currency.isActive(attachment.getCurrencyId())
                    || attachment.getRateNQT() <= 0
                    || attachment.getUnits() == 0) {
                throw new NxtException.NotValidException("Invalid exchange: " + attachment.getJSONObject());
            }
        }

        @Override
        boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemExchange attachment = (Attachment.MonetarySystemExchange) transaction.getAttachment();
            if (attachment.isBuy()) {
                if (senderAccount.getUnconfirmedBalanceNQT() >= Convert.safeMultiply(attachment.getUnits(), attachment.getRateNQT())) {
                    senderAccount.addToUnconfirmedBalanceNQT(-Convert.safeMultiply(attachment.getUnits(), attachment.getRateNQT()));
                    return true;
                }
            } else {
                if (senderAccount.getUnconfirmedCurrencyUnits(attachment.getCurrencyId()) >= -attachment.getUnits()) {
                    senderAccount.addToUnconfirmedCurrencyUnits(attachment.getCurrencyId(), attachment.getUnits());
                    return true;
                }
            }
            return false;
        }

        @Override
        void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemExchange attachment = (Attachment.MonetarySystemExchange) transaction.getAttachment();
            if (attachment.isBuy()) {
                senderAccount.addToUnconfirmedBalanceNQT(Convert.safeMultiply(attachment.getUnits(), attachment.getRateNQT()));
            } else {
                senderAccount.addToUnconfirmedCurrencyUnits(attachment.getCurrencyId(), -attachment.getUnits());
            }
        }

        @Override
        void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.MonetarySystemExchange attachment = (Attachment.MonetarySystemExchange) transaction.getAttachment();
            if (attachment.isBuy()) {
                CurrencyExchange.exchangeNXTForCurrency(senderAccount, attachment.getCurrencyId(), attachment.getRateNQT(), attachment.getUnits());
            } else {
                CurrencyExchange.exchangeCurrencyForNXT(senderAccount, attachment.getCurrencyId(), attachment.getRateNQT(), -attachment.getUnits());
            }
        }

        @Override
        public boolean hasRecipient() {
            return false;
        }

    };

    public static final TransactionType MONEY_MINTING = new MonetarySystem() {

        @Override
        public byte getSubtype() {
            return TransactionType.SUBTYPE_MONETARY_SYSTEM_MONEY_MINTING;
        }

        @Override
        Attachment.MonetarySystemMoneyMinting parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemMoneyMinting(buffer, transactionVersion);
        }

        @Override
        Attachment.MonetarySystemMoneyMinting parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemMoneyMinting(attachmentData);
        }

        //TODO: exceptions
        @Override
        void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            if (Nxt.getBlockchain().getLastBlock().getHeight() < Constants.MONETARY_SYSTEM_BLOCK) {
                throw new NxtException.NotYetEnabledException("Monetary System not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
            }
            Attachment.MonetarySystemMoneyMinting attachment = (Attachment.MonetarySystemMoneyMinting) transaction.getAttachment();
            if (transaction.getAmountNQT() != 0
                    || !Currency.isActive(attachment.getCurrencyId())
                    || !CurrencyType.getCurrencyType(Currency.getCurrency(attachment.getCurrencyId()).getType()).isMintable()
                    || attachment.getUnits() <= 0 || attachment.getUnits() > Currency.getCurrency(attachment.getCurrencyId()).getTotalSupply() / Constants.MAX_MINTING_RATIO) {
                throw new NxtException.NotValidException("Invalid money minting: " + attachment.getJSONObject());
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
            Attachment.MonetarySystemMoneyMinting attachment = (Attachment.MonetarySystemMoneyMinting) transaction.getAttachment();
            CurrencyMint.mintMoney(senderAccount, attachment.getNonce(), attachment.getCurrencyId(), attachment.getUnits(), attachment.getCounter());
        }

        @Override
        public boolean hasRecipient() {
            return false;
        }

    };

    public static final TransactionType SHUFFLING_INITIATION = new MonetarySystem() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_MONETARY_SYSTEM_SHUFFLING_INITIATION;
        }

        @Override
        Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemShufflingInitiation(buffer, transactionVersion);
        }

        @Override
        Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemShufflingInitiation(attachmentData);
        }

        @Override
        void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            if (Nxt.getBlockchain().getLastBlock().getHeight() < Constants.MONETARY_SYSTEM_BLOCK) {
                throw new NxtException.NotYetEnabledException("Monetary System not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
            }
            Attachment.MonetarySystemShufflingInitiation attachment = (Attachment.MonetarySystemShufflingInitiation) transaction.getAttachment();
            if (transaction.getAmountNQT() != 0
                    || !Currency.isActive(attachment.getCurrencyId())
                    || attachment.getAmount() <= 0 || attachment.getAmount() > Constants.MAX_CURRENCY_TOTAL_SUPPLY
                    || attachment.getNumberOfParticipants() < Constants.MIN_NUMBER_OF_SHUFFLING_PARTICIPANTS || attachment.getNumberOfParticipants() > Constants.MAX_NUMBER_OF_SHUFFLING_PARTICIPANTS
                    || attachment.getMaxInitiationDelay() < Constants.MIN_SHUFFLING_DELAY || attachment.getMaxInitiationDelay() > Constants.MAX_SHUFFLING_DELAY
                    || attachment.getMaxContinuationDelay() < Constants.MIN_SHUFFLING_DELAY || attachment.getMaxContinuationDelay() > Constants.MAX_SHUFFLING_DELAY
                    || attachment.getMaxFinalizationDelay() < Constants.MIN_SHUFFLING_DELAY || attachment.getMaxFinalizationDelay() > Constants.MAX_SHUFFLING_DELAY
                    || attachment.getMaxCancellationDelay() < Constants.MIN_SHUFFLING_DELAY || attachment.getMaxCancellationDelay() > Constants.MAX_SHUFFLING_DELAY) {
                throw new NxtException.NotValidException("Invalid shuffling initiation: " + attachment.getJSONObject());
            }
        }

        @Override
        boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemShufflingInitiation attachment = (Attachment.MonetarySystemShufflingInitiation) transaction.getAttachment();
            if (senderAccount.getUnconfirmedCurrencyUnits(attachment.getCurrencyId()) >= attachment.getAmount()) {
                senderAccount.addToUnconfirmedCurrencyUnits(attachment.getCurrencyId(), -attachment.getAmount());
                return true;
            }
            return false;
        }

        @Override
        void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.MonetarySystemShufflingInitiation attachment = (Attachment.MonetarySystemShufflingInitiation) transaction.getAttachment();
            CoinShuffler.initiateShuffling(transaction.getId(), senderAccount, attachment.getCurrencyId(), attachment.getAmount(), attachment.getNumberOfParticipants(), attachment.getMaxInitiationDelay(), attachment.getMaxContinuationDelay(), attachment.getMaxFinalizationDelay(), attachment.getMaxCancellationDelay());
        }

        @Override
        void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemShufflingInitiation attachment = (Attachment.MonetarySystemShufflingInitiation) transaction.getAttachment();
            senderAccount.addToUnconfirmedCurrencyUnits(attachment.getCurrencyId(), attachment.getAmount());
        }

        @Override
        public boolean hasRecipient() {
            return false;
        }
    };

    public static final TransactionType SHUFFLING_CONTINUATION = new MonetarySystem() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_MONETARY_SYSTEM_SHUFFLING_CONTINUATION;
        }

        @Override
        Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemShufflingContinuation(buffer, transactionVersion);
        }

        @Override
        Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemShufflingContinuation(attachmentData);
        }

        @Override
        void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            if (Nxt.getBlockchain().getLastBlock().getHeight() < Constants.MONETARY_SYSTEM_BLOCK) {
                throw new NxtException.NotYetEnabledException("Monetary System not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
            }
            Attachment.MonetarySystemShufflingContinuation attachment = (Attachment.MonetarySystemShufflingContinuation) transaction.getAttachment();
            if (transaction.getAmountNQT() != 0
                    || !CoinShuffler.isContinued(attachment.getShufflingId())
                    || !CoinShuffler.isParticipant(transaction.getSenderId(), attachment.getShufflingId())
                    || CoinShuffler.sentEncryptedRecipients(transaction.getSenderId(), attachment.getShufflingId())) {
                throw new NxtException.NotValidException("Invalid shuffling continuation: " + attachment.getJSONObject());
            }
        }

        @Override
        boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.MonetarySystemShufflingContinuation attachment = (Attachment.MonetarySystemShufflingContinuation) transaction.getAttachment();
            CoinShuffler.continueShuffling(senderAccount, attachment.getShufflingId(), attachment.getRecipients());
        }

        @Override
        void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        @Override
        public boolean hasRecipient() {
            return false;
        }
    };

    public static final TransactionType SHUFFLING_FINALIZATION = new MonetarySystem() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_MONETARY_SYSTEM_SHUFFLING_FINALIZATION;
        }

        @Override
        Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemShufflingFinalization(buffer, transactionVersion);
        }

        @Override
        Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
            return new Attachment.MonetarySystemShufflingFinalization(attachmentData);
        }

        @Override
        void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            if (Nxt.getBlockchain().getLastBlock().getHeight() < Constants.MONETARY_SYSTEM_BLOCK) {
                throw new NxtException.NotYetEnabledException("Monetary System not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
            }
            Attachment.MonetarySystemShufflingFinalization attachment = (Attachment.MonetarySystemShufflingFinalization) transaction.getAttachment();
            if (transaction.getAmountNQT() != 0
                    || !CoinShuffler.isFinalized(attachment.getShufflingId())
                    || !CoinShuffler.isParticipant(transaction.getSenderId(), attachment.getShufflingId())
                    || CoinShuffler.sentDecryptedRecipients(transaction.getSenderId(), attachment.getShufflingId())
                    || attachment.getRecipients().length != CoinShuffler.getNumberOfParticipants(attachment.getShufflingId())) {
                throw new NxtException.NotValidException("Invalid shuffling finalization: " + attachment.getJSONObject());
            }
        }

        @Override
        boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.MonetarySystemShufflingFinalization attachment = (Attachment.MonetarySystemShufflingFinalization) transaction.getAttachment();
            CoinShuffler.finalizeShuffling(senderAccount, attachment.getShufflingId(), attachment.getRecipients());
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
            if (Nxt.getBlockchain().getLastBlock().getHeight() < Constants.MONETARY_SYSTEM_BLOCK) {
                throw new NxtException.NotYetEnabledException("Monetary System not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
            }
            Attachment.MonetarySystemShufflingCancellation attachment = (Attachment.MonetarySystemShufflingCancellation) transaction.getAttachment();
            if (transaction.getAmountNQT() != 0
                    || (!CoinShuffler.isFinalized(attachment.getShufflingId()) && !CoinShuffler.isCancelled(attachment.getShufflingId()))
                    || !CoinShuffler.isParticipant(transaction.getSenderId(), attachment.getShufflingId())
                    || CoinShuffler.sentDecryptedRecipients(transaction.getSenderId(), attachment.getShufflingId())
                    || CoinShuffler.sentKeys(transaction.getSenderId(), attachment.getShufflingId())
                    || attachment.getKeys().length > 32 * (CoinShuffler.getNumberOfParticipants(attachment.getShufflingId()) - 1)) {
                throw new NxtException.NotValidException("Invalid shuffling cancellation: " + attachment.getJSONObject());
            }
        }

        @Override
        boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.MonetarySystemShufflingCancellation attachment = (Attachment.MonetarySystemShufflingCancellation) transaction.getAttachment();
            CoinShuffler.cancelShuffling(senderAccount, attachment.getShufflingId(), attachment.getKeys());
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

