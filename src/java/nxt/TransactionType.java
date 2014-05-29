package nxt;

import nxt.crypto.EncryptedData;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public abstract class TransactionType {

    private static final byte TYPE_PAYMENT = 0;
    private static final byte TYPE_MESSAGING = 1;
    private static final byte TYPE_COLORED_COINS = 2;
    private static final byte TYPE_DIGITAL_GOODS = 3;
    private static final byte TYPE_ACCOUNT_CONTROL = 4;

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
            default:
                return null;
        }
    }

    private TransactionType() {
    }

    public abstract byte getType();

    public abstract byte getSubtype();

    final void loadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {
        doLoadAttachment(transaction, buffer);
        //validateAttachment(transaction);
    }

    abstract void doLoadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException;

    final void loadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {
        doLoadAttachment(transaction, attachmentData);
        //validateAttachment(transaction);
    }

    abstract void doLoadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException;

    abstract void validateAttachment(Transaction transaction) throws NxtException.ValidationException;

    // return false iff double spending
    final boolean applyUnconfirmed(Transaction transaction, Account senderAccount) {
        long totalAmountNQT = Convert.safeAdd(transaction.getAmountNQT(), transaction.getFeeNQT());
        if (transaction.getReferencedTransactionFullHash() != null) {
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
        if (transaction.getReferencedTransactionFullHash() != null) {
            senderAccount.addToUnconfirmedBalanceNQT(Constants.UNCONFIRMED_POOL_DEPOSIT_NQT);
        }
        recipientAccount.addToBalanceAndUnconfirmedBalanceNQT(transaction.getAmountNQT());
        applyAttachment(transaction, senderAccount, recipientAccount);
    }

    abstract void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount);

    final void undoUnconfirmed(Transaction transaction, Account senderAccount) {
        senderAccount.addToUnconfirmedBalanceNQT(Convert.safeAdd(transaction.getAmountNQT(), transaction.getFeeNQT()));
        if (transaction.getReferencedTransactionFullHash() != null) {
            senderAccount.addToUnconfirmedBalanceNQT(Constants.UNCONFIRMED_POOL_DEPOSIT_NQT);
        }
        undoAttachmentUnconfirmed(transaction, senderAccount);
    }

    abstract void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount);

    final void undo(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
        senderAccount.addToBalanceNQT(Convert.safeAdd(transaction.getAmountNQT(), transaction.getFeeNQT()));
        if (transaction.getReferencedTransactionFullHash() != null) {
            senderAccount.addToUnconfirmedBalanceNQT(- Constants.UNCONFIRMED_POOL_DEPOSIT_NQT);
        }
        recipientAccount.addToBalanceAndUnconfirmedBalanceNQT(-transaction.getAmountNQT());
        undoAttachment(transaction, senderAccount, recipientAccount);
    }

    abstract void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException;

    abstract void updateTotals(Transaction transaction, Map<Long, Long> accumulatedAmounts,
                               Map<Long, Map<Long, Long>> accumulatedAssetQuantities, Long accumulatedAmount);

    boolean isDuplicate(Transaction transaction, Map<TransactionType, Set<String>> duplicates) {
        return false;
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

        public static final TransactionType ORDINARY = new Payment() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_PAYMENT_ORDINARY_PAYMENT;
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, ByteBuffer buffer) {
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, JSONObject attachmentData) {
            }

            @Override
            boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                return true;
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            }

            @Override
            void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            }

            @Override
            void updateTotals(Transaction transaction, Map<Long, Long> accumulatedAmounts,
                              Map<Long, Map<Long, Long>> accumulatedAssetQuantities, Long accumulatedAmount) {
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                if (transaction.getAmountNQT() <= 0 || transaction.getAmountNQT() >= Constants.MAX_BALANCE_NQT) {
                    throw new NxtException.ValidationException("Invalid ordinary payment");
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
        final void updateTotals(Transaction transaction, Map<Long, Long> accumulatedAmounts,
                                Map<Long, Map<Long, Long>> accumulatedAssetQuantities, Long accumulatedAmount) {
        }

        public final static TransactionType ARBITRARY_MESSAGE = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ARBITRARY_MESSAGE;
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                int messageLength = buffer.getInt();
                if (messageLength > Constants.MAX_ARBITRARY_MESSAGE_LENGTH) {
                    throw new NxtException.ValidationException("Invalid arbitrary message length: " + messageLength);
                }
                byte[] message = new byte[messageLength];
                buffer.get(message);
                transaction.setAttachment(new Attachment.MessagingArbitraryMessage(message));
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                String message = (String) attachmentData.get("message");
                transaction.setAttachment(new Attachment.MessagingArbitraryMessage(Convert.parseHexString(message)));
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.MessagingArbitraryMessage attachment = (Attachment.MessagingArbitraryMessage) transaction.getAttachment();
                if (transaction.getAmountNQT() != 0 || attachment.getMessage().length > Constants.MAX_ARBITRARY_MESSAGE_LENGTH) {
                    throw new NxtException.ValidationException("Invalid arbitrary message: " + attachment.getJSONObject());
                }
            }

        };

        public static final TransactionType ALIAS_ASSIGNMENT = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT;
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                String aliasName = readString(buffer, buffer.get(), Constants.MAX_ALIAS_LENGTH);
                String aliasURI = readString(buffer, buffer.getShort(), Constants.MAX_ALIAS_URI_LENGTH);
                transaction.setAttachment(new Attachment.MessagingAliasAssignment(aliasName, aliasURI));
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                String alias = (String) attachmentData.get("alias");
                String uri = (String) attachmentData.get("uri");
                transaction.setAttachment(new Attachment.MessagingAliasAssignment(alias, uri));
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
                Set<String> myDuplicates = duplicates.get(this);
                if (myDuplicates == null) {
                    myDuplicates = new HashSet<>();
                    duplicates.put(this, myDuplicates);
                }
                Attachment.MessagingAliasAssignment attachment = (Attachment.MessagingAliasAssignment) transaction.getAttachment();
                return !myDuplicates.add(attachment.getAliasName().toLowerCase());
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.MessagingAliasAssignment attachment = (Attachment.MessagingAliasAssignment) transaction.getAttachment();
                if (!Genesis.CREATOR_ID.equals(transaction.getRecipientId()) || transaction.getAmountNQT() != 0
                        || attachment.getAliasName().length() == 0
                        || attachment.getAliasName().length() > Constants.MAX_ALIAS_LENGTH
                        || attachment.getAliasURI().length() > Constants.MAX_ALIAS_URI_LENGTH) {
                    throw new NxtException.ValidationException("Invalid alias assignment: " + attachment.getJSONObject());
                }
                String normalizedAlias = attachment.getAliasName().toLowerCase();
                for (int i = 0; i < normalizedAlias.length(); i++) {
                    if (Constants.ALPHABET.indexOf(normalizedAlias.charAt(i)) < 0) {
                        throw new NxtException.ValidationException("Invalid alias name: " + normalizedAlias);
                    }
                }
                Alias alias = Alias.getAlias(normalizedAlias);
                if (alias != null && ! alias.getAccountId().equals(transaction.getSenderId())) {
                    throw new NxtException.ValidationException("Alias already owned by another account: " + normalizedAlias);
                }
            }

        };

        public static final TransactionType ALIAS_SELL = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ALIAS_SELL;
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                String alias = readString(buffer, buffer.get(), Constants.MAX_ALIAS_LENGTH);
                long priceNQT = buffer.getLong();
                transaction.setAttachment(new Attachment.MessagingAliasSell(alias, priceNQT));
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                String alias = (String) attachmentData.get("alias");
                long priceNQT = (Long) attachmentData.get("priceNQT");
                transaction.setAttachment(new Attachment.MessagingAliasSell(alias, priceNQT));
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                final Attachment.MessagingAliasSell attachment =
                        (Attachment.MessagingAliasSell) transaction.getAttachment();
                final String aliasName = attachment.getAliasName();
                final long priceNQT = attachment.getPriceNQT();
                final Long buyerId = recipientAccount.getId();
                if (priceNQT > 0) {
                    Alias.addSellOffer(aliasName, priceNQT, buyerId);
                } else {
                    Alias.changeOwner(Account.getAccount(buyerId), aliasName, transaction.getBlockTimestamp());
                }
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                throw new UndoNotSupportedException("Reversal of alias sell offer not supported");
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Set<String>> duplicates) {
                Set<String> myDuplicates = duplicates.get(this);
                if (myDuplicates == null) {
                    myDuplicates = new HashSet<>();
                    duplicates.put(this, myDuplicates);
                }
                Attachment.MessagingAliasSell attachment = (Attachment.MessagingAliasSell) transaction.getAttachment();
                return !myDuplicates.add(attachment.getAliasName().toLowerCase());
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                if (Nxt.getBlockchain().getLastBlock().getHeight() < Constants.ALIAS_TRANSFER_BLOCK) {
                    throw new NotYetEnabledException("Alias transfer not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
                }
                if (transaction.getAmountNQT() != 0) {
                    throw new NxtException.ValidationException("Invalid sell alias transaction: " +
                            transaction.getJSONObject());
                }
                final Attachment.MessagingAliasSell attachment =
                        (Attachment.MessagingAliasSell) transaction.getAttachment();
                final String aliasName = attachment.getAliasName();
                if (aliasName == null || aliasName.length() == 0) {
                    throw new NxtException.ValidationException("Missing alias name");
                }
                final Alias alias = Alias.getAlias(aliasName);
                if (alias == null) {
                    throw new NxtException.ValidationException("Alias hasn't been registered yet: " + aliasName);
                } else if (! alias.getAccountId().equals(transaction.getSenderId())) {
                    throw new NxtException.ValidationException("Alias doesn't belong to sender: " + aliasName);
                }
                long priceNQT = attachment.getPriceNQT();
                if (priceNQT < 0 || priceNQT > Constants.MAX_BALANCE_NQT) {
                    throw new NxtException.ValidationException("Invalid alias sell price: " + priceNQT);
                }
                if (priceNQT == 0 && transaction.getRecipientId().equals(Genesis.CREATOR_ID)) {
                    throw new NxtException.ValidationException("Transferring aliases to Genesis account not allowed");
                }
            }
        };

        public static final TransactionType ALIAS_BUY = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ALIAS_BUY;
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                String alias = readString(buffer, buffer.get(), Constants.MAX_ALIAS_LENGTH);
                transaction.setAttachment(new Attachment.MessagingAliasBuy(alias));
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                String alias = (String) attachmentData.get("alias");
                transaction.setAttachment(new Attachment.MessagingAliasBuy(alias));
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
                Set<String> myDuplicates = duplicates.get(this);
                if (myDuplicates == null) {
                    myDuplicates = new HashSet<>();
                    duplicates.put(this, myDuplicates);
                }
                Attachment.MessagingAliasBuy attachment = (Attachment.MessagingAliasBuy) transaction.getAttachment();
                return !myDuplicates.add(attachment.getAliasName().toLowerCase());
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                if (Nxt.getBlockchain().getLastBlock().getHeight() < Constants.ALIAS_TRANSFER_BLOCK) {
                    throw new NotYetEnabledException("Alias transfer not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
                }
                final Attachment.MessagingAliasBuy attachment =
                        (Attachment.MessagingAliasBuy) transaction.getAttachment();
                final String aliasName = attachment.getAliasName();
                final Alias alias = Alias.getAlias(aliasName);
                if (alias == null) {
                    throw new NxtException.ValidationException("Alias hasn't been registered yet: " + aliasName);
                } else if (! alias.getAccountId().equals(transaction.getRecipientId())) {
                    throw new NxtException.ValidationException("Alias is owned by account other than recipient: "
                            + Convert.toUnsignedLong(alias.getAccountId()));
                }
                Alias.Offer offer = Alias.getOffer(aliasName);
                if (offer == null) {
                    throw new NxtException.ValidationException("Alias is not for sale: " + aliasName);
                }
                if (transaction.getAmountNQT() < offer.getPriceNQT()) {
                    String msg = "Price is too low for: " + aliasName + " ("
                            + transaction.getAmountNQT() + " < " + offer.getPriceNQT() + ")";
                    throw new NxtException.ValidationException(msg);
                }
                if (! offer.getBuyerId().equals(Genesis.CREATOR_ID) && ! offer.getBuyerId().equals(transaction.getSenderId())) {
                    throw new NxtException.ValidationException("Wrong buyer for " + aliasName + ": "
                            + Convert.toUnsignedLong(transaction.getSenderId()) + " expected: "
                            + Convert.toUnsignedLong(offer.getBuyerId()));
                }
            }
        };

        public final static TransactionType POLL_CREATION = new Messaging() {
            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_POLL_CREATION;
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                String pollName = readString(buffer, buffer.getShort(), Constants.MAX_POLL_NAME_LENGTH);
                String pollDescription = readString(buffer, buffer.getShort(), Constants.MAX_POLL_DESCRIPTION_LENGTH);
                int numberOfOptions = buffer.get();
                if (numberOfOptions > Constants.MAX_POLL_OPTION_COUNT) {
                    throw new NxtException.ValidationException("Invalid number of poll options: " + numberOfOptions);
                }
                String[] pollOptions = new String[numberOfOptions];
                for (int i = 0; i < numberOfOptions; i++) {
                    pollOptions[i] = readString(buffer, buffer.getShort(), Constants.MAX_POLL_OPTION_LENGTH);
                }
                byte minNumberOfOptions = buffer.get();
                byte maxNumberOfOptions = buffer.get();
                boolean optionsAreBinary = buffer.get() != 0;
                transaction.setAttachment(new Attachment.MessagingPollCreation(pollName, pollDescription, pollOptions,
                        minNumberOfOptions, maxNumberOfOptions, optionsAreBinary));
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {

                String pollName = ((String) attachmentData.get("name")).trim();
                String pollDescription = ((String) attachmentData.get("description")).trim();
                JSONArray options = (JSONArray) attachmentData.get("options");
                String[] pollOptions = new String[options.size()];
                for (int i = 0; i < pollOptions.length; i++) {
                    pollOptions[i] = ((String) options.get(i)).trim();
                }
                byte minNumberOfOptions = ((Long) attachmentData.get("minNumberOfOptions")).byteValue();
                byte maxNumberOfOptions = ((Long) attachmentData.get("maxNumberOfOptions")).byteValue();
                boolean optionsAreBinary = (Boolean) attachmentData.get("optionsAreBinary");

                transaction.setAttachment(new Attachment.MessagingPollCreation(pollName, pollDescription, pollOptions,
                        minNumberOfOptions, maxNumberOfOptions, optionsAreBinary));
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
                    throw new NotYetEnabledException("Voting System not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
                }
                Attachment.MessagingPollCreation attachment = (Attachment.MessagingPollCreation) transaction.getAttachment();
                for (int i = 0; i < attachment.getPollOptions().length; i++) {
                    if (attachment.getPollOptions()[i].length() > Constants.MAX_POLL_OPTION_LENGTH) {
                        throw new NxtException.ValidationException("Invalid poll options length: " + attachment.getJSONObject());
                    }
                }
                if (attachment.getPollName().length() > Constants.MAX_POLL_NAME_LENGTH
                        || attachment.getPollDescription().length() > Constants.MAX_POLL_DESCRIPTION_LENGTH
                        || attachment.getPollOptions().length > Constants.MAX_POLL_OPTION_COUNT
                        || transaction.getAmountNQT() != 0
                        || !Genesis.CREATOR_ID.equals(transaction.getRecipientId())) {
                    throw new NxtException.ValidationException("Invalid poll attachment: " + attachment.getJSONObject());
                }
            }

        };

        public final static TransactionType VOTE_CASTING = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_VOTE_CASTING;
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                Long pollId = buffer.getLong();
                int numberOfOptions = buffer.get();
                if (numberOfOptions > Constants.MAX_POLL_OPTION_COUNT) {
                    throw new NxtException.ValidationException("Error parsing vote casting parameters");
                }
                byte[] pollVote = new byte[numberOfOptions];
                buffer.get(pollVote);
                transaction.setAttachment(new Attachment.MessagingVoteCasting(pollId, pollVote));
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                Long pollId = Convert.parseUnsignedLong((String)attachmentData.get("pollId"));
                JSONArray vote = (JSONArray)attachmentData.get("vote");
                byte[] pollVote = new byte[vote.size()];
                for (int i = 0; i < pollVote.length; i++) {
                    pollVote[i] = ((Long) vote.get(i)).byteValue();
                }
                transaction.setAttachment(new Attachment.MessagingVoteCasting(pollId, pollVote));
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
                    throw new NotYetEnabledException("Voting System not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
                }
                Attachment.MessagingVoteCasting attachment = (Attachment.MessagingVoteCasting) transaction.getAttachment();
                if (attachment.getPollId() == null || attachment.getPollVote() == null
                        || attachment.getPollVote().length > Constants.MAX_POLL_OPTION_COUNT) {
                    throw new NxtException.ValidationException("Invalid vote casting attachment: " + attachment.getJSONObject());
                }
                if (Poll.getPoll(attachment.getPollId()) == null) {
                    throw new NxtException.ValidationException("Invalid poll: " + Convert.toUnsignedLong(attachment.getPollId()));
                }
                if (transaction.getAmountNQT() != 0 || !Genesis.CREATOR_ID.equals(transaction.getRecipientId())) {
                    throw new NxtException.ValidationException("Invalid vote casting amount or recipient");
                }
            }

        };

        public static final TransactionType HUB_ANNOUNCEMENT = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_HUB_ANNOUNCEMENT;
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                long minFeePerByte = buffer.getLong();
                int numberOfUris = buffer.get();
                if (numberOfUris > Constants.MAX_HUB_ANNOUNCEMENT_URIS) {
                    throw new NxtException.ValidationException("Invalid number of URIs: " + numberOfUris);
                }
                String[] uris = new String[numberOfUris];
                for (int i = 0; i < uris.length; i++) {
                    uris[i] = readString(buffer, buffer.getShort(), Constants.MAX_HUB_ANNOUNCEMENT_URI_LENGTH);
                }
                transaction.setAttachment(new Attachment.MessagingHubAnnouncement(minFeePerByte, uris));
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                long minFeePerByte = (Long) attachmentData.get("minFeePerByte");
                String[] uris;
                try {
                    JSONArray urisData = (JSONArray) attachmentData.get("uris");
                    uris = new String[urisData.size()];
                    for (int i = 0; i < uris.length; i++) {
                        uris[i] = (String) urisData.get(i);
                    }
                } catch (RuntimeException e) {
                    throw new NxtException.ValidationException("Error parsing hub terminal announcement parameters", e);
                }

                transaction.setAttachment(new Attachment.MessagingHubAnnouncement(minFeePerByte, uris));
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
                    throw new NotYetEnabledException("Hub terminal announcement not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
                }
                Attachment.MessagingHubAnnouncement attachment = (Attachment.MessagingHubAnnouncement) transaction.getAttachment();
                if (!Genesis.CREATOR_ID.equals(transaction.getRecipientId())
                        || transaction.getAmountNQT() != 0
                        || attachment.getMinFeePerByteNQT() < 0 || attachment.getMinFeePerByteNQT() > Constants.MAX_BALANCE_NQT
                        || attachment.getUris().length > Constants.MAX_HUB_ANNOUNCEMENT_URIS) {
                    // cfb: "0" is allowed to show that another way to determine the min fee should be used
                    throw new NxtException.ValidationException("Invalid hub terminal announcement: " + attachment.getJSONObject());
                }
                for (String uri : attachment.getUris()) {
                    if (uri.length() > Constants.MAX_HUB_ANNOUNCEMENT_URI_LENGTH) {
                        throw new NxtException.ValidationException("Invalid URI length: " + uri.length());
                    }
                    //TODO: also check URI validity here?
                }
            }

        };

        public static final Messaging ACCOUNT_INFO = new Messaging() {

            @Override
            public byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ACCOUNT_INFO;
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                String name = readString(buffer, buffer.get(), Constants.MAX_ACCOUNT_NAME_LENGTH);
                String description = readString(buffer, buffer.getShort(), Constants.MAX_ACCOUNT_DESCRIPTION_LENGTH);
                transaction.setAttachment(new Attachment.MessagingAccountInfo(name, description));
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                String name = (String) attachmentData.get("name");
                String description = (String) attachmentData.get("description");
                transaction.setAttachment(new Attachment.MessagingAccountInfo(name.trim(), description.trim()));
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.MessagingAccountInfo attachment = (Attachment.MessagingAccountInfo)transaction.getAttachment();
                if (! Genesis.CREATOR_ID.equals(transaction.getRecipientId()) || transaction.getAmountNQT() != 0
                        || attachment.getName().length() > Constants.MAX_ACCOUNT_NAME_LENGTH
                        || attachment.getDescription().length() > Constants.MAX_ACCOUNT_DESCRIPTION_LENGTH
                        ) {
                    throw new NxtException.ValidationException("Invalid account info issuance: " + attachment.getJSONObject());
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
            void doLoadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                String name = readString(buffer, buffer.get(), Constants.MAX_ASSET_NAME_LENGTH);
                String description = readString(buffer, buffer.getShort(), Constants.MAX_ASSET_DESCRIPTION_LENGTH);
                long quantityQNT = buffer.getLong();
                byte decimals = buffer.get();
                transaction.setAttachment(new Attachment.ColoredCoinsAssetIssuance(name, description,
                        quantityQNT, decimals));
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                String name = (String) attachmentData.get("name");
                String description = (String) attachmentData.get("description");
                long quantityQNT = (Long) attachmentData.get("quantityQNT");
                byte decimals = ((Long) attachmentData.get("decimals")).byteValue();
                transaction.setAttachment(new Attachment.ColoredCoinsAssetIssuance(name.trim(), description.trim(),
                        quantityQNT, decimals));
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
            void updateTotals(Transaction transaction, Map<Long, Long> accumulatedAmounts,
                              Map<Long, Map<Long, Long>> accumulatedAssetQuantities, Long accumulatedAmount) {
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.ColoredCoinsAssetIssuance attachment = (Attachment.ColoredCoinsAssetIssuance)transaction.getAttachment();
                if (! Genesis.CREATOR_ID.equals(transaction.getRecipientId()) || transaction.getAmountNQT() != 0
                        || transaction.getFeeNQT() < Constants.ASSET_ISSUANCE_FEE_NQT
                        || attachment.getName().length() < Constants.MIN_ASSET_NAME_LENGTH
                        || attachment.getName().length() > Constants.MAX_ASSET_NAME_LENGTH
                        || attachment.getDescription().length() > Constants.MAX_ASSET_DESCRIPTION_LENGTH
                        || attachment.getDecimals() < 0 || attachment.getDecimals() > 8
                        || attachment.getQuantityQNT() <= 0
                        || attachment.getQuantityQNT() > Constants.MAX_ASSET_QUANTITY_QNT
                        ) {
                    throw new NxtException.ValidationException("Invalid asset issuance: " + attachment.getJSONObject());
                }
                String normalizedName = attachment.getName().toLowerCase();
                for (int i = 0; i < normalizedName.length(); i++) {
                    if (Constants.ALPHABET.indexOf(normalizedName.charAt(i)) < 0) {
                        throw new NxtException.ValidationException("Invalid asset name: " + normalizedName);
                    }
                }
            }

        };

        public static final TransactionType ASSET_TRANSFER = new ColoredCoins() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COLORED_COINS_ASSET_TRANSFER;
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                Long assetId = Convert.zeroToNull(buffer.getLong());
                long quantityQNT = buffer.getLong();
                String comment = readString(buffer, buffer.getShort(), Constants.MAX_ASSET_TRANSFER_COMMENT_LENGTH);
                transaction.setAttachment(new Attachment.ColoredCoinsAssetTransfer(assetId, quantityQNT, comment));
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                Long assetId = Convert.parseUnsignedLong((String) attachmentData.get("asset"));
                long quantityQNT = (Long) attachmentData.get("quantityQNT");
                String comment = (String) attachmentData.get("comment");
                transaction.setAttachment(new Attachment.ColoredCoinsAssetTransfer(assetId, quantityQNT, comment));
            }

            @Override
            boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer) transaction.getAttachment();
                Long unconfirmedAssetBalance = senderAccount.getUnconfirmedAssetBalanceQNT(attachment.getAssetId());
                if (unconfirmedAssetBalance != null && unconfirmedAssetBalance >= attachment.getQuantityQNT()) {
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
            void updateTotals(Transaction transaction, Map<Long, Long> accumulatedAmounts,
                              Map<Long, Map<Long, Long>> accumulatedAssetQuantities, Long accumulatedAmount) {
                Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer) transaction.getAttachment();
                Map<Long, Long> accountAccumulatedAssetQuantities = accumulatedAssetQuantities.get(transaction.getSenderId());
                if (accountAccumulatedAssetQuantities == null) {
                    accountAccumulatedAssetQuantities = new HashMap<>();
                    accumulatedAssetQuantities.put(transaction.getSenderId(), accountAccumulatedAssetQuantities);
                }
                Long assetAccumulatedAssetQuantities = accountAccumulatedAssetQuantities.get(attachment.getAssetId());
                if (assetAccumulatedAssetQuantities == null) {
                    assetAccumulatedAssetQuantities = 0L;
                }
                accountAccumulatedAssetQuantities.put(attachment.getAssetId(),
                        Convert.safeAdd(assetAccumulatedAssetQuantities, attachment.getQuantityQNT()));
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer)transaction.getAttachment();
                if (transaction.getAmountNQT() != 0
                        || attachment.getComment().length() > Constants.MAX_ASSET_TRANSFER_COMMENT_LENGTH
                        || attachment.getAssetId() == null) {
                    throw new NxtException.ValidationException("Invalid asset transfer amount or comment: " + attachment.getJSONObject());
                }
                Asset asset = Asset.getAsset(attachment.getAssetId());
                if (asset == null || attachment.getQuantityQNT() <= 0 || attachment.getQuantityQNT() > asset.getQuantityQNT()) {
                    throw new NxtException.ValidationException("Invalid asset transfer asset or quantity: " + attachment.getJSONObject());
                }
            }

        };

        abstract static class ColoredCoinsOrderPlacement extends ColoredCoins {

            abstract Attachment.ColoredCoinsOrderPlacement makeAttachment(Long asset, long quantityQNT, long priceNQT);

            @Override
            final void doLoadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                Long assetId = Convert.zeroToNull(buffer.getLong());
                long quantityQNT = buffer.getLong();
                long priceNQT = buffer.getLong();
                transaction.setAttachment(makeAttachment(assetId, quantityQNT, priceNQT));
            }

            @Override
            final void doLoadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                Long assetId = Convert.parseUnsignedLong((String) attachmentData.get("asset"));
                long quantityQNT = (Long) attachmentData.get("quantityQNT");
                long priceNQT = (Long) attachmentData.get("priceNQT");
                transaction.setAttachment(makeAttachment(assetId, quantityQNT, priceNQT));
            }

            @Override
            final void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.ColoredCoinsOrderPlacement attachment = (Attachment.ColoredCoinsOrderPlacement)transaction.getAttachment();
                if (! Genesis.CREATOR_ID.equals(transaction.getRecipientId()) || transaction.getAmountNQT() != 0
                        || attachment.getPriceNQT() <= 0 || attachment.getPriceNQT() > Constants.MAX_BALANCE_NQT
                        || attachment.getAssetId() == null) {
                    throw new NxtException.ValidationException("Invalid asset order placement: " + attachment.getJSONObject());
                }
                Asset asset = Asset.getAsset(attachment.getAssetId());
                if (asset == null || attachment.getQuantityQNT() <= 0 || attachment.getQuantityQNT() > asset.getQuantityQNT()) {
                    throw new NxtException.ValidationException("Invalid asset order placement asset or quantity: " + attachment.getJSONObject());
                }
            }

        }

        public static final TransactionType ASK_ORDER_PLACEMENT = new ColoredCoins.ColoredCoinsOrderPlacement() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT;
            }

            final Attachment.ColoredCoinsOrderPlacement makeAttachment(Long assetId, long quantityQNT, long priceNQT) {
                return new Attachment.ColoredCoinsAskOrderPlacement(assetId, quantityQNT, priceNQT);
            }

            @Override
            boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.ColoredCoinsAskOrderPlacement attachment = (Attachment.ColoredCoinsAskOrderPlacement) transaction.getAttachment();
                Long unconfirmedAssetBalance = senderAccount.getUnconfirmedAssetBalanceQNT(attachment.getAssetId());
                if (unconfirmedAssetBalance != null && unconfirmedAssetBalance >= attachment.getQuantityQNT()) {
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
            void updateTotals(Transaction transaction, Map<Long, Long> accumulatedAmounts,
                              Map<Long, Map<Long, Long>> accumulatedAssetQuantities, Long accumulatedAmount) {
                Attachment.ColoredCoinsAskOrderPlacement attachment = (Attachment.ColoredCoinsAskOrderPlacement) transaction.getAttachment();
                Map<Long, Long> accountAccumulatedAssetQuantities = accumulatedAssetQuantities.get(transaction.getSenderId());
                if (accountAccumulatedAssetQuantities == null) {
                    accountAccumulatedAssetQuantities = new HashMap<>();
                    accumulatedAssetQuantities.put(transaction.getSenderId(), accountAccumulatedAssetQuantities);
                }
                Long assetAccumulatedAssetQuantities = accountAccumulatedAssetQuantities.get(attachment.getAssetId());
                if (assetAccumulatedAssetQuantities == null) {
                    assetAccumulatedAssetQuantities = 0L;
                }
                accountAccumulatedAssetQuantities.put(attachment.getAssetId(),
                        Convert.safeAdd(assetAccumulatedAssetQuantities, attachment.getQuantityQNT()));
            }

        };

        public final static TransactionType BID_ORDER_PLACEMENT = new ColoredCoins.ColoredCoinsOrderPlacement() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT;
            }

            final Attachment.ColoredCoinsOrderPlacement makeAttachment(Long assetId, long quantityQNT, long priceNQT) {
                return new Attachment.ColoredCoinsBidOrderPlacement(assetId, quantityQNT, priceNQT);
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
            void updateTotals(Transaction transaction, Map<Long, Long> accumulatedAmounts,
                              Map<Long, Map<Long, Long>> accumulatedAssetQuantities, Long accumulatedAmount) {
                Attachment.ColoredCoinsBidOrderPlacement attachment = (Attachment.ColoredCoinsBidOrderPlacement) transaction.getAttachment();
                accumulatedAmounts.put(transaction.getSenderId(),
                        Convert.safeAdd(accumulatedAmount, Convert.safeMultiply(attachment.getQuantityQNT(), attachment.getPriceNQT())));
            }

        };

        abstract static class ColoredCoinsOrderCancellation extends ColoredCoins {

            @Override
            final void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                if (! Genesis.CREATOR_ID.equals(transaction.getRecipientId()) || transaction.getAmountNQT() != 0) {
                    throw new NxtException.ValidationException("Invalid asset order cancellation amount or recipient");
                }
                Attachment.ColoredCoinsOrderCancellation attachment = (Attachment.ColoredCoinsOrderCancellation) transaction.getAttachment();
                if (attachment.getOrderId() == null) {
                    throw new NxtException.ValidationException("Invalid order cancellation attachment: " + attachment.getJSONObject());
                }
                doValidateAttachment(transaction);
            }

            abstract void doValidateAttachment(Transaction transaction) throws NxtException.ValidationException;

            @Override
            final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                return true;
            }

            @Override
            final void updateTotals(Transaction transaction, Map<Long, Long> accumulatedAmounts,
                                    Map<Long, Map<Long, Long>> accumulatedAssetQuantities, Long accumulatedAmount) {
            }

            @Override
            final void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                throw new UndoNotSupportedException("Reversal of order cancellation not supported");
            }

            @Override
            final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            }

        }

        public static final TransactionType ASK_ORDER_CANCELLATION = new ColoredCoins.ColoredCoinsOrderCancellation() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION;
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                transaction.setAttachment(new Attachment.ColoredCoinsAskOrderCancellation(Convert.zeroToNull(buffer.getLong())));
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                transaction.setAttachment(new Attachment.ColoredCoinsAskOrderCancellation(
                        Convert.parseUnsignedLong((String) attachmentData.get("order"))));
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
                    throw new NxtException.ValidationException("Invalid ask order: " + Convert.toUnsignedLong(attachment.getOrderId()));
                }
            }

        };

        public static final TransactionType BID_ORDER_CANCELLATION = new ColoredCoins.ColoredCoinsOrderCancellation() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION;
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                transaction.setAttachment(new Attachment.ColoredCoinsBidOrderCancellation(Convert.zeroToNull(buffer.getLong())));
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                transaction.setAttachment(new Attachment.ColoredCoinsBidOrderCancellation(
                        Convert.parseUnsignedLong((String) attachmentData.get("order"))));
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
                    throw new NxtException.ValidationException("Invalid bid order: " + Convert.toUnsignedLong(attachment.getOrderId()));
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
        void updateTotals(Transaction transaction, Map<Long, Long> accumulatedAmounts,
                          Map<Long, Map<Long, Long>> accumulatedAssetQuantities, Long accumulatedAmount) {
        }

        @Override
        final void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            if (Nxt.getBlockchain().getLastBlock().getHeight() < Constants.DIGITAL_GOODS_STORE_BLOCK) {
                throw new NotYetEnabledException("Digital goods listing not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
            }
            if (!Genesis.CREATOR_ID.equals(transaction.getRecipientId())
                    || transaction.getAmountNQT() != 0) {
                throw new NxtException.ValidationException("Invalid digital goods transaction");
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
            void doLoadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                String name = readString(buffer, buffer.getShort(), Constants.MAX_DGS_LISTING_NAME_LENGTH);
                String description = readString(buffer, buffer.getShort(), Constants.MAX_DGS_LISTING_DESCRIPTION_LENGTH);
                String tags = readString(buffer, buffer.getShort(), Constants.MAX_DGS_LISTING_TAGS_LENGTH);
                int quantity = buffer.getInt();
                long priceNQT = buffer.getLong();
                transaction.setAttachment(new Attachment.DigitalGoodsListing(name, description, tags, quantity, priceNQT));
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                String name = (String) attachmentData.get("name");
                String description = (String) attachmentData.get("description");
                String tags = (String) attachmentData.get("tags");
                int quantity = ((Long) attachmentData.get("quantity")).intValue();
                long priceNQT = (Long) attachmentData.get("priceNQT");
                transaction.setAttachment(new Attachment.DigitalGoodsListing(name, description, tags, quantity, priceNQT));
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
                    throw new NxtException.ValidationException("Invalid digital goods listing: " + attachment.getJSONObject());
                }
            }

        };

        public static final TransactionType DELISTING = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_DELISTING;
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                Long goodsId = buffer.getLong();
                transaction.setAttachment(new Attachment.DigitalGoodsDelisting(goodsId));
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                Long goodsId = Convert.parseUnsignedLong((String)attachmentData.get("goods"));
                transaction.setAttachment(new Attachment.DigitalGoodsDelisting(goodsId));
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
                if (goods == null || goods.isDelisted()
                        || !transaction.getSenderId().equals(goods.getSellerId())) {
                    throw new NxtException.ValidationException("Invalid digital goods delisting: " + attachment.getJSONObject());
                }
            }

        };

        public static final TransactionType PRICE_CHANGE = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_PRICE_CHANGE;
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                Long goodsId = buffer.getLong();
                long priceNQT = buffer.getLong();
                transaction.setAttachment(new Attachment.DigitalGoodsPriceChange(goodsId, priceNQT));
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                Long goodsId = Convert.parseUnsignedLong((String)attachmentData.get("goods"));
                long priceNQT = (Long)attachmentData.get("priceNQT");
                transaction.setAttachment(new Attachment.DigitalGoodsPriceChange(goodsId, priceNQT));
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
                        || goods == null || goods.isDelisted()
                        || !transaction.getSenderId().equals(goods.getSellerId())) {
                    throw new NxtException.ValidationException("Invalid digital goods price change: " + attachment.getJSONObject());
                }
            }

        };

        public static final TransactionType QUANTITY_CHANGE = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_QUANTITY_CHANGE;
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                Long goodsId = buffer.getLong();
                int deltaQuantity = buffer.getInt();
                transaction.setAttachment(new Attachment.DigitalGoodsQuantityChange(goodsId, deltaQuantity));
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                Long goodsId = Convert.parseUnsignedLong((String)attachmentData.get("goods"));
                int deltaQuantity = ((Long)attachmentData.get("deltaQuantity")).intValue();
                transaction.setAttachment(new Attachment.DigitalGoodsQuantityChange(goodsId, deltaQuantity));
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
                if (goods == null || goods.isDelisted()
                        || attachment.getDeltaQuantity() < -Constants.MAX_DGS_LISTING_QUANTITY
                        || attachment.getDeltaQuantity() > Constants.MAX_DGS_LISTING_QUANTITY
                        || ! transaction.getSenderId().equals(goods.getSellerId())) {
                    throw new NxtException.ValidationException("Invalid digital goods quantity change: " + attachment.getJSONObject());
                }
            }

        };

        public static final TransactionType PURCHASE = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_PURCHASE;
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                Long goodsId = buffer.getLong();
                int quantity = buffer.getInt();
                long priceNQT = buffer.getLong();
                int deliveryDeadline = buffer.getInt();
                EncryptedData note = readEncryptedData(buffer, buffer.getShort(), Constants.MAX_DGS_NOTE_LENGTH);
                transaction.setAttachment(new Attachment.DigitalGoodsPurchase(goodsId, quantity, priceNQT, deliveryDeadline, note));
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                Long goodsId = Convert.parseUnsignedLong((String)attachmentData.get("goods"));
                int quantity = ((Long)attachmentData.get("quantity")).intValue();
                long priceNQT = (Long)attachmentData.get("priceNQT");
                int deliveryDeadlineTimestamp = ((Long)attachmentData.get("deliveryDeadlineTimestamp")).intValue();
                EncryptedData note = new EncryptedData(Convert.parseHexString((String)attachmentData.get("note")),
                        Convert.parseHexString((String)attachmentData.get("noteNonce")));
                transaction.setAttachment(new Attachment.DigitalGoodsPurchase(goodsId, quantity, priceNQT,
                        deliveryDeadlineTimestamp, note));
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
            void updateTotals(Transaction transaction, Map<Long, Long> accumulatedAmounts,
                              Map<Long, Map<Long, Long>> accumulatedAssetQuantities, Long accumulatedAmount) {
                Attachment.DigitalGoodsPurchase attachment = (Attachment.DigitalGoodsPurchase) transaction.getAttachment();
                accumulatedAmounts.put(transaction.getSenderId(),
                        Convert.safeAdd(accumulatedAmount, Convert.safeMultiply(attachment.getQuantity(), attachment.getPriceNQT())));
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsPurchase attachment = (Attachment.DigitalGoodsPurchase) transaction.getAttachment();
                DigitalGoodsStore.purchase(transaction.getId(), transaction.getSenderId(), attachment.getGoodsId(),
                        attachment.getQuantity(), attachment.getPriceNQT(), attachment.getDeliveryDeadlineTimestamp(),
                        attachment.getNote(), transaction.getTimestamp());
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
                        || attachment.getNote().getData().length > 3 * Constants.MAX_DGS_NOTE_LENGTH
                        || attachment.getNote().getNonce().length != 32
                        || goods == null || goods.isDelisted()
                        || attachment.getQuantity() > goods.getQuantity()
                        || attachment.getPriceNQT() != goods.getPriceNQT()
                        || attachment.getDeliveryDeadlineTimestamp() <= Nxt.getBlockchain().getLastBlock().getTimestamp()) {
                    throw new NxtException.ValidationException("Invalid digital goods purchase: " + attachment.getJSONObject());
                }
            }

        };

        public static final TransactionType DELIVERY = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_DELIVERY;
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                Long purchaseId = buffer.getLong();
                EncryptedData goods = readEncryptedData(buffer, buffer.getShort(), Constants.MAX_DGS_GOODS_LENGTH);
                long discountNQT = buffer.getLong();
                transaction.setAttachment(new Attachment.DigitalGoodsDelivery(purchaseId, goods, discountNQT));
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                Long purchaseId = Convert.parseUnsignedLong((String)attachmentData.get("purchase"));
                EncryptedData goods = new EncryptedData(Convert.parseHexString((String)attachmentData.get("goodsData")),
                        Convert.parseHexString((String)attachmentData.get("goodsNonce")));
                long discountNQT = (Long)attachmentData.get("discountNQT");
                transaction.setAttachment(new Attachment.DigitalGoodsDelivery(purchaseId, goods, discountNQT));
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsDelivery attachment = (Attachment.DigitalGoodsDelivery)transaction.getAttachment();
                DigitalGoodsStore.deliver(transaction.getSenderId(), attachment.getPurchaseId(),
                        attachment.getDiscountNQT(), attachment.getGoods());
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
                        || attachment.getGoods().getNonce().length != 32
                        || attachment.getDiscountNQT() < 0 || attachment.getDiscountNQT() > Constants.MAX_BALANCE_NQT
                        || purchase == null
                        || attachment.getDiscountNQT() > purchase.getPriceNQT()
                        || !transaction.getSenderId().equals(purchase.getSellerId())) {
                    throw new NxtException.ValidationException("Invalid digital goods delivery: " + attachment.getJSONObject());
                }
            }

        };

        public static final TransactionType FEEDBACK = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_FEEDBACK;
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                Long purchaseId = buffer.getLong();
                EncryptedData note = readEncryptedData(buffer, buffer.getShort(), Constants.MAX_DGS_NOTE_LENGTH);
                transaction.setAttachment(new Attachment.DigitalGoodsFeedback(purchaseId, note));
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                Long purchaseId = Convert.parseUnsignedLong((String)attachmentData.get("purchase"));
                EncryptedData note = new EncryptedData(Convert.parseHexString((String)attachmentData.get("note")),
                        Convert.parseHexString((String)attachmentData.get("noteNonce")));
                transaction.setAttachment(new Attachment.DigitalGoodsFeedback(purchaseId, note));
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsFeedback attachment = (Attachment.DigitalGoodsFeedback)transaction.getAttachment();
                DigitalGoodsStore.feedback(attachment.getPurchaseId(), attachment.getNote());
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount)
                    throws UndoNotSupportedException {
                Attachment.DigitalGoodsFeedback attachment = (Attachment.DigitalGoodsFeedback)transaction.getAttachment();
                DigitalGoodsStore.undoFeedback(attachment.getPurchaseId());
            }

            @Override
            void doValidateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.DigitalGoodsFeedback attachment = (Attachment.DigitalGoodsFeedback) transaction.getAttachment();
                DigitalGoodsStore.Purchase purchase = DigitalGoodsStore.getPurchase(attachment.getPurchaseId());
                if (attachment.getNote().getData().length > Constants.MAX_DGS_NOTE_LENGTH
                        || attachment.getNote().getNonce().length != 32
                        || purchase == null
                        || purchase.getFeedbackNote() != null
                        || ! transaction.getSenderId().equals(purchase.getBuyerId())) {
                    throw new NxtException.ValidationException("Invalid digital goods feedback: " + attachment.getJSONObject());
                }
            }

        };

        public static final TransactionType REFUND = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_REFUND;
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                Long purchaseId = buffer.getLong();
                long refundNQT = buffer.getLong();
                EncryptedData note = readEncryptedData(buffer, buffer.getShort(), Constants.MAX_DGS_NOTE_LENGTH);
                transaction.setAttachment(new Attachment.DigitalGoodsRefund(purchaseId, refundNQT, note));
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                Long purchaseId = Convert.parseUnsignedLong((String)attachmentData.get("purchase"));
                long refundNQT = (Long)attachmentData.get("refundNQT");
                EncryptedData note = new EncryptedData(Convert.parseHexString((String)attachmentData.get("note")),
                        Convert.parseHexString((String)attachmentData.get("noteNonce")));
                transaction.setAttachment(new Attachment.DigitalGoodsRefund(purchaseId, refundNQT, note));
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
                        attachment.getRefundNQT(), attachment.getNote());
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                Attachment.DigitalGoodsRefund attachment = (Attachment.DigitalGoodsRefund) transaction.getAttachment();
                DigitalGoodsStore.undoRefund(transaction.getSenderId(), attachment.getPurchaseId(), attachment.getRefundNQT());
            }

            @Override
            void updateTotals(Transaction transaction, Map<Long, Long> accumulatedAmounts,
                              Map<Long, Map<Long, Long>> accumulatedAssetQuantities, Long accumulatedAmount) {
                Attachment.DigitalGoodsRefund attachment = (Attachment.DigitalGoodsRefund) transaction.getAttachment();
                accumulatedAmounts.put(transaction.getSenderId(),
                        Convert.safeAdd(accumulatedAmount, attachment.getRefundNQT()));
            }

            @Override
            void doValidateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.DigitalGoodsRefund attachment = (Attachment.DigitalGoodsRefund) transaction.getAttachment();
                DigitalGoodsStore.Purchase purchase = DigitalGoodsStore.getPurchase(attachment.getPurchaseId());
                if (attachment.getRefundNQT() < 0 || attachment.getRefundNQT() > Constants.MAX_BALANCE_NQT
                        || attachment.getNote().getData().length > Constants.MAX_DGS_NOTE_LENGTH
                        || attachment.getNote().getNonce().length != 32
                        || purchase == null
                        || purchase.getRefundNote() != null
                        || ! transaction.getSenderId().equals(purchase.getSellerId())) {
                    throw new NxtException.ValidationException("Invalid digital goods refund: " + attachment.getJSONObject());
                }
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
        final void updateTotals(Transaction transaction, Map<Long, Long> accumulatedAmounts,
                                Map<Long, Map<Long, Long>> accumulatedAssetQuantities, Long accumulatedAmount) {}

        public static final TransactionType EFFECTIVE_BALANCE_LEASING = new AccountControl() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_ACCOUNT_CONTROL_EFFECTIVE_BALANCE_LEASING;
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                short period = buffer.getShort();
                transaction.setAttachment(new Attachment.AccountControlEffectiveBalanceLeasing(period));
            }

            @Override
            void doLoadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                short period = ((Long) attachmentData.get("period")).shortValue();
                transaction.setAttachment(new Attachment.AccountControlEffectiveBalanceLeasing(period));
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
                Attachment.AccountControlEffectiveBalanceLeasing attachment = (Attachment.AccountControlEffectiveBalanceLeasing)transaction.getAttachment();
                Account recipientAccount = Account.getAccount(transaction.getRecipientId());
                if (transaction.getRecipientId().equals(transaction.getSenderId())
                        || transaction.getAmountNQT() != 0
                        || attachment.getPeriod() < 1440
                        || recipientAccount == null
                        || (recipientAccount.getPublicKey() == null && ! transaction.getStringId().equals("5081403377391821646"))) {
                    throw new NxtException.ValidationException("Invalid effective balance leasing: "
                            + transaction.getJSONObject() + " transaction " + transaction.getStringId());
                }
            }

        };

    }

    private static String readString(ByteBuffer buffer, int numBytes, int maxLength) throws NxtException.ValidationException {
        if (numBytes > 3 * maxLength) {
            throw new NxtException.ValidationException("Max parameter length exceeded");
        }
        byte[] bytes = new byte[numBytes];
        buffer.get(bytes);
        return Convert.toString(bytes);
    }

    private static EncryptedData readEncryptedData(ByteBuffer buffer, int noteBytesLength, int maxLength)
            throws NxtException.ValidationException {
        if (noteBytesLength == 0) {
            return EncryptedData.EMPTY_DATA;
        }
        if (noteBytesLength > 3 * maxLength) {
            throw new NxtException.ValidationException("Max note length exceeded");
        }
        byte[] noteBytes = new byte[noteBytesLength];
        buffer.get(noteBytes);
        byte[] noteNonceBytes = new byte[32];
        buffer.get(noteNonceBytes);
        return new EncryptedData(noteBytes, noteNonceBytes);
    }

    public static final class UndoNotSupportedException extends NxtException {

        UndoNotSupportedException(String message) {
            super(message);
        }

    }

    public static final class NotYetEnabledException extends NxtException.ValidationException {

        NotYetEnabledException(String message) {
            super(message);
        }
    }
}
