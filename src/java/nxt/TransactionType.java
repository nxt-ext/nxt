package nxt;

import nxt.util.Convert;
import nxt.util.SuperComplexNumber;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public abstract class TransactionType {

    private static final byte TYPE_PAYMENT = 0;
    private static final byte TYPE_MESSAGING = 1;
    private static final byte TYPE_COLORED_COINS = 2;
    private static final byte TYPE_DIGITAL_GOODS = 3;
    private static final byte TYPE_ACCOUNT_CONTROL = 4;
    private static final byte TYPE_MONETARY_SYSTEM = 5;

    private static final byte SUBTYPE_PAYMENT_ORDINARY_PAYMENT = 0;

    private static final byte SUBTYPE_MESSAGING_ARBITRARY_MESSAGE = 0;
    private static final byte SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT = 1;
    private static final byte SUBTYPE_MESSAGING_POLL_CREATION = 2;
    private static final byte SUBTYPE_MESSAGING_VOTE_CASTING = 3;
    private static final byte SUBTYPE_MESSAGING_HUB_ANNOUNCEMENT = 4;
    private static final byte SUBTYPE_MESSAGING_ACCOUNT_INFO = 5;
    private static final byte SUBTYPE_MESSAGING_ALIAS_SELL = 6;
    private static final byte SUBTYPE_MESSAGING_ALIAS_BUY = 7;

    private static final byte SUBTYPE_COLORED_COINS_ASSET_ISSUANCE = 0;
    private static final byte SUBTYPE_COLORED_COINS_ASSET_TRANSFER = 1;
    private static final byte SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT = 2;
    private static final byte SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT = 3;
    private static final byte SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION = 4;
    private static final byte SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION = 5;

    private static final byte SUBTYPE_DIGITAL_GOODS_LISTING = 0;
    private static final byte SUBTYPE_DIGITAL_GOODS_DELISTING = 1;
    private static final byte SUBTYPE_DIGITAL_GOODS_PRICE_CHANGE = 2;
    private static final byte SUBTYPE_DIGITAL_GOODS_QUANTITY_CHANGE = 3;
    private static final byte SUBTYPE_DIGITAL_GOODS_PURCHASE = 4;
    private static final byte SUBTYPE_DIGITAL_GOODS_DELIVERY = 5;
    private static final byte SUBTYPE_DIGITAL_GOODS_FEEDBACK = 6;
    private static final byte SUBTYPE_DIGITAL_GOODS_REFUND = 7;

    private static final byte SUBTYPE_MONETARY_SYSTEM_CURRENCY_ISSUANCE = 0;
    private static final byte SUBTYPE_MONETARY_SYSTEM_RESERVE_INCREASE = 1;
    private static final byte SUBTYPE_MONETARY_SYSTEM_RESERVE_CLAIM = 2;
    private static final byte SUBTYPE_MONETARY_SYSTEM_MONEY_TRANSFER = 3;
    private static final byte SUBTYPE_MONETARY_SYSTEM_EXCHANGE_OFFER_PUBLICATION = 4;
    private static final byte SUBTYPE_MONETARY_SYSTEM_EXCHANGE = 5;
    private static final byte SUBTYPE_MONETARY_SYSTEM_MONEY_MINTING = 6;

    private static final byte SUBTYPE_ACCOUNT_CONTROL_EFFECTIVE_BALANCE_LEASING = 0;

    public static TransactionType findTransactionType(byte type, byte subtype) {
        switch (type) {
            case TYPE_PAYMENT:
                switch (subtype) {
                    case SUBTYPE_PAYMENT_ORDINARY_PAYMENT:
                        return Payment.ORDINARY;
                    default:
                        return null;
                }
            case TYPE_MESSAGING:
                switch (subtype) {
                    case SUBTYPE_MESSAGING_ARBITRARY_MESSAGE:
                        return Messaging.ARBITRARY_MESSAGE;
                    case SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT:
                        return Messaging.ALIAS_ASSIGNMENT;
                    case SUBTYPE_MESSAGING_POLL_CREATION:
                        return Messaging.POLL_CREATION;
                    case SUBTYPE_MESSAGING_VOTE_CASTING:
                        return Messaging.VOTE_CASTING;
                    case SUBTYPE_MESSAGING_HUB_ANNOUNCEMENT:
                        return Messaging.HUB_ANNOUNCEMENT;
                    case SUBTYPE_MESSAGING_ACCOUNT_INFO:
                        return Messaging.ACCOUNT_INFO;
                    case SUBTYPE_MESSAGING_ALIAS_SELL:
                        return Messaging.ALIAS_SELL;
                    case SUBTYPE_MESSAGING_ALIAS_BUY:
                        return Messaging.ALIAS_BUY;
                    default:
                        return null;
                }
            case TYPE_COLORED_COINS:
                switch (subtype) {
                    case SUBTYPE_COLORED_COINS_ASSET_ISSUANCE:
                        return ColoredCoins.ASSET_ISSUANCE;
                    case SUBTYPE_COLORED_COINS_ASSET_TRANSFER:
                        return ColoredCoins.ASSET_TRANSFER;
                    case SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT:
                        return ColoredCoins.ASK_ORDER_PLACEMENT;
                    case SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT:
                        return ColoredCoins.BID_ORDER_PLACEMENT;
                    case SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION:
                        return ColoredCoins.ASK_ORDER_CANCELLATION;
                    case SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION:
                        return ColoredCoins.BID_ORDER_CANCELLATION;
                    default:
                        return null;
                }
            case TYPE_DIGITAL_GOODS:
                switch (subtype) {
                    case SUBTYPE_DIGITAL_GOODS_LISTING:
                        return DigitalGoods.LISTING;
                    case SUBTYPE_DIGITAL_GOODS_DELISTING:
                        return DigitalGoods.DELISTING;
                    case SUBTYPE_DIGITAL_GOODS_PRICE_CHANGE:
                        return DigitalGoods.PRICE_CHANGE;
                    case SUBTYPE_DIGITAL_GOODS_QUANTITY_CHANGE:
                        return DigitalGoods.QUANTITY_CHANGE;
                    case SUBTYPE_DIGITAL_GOODS_PURCHASE:
                        return DigitalGoods.PURCHASE;
                    case SUBTYPE_DIGITAL_GOODS_DELIVERY:
                        return DigitalGoods.DELIVERY;
                    case SUBTYPE_DIGITAL_GOODS_FEEDBACK:
                        return DigitalGoods.FEEDBACK;
                    case SUBTYPE_DIGITAL_GOODS_REFUND:
                        return DigitalGoods.REFUND;
                    default:
                        return null;
                }
            case TYPE_ACCOUNT_CONTROL:
                switch (subtype) {
                    case SUBTYPE_ACCOUNT_CONTROL_EFFECTIVE_BALANCE_LEASING:
                        return AccountControl.EFFECTIVE_BALANCE_LEASING;
                    default:
                        return null;
                }
            case TYPE_MONETARY_SYSTEM:
                switch (subtype) {
                    case SUBTYPE_MONETARY_SYSTEM_CURRENCY_ISSUANCE:
                        return MonetarySystem.CURRENCY_ISSUANCE;
                    case SUBTYPE_MONETARY_SYSTEM_RESERVE_INCREASE:
                        return MonetarySystem.RESERVE_INCREASE;
                    case SUBTYPE_MONETARY_SYSTEM_RESERVE_CLAIM:
                        return MonetarySystem.RESERVE_CLAIM;
                    case SUBTYPE_MONETARY_SYSTEM_MONEY_TRANSFER:
                        return MonetarySystem.MONEY_TRANSFER;
                    case SUBTYPE_MONETARY_SYSTEM_EXCHANGE_OFFER_PUBLICATION:
                        return MonetarySystem.EXCHANGE_OFFER_PUBLICATION;
                    case SUBTYPE_MONETARY_SYSTEM_EXCHANGE:
                        return MonetarySystem.EXCHANGE;
                    case SUBTYPE_MONETARY_SYSTEM_MONEY_MINTING:
                        return MonetarySystem.MONEY_MINTING;
                }
            default:
                return null;
        }
    }

    private TransactionType() {
    }

    public abstract byte getType();

    public abstract byte getSubtype();

    abstract Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException;

    abstract Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException;

    abstract void validateAttachment(Transaction transaction) throws NxtException.ValidationException;

    // return false iff double spending
    final boolean applyUnconfirmed(Transaction transaction, Account senderAccount) {
        long totalAmountNQT = Convert.safeAdd(transaction.getAmountNQT(), transaction.getFeeNQT());
        if (transaction.getReferencedTransactionFullHash() != null
                && transaction.getTimestamp() > Constants.REFERENCED_TRANSACTION_FULL_HASH_BLOCK_TIMESTAMP) {
            totalAmountNQT = Convert.safeAdd(totalAmountNQT, Constants.UNCONFIRMED_POOL_DEPOSIT_NQT);
        }
        if (senderAccount.getUnconfirmedBalanceNQT() < totalAmountNQT
                && !(transaction.getTimestamp() == 0 && Arrays.equals(senderAccount.getPublicKey(), Genesis.CREATOR_PUBLIC_KEY))) {
            return false;
        }
        senderAccount.addToUnconfirmedBalanceNQT(-totalAmountNQT);
        if (!applyAttachmentUnconfirmed(transaction, senderAccount)) {
            senderAccount.addToUnconfirmedBalanceNQT(totalAmountNQT);
            return false;
        }
        return true;
    }

    abstract boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount);

    final void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
        senderAccount.addToBalanceNQT(- (Convert.safeAdd(transaction.getAmountNQT(), transaction.getFeeNQT())));
        if (transaction.getReferencedTransactionFullHash() != null
                && transaction.getTimestamp() > Constants.REFERENCED_TRANSACTION_FULL_HASH_BLOCK_TIMESTAMP) {
            senderAccount.addToUnconfirmedBalanceNQT(Constants.UNCONFIRMED_POOL_DEPOSIT_NQT);
        }
        if (recipientAccount != null) {
            recipientAccount.addToBalanceAndUnconfirmedBalanceNQT(transaction.getAmountNQT());
        }
        applyAttachment(transaction, senderAccount, recipientAccount);
    }

    abstract void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount);

    final void undoUnconfirmed(Transaction transaction, Account senderAccount) {
        senderAccount.addToUnconfirmedBalanceNQT(Convert.safeAdd(transaction.getAmountNQT(), transaction.getFeeNQT()));
        if (transaction.getReferencedTransactionFullHash() != null
                && transaction.getTimestamp() > Constants.REFERENCED_TRANSACTION_FULL_HASH_BLOCK_TIMESTAMP) {
            senderAccount.addToUnconfirmedBalanceNQT(Constants.UNCONFIRMED_POOL_DEPOSIT_NQT);
        }
        undoAttachmentUnconfirmed(transaction, senderAccount);
    }

    abstract void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount);

    final void undo(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
        senderAccount.addToBalanceNQT(Convert.safeAdd(transaction.getAmountNQT(), transaction.getFeeNQT()));
        if (transaction.getReferencedTransactionFullHash() != null
                && transaction.getTimestamp() > Constants.REFERENCED_TRANSACTION_FULL_HASH_BLOCK_TIMESTAMP) {
            senderAccount.addToUnconfirmedBalanceNQT(- Constants.UNCONFIRMED_POOL_DEPOSIT_NQT);
        }
        if (recipientAccount != null) {
            recipientAccount.addToBalanceAndUnconfirmedBalanceNQT(-transaction.getAmountNQT());
        }
        undoAttachment(transaction, senderAccount, recipientAccount);
    }

    abstract void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException;

    abstract void updateSpending(Transaction transaction, SuperComplexNumber spending);

    boolean isDuplicate(Transaction transaction, Map<TransactionType, Set<String>> duplicates) {
        return false;
    }

    static boolean isDuplicate(TransactionType uniqueType, String key, Map<TransactionType, Set<String>> duplicates) {
        Set<String> typeDuplicates = duplicates.get(uniqueType);
        if (typeDuplicates == null) {
            typeDuplicates = new HashSet<>();
            duplicates.put(uniqueType, typeDuplicates);
        }
        return ! typeDuplicates.add(key);
    }

    public abstract boolean hasRecipient();

    @Override
    public final String toString() {
        return "type: " + getType() + ", subtype: " + getSubtype();
    }

    /*
    Collection<TransactionType> getPhasingTransactionTypes() {
        return Collections.emptyList();
    }

    Collection<TransactionType> getPhasedTransactionTypes() {
        return Collections.emptyList();
    }
    */

    public static abstract class Payment extends TransactionType {

        private Payment() {
        }

        @Override
        public final byte getType() {
            return TransactionType.TYPE_PAYMENT;
        }

        @Override
        final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        final void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        }

        @Override
        final void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        }

        @Override
        final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        @Override
        final public boolean hasRecipient() {
            return true;
        }

        public static final TransactionType ORDINARY = new Payment() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_PAYMENT_ORDINARY_PAYMENT;
            }

            @Override
            Attachment.EmptyAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return Attachment.ORDINARY_PAYMENT;
            }

            @Override
            void updateSpending(Transaction transaction, SuperComplexNumber spending) {
            }

            @Override
            Attachment.EmptyAttachment parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return Attachment.ORDINARY_PAYMENT;
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                if (transaction.getAmountNQT() <= 0 || transaction.getAmountNQT() >= Constants.MAX_BALANCE_NQT) {
                    throw new NxtException.NotValidException("Invalid ordinary payment");
                }
            }

        };

    }

    public static abstract class Messaging extends TransactionType {

        private Messaging() {
        }

        @Override
        public final byte getType() {
            return TransactionType.TYPE_MESSAGING;
        }

        @Override
        final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        @Override
        void updateSpending(Transaction transaction, SuperComplexNumber spending) {
        }

        public final static TransactionType ARBITRARY_MESSAGE = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ARBITRARY_MESSAGE;
            }

            @Override
            Attachment.EmptyAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return Attachment.ARBITRARY_MESSAGE;
            }

            @Override
            Attachment.EmptyAttachment parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return Attachment.ARBITRARY_MESSAGE;
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment attachment = transaction.getAttachment();
                if (transaction.getAmountNQT() != 0) {
                    throw new NxtException.NotValidException("Invalid arbitrary message: " + attachment.getJSONObject());
                }
            }

            @Override
            public boolean hasRecipient() {
                return true;
            }

        };

        public static final TransactionType ALIAS_ASSIGNMENT = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT;
            }

            @Override
            Attachment.MessagingAliasAssignment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.MessagingAliasAssignment(buffer, transactionVersion);
            }

            @Override
            Attachment.MessagingAliasAssignment parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.MessagingAliasAssignment(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingAliasAssignment attachment = (Attachment.MessagingAliasAssignment) transaction.getAttachment();
                Alias.addOrUpdateAlias(senderAccount, transaction.getId(), attachment.getAliasName(),
                        attachment.getAliasURI(), transaction.getBlockTimestamp());
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                Attachment.MessagingAliasAssignment attachment = (Attachment.MessagingAliasAssignment) transaction.getAttachment();
                Alias alias = Alias.getAlias(attachment.getAliasName());
                if (alias.getId().equals(transaction.getId())) {
                    Alias.remove(alias);
                } else {
                    // alias has been updated, can't tell what was its previous uri
                    throw new UndoNotSupportedException("Reversal of alias assignment not supported");
                }
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Set<String>> duplicates) {
                Attachment.MessagingAliasAssignment attachment = (Attachment.MessagingAliasAssignment) transaction.getAttachment();
                return isDuplicate(Messaging.ALIAS_ASSIGNMENT, attachment.getAliasName().toLowerCase(), duplicates);
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.MessagingAliasAssignment attachment = (Attachment.MessagingAliasAssignment) transaction.getAttachment();
                if (attachment.getAliasName().length() == 0
                        || attachment.getAliasName().length() > Constants.MAX_ALIAS_LENGTH
                        || attachment.getAliasURI().length() > Constants.MAX_ALIAS_URI_LENGTH) {
                    throw new NxtException.NotValidException("Invalid alias assignment: " + attachment.getJSONObject());
                }
                String normalizedAlias = attachment.getAliasName().toLowerCase();
                for (int i = 0; i < normalizedAlias.length(); i++) {
                    if (Constants.ALPHABET.indexOf(normalizedAlias.charAt(i)) < 0) {
                        throw new NxtException.NotValidException("Invalid alias name: " + normalizedAlias);
                    }
                }
                Alias alias = Alias.getAlias(normalizedAlias);
                if (alias != null && ! alias.getAccountId().equals(transaction.getSenderId())) {
                    throw new NxtException.NotCurrentlyValidException("Alias already owned by another account: " + normalizedAlias);
                }
            }

            @Override
            public boolean hasRecipient() {
                return false;
            }

        };

        public static final TransactionType ALIAS_SELL = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ALIAS_SELL;
            }

            @Override
            Attachment.MessagingAliasSell parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.MessagingAliasSell(buffer, transactionVersion);
            }

            @Override
            Attachment.MessagingAliasSell parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.MessagingAliasSell(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                final Attachment.MessagingAliasSell attachment =
                        (Attachment.MessagingAliasSell) transaction.getAttachment();
                final String aliasName = attachment.getAliasName();
                final long priceNQT = attachment.getPriceNQT();
                if (priceNQT > 0) {
                    Alias.addSellOffer(aliasName, priceNQT, recipientAccount);
                } else {
                    Alias.changeOwner(recipientAccount, aliasName, transaction.getBlockTimestamp());
                }
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                throw new UndoNotSupportedException("Reversal of alias sell offer not supported");
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Set<String>> duplicates) {
                Attachment.MessagingAliasSell attachment = (Attachment.MessagingAliasSell) transaction.getAttachment();
                // not a bug, uniqueness is based on Messaging.ALIAS_ASSIGNMENT
                return isDuplicate(Messaging.ALIAS_ASSIGNMENT, attachment.getAliasName().toLowerCase(), duplicates);
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                if (transaction.getAmountNQT() != 0) {
                    throw new NxtException.NotValidException("Invalid sell alias transaction: " +
                            transaction.getJSONObject());
                }
                final Attachment.MessagingAliasSell attachment =
                        (Attachment.MessagingAliasSell) transaction.getAttachment();
                final String aliasName = attachment.getAliasName();
                if (aliasName == null || aliasName.length() == 0) {
                    throw new NxtException.NotValidException("Missing alias name");
                }
                long priceNQT = attachment.getPriceNQT();
                if (priceNQT < 0 || priceNQT > Constants.MAX_BALANCE_NQT) {
                    throw new NxtException.NotValidException("Invalid alias sell price: " + priceNQT);
                }
                if (priceNQT == 0) {
                    if (Genesis.CREATOR_ID.equals(transaction.getRecipientId())) {
                        throw new NxtException.NotValidException("Transferring aliases to Genesis account not allowed");
                    } else if (transaction.getRecipientId() == null) {
                        throw new NxtException.NotValidException("Missing alias transfer recipient");
                    }
                }
                final Alias alias = Alias.getAlias(aliasName);
                if (alias == null) {
                    throw new NxtException.NotCurrentlyValidException("Alias hasn't been registered yet: " + aliasName);
                } else if (! alias.getAccountId().equals(transaction.getSenderId())) {
                    throw new NxtException.NotCurrentlyValidException("Alias doesn't belong to sender: " + aliasName);
                }
            }

            @Override
            public boolean hasRecipient() {
                return true;
            }

        };

        public static final TransactionType ALIAS_BUY = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ALIAS_BUY;
            }

            @Override
            Attachment.MessagingAliasBuy parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.MessagingAliasBuy(buffer, transactionVersion);
            }

            @Override
            Attachment.MessagingAliasBuy parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.MessagingAliasBuy(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                final Attachment.MessagingAliasBuy attachment =
                        (Attachment.MessagingAliasBuy) transaction.getAttachment();
                final String aliasName = attachment.getAliasName();
                Alias.changeOwner(senderAccount, aliasName, transaction.getBlockTimestamp());
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                throw new UndoNotSupportedException("Reversal of alias buy not supported");
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Set<String>> duplicates) {
                Attachment.MessagingAliasBuy attachment = (Attachment.MessagingAliasBuy) transaction.getAttachment();
                // not a bug, uniqueness is based on Messaging.ALIAS_ASSIGNMENT
                return isDuplicate(Messaging.ALIAS_ASSIGNMENT, attachment.getAliasName().toLowerCase(), duplicates);
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                final Attachment.MessagingAliasBuy attachment =
                        (Attachment.MessagingAliasBuy) transaction.getAttachment();
                final String aliasName = attachment.getAliasName();
                final Alias alias = Alias.getAlias(aliasName);
                if (alias == null) {
                    throw new NxtException.NotCurrentlyValidException("Alias hasn't been registered yet: " + aliasName);
                } else if (! alias.getAccountId().equals(transaction.getRecipientId())) {
                    throw new NxtException.NotCurrentlyValidException("Alias is owned by account other than recipient: "
                            + Convert.toUnsignedLong(alias.getAccountId()));
                }
                Alias.Offer offer = Alias.getOffer(aliasName);
                if (offer == null) {
                    throw new NxtException.NotCurrentlyValidException("Alias is not for sale: " + aliasName);
                }
                if (transaction.getAmountNQT() < offer.getPriceNQT()) {
                    String msg = "Price is too low for: " + aliasName + " ("
                            + transaction.getAmountNQT() + " < " + offer.getPriceNQT() + ")";
                    throw new NxtException.NotCurrentlyValidException(msg);
                }
                if (offer.getBuyerId() != null && ! offer.getBuyerId().equals(transaction.getSenderId())) {
                    throw new NxtException.NotCurrentlyValidException("Wrong buyer for " + aliasName + ": "
                            + Convert.toUnsignedLong(transaction.getSenderId()) + " expected: "
                            + Convert.toUnsignedLong(offer.getBuyerId()));
                }
            }

            @Override
            public boolean hasRecipient() {
                return true;
            }

        };

        public final static TransactionType POLL_CREATION = new Messaging() {
            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_POLL_CREATION;
            }

            @Override
            Attachment.MessagingPollCreation parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.MessagingPollCreation(buffer, transactionVersion);
            }

            @Override
            Attachment.MessagingPollCreation parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.MessagingPollCreation(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingPollCreation attachment = (Attachment.MessagingPollCreation) transaction.getAttachment();
                Poll.addPoll(transaction.getId(), attachment.getPollName(), attachment.getPollDescription(), attachment.getPollOptions(),
                        attachment.getMinNumberOfOptions(), attachment.getMaxNumberOfOptions(), attachment.isOptionsAreBinary());
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                throw new UndoNotSupportedException("Reversal of poll creation not supported");
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                if (Nxt.getBlockchain().getLastBlock().getHeight() < Constants.VOTING_SYSTEM_BLOCK) {
                    throw new NxtException.NotYetEnabledException("Voting System not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
                }
                Attachment.MessagingPollCreation attachment = (Attachment.MessagingPollCreation) transaction.getAttachment();
                for (int i = 0; i < attachment.getPollOptions().length; i++) {
                    if (attachment.getPollOptions()[i].length() > Constants.MAX_POLL_OPTION_LENGTH) {
                        throw new NxtException.NotValidException("Invalid poll options length: " + attachment.getJSONObject());
                    }
                }
                if (attachment.getPollName().length() > Constants.MAX_POLL_NAME_LENGTH
                        || attachment.getPollDescription().length() > Constants.MAX_POLL_DESCRIPTION_LENGTH
                        || attachment.getPollOptions().length > Constants.MAX_POLL_OPTION_COUNT) {
                    throw new NxtException.NotValidException("Invalid poll attachment: " + attachment.getJSONObject());
                }
            }

            @Override
            public boolean hasRecipient() {
                return false;
            }

        };

        public final static TransactionType VOTE_CASTING = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_VOTE_CASTING;
            }

            @Override
            Attachment.MessagingVoteCasting parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.MessagingVoteCasting(buffer, transactionVersion);
            }

            @Override
            Attachment.MessagingVoteCasting parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.MessagingVoteCasting(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingVoteCasting attachment = (Attachment.MessagingVoteCasting) transaction.getAttachment();
                Poll poll = Poll.getPoll(attachment.getPollId());
                if (poll != null) {
                    Vote vote = Vote.addVote(transaction.getId(), attachment.getPollId(), transaction.getSenderId(),
                            attachment.getPollVote());
                    poll.addVoter(transaction.getSenderId(), vote.getId());
                }
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                throw new UndoNotSupportedException("Reversal of vote casting not supported");
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                if (Nxt.getBlockchain().getLastBlock().getHeight() < Constants.VOTING_SYSTEM_BLOCK) {
                    throw new NxtException.NotYetEnabledException("Voting System not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
                }
                Attachment.MessagingVoteCasting attachment = (Attachment.MessagingVoteCasting) transaction.getAttachment();
                if (attachment.getPollId() == null || attachment.getPollVote() == null
                        || attachment.getPollVote().length > Constants.MAX_POLL_OPTION_COUNT) {
                    throw new NxtException.NotValidException("Invalid vote casting attachment: " + attachment.getJSONObject());
                }
                if (Poll.getPoll(attachment.getPollId()) == null) {
                    throw new NxtException.NotCurrentlyValidException("Invalid poll: " + Convert.toUnsignedLong(attachment.getPollId()));
                }
            }

            @Override
            public boolean hasRecipient() {
                return false;
            }

        };

        public static final TransactionType HUB_ANNOUNCEMENT = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_HUB_ANNOUNCEMENT;
            }

            @Override
            Attachment.MessagingHubAnnouncement parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.MessagingHubAnnouncement(buffer, transactionVersion);
            }

            @Override
            Attachment.MessagingHubAnnouncement parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.MessagingHubAnnouncement(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingHubAnnouncement attachment = (Attachment.MessagingHubAnnouncement) transaction.getAttachment();
                Hub.addOrUpdateHub(senderAccount.getId(), attachment.getMinFeePerByteNQT(), attachment.getUris());
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                Hub.removeHub(senderAccount.getId());
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                if (Nxt.getBlockchain().getLastBlock().getHeight() < Constants.TRANSPARENT_FORGING_BLOCK_7) {
                    throw new NxtException.NotYetEnabledException("Hub terminal announcement not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
                }
                Attachment.MessagingHubAnnouncement attachment = (Attachment.MessagingHubAnnouncement) transaction.getAttachment();
                if (attachment.getMinFeePerByteNQT() < 0 || attachment.getMinFeePerByteNQT() > Constants.MAX_BALANCE_NQT
                        || attachment.getUris().length > Constants.MAX_HUB_ANNOUNCEMENT_URIS) {
                    // cfb: "0" is allowed to show that another way to determine the min fee should be used
                    throw new NxtException.NotValidException("Invalid hub terminal announcement: " + attachment.getJSONObject());
                }
                for (String uri : attachment.getUris()) {
                    if (uri.length() > Constants.MAX_HUB_ANNOUNCEMENT_URI_LENGTH) {
                        throw new NxtException.NotValidException("Invalid URI length: " + uri.length());
                    }
                    //TODO: also check URI validity here?
                }
            }

            @Override
            public boolean hasRecipient() {
                return false;
            }

        };

        public static final Messaging ACCOUNT_INFO = new Messaging() {

            @Override
            public byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ACCOUNT_INFO;
            }

            @Override
            Attachment.MessagingAccountInfo parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.MessagingAccountInfo(buffer, transactionVersion);
            }

            @Override
            Attachment.MessagingAccountInfo parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.MessagingAccountInfo(attachmentData);
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.MessagingAccountInfo attachment = (Attachment.MessagingAccountInfo)transaction.getAttachment();
                if (attachment.getName().length() > Constants.MAX_ACCOUNT_NAME_LENGTH
                        || attachment.getDescription().length() > Constants.MAX_ACCOUNT_DESCRIPTION_LENGTH
                        ) {
                    throw new NxtException.NotValidException("Invalid account info issuance: " + attachment.getJSONObject());
                }
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingAccountInfo attachment = (Attachment.MessagingAccountInfo) transaction.getAttachment();
                senderAccount.setAccountInfo(attachment.getName(), attachment.getDescription());
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                throw new UndoNotSupportedException("Undoing account info not supported");
            }

            @Override
            public boolean hasRecipient() {
                return false;
            }

        };

    }

    public static abstract class ColoredCoins extends TransactionType {

        private ColoredCoins() {}

        @Override
        public final byte getType() {
            return TransactionType.TYPE_COLORED_COINS;
        }

        public static final TransactionType ASSET_ISSUANCE = new ColoredCoins() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COLORED_COINS_ASSET_ISSUANCE;
            }

            @Override
            Attachment.ColoredCoinsAssetIssuance parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.ColoredCoinsAssetIssuance(buffer, transactionVersion);
            }

            @Override
            Attachment.ColoredCoinsAssetIssuance parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.ColoredCoinsAssetIssuance(attachmentData);
            }

            @Override
            boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                return true;
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsAssetIssuance attachment = (Attachment.ColoredCoinsAssetIssuance) transaction.getAttachment();
                Long assetId = transaction.getId();
                Asset.addAsset(assetId, transaction.getSenderId(), attachment.getName(), attachment.getDescription(),
                        attachment.getQuantityQNT(), attachment.getDecimals());
                senderAccount.addToAssetAndUnconfirmedAssetBalanceQNT(assetId, attachment.getQuantityQNT());
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsAssetIssuance attachment = (Attachment.ColoredCoinsAssetIssuance) transaction.getAttachment();
                Long assetId = transaction.getId();
                senderAccount.addToAssetAndUnconfirmedAssetBalanceQNT(assetId, -attachment.getQuantityQNT());
                Asset.removeAsset(assetId);
            }

            @Override
            void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            }

            @Override
            void updateSpending(Transaction transaction, SuperComplexNumber spending) {
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.ColoredCoinsAssetIssuance attachment = (Attachment.ColoredCoinsAssetIssuance)transaction.getAttachment();
                if (transaction.getFeeNQT() < Constants.ASSET_ISSUANCE_FEE_NQT
                        || attachment.getName().length() < Constants.MIN_ASSET_NAME_LENGTH
                        || attachment.getName().length() > Constants.MAX_ASSET_NAME_LENGTH
                        || attachment.getDescription().length() > Constants.MAX_ASSET_DESCRIPTION_LENGTH
                        || attachment.getDecimals() < 0 || attachment.getDecimals() > 8
                        || attachment.getQuantityQNT() <= 0
                        || attachment.getQuantityQNT() > Constants.MAX_ASSET_QUANTITY_QNT
                        ) {
                    throw new NxtException.NotValidException("Invalid asset issuance: " + attachment.getJSONObject());
                }
                String normalizedName = attachment.getName().toLowerCase();
                for (int i = 0; i < normalizedName.length(); i++) {
                    if (Constants.ALPHABET.indexOf(normalizedName.charAt(i)) < 0) {
                        throw new NxtException.NotValidException("Invalid asset name: " + normalizedName);
                    }
                }
            }

            @Override
            public boolean hasRecipient() {
                return false;
            }

        };

        public static final TransactionType ASSET_TRANSFER = new ColoredCoins() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COLORED_COINS_ASSET_TRANSFER;
            }

            @Override
            Attachment.ColoredCoinsAssetTransfer parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.ColoredCoinsAssetTransfer(buffer, transactionVersion);
            }

            @Override
            Attachment.ColoredCoinsAssetTransfer parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.ColoredCoinsAssetTransfer(attachmentData);
            }

            @Override
            boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer) transaction.getAttachment();
                if (senderAccount.getUnconfirmedAssetBalanceQNT(attachment.getAssetId()) >= attachment.getQuantityQNT()) {
                    senderAccount.addToUnconfirmedAssetBalanceQNT(attachment.getAssetId(), -attachment.getQuantityQNT());
                    return true;
                }
                return false;
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer) transaction.getAttachment();
                senderAccount.addToAssetBalanceQNT(attachment.getAssetId(), -attachment.getQuantityQNT());
                recipientAccount.addToAssetAndUnconfirmedAssetBalanceQNT(attachment.getAssetId(), attachment.getQuantityQNT());
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer) transaction.getAttachment();
                senderAccount.addToAssetBalanceQNT(attachment.getAssetId(), attachment.getQuantityQNT());
                recipientAccount.addToAssetAndUnconfirmedAssetBalanceQNT(attachment.getAssetId(), -attachment.getQuantityQNT());
            }

            @Override
            void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer) transaction.getAttachment();
                senderAccount.addToUnconfirmedAssetBalanceQNT(attachment.getAssetId(), attachment.getQuantityQNT());
            }

            @Override
            void updateSpending(Transaction transaction, SuperComplexNumber spending) {
                Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer)transaction.getAttachment();
                spending.add(attachment.getAssetId(), attachment.getQuantityQNT());
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer)transaction.getAttachment();
                if (transaction.getAmountNQT() != 0
                        || attachment.getComment() != null && attachment.getComment().length() > Constants.MAX_ASSET_TRANSFER_COMMENT_LENGTH
                        || attachment.getAssetId() == null) {
                    throw new NxtException.NotValidException("Invalid asset transfer amount or comment: " + attachment.getJSONObject());
                }
                if (transaction.getVersion() > 0 && attachment.getComment() != null) {
                    throw new NxtException.NotValidException("Asset transfer comments no longer allowed, use message " +
                            "or encrypted message appendix instead");
                }
                Asset asset = Asset.getAsset(attachment.getAssetId());
                if (attachment.getQuantityQNT() <= 0 || (asset != null && attachment.getQuantityQNT() > asset.getQuantityQNT())) {
                    throw new NxtException.NotValidException("Invalid asset transfer asset or quantity: " + attachment.getJSONObject());
                }
                if (asset == null) {
                    throw new NxtException.NotCurrentlyValidException("Asset " + Convert.toUnsignedLong(attachment.getAssetId()) +
                            " does not exist yet");
                }
            }

            @Override
            public boolean hasRecipient() {
                return true;
            }

        };

        abstract static class ColoredCoinsOrderPlacement extends ColoredCoins {

            @Override
            final void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.ColoredCoinsOrderPlacement attachment = (Attachment.ColoredCoinsOrderPlacement)transaction.getAttachment();
                if (attachment.getPriceNQT() <= 0 || attachment.getPriceNQT() > Constants.MAX_BALANCE_NQT
                        || attachment.getAssetId() == null) {
                    throw new NxtException.NotValidException("Invalid asset order placement: " + attachment.getJSONObject());
                }
                Asset asset = Asset.getAsset(attachment.getAssetId());
                if (attachment.getQuantityQNT() <= 0 || (asset != null && attachment.getQuantityQNT() > asset.getQuantityQNT())) {
                    throw new NxtException.NotValidException("Invalid asset order placement asset or quantity: " + attachment.getJSONObject());
                }
                if (asset == null) {
                    throw new NxtException.NotCurrentlyValidException("Asset " + Convert.toUnsignedLong(attachment.getAssetId()) +
                            " does not exist yet");
                }
            }

            @Override
            final public boolean hasRecipient() {
                return false;
            }

        }

        public static final TransactionType ASK_ORDER_PLACEMENT = new ColoredCoins.ColoredCoinsOrderPlacement() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT;
            }

            @Override
            Attachment.ColoredCoinsAskOrderPlacement parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.ColoredCoinsAskOrderPlacement(buffer, transactionVersion);
            }

            @Override
            Attachment.ColoredCoinsAskOrderPlacement parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.ColoredCoinsAskOrderPlacement(attachmentData);
            }

            @Override
            boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.ColoredCoinsAskOrderPlacement attachment = (Attachment.ColoredCoinsAskOrderPlacement) transaction.getAttachment();
                if (senderAccount.getUnconfirmedAssetBalanceQNT(attachment.getAssetId()) >= attachment.getQuantityQNT()) {
                    senderAccount.addToUnconfirmedAssetBalanceQNT(attachment.getAssetId(), -attachment.getQuantityQNT());
                    return true;
                }
                return false;
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsAskOrderPlacement attachment = (Attachment.ColoredCoinsAskOrderPlacement) transaction.getAttachment();
                if (Asset.getAsset(attachment.getAssetId()) != null) {
                    Order.Ask.addOrder(transaction.getId(), senderAccount, attachment.getAssetId(),
                            attachment.getQuantityQNT(), attachment.getPriceNQT());
                }
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                Attachment.ColoredCoinsAskOrderPlacement attachment = (Attachment.ColoredCoinsAskOrderPlacement) transaction.getAttachment();
                Order.Ask askOrder = Order.Ask.removeOrder(transaction.getId());
                if (askOrder == null || askOrder.getQuantityQNT() != attachment.getQuantityQNT()
                        || !askOrder.getAssetId().equals(attachment.getAssetId())) {
                    //undoing of partially filled orders not supported yet
                    throw new UndoNotSupportedException("Ask order already filled");
                }
            }

            @Override
            void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.ColoredCoinsAskOrderPlacement attachment = (Attachment.ColoredCoinsAskOrderPlacement) transaction.getAttachment();
                senderAccount.addToUnconfirmedAssetBalanceQNT(attachment.getAssetId(), attachment.getQuantityQNT());
            }

            @Override
            void updateSpending(Transaction transaction, SuperComplexNumber spending) {
                Attachment.ColoredCoinsAskOrderPlacement attachment = (Attachment.ColoredCoinsAskOrderPlacement)transaction.getAttachment();
                spending.add(attachment.getAssetId(), attachment.getQuantityQNT());
            }

        };

        public final static TransactionType BID_ORDER_PLACEMENT = new ColoredCoins.ColoredCoinsOrderPlacement() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT;
            }

            @Override
            Attachment.ColoredCoinsBidOrderPlacement parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.ColoredCoinsBidOrderPlacement(buffer, transactionVersion);
            }

            @Override
            Attachment.ColoredCoinsBidOrderPlacement parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.ColoredCoinsBidOrderPlacement(attachmentData);
            }

            @Override
            boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.ColoredCoinsBidOrderPlacement attachment = (Attachment.ColoredCoinsBidOrderPlacement) transaction.getAttachment();
                if (senderAccount.getUnconfirmedBalanceNQT() >= Convert.safeMultiply(attachment.getQuantityQNT(), attachment.getPriceNQT())) {
                    senderAccount.addToUnconfirmedBalanceNQT(-Convert.safeMultiply(attachment.getQuantityQNT(), attachment.getPriceNQT()));
                    return true;
                }
                return false;
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsBidOrderPlacement attachment = (Attachment.ColoredCoinsBidOrderPlacement) transaction.getAttachment();
                if (Asset.getAsset(attachment.getAssetId()) != null) {
                    Order.Bid.addOrder(transaction.getId(), senderAccount, attachment.getAssetId(),
                            attachment.getQuantityQNT(), attachment.getPriceNQT());
                }
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                Attachment.ColoredCoinsBidOrderPlacement attachment = (Attachment.ColoredCoinsBidOrderPlacement) transaction.getAttachment();
                Order.Bid bidOrder = Order.Bid.removeOrder(transaction.getId());
                if (bidOrder == null || bidOrder.getQuantityQNT() != attachment.getQuantityQNT()
                        || !bidOrder.getAssetId().equals(attachment.getAssetId())) {
                    //undoing of partially filled orders not supported yet
                    throw new UndoNotSupportedException("Bid order already filled");
                }
            }

            @Override
            void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.ColoredCoinsBidOrderPlacement attachment = (Attachment.ColoredCoinsBidOrderPlacement) transaction.getAttachment();
                senderAccount.addToUnconfirmedBalanceNQT(Convert.safeMultiply(attachment.getQuantityQNT(), attachment.getPriceNQT()));
            }

            @Override
            void updateSpending(Transaction transaction, SuperComplexNumber spending) {
                Attachment.ColoredCoinsBidOrderPlacement attachment = (Attachment.ColoredCoinsBidOrderPlacement)transaction.getAttachment();
                spending.add(Constants.NXT_CURRENCY_ID, Convert.safeMultiply(attachment.getQuantityQNT(), attachment.getPriceNQT()));
            }

        };

        abstract static class ColoredCoinsOrderCancellation extends ColoredCoins {

            @Override
            final void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.ColoredCoinsOrderCancellation attachment = (Attachment.ColoredCoinsOrderCancellation) transaction.getAttachment();
                if (attachment.getOrderId() == null) {
                    throw new NxtException.NotValidException("Invalid order cancellation attachment: " + attachment.getJSONObject());
                }
                doValidateAttachment(transaction);
            }

            abstract void doValidateAttachment(Transaction transaction) throws NxtException.ValidationException;

            @Override
            final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                return true;
            }

            @Override
            void updateSpending(Transaction transaction, SuperComplexNumber spending) {
            }

            @Override
            final void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                throw new UndoNotSupportedException("Reversal of order cancellation not supported");
            }

            @Override
            final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            }

            @Override
            public boolean hasRecipient() {
                return false;
            }

        }

        public static final TransactionType ASK_ORDER_CANCELLATION = new ColoredCoins.ColoredCoinsOrderCancellation() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION;
            }

            @Override
            Attachment.ColoredCoinsAskOrderCancellation parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.ColoredCoinsAskOrderCancellation(buffer, transactionVersion);
            }

            @Override
            Attachment.ColoredCoinsAskOrderCancellation parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.ColoredCoinsAskOrderCancellation(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsAskOrderCancellation attachment = (Attachment.ColoredCoinsAskOrderCancellation) transaction.getAttachment();
                Order order = Order.Ask.removeOrder(attachment.getOrderId());
                if (order != null) {
                    senderAccount.addToUnconfirmedAssetBalanceQNT(order.getAssetId(), order.getQuantityQNT());
                }
            }

            @Override
            void doValidateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.ColoredCoinsAskOrderCancellation attachment = (Attachment.ColoredCoinsAskOrderCancellation) transaction.getAttachment();
                if (Order.Ask.getAskOrder(attachment.getOrderId()) == null) {
                    throw new NxtException.NotCurrentlyValidException("Invalid ask order: " + Convert.toUnsignedLong(attachment.getOrderId()));
                }
            }

        };

        public static final TransactionType BID_ORDER_CANCELLATION = new ColoredCoins.ColoredCoinsOrderCancellation() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION;
            }

            @Override
            Attachment.ColoredCoinsBidOrderCancellation parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.ColoredCoinsBidOrderCancellation(buffer, transactionVersion);
            }

            @Override
            Attachment.ColoredCoinsBidOrderCancellation parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.ColoredCoinsBidOrderCancellation(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsBidOrderCancellation attachment = (Attachment.ColoredCoinsBidOrderCancellation) transaction.getAttachment();
                Order order = Order.Bid.removeOrder(attachment.getOrderId());
                if (order != null) {
                    senderAccount.addToUnconfirmedBalanceNQT(Convert.safeMultiply(order.getQuantityQNT(), order.getPriceNQT()));
                }
            }

            @Override
            void doValidateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.ColoredCoinsBidOrderCancellation attachment = (Attachment.ColoredCoinsBidOrderCancellation) transaction.getAttachment();
                if (Order.Bid.getBidOrder(attachment.getOrderId()) == null) {
                    throw new NxtException.NotCurrentlyValidException("Invalid bid order: " + Convert.toUnsignedLong(attachment.getOrderId()));
                }
            }

        };
    }

    public static abstract class DigitalGoods extends TransactionType {

        private DigitalGoods() {
        }

        @Override
        public final byte getType() {
            return TransactionType.TYPE_DIGITAL_GOODS;
        }

        @Override
        boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        @Override
        void updateSpending(Transaction transaction, SuperComplexNumber spending) {
        }

        @Override
        final void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            if (transaction.getAmountNQT() != 0) {
                throw new NxtException.NotValidException("Invalid digital goods transaction");
            }
            doValidateAttachment(transaction);
        }

        abstract void doValidateAttachment(Transaction transaction) throws NxtException.ValidationException;


        public static final TransactionType LISTING = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_LISTING;
            }

            @Override
            Attachment.DigitalGoodsListing parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsListing(buffer, transactionVersion);
            }

            @Override
            Attachment.DigitalGoodsListing parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsListing(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsListing attachment = (Attachment.DigitalGoodsListing) transaction.getAttachment();
                DigitalGoodsStore.listGoods(transaction.getId(), transaction.getSenderId(), attachment.getName(), attachment.getDescription(),
                        attachment.getTags(), attachment.getQuantity(), attachment.getPriceNQT());
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                DigitalGoodsStore.undoListGoods(transaction.getId());
            }

            @Override
            void doValidateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.DigitalGoodsListing attachment = (Attachment.DigitalGoodsListing) transaction.getAttachment();
                if (attachment.getName().length() == 0
                        || attachment.getName().length() > Constants.MAX_DGS_LISTING_NAME_LENGTH
                        || attachment.getDescription().length() > Constants.MAX_DGS_LISTING_DESCRIPTION_LENGTH
                        || attachment.getTags().length() > Constants.MAX_DGS_LISTING_TAGS_LENGTH
                        || attachment.getQuantity() < 0 || attachment.getQuantity() > Constants.MAX_DGS_LISTING_QUANTITY
                        || attachment.getPriceNQT() <= 0 || attachment.getPriceNQT() > Constants.MAX_BALANCE_NQT) {
                    throw new NxtException.NotValidException("Invalid digital goods listing: " + attachment.getJSONObject());
                }
            }

            @Override
            public boolean hasRecipient() {
                return false;
            }

        };

        public static final TransactionType DELISTING = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_DELISTING;
            }

            @Override
            Attachment.DigitalGoodsDelisting parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsDelisting(buffer, transactionVersion);
            }

            @Override
            Attachment.DigitalGoodsDelisting parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsDelisting(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsDelisting attachment = (Attachment.DigitalGoodsDelisting) transaction.getAttachment();
                DigitalGoodsStore.delistGoods(attachment.getGoodsId());
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                DigitalGoodsStore.undoDelistGoods(transaction.getId());
            }

            @Override
            void doValidateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.DigitalGoodsDelisting attachment = (Attachment.DigitalGoodsDelisting) transaction.getAttachment();
                DigitalGoodsStore.Goods goods = DigitalGoodsStore.getGoods(attachment.getGoodsId());
                if (goods != null && ! transaction.getSenderId().equals(goods.getSellerId())) {
                    throw new NxtException.NotValidException("Invalid digital goods delisting - seller is different: " + attachment.getJSONObject());
                }
                if (goods == null || goods.isDelisted()) {
                    throw new NxtException.NotCurrentlyValidException("Goods " + Convert.toUnsignedLong(attachment.getGoodsId()) +
                            "not yet listed or already delisted");
                }
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Set<String>> duplicates) {
                Attachment.DigitalGoodsDelisting attachment = (Attachment.DigitalGoodsDelisting) transaction.getAttachment();
                return isDuplicate(DigitalGoods.DELISTING, Convert.toUnsignedLong(attachment.getGoodsId()), duplicates);
            }

            @Override
            public boolean hasRecipient() {
                return false;
            }

        };

        public static final TransactionType PRICE_CHANGE = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_PRICE_CHANGE;
            }

            @Override
            Attachment.DigitalGoodsPriceChange parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsPriceChange(buffer, transactionVersion);
            }

            @Override
            Attachment.DigitalGoodsPriceChange parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsPriceChange(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsPriceChange attachment = (Attachment.DigitalGoodsPriceChange) transaction.getAttachment();
                DigitalGoodsStore.changePrice(attachment.getGoodsId(), attachment.getPriceNQT());
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                throw new UndoNotSupportedException("Reversal of digital goods price change not supported");
            }

            @Override
            void doValidateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.DigitalGoodsPriceChange attachment = (Attachment.DigitalGoodsPriceChange) transaction.getAttachment();
                DigitalGoodsStore.Goods goods = DigitalGoodsStore.getGoods(attachment.getGoodsId());
                if (attachment.getPriceNQT() <= 0 || attachment.getPriceNQT() > Constants.MAX_BALANCE_NQT
                        || (goods != null && ! transaction.getSenderId().equals(goods.getSellerId()))) {
                    throw new NxtException.NotValidException("Invalid digital goods price change: " + attachment.getJSONObject());
                }
                if (goods == null || goods.isDelisted()) {
                    throw new NxtException.NotCurrentlyValidException("Goods " + Convert.toUnsignedLong(attachment.getGoodsId()) +
                            "not yet listed or already delisted");
                }
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Set<String>> duplicates) {
                Attachment.DigitalGoodsPriceChange attachment = (Attachment.DigitalGoodsPriceChange) transaction.getAttachment();
                // not a bug, uniqueness is based on DigitalGoods.DELISTING
                return isDuplicate(DigitalGoods.DELISTING, Convert.toUnsignedLong(attachment.getGoodsId()), duplicates);
            }

            @Override
            public boolean hasRecipient() {
                return false;
            }

        };

        public static final TransactionType QUANTITY_CHANGE = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_QUANTITY_CHANGE;
            }

            @Override
            Attachment.DigitalGoodsQuantityChange parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsQuantityChange(buffer, transactionVersion);
            }

            @Override
            Attachment.DigitalGoodsQuantityChange parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsQuantityChange(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsQuantityChange attachment = (Attachment.DigitalGoodsQuantityChange) transaction.getAttachment();
                DigitalGoodsStore.changeQuantity(attachment.getGoodsId(), attachment.getDeltaQuantity());
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                throw new UndoNotSupportedException("Reversal of digital goods quantity change not supported");
            }

            @Override
            void doValidateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.DigitalGoodsQuantityChange attachment = (Attachment.DigitalGoodsQuantityChange) transaction.getAttachment();
                DigitalGoodsStore.Goods goods = DigitalGoodsStore.getGoods(attachment.getGoodsId());
                if (attachment.getDeltaQuantity() < -Constants.MAX_DGS_LISTING_QUANTITY
                        || attachment.getDeltaQuantity() > Constants.MAX_DGS_LISTING_QUANTITY
                        || (goods != null && ! transaction.getSenderId().equals(goods.getSellerId()))) {
                    throw new NxtException.NotValidException("Invalid digital goods quantity change: " + attachment.getJSONObject());
                }
                if (goods == null || goods.isDelisted()) {
                    throw new NxtException.NotCurrentlyValidException("Goods " + Convert.toUnsignedLong(attachment.getGoodsId()) +
                            "not yet listed or already delisted");
                }
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Set<String>> duplicates) {
                Attachment.DigitalGoodsQuantityChange attachment = (Attachment.DigitalGoodsQuantityChange) transaction.getAttachment();
                // not a bug, uniqueness is based on DigitalGoods.DELISTING
                return isDuplicate(DigitalGoods.DELISTING, Convert.toUnsignedLong(attachment.getGoodsId()), duplicates);
            }

            @Override
            public boolean hasRecipient() {
                return false;
            }

        };

        public static final TransactionType PURCHASE = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_PURCHASE;
            }

            @Override
            Attachment.DigitalGoodsPurchase parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsPurchase(buffer, transactionVersion);
            }

            @Override
            Attachment.DigitalGoodsPurchase parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsPurchase(attachmentData);
            }

            @Override
            boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.DigitalGoodsPurchase attachment = (Attachment.DigitalGoodsPurchase) transaction.getAttachment();
                if (senderAccount.getUnconfirmedBalanceNQT() >= Convert.safeMultiply(attachment.getQuantity(), attachment.getPriceNQT())) {
                    senderAccount.addToUnconfirmedBalanceNQT(-Convert.safeMultiply(attachment.getQuantity(), attachment.getPriceNQT()));
                    return true;
                }
                return false;
            }

            @Override
            void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.DigitalGoodsPurchase attachment = (Attachment.DigitalGoodsPurchase) transaction.getAttachment();
                senderAccount.addToUnconfirmedBalanceNQT(Convert.safeMultiply(attachment.getQuantity(), attachment.getPriceNQT()));
            }

            @Override
            void updateSpending(Transaction transaction, SuperComplexNumber spending) {
                Attachment.DigitalGoodsPurchase attachment = (Attachment.DigitalGoodsPurchase) transaction.getAttachment();
                spending.add(Constants.NXT_CURRENCY_ID, Convert.safeMultiply(attachment.getQuantity(), attachment.getPriceNQT()));
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsPurchase attachment = (Attachment.DigitalGoodsPurchase) transaction.getAttachment();
                DigitalGoodsStore.purchase(transaction.getId(), transaction.getSenderId(), attachment.getGoodsId(),
                        attachment.getQuantity(), attachment.getPriceNQT(), attachment.getDeliveryDeadlineTimestamp(),
                        transaction.getEncryptedMessage(), transaction.getTimestamp());
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                Attachment.DigitalGoodsPurchase attachment = (Attachment.DigitalGoodsPurchase) transaction.getAttachment();
                DigitalGoodsStore.undoPurchase(transaction.getId(), transaction.getSenderId(),
                        attachment.getQuantity(), attachment.getPriceNQT());
            }

            @Override
            void doValidateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.DigitalGoodsPurchase attachment = (Attachment.DigitalGoodsPurchase) transaction.getAttachment();
                DigitalGoodsStore.Goods goods = DigitalGoodsStore.getGoods(attachment.getGoodsId());
                if (attachment.getQuantity() <= 0 || attachment.getQuantity() > Constants.MAX_DGS_LISTING_QUANTITY
                        || attachment.getPriceNQT() <= 0 || attachment.getPriceNQT() > Constants.MAX_BALANCE_NQT
                        || (goods != null && ! goods.getSellerId().equals(transaction.getRecipientId()))) {
                    throw new NxtException.NotValidException("Invalid digital goods purchase: " + attachment.getJSONObject());
                }
                if (transaction.getEncryptedMessage() != null && ! transaction.getEncryptedMessage().isText()) {
                    throw new NxtException.NotValidException("Only text encrypted messages allowed");
                }
                if (goods == null || goods.isDelisted()) {
                    throw new NxtException.NotCurrentlyValidException("Goods " + Convert.toUnsignedLong(attachment.getGoodsId()) +
                            "not yet listed or already delisted");
                }
                if (attachment.getQuantity() > goods.getQuantity() || attachment.getPriceNQT() != goods.getPriceNQT()) {
                    throw new NxtException.NotCurrentlyValidException("Goods price or quantity changed: " + attachment.getJSONObject());
                }
                if (attachment.getDeliveryDeadlineTimestamp() <= Nxt.getBlockchain().getLastBlock().getTimestamp()) {
                    throw new NxtException.NotCurrentlyValidException("Delivery deadline has already expired: " + attachment.getDeliveryDeadlineTimestamp());
                }
            }

            @Override
            public boolean hasRecipient() {
                return true;
            }

        };

        public static final TransactionType DELIVERY = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_DELIVERY;
            }

            @Override
            Attachment.DigitalGoodsDelivery parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsDelivery(buffer, transactionVersion);
            }

            @Override
            Attachment.DigitalGoodsDelivery parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsDelivery(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsDelivery attachment = (Attachment.DigitalGoodsDelivery)transaction.getAttachment();
                DigitalGoodsStore.deliver(transaction.getSenderId(), attachment.getPurchaseId(),
                        attachment.getDiscountNQT(), attachment.getGoods(), attachment.goodsIsText());
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                Attachment.DigitalGoodsDelivery attachment = (Attachment.DigitalGoodsDelivery) transaction.getAttachment();
                DigitalGoodsStore.undoDeliver(transaction.getSenderId(), attachment.getPurchaseId(), attachment.getDiscountNQT());
            }

            @Override
            void doValidateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.DigitalGoodsDelivery attachment = (Attachment.DigitalGoodsDelivery) transaction.getAttachment();
                DigitalGoodsStore.Purchase purchase = DigitalGoodsStore.getPendingPurchase(attachment.getPurchaseId());
                if (attachment.getGoods().getData().length > Constants.MAX_DGS_GOODS_LENGTH
                        || attachment.getGoods().getData().length == 0
                        || attachment.getGoods().getNonce().length != 32
                        || attachment.getDiscountNQT() < 0 || attachment.getDiscountNQT() > Constants.MAX_BALANCE_NQT
                        || (purchase != null &&
                        (! purchase.getBuyerId().equals(transaction.getRecipientId())
                                || ! transaction.getSenderId().equals(purchase.getSellerId())
                                || attachment.getDiscountNQT() > Convert.safeMultiply(purchase.getPriceNQT(), purchase.getQuantity())))) {
                    throw new NxtException.NotValidException("Invalid digital goods delivery: " + attachment.getJSONObject());
                }
                if (purchase == null || purchase.getEncryptedGoods() != null) {
                    throw new NxtException.NotCurrentlyValidException("Purchase does not exist yet, or already delivered: "
                            + attachment.getJSONObject());
                }
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Set<String>> duplicates) {
                Attachment.DigitalGoodsDelivery attachment = (Attachment.DigitalGoodsDelivery) transaction.getAttachment();
                return isDuplicate(DigitalGoods.DELIVERY, Convert.toUnsignedLong(attachment.getPurchaseId()), duplicates);
            }

            @Override
            public boolean hasRecipient() {
                return true;
            }

        };

        public static final TransactionType FEEDBACK = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_FEEDBACK;
            }

            @Override
            Attachment.DigitalGoodsFeedback parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsFeedback(buffer, transactionVersion);
            }

            @Override
            Attachment.DigitalGoodsFeedback parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsFeedback(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsFeedback attachment = (Attachment.DigitalGoodsFeedback)transaction.getAttachment();
                DigitalGoodsStore.feedback(attachment.getPurchaseId(), transaction.getEncryptedMessage(), transaction.getMessage());
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount)
                    throws UndoNotSupportedException {
                Attachment.DigitalGoodsFeedback attachment = (Attachment.DigitalGoodsFeedback)transaction.getAttachment();
                DigitalGoodsStore.undoFeedback(attachment.getPurchaseId(), transaction.getEncryptedMessage(), transaction.getMessage());
            }

            @Override
            void doValidateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.DigitalGoodsFeedback attachment = (Attachment.DigitalGoodsFeedback) transaction.getAttachment();
                DigitalGoodsStore.Purchase purchase = DigitalGoodsStore.getPurchase(attachment.getPurchaseId());
                if (purchase != null &&
                        (! purchase.getSellerId().equals(transaction.getRecipientId())
                                || ! transaction.getSenderId().equals(purchase.getBuyerId()))) {
                    throw new NxtException.NotValidException("Invalid digital goods feedback: " + attachment.getJSONObject());
                }
                if (transaction.getEncryptedMessage() == null && transaction.getMessage() == null) {
                    throw new NxtException.NotValidException("Missing feedback message");
                }
                if (transaction.getEncryptedMessage() != null && ! transaction.getEncryptedMessage().isText()) {
                    throw new NxtException.NotValidException("Only text encrypted messages allowed");
                }
                if (transaction.getMessage() != null && ! transaction.getMessage().isText()) {
                    throw new NxtException.NotValidException("Only text public messages allowed");
                }
                if (purchase == null || purchase.getEncryptedGoods() == null) {
                    throw new NxtException.NotCurrentlyValidException("Purchase does not exist yet or not yet delivered");
                }
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Set<String>> duplicates) {
                Attachment.DigitalGoodsFeedback attachment = (Attachment.DigitalGoodsFeedback) transaction.getAttachment();
                return isDuplicate(DigitalGoods.FEEDBACK, Convert.toUnsignedLong(attachment.getPurchaseId()), duplicates);
            }

            @Override
            public boolean hasRecipient() {
                return true;
            }

        };

        public static final TransactionType REFUND = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_REFUND;
            }

            @Override
            Attachment.DigitalGoodsRefund parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsRefund(buffer, transactionVersion);
            }

            @Override
            Attachment.DigitalGoodsRefund parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsRefund(attachmentData);
            }

            @Override
            boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.DigitalGoodsRefund attachment = (Attachment.DigitalGoodsRefund) transaction.getAttachment();
                if (senderAccount.getUnconfirmedBalanceNQT() >= attachment.getRefundNQT()) {
                    senderAccount.addToUnconfirmedBalanceNQT(-attachment.getRefundNQT());
                    return true;
                }
                return false;
            }

            @Override
            void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.DigitalGoodsRefund attachment = (Attachment.DigitalGoodsRefund) transaction.getAttachment();
                senderAccount.addToUnconfirmedBalanceNQT(attachment.getRefundNQT());
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsRefund attachment = (Attachment.DigitalGoodsRefund) transaction.getAttachment();
                DigitalGoodsStore.refund(transaction.getSenderId(), attachment.getPurchaseId(),
                        attachment.getRefundNQT(), transaction.getEncryptedMessage());
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                Attachment.DigitalGoodsRefund attachment = (Attachment.DigitalGoodsRefund) transaction.getAttachment();
                DigitalGoodsStore.undoRefund(transaction.getSenderId(), attachment.getPurchaseId(), attachment.getRefundNQT());
            }

            @Override
            void updateSpending(Transaction transaction, SuperComplexNumber spending) {
                Attachment.DigitalGoodsRefund attachment = (Attachment.DigitalGoodsRefund) transaction.getAttachment();
                spending.add(Constants.NXT_CURRENCY_ID, attachment.getRefundNQT());
            }

            @Override
            void doValidateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.DigitalGoodsRefund attachment = (Attachment.DigitalGoodsRefund) transaction.getAttachment();
                DigitalGoodsStore.Purchase purchase = DigitalGoodsStore.getPurchase(attachment.getPurchaseId());
                if (attachment.getRefundNQT() < 0 || attachment.getRefundNQT() > Constants.MAX_BALANCE_NQT
                        || (purchase != null &&
                        (! purchase.getBuyerId().equals(transaction.getRecipientId())
                                || ! transaction.getSenderId().equals(purchase.getSellerId())))) {
                    throw new NxtException.NotValidException("Invalid digital goods refund: " + attachment.getJSONObject());
                }
                if (transaction.getEncryptedMessage() != null && ! transaction.getEncryptedMessage().isText()) {
                    throw new NxtException.NotValidException("Only text encrypted messages allowed");
                }
                if (purchase == null || purchase.getEncryptedGoods() == null || purchase.getRefundNQT() != 0) {
                    throw new NxtException.NotCurrentlyValidException("Purchase does not exist or is not delivered or is already refunded");
                }
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Set<String>> duplicates) {
                Attachment.DigitalGoodsRefund attachment = (Attachment.DigitalGoodsRefund) transaction.getAttachment();
                return isDuplicate(DigitalGoods.REFUND, Convert.toUnsignedLong(attachment.getPurchaseId()), duplicates);
            }

            @Override
            public boolean hasRecipient() {
                return true;
            }

        };

    }

    public static abstract class AccountControl extends TransactionType {

        private AccountControl() {
        }

        @Override
        public final byte getType() {
            return TransactionType.TYPE_ACCOUNT_CONTROL;
        }

        @Override
        final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        @Override
        void updateSpending(Transaction transaction, SuperComplexNumber spending) {
        }

        public static final TransactionType EFFECTIVE_BALANCE_LEASING = new AccountControl() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_ACCOUNT_CONTROL_EFFECTIVE_BALANCE_LEASING;
            }

            @Override
            Attachment.AccountControlEffectiveBalanceLeasing parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.AccountControlEffectiveBalanceLeasing(buffer, transactionVersion);
            }

            @Override
            Attachment.AccountControlEffectiveBalanceLeasing parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.AccountControlEffectiveBalanceLeasing(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.AccountControlEffectiveBalanceLeasing attachment = (Attachment.AccountControlEffectiveBalanceLeasing) transaction.getAttachment();
                Account.getAccount(transaction.getSenderId()).leaseEffectiveBalance(transaction.getRecipientId(), attachment.getPeriod());
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                throw new UndoNotSupportedException("Reversal of effective balance leasing not supported");
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.AccountControlEffectiveBalanceLeasing attachment = (Attachment.AccountControlEffectiveBalanceLeasing) transaction.getAttachment();
                Account recipientAccount = Account.getAccount(transaction.getRecipientId());
                if (transaction.getSenderId().equals(transaction.getRecipientId())
                        || transaction.getAmountNQT() != 0
                        || attachment.getPeriod() < 1440) {
                    throw new NxtException.NotValidException("Invalid effective balance leasing: "
                            + transaction.getJSONObject() + " transaction " + transaction.getStringId());
                }
                if (recipientAccount == null
                        || (recipientAccount.getPublicKey() == null && !transaction.getStringId().equals("5081403377391821646"))) {
                    throw new NxtException.NotCurrentlyValidException("Invalid effective balance leasing: "
                            + " recipient account " + transaction.getRecipientId() + " not found or no public key published");
                }
            }

            @Override
            public boolean hasRecipient() {
                return true;
            }

        };

    }

    public static abstract class MonetarySystem extends TransactionType {

        private MonetarySystem() {
        }

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
            Attachment.MonetarySystemCurrencyIssuance parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.MonetarySystemCurrencyIssuance(buffer, transactionVersion);
            }

            @Override
            Attachment.MonetarySystemCurrencyIssuance parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.MonetarySystemCurrencyIssuance(attachmentData);
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Set<String>> duplicates) {
                return true; // TODO: cfb: @JLP, how is it better to check duplicates of NAMES and CODES if the same NAME and CODE is allowed?
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                if (Nxt.getBlockchain().getLastBlock().getHeight() < Constants.MONETARY_SYSTEM_BLOCK) {
                    throw new NxtException.NotYetEnabledException("Monetary System not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
                }

                Attachment.MonetarySystemCurrencyIssuance attachment = (Attachment.MonetarySystemCurrencyIssuance)transaction.getAttachment();

                //TODO: fix exceptions
                if (!Genesis.CREATOR_ID.equals(transaction.getRecipientId())
                        || transaction.getAmountNQT() != 0
                        || transaction.getFeeNQT() < Constants.CURRENCY_ISSUANCE_FEE_NQT
                        || attachment.getName().length() < Constants.MIN_CURRENCY_NAME_LENGTH || attachment.getName().length() > Constants.MAX_CURRENCY_NAME_LENGTH
                        || attachment.getCode().length() != Constants.CURRENCY_CODE_LENGTH
                        || attachment.getDescription().length() > Constants.MAX_CURRENCY_DESCRIPTION_LENGTH
                        || !CurrencyType.getCurrencyType(attachment.getType()).isCurrencyIssuanceAttachmentValid(transaction)
                        || attachment.getTotalSupply() <= 0 || attachment.getTotalSupply() > Constants.MAX_CURRENCY_TOTAL_SUPPLY
                        || attachment.getIssuanceHeight() < 0
                        || attachment.getMinReservePerUnitNQT() < 0 || attachment.getMinReservePerUnitNQT() > Constants.MAX_BALANCE_NQT
                        || attachment.getRuleset() != 0) {
                    throw new NxtException.NotValidException("Invalid currency issuance: " + attachment.getJSONObject());
                }

                String normalizedName = attachment.getName().toLowerCase();
                for (int i = 0; i < normalizedName.length(); i++) {
                    if (Constants.ALPHABET.indexOf(normalizedName.charAt(i)) < 0) {
                        throw new NxtException.NotValidException("Invalid currency name: " + normalizedName);
                    }
                }
                if (Currency.isNameSquatted(normalizedName)) {
                    throw new NxtException.NotValidException("Currency name already squatted: " + normalizedName);
                }
                for (int i = 0; i < attachment.getCode().length(); i++) {
                    if (Constants.ALLOWED_CURRENCY_CODE_LETTERS.indexOf(attachment.getCode().charAt(i)) < 0) {
                        throw new NxtException.NotValidException("Invalid currency code: " + attachment.getCode());
                    }
                }
                if (Currency.isCodeSquatted(attachment.getCode())) {
                    throw new NxtException.NotValidException("Currency code already squatted: " + attachment.getCode());
                }
            }

            @Override
            boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                return CurrencyType.getCurrencyType(((Attachment.MonetarySystemCurrencyIssuance)transaction.getAttachment()).getType()).applyCurrencyIssuanceAttachmentUnconfirmed(transaction, senderAccount);
            }

            @Override
            void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                CurrencyType.getCurrencyType(((Attachment.MonetarySystemCurrencyIssuance)transaction.getAttachment()).getType()).undoCurrencyIssuanceAttachmentUnconfirmed(transaction, senderAccount);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                CurrencyType.getCurrencyType(((Attachment.MonetarySystemCurrencyIssuance)transaction.getAttachment()).getType()).applyCurrencyIssuanceAttachment(transaction, senderAccount, recipientAccount);
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                CurrencyType.getCurrencyType(((Attachment.MonetarySystemCurrencyIssuance)transaction.getAttachment()).getType()).undoCurrencyIssuanceAttachment(transaction, senderAccount, recipientAccount);
            }

            @Override
            void updateSpending(Transaction transaction, SuperComplexNumber spending) {
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
                Attachment.MonetarySystemReserveIncrease attachment = (Attachment.MonetarySystemReserveIncrease)transaction.getAttachment();
                //TODO: exceptions
                if (!Genesis.CREATOR_ID.equals(transaction.getRecipientId())
                        || transaction.getAmountNQT() != 0
                        || attachment.getAmountNQT() <= 0) {
                    throw new NxtException.NotValidException("Invalid reserve increase: " + attachment.getJSONObject());
                }
            }

            @Override
            boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.MonetarySystemReserveIncrease attachment = (Attachment.MonetarySystemReserveIncrease)transaction.getAttachment();
                if (senderAccount.getUnconfirmedBalanceNQT() >= Convert.safeMultiply(Currency.getCurrency(attachment.getCurrencyId()).getTotalSupply(), attachment.getAmountNQT())) {
                    senderAccount.addToUnconfirmedBalanceNQT(-Convert.safeMultiply(Currency.getCurrency(attachment.getCurrencyId()).getTotalSupply(), attachment.getAmountNQT()));
                    return true;
                }
                return false;
            }

            @Override
            void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.MonetarySystemReserveIncrease attachment = (Attachment.MonetarySystemReserveIncrease)transaction.getAttachment();
                senderAccount.addToUnconfirmedBalanceNQT(Convert.safeMultiply(Currency.getCurrency(attachment.getCurrencyId()).getTotalSupply(), attachment.getAmountNQT()));
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MonetarySystemReserveIncrease attachment = (Attachment.MonetarySystemReserveIncrease)transaction.getAttachment();
                Currency.increaseReserve(senderAccount, attachment.getCurrencyId(), attachment.getAmountNQT());
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                throw new UndoNotSupportedException("Reversal of reserve increase not supported");
            }

            @Override
            void updateSpending(Transaction transaction, SuperComplexNumber spending) {
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
                Attachment.MonetarySystemReserveClaim attachment = (Attachment.MonetarySystemReserveClaim)transaction.getAttachment();
                if (!Genesis.CREATOR_ID.equals(transaction.getRecipientId())
                        || transaction.getAmountNQT() != 0
                        || !Currency.isIssued(attachment.getCurrencyId())
                        || attachment.getUnits() <= 0) {
                    throw new NxtException.NotValidException("Invalid reserve claim: " + attachment.getJSONObject());
                }
            }

            @Override
            boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.MonetarySystemReserveClaim attachment = (Attachment.MonetarySystemReserveClaim)transaction.getAttachment();
                if (senderAccount.getUnconfirmedCurrencyBalanceQNT(attachment.getCurrencyId()) >= attachment.getUnits()) {
                    senderAccount.addToUnconfirmedCurrencyBalanceQNT(attachment.getCurrencyId(), -attachment.getUnits());
                    return true;
                }
                return false;
            }

            @Override
            void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.MonetarySystemReserveClaim attachment = (Attachment.MonetarySystemReserveClaim)transaction.getAttachment();
                senderAccount.addToUnconfirmedCurrencyBalanceQNT(attachment.getCurrencyId(), attachment.getUnits());
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MonetarySystemReserveClaim attachment = (Attachment.MonetarySystemReserveClaim)transaction.getAttachment();
                Currency.claimReserve(senderAccount, attachment.getCurrencyId(), attachment.getUnits());
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                throw new UndoNotSupportedException("Reversal of reserve claim not supported");
            }

            @Override
            void updateSpending(Transaction transaction, SuperComplexNumber spending) {
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

            //TODO: exceptions
            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                if (Nxt.getBlockchain().getLastBlock().getHeight() < Constants.MONETARY_SYSTEM_BLOCK) {
                    throw new NxtException.NotYetEnabledException("Monetary System not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
                }
                Attachment.MonetarySystemMoneyTransfer attachment = (Attachment.MonetarySystemMoneyTransfer)transaction.getAttachment();
                for (int i = 0; i < attachment.getSize(); i++) {
                    Attachment.MonetarySystemMoneyTransfer.Entry entry = attachment.getEntry(i);
                    if (!Currency.isIssued(entry.getCurrencyId())
                            || entry.getUnits() <= 0) {
                        throw new NxtException.NotValidException("Invalid money transfer: " + attachment.getJSONObject());
                    }
                }
                if (!Genesis.CREATOR_ID.equals(transaction.getRecipientId())
                        || transaction.getAmountNQT() != 0
                        || attachment.getComment().length() > Constants.MAX_MONEY_TRANSFER_COMMENT_LENGTH) {
                    throw new NxtException.NotValidException("Invalid money transfer: " + attachment.getJSONObject());
                }
            }

            @Override
            boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.MonetarySystemMoneyTransfer attachment = (Attachment.MonetarySystemMoneyTransfer)transaction.getAttachment();
                if (attachment.getTransfer().isCovered(senderAccount.getUnconfirmedCurrencyBalances())) {
                    senderAccount.getUnconfirmedCurrencyBalances().subtract(attachment.getTransfer());
                    return true;
                }
                return false;
            }

            @Override
            void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.MonetarySystemMoneyTransfer attachment = (Attachment.MonetarySystemMoneyTransfer)transaction.getAttachment();
                senderAccount.getUnconfirmedCurrencyBalances().add(attachment.getTransfer());
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MonetarySystemMoneyTransfer attachment = (Attachment.MonetarySystemMoneyTransfer)transaction.getAttachment();
                Currency.transferMoney(senderAccount, attachment.getEntries());
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                throw new UndoNotSupportedException("Reversal of money transfer not supported");
            }

            @Override
            void updateSpending(Transaction transaction, SuperComplexNumber spending) {
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
                Attachment.MonetarySystemExchangeOfferPublication attachment = (Attachment.MonetarySystemExchangeOfferPublication)transaction.getAttachment();
                if (!Genesis.CREATOR_ID.equals(transaction.getRecipientId())
                        || transaction.getAmountNQT() != 0
                        || !Currency.isIssued(attachment.getCurrencyId())
                        || attachment.getBuyingRateNQT() <= 0
                        || attachment.getSellingRateNQT() <= 0
                        || attachment.getTotalBuyingLimit() < 0
                        || attachment.getTotalSellingLimit() < 0
                        || attachment.getInitialBuyingSupply() < 0
                        || attachment.getInitialSellingSupply() < 0
                        || attachment.getExpirationHeight() < 0) {
                    throw new NxtException.NotValidException("Invalid exchange offer publication: " + attachment.getJSONObject());
                }
            }

            @Override
            boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.MonetarySystemExchangeOfferPublication attachment = (Attachment.MonetarySystemExchangeOfferPublication)transaction.getAttachment();
                if (senderAccount.getUnconfirmedBalanceNQT() >= Convert.safeMultiply(attachment.getInitialBuyingSupply(), attachment.getBuyingRateNQT()) && senderAccount.getUnconfirmedCurrencyBalanceQNT(attachment.getCurrencyId()) >= attachment.getInitialSellingSupply()) {
                    senderAccount.addToUnconfirmedBalanceNQT(-Convert.safeMultiply(attachment.getInitialBuyingSupply(), attachment.getBuyingRateNQT()));
                    senderAccount.addToUnconfirmedCurrencyBalanceQNT(attachment.getCurrencyId(), -attachment.getInitialSellingSupply());
                    return true;
                }
                return false;

            }

            @Override
            void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.MonetarySystemExchangeOfferPublication attachment = (Attachment.MonetarySystemExchangeOfferPublication)transaction.getAttachment();
                senderAccount.addToUnconfirmedBalanceNQT(Convert.safeMultiply(attachment.getInitialBuyingSupply(), attachment.getBuyingRateNQT()));
                senderAccount.addToUnconfirmedCurrencyBalanceQNT(attachment.getCurrencyId(), attachment.getInitialSellingSupply());
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MonetarySystemExchangeOfferPublication attachment = (Attachment.MonetarySystemExchangeOfferPublication)transaction.getAttachment();
                CurrencyExchange.publicateOffer(senderAccount, attachment.getCurrencyId(), attachment.getBuyingRateNQT(), attachment.getSellingRateNQT(), attachment.getTotalBuyingLimit(), attachment.getTotalSellingLimit(), attachment.getInitialBuyingSupply(), attachment.getInitialSellingSupply(), attachment.getExpirationHeight());
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                throw new UndoNotSupportedException("Reversal of exchange offer publication not supported");
            }

            @Override
            void updateSpending(Transaction transaction, SuperComplexNumber spending) {
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

            //TODO: exceptions
            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                if (Nxt.getBlockchain().getLastBlock().getHeight() < Constants.MONETARY_SYSTEM_BLOCK) {
                    throw new NxtException.NotYetEnabledException("Monetary System not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
                }
                Attachment.MonetarySystemExchange attachment = (Attachment.MonetarySystemExchange)transaction.getAttachment();
                if (!Genesis.CREATOR_ID.equals(transaction.getRecipientId())
                        || transaction.getAmountNQT() != 0
                        || !Currency.isIssued(attachment.getCurrencyId())
                        || attachment.getRateNQT() <= 0
                        || attachment.getUnits() == 0) {
                    throw new NxtException.NotValidException("Invalid exchange: " + attachment.getJSONObject());
                }
            }

            @Override
            boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.MonetarySystemExchange attachment = (Attachment.MonetarySystemExchange)transaction.getAttachment();
                if (attachment.isPurchase()) {
                    if (senderAccount.getUnconfirmedBalanceNQT() >= Convert.safeMultiply(-attachment.getUnits(), attachment.getRateNQT())) {
                        senderAccount.addToUnconfirmedBalanceNQT(-Convert.safeMultiply(-attachment.getUnits(), attachment.getRateNQT()));
                        return true;
                    }
                } else {
                    if (senderAccount.getUnconfirmedCurrencyBalanceQNT(attachment.getCurrencyId()) >= attachment.getUnits()) {
                        senderAccount.addToUnconfirmedCurrencyBalanceQNT(attachment.getCurrencyId(), -attachment.getUnits());
                        return true;
                    }
                }
                return false;
            }

            @Override
            void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.MonetarySystemExchange attachment = (Attachment.MonetarySystemExchange)transaction.getAttachment();
                if (attachment.isPurchase()) {
                    senderAccount.addToUnconfirmedBalanceNQT(Convert.safeMultiply(-attachment.getUnits(), attachment.getRateNQT()));
                } else {
                    senderAccount.addToUnconfirmedCurrencyBalanceQNT(attachment.getCurrencyId(), attachment.getUnits());
                }
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MonetarySystemExchange attachment = (Attachment.MonetarySystemExchange)transaction.getAttachment();
                if (attachment.isPurchase()) {
                    CurrencyExchange.exchangeNXTForMoney(senderAccount, attachment.getCurrencyId(), attachment.getRateNQT(), -attachment.getUnits());
                } else {
                    CurrencyExchange.exchangeMoneyForNXT(senderAccount, attachment.getCurrencyId(), attachment.getRateNQT(), attachment.getUnits());
                }
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                throw new UndoNotSupportedException("Reversal of exchange not supported");
            }

            @Override
            void updateSpending(Transaction transaction, SuperComplexNumber spending) {
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
                Attachment.MonetarySystemMoneyMinting attachment = (Attachment.MonetarySystemMoneyMinting)transaction.getAttachment();
                if (!Genesis.CREATOR_ID.equals(transaction.getRecipientId())
                        || transaction.getAmountNQT() != 0
                        || !Currency.isIssued(attachment.getCurrencyId())
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
                Attachment.MonetarySystemMoneyMinting attachment = (Attachment.MonetarySystemMoneyMinting)transaction.getAttachment();
                CurrencyMint.mintMoney(senderAccount, attachment.getNonce(), attachment.getCurrencyId(), attachment.getUnits(), attachment.getCounter());
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                throw new UndoNotSupportedException("Reversal of money minting not supported");
            }

            @Override
            void updateSpending(Transaction transaction, SuperComplexNumber spending) {
            }

            @Override
            public boolean hasRecipient() {
                return false;
            }

        };

    }

    public static final class UndoNotSupportedException extends NxtException {

        UndoNotSupportedException(String message) {
            super(message);
        }

    }

}
