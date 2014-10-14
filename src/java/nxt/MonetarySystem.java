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
            Attachment.MonetarySystemCurrencyIssuance attachment = (Attachment.MonetarySystemCurrencyIssuance) transaction.getAttachment();
            CurrencyType.validateCurrencyNaming(attachment);
            CurrencyType.validate(attachment, attachment.getType(), transaction);
            if (attachment.getTotalSupply() > Constants.MAX_CURRENCY_TOTAL_SUPPLY
                    || attachment.getIssuanceHeight() < 0
                    || attachment.getMinReservePerUnitNQT() < 0 || attachment.getMinReservePerUnitNQT() > Constants.MAX_BALANCE_NQT
                    || attachment.getRuleset() != 0) {
                throw new NxtException.NotValidException("Invalid currency issuance: " + attachment.getJSONObject());
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
            Attachment.MonetarySystemReserveIncrease attachment = (Attachment.MonetarySystemReserveIncrease) transaction.getAttachment();
            int type = CurrencyType.getCurrencyType(attachment.getCurrencyId());
            CurrencyType.validate(attachment, type, transaction);
            if (attachment.getAmountNQT() <= 0) {
                throw new NxtException.NotValidException("Reserve increase NXT amount must be positive: " + attachment.getAmountNQT());
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

        @Override
        void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            Attachment.MonetarySystemReserveClaim attachment = (Attachment.MonetarySystemReserveClaim) transaction.getAttachment();
            int type = CurrencyType.getCurrencyType(attachment.getCurrencyId());
            CurrencyType.validate(attachment, type, transaction);
            if (attachment.getUnits() <= 0) {
                throw new NxtException.NotValidException("Reserve claim number of units must be positive: " + attachment.getUnits());
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
            int type = CurrencyType.getCurrencyType(attachment.getCurrencyId());
            CurrencyType.validate(attachment, type, transaction);
            if (!Currency.isActive(attachment.getCurrencyId()) || attachment.getUnits() <= 0) {
                throw new NxtException.NotValidException("Invalid currency transfer: " + attachment.getJSONObject());
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
            Currency.transferCurrency(senderAccount, attachment.getRecipientId(), attachment.getCurrencyId(), attachment.getUnits());
            CurrencyTransfer.addTransfer(transaction, attachment);
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

        @Override
        void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            Attachment.MonetarySystemExchangeOfferPublication attachment = (Attachment.MonetarySystemExchangeOfferPublication) transaction.getAttachment();
            int type = CurrencyType.getCurrencyType(attachment.getCurrencyId());
            CurrencyType.validate(attachment, type, transaction);
            if (!Currency.isActive(attachment.getCurrencyId())
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
            Attachment.MonetarySystemExchange attachment = (Attachment.MonetarySystemExchange) transaction.getAttachment();
            int type = CurrencyType.getCurrencyType(attachment.getCurrencyId());
            CurrencyType.validate(attachment, type, transaction);
            if (!Currency.isActive(attachment.getCurrencyId())
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

    public static final TransactionType CURRNECY_MINTING = new MonetarySystem() {

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
            int type = CurrencyType.getCurrencyType(attachment.getCurrencyId());
            CurrencyType.validate(attachment, type, transaction);
            if (!Currency.isActive(attachment.getCurrencyId()) ||
                    attachment.getUnits() <= 0 ||
                    attachment.getUnits() > Currency.getCurrency(attachment.getCurrencyId()).getTotalSupply() / Constants.MAX_MINTING_RATIO) {
                throw new NxtException.NotValidException("Invalid currency minting: " + attachment.getJSONObject());
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
            CurrencyMint.mintCurrency(senderAccount, attachment.getNonce(), attachment.getCurrencyId(), attachment.getUnits(), attachment.getCounter());
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
            Attachment.MonetarySystemShufflingInitiation attachment = (Attachment.MonetarySystemShufflingInitiation) transaction.getAttachment();
            int type = CurrencyType.getCurrencyType(attachment.getCurrencyId());
            CurrencyType.validate(attachment, type, transaction);
            if (!Currency.isActive(attachment.getCurrencyId())
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
            Attachment.MonetarySystemShufflingContinuation attachment = (Attachment.MonetarySystemShufflingContinuation) transaction.getAttachment();
            int type = CurrencyType.getCurrencyType(attachment.getCurrencyId());
            CurrencyType.validate(attachment, type, transaction);
            if (!CoinShuffler.isContinued(attachment.getShufflingId())
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
            Attachment.MonetarySystemShufflingFinalization attachment = (Attachment.MonetarySystemShufflingFinalization) transaction.getAttachment();
            int type = CurrencyType.getCurrencyType(attachment.getCurrencyId());
            CurrencyType.validate(attachment, type, transaction);
            if (!CoinShuffler.isFinalized(attachment.getShufflingId())
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
            Attachment.MonetarySystemShufflingCancellation attachment = (Attachment.MonetarySystemShufflingCancellation) transaction.getAttachment();
            int type = CurrencyType.getCurrencyType(attachment.getCurrencyId());
            CurrencyType.validate(attachment, type, transaction);
            if (!CoinShuffler.isFinalized(attachment.getShufflingId()) && !CoinShuffler.isCancelled(attachment.getShufflingId())
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

