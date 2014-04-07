package nxt;

import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.UnsupportedEncodingException;
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
    private static final byte SUBTYPE_PAYMENT_ORDINARY_PAYMENT = 0;
    private static final byte SUBTYPE_MESSAGING_ARBITRARY_MESSAGE = 0;
    private static final byte SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT = 1;
    private static final byte SUBTYPE_MESSAGING_POLL_CREATION = 2;
    private static final byte SUBTYPE_MESSAGING_VOTE_CASTING = 3;
    private static final byte SUBTYPE_COLORED_COINS_ASSET_ISSUANCE = 0;
    private static final byte SUBTYPE_COLORED_COINS_ASSET_TRANSFER = 1;
    private static final byte SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT = 2;
    private static final byte SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT = 3;
    private static final byte SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION = 4;
    private static final byte SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION = 5;

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
            default:
                return null;
        }
    }

    private TransactionType() {}

    public abstract byte getType();

    public abstract byte getSubtype();

    abstract void loadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException;

    abstract void loadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException;

    abstract void validateAttachment(TransactionImpl transaction) throws NxtException.ValidationException;

    // return false iff double spending
    final boolean applyUnconfirmed(Transaction transaction, Account senderAccount) {
        long totalAmountNQT = Convert.safeAdd(transaction.getAmountNQT(), transaction.getFeeNQT());
        if (senderAccount.getUnconfirmedBalanceNQT() < totalAmountNQT
                && ! (transaction.getTimestamp() == 0 && Arrays.equals(senderAccount.getPublicKey(), Genesis.CREATOR_PUBLIC_KEY))) {
            return false;
        }
        senderAccount.addToUnconfirmedBalanceNQT(- totalAmountNQT);
        if (! applyAttachmentUnconfirmed(transaction, senderAccount)) {
            senderAccount.addToUnconfirmedBalanceNQT(totalAmountNQT);
            return false;
        }
        return true;
    }

    abstract boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount);

    final void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
        senderAccount.addToBalanceNQT(- (Convert.safeAdd(transaction.getAmountNQT(), transaction.getFeeNQT())));
        applyAttachment(transaction, senderAccount, recipientAccount);
    }

    abstract void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount);

    final void undoUnconfirmed(Transaction transaction, Account senderAccount) {
        senderAccount.addToUnconfirmedBalanceNQT(Convert.safeAdd(transaction.getAmountNQT(), transaction.getFeeNQT()));
        undoAttachmentUnconfirmed(transaction, senderAccount);
    }

    abstract void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount);

    final void undo(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
        senderAccount.addToBalanceNQT(Convert.safeAdd(transaction.getAmountNQT(), transaction.getFeeNQT()));
        undoAttachment(transaction, senderAccount, recipientAccount);
    }

    abstract void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException;

    abstract void updateTotals(Transaction transaction, Map<Long, Long> accumulatedAmounts,
                               Map<Long, Map<Long, Long>> accumulatedAssetQuantities, Long accumulatedAmount);

    boolean isDuplicate(Transaction transaction, Map<TransactionType, Set<String>> duplicates) {
        return false;
    }

    public static abstract class Payment extends TransactionType {

        private Payment() {}

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
            final void loadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                validateAttachment(transaction);
            }

            @Override
            final void loadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                validateAttachment(transaction);
            }

            @Override
            boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                return true;
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                recipientAccount.addToBalanceAndUnconfirmedBalanceNQT(transaction.getAmountNQT());
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                recipientAccount.addToBalanceAndUnconfirmedBalanceNQT(-transaction.getAmountNQT());
            }

            @Override
            void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {}

            @Override
            void updateTotals(Transaction transaction, Map<Long, Long> accumulatedAmounts,
                              Map<Long, Map<Long, Long>> accumulatedAssetQuantities, Long accumulatedAmount) {}

            @Override
            void validateAttachment(TransactionImpl transaction) throws NxtException.ValidationException {
                if (transaction.getAmountNQT() <= 0 || transaction.getAmountNQT() >= Constants.MAX_BALANCE_NQT) {
                    throw new NxtException.ValidationException("Invalid ordinary payment: " + transaction.getAttachment().getJSONObject());
                }
            }

        };
    }

    public static abstract class Messaging extends TransactionType {

        private Messaging() {}

        @Override
        public final byte getType() {
            return TransactionType.TYPE_MESSAGING;
        }

        @Override
        final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {}

        @Override
        final void updateTotals(Transaction transaction, Map<Long, Long> accumulatedAmounts,
                          Map<Long, Map<Long, Long>> accumulatedAssetQuantities, Long accumulatedAmount) {}

        public final static TransactionType ARBITRARY_MESSAGE = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ARBITRARY_MESSAGE;
            }

            @Override
            void loadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                int messageLength = buffer.getInt();
                if (messageLength > Constants.MAX_ARBITRARY_MESSAGE_LENGTH) {
                    throw new NxtException.ValidationException("Invalid arbitrary message length: " + messageLength);
                }
                byte[] message = new byte[messageLength];
                buffer.get(message);
                transaction.setAttachment(new Attachment.MessagingArbitraryMessage(message));
                validateAttachment(transaction);
            }

            @Override
            void loadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                String message = (String)attachmentData.get("message");
                transaction.setAttachment(new Attachment.MessagingArbitraryMessage(Convert.parseHexString(message)));
                validateAttachment(transaction);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {}

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {}

            @Override
            void validateAttachment(TransactionImpl transaction) throws NxtException.ValidationException {
                if (Nxt.getBlockchain().getLastBlock().getHeight() < Constants.ARBITRARY_MESSAGES_BLOCK) {
                    throw new NotYetEnabledException("Arbitrary messages not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
                }
                Attachment.MessagingArbitraryMessage attachment = (Attachment.MessagingArbitraryMessage)transaction.getAttachment();
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
            void loadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                int aliasLength = buffer.get();
                if (aliasLength > Constants.MAX_ALIAS_LENGTH * 3) {
                    throw new NxtException.ValidationException("Max alias length exceeded");
                }
                byte[] alias = new byte[aliasLength];
                buffer.get(alias);
                int uriLength = buffer.getShort();
                if (uriLength > Constants.MAX_ALIAS_URI_LENGTH * 3) {
                    throw new NxtException.ValidationException("Max alias URI length exceeded");
                }
                byte[] uri = new byte[uriLength];
                buffer.get(uri);
                try {
                    transaction.setAttachment(new Attachment.MessagingAliasAssignment(new String(alias, "UTF-8"),
                            new String(uri, "UTF-8")));
                    validateAttachment(transaction);
                } catch (RuntimeException|UnsupportedEncodingException e) {
                    throw new NxtException.ValidationException(e.toString());
                }
            }

            @Override
            void loadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                String alias = (String)attachmentData.get("alias");
                String uri = (String)attachmentData.get("uri");
                transaction.setAttachment(new Attachment.MessagingAliasAssignment(alias, uri));
                validateAttachment(transaction);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingAliasAssignment attachment = (Attachment.MessagingAliasAssignment)transaction.getAttachment();
                Alias.addOrUpdateAlias(senderAccount, transaction.getId(), attachment.getAliasName(),
                        attachment.getAliasURI(), transaction.getBlockTimestamp());
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                // can't tell whether Alias existed before and what was its previous uri
                throw new UndoNotSupportedException(transaction, "Reversal of alias assignment not supported");
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Set<String>> duplicates) {
                Set<String> myDuplicates = duplicates.get(this);
                if (myDuplicates == null) {
                    myDuplicates = new HashSet<>();
                    duplicates.put(this, myDuplicates);
                }
                Attachment.MessagingAliasAssignment attachment = (Attachment.MessagingAliasAssignment)transaction.getAttachment();
                return ! myDuplicates.add(attachment.getAliasName().toLowerCase());
            }

            @Override
            void validateAttachment(TransactionImpl transaction) throws NxtException.ValidationException {
                if (Nxt.getBlockchain().getLastBlock().getHeight() < Constants.ALIAS_SYSTEM_BLOCK) {
                    throw new NotYetEnabledException("Aliases not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
                }
                Attachment.MessagingAliasAssignment attachment = (Attachment.MessagingAliasAssignment)transaction.getAttachment();
                if (! Genesis.CREATOR_ID.equals(transaction.getRecipientId()) || transaction.getAmountNQT() != 0
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
                if (alias != null && ! Arrays.equals(alias.getAccount().getPublicKey(), transaction.getSenderPublicKey())) {
                    throw new NxtException.ValidationException("Alias already owned by another account: " + normalizedAlias);
                }
            }

        };

        public final static TransactionType POLL_CREATION = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_POLL_CREATION;
            }

            @Override
            void loadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {

                String pollName, pollDescription;
                String[] pollOptions;
                byte minNumberOfOptions, maxNumberOfOptions;
                boolean optionsAreBinary;

                try {
                    int pollNameBytesLength = buffer.getShort();
                    if (pollNameBytesLength > 4 * Constants.MAX_POLL_NAME_LENGTH) {
                        throw new NxtException.ValidationException("Error parsing poll name");
                    }
                    byte[] pollNameBytes = new byte[pollNameBytesLength];
                    buffer.get(pollNameBytes);
                    pollName = (new String(pollNameBytes, "UTF-8")).trim();
                } catch (RuntimeException | UnsupportedEncodingException e) {
                    throw new NxtException.ValidationException("Error parsing poll name", e);
                }

                try {
                    int pollDescriptionBytesLength = buffer.getShort();
                    if (pollDescriptionBytesLength > 4 * Constants.MAX_POLL_DESCRIPTION_LENGTH) {
                        throw new NxtException.ValidationException("Error parsing poll description");
                    }
                    byte[] pollDescriptionBytes = new byte[pollDescriptionBytesLength];
                    buffer.get(pollDescriptionBytes);
                    pollDescription = (new String(pollDescriptionBytes, "UTF-8")).trim();
                } catch (RuntimeException | UnsupportedEncodingException e) {
                    throw new NxtException.ValidationException("Error parsing poll name", e);
                }

                try {
                    int numberOfOptions = buffer.get();
                    if (numberOfOptions > Constants.MAX_POLL_OPTION_COUNT) {
                        throw new NxtException.ValidationException("Invalid number of poll options: " + numberOfOptions);
                    }
                    pollOptions = new String[numberOfOptions];
                    for (int i = 0; i < numberOfOptions; i++) {
                        int pollOptionBytesLength = buffer.getShort();
                        if (pollOptionBytesLength > 4 * Constants.MAX_POLL_OPTION_LENGTH) {
                            throw new NxtException.ValidationException("Error parsing poll options");
                        }
                        byte[] pollOptionBytes = new byte[pollOptionBytesLength];
                        buffer.get(pollOptionBytes);
                        pollOptions[i] = (new String(pollOptionBytes, "UTF-8")).trim();
                    }
                } catch (RuntimeException | UnsupportedEncodingException e) {
                    throw new NxtException.ValidationException("Error parsing poll options", e);
                }

                try {
                    minNumberOfOptions = buffer.get();
                    maxNumberOfOptions = buffer.get();
                    optionsAreBinary = buffer.get() != 0;
                } catch (RuntimeException e) {
                    throw new NxtException.ValidationException("Error parsing poll creation parameters", e);
                }

                transaction.setAttachment(new Attachment.MessagingPollCreation(pollName, pollDescription, pollOptions,
                        minNumberOfOptions, maxNumberOfOptions, optionsAreBinary));
                validateAttachment(transaction);

            }

            @Override
            void loadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {

                String pollName = ((String)attachmentData.get("name")).trim();
                String pollDescription = ((String)attachmentData.get("description")).trim();
                JSONArray options = (JSONArray)attachmentData.get("options");
                String[] pollOptions = new String[options.size()];
                for (int i = 0; i < pollOptions.length; i++) {
                    pollOptions[i] = ((String)options.get(i)).trim();
                }
                byte minNumberOfOptions = ((Long)attachmentData.get("minNumberOfOptions")).byteValue();
                byte maxNumberOfOptions = ((Long)attachmentData.get("maxNumberOfOptions")).byteValue();
                boolean optionsAreBinary = (Boolean)attachmentData.get("optionsAreBinary");

                transaction.setAttachment(new Attachment.MessagingPollCreation(pollName, pollDescription, pollOptions,
                        minNumberOfOptions, maxNumberOfOptions, optionsAreBinary));
                validateAttachment(transaction);

            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingPollCreation attachment = (Attachment.MessagingPollCreation)transaction.getAttachment();
                Poll.addPoll(transaction.getId(), attachment.getPollName(), attachment.getPollDescription(), attachment.getPollOptions(),
                        attachment.getMinNumberOfOptions(), attachment.getMaxNumberOfOptions(), attachment.isOptionsAreBinary());
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                throw new UndoNotSupportedException(transaction, "Reversal of poll creation not supported");
            }

            @Override
            void validateAttachment(TransactionImpl transaction) throws NxtException.ValidationException {
                if (Nxt.getBlockchain().getLastBlock().getHeight() < Constants.VOTING_SYSTEM_BLOCK) {
                    throw new NotYetEnabledException("Voting System not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
                }
                Attachment.MessagingPollCreation attachment = (Attachment.MessagingPollCreation)transaction.getAttachment();
                for (int i = 0; i < attachment.getPollOptions().length; i++) {
                    if (attachment.getPollOptions()[i].length() > Constants.MAX_POLL_OPTION_LENGTH) {
                        throw new NxtException.ValidationException("Invalid poll options length: " + attachment.getJSONObject());
                    }
                }
                if (attachment.getPollName().length() > Constants.MAX_POLL_NAME_LENGTH
                        || attachment.getPollDescription().length() > Constants.MAX_POLL_DESCRIPTION_LENGTH
                        || attachment.getPollOptions().length > Constants.MAX_POLL_OPTION_COUNT
                        || transaction.getAmountNQT() != 0
                        || ! Genesis.CREATOR_ID.equals(transaction.getRecipientId())) {
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
            void loadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {

                Long pollId;
                byte[] pollVote;

                try {
                    pollId = buffer.getLong();
                    int numberOfOptions = buffer.get();
                    if (numberOfOptions > Constants.MAX_POLL_OPTION_COUNT) {
                        throw new NxtException.ValidationException("Error parsing vote casting parameters");
                    }
                    pollVote = new byte[numberOfOptions];
                    buffer.get(pollVote);
                } catch (RuntimeException e) {
                    throw new NxtException.ValidationException("Error parsing vote casting parameters", e);
                }

                transaction.setAttachment(new Attachment.MessagingVoteCasting(pollId, pollVote));
                validateAttachment(transaction);

            }

            @Override
            void loadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {

                Long pollId = (Long)attachmentData.get("pollId");
                JSONArray vote = (JSONArray)attachmentData.get("vote");
                byte[] pollVote = new byte[vote.size()];
                for (int i = 0; i < pollVote.length; i++) {
                    pollVote[i] = ((Long)vote.get(i)).byteValue();
                }

                transaction.setAttachment(new Attachment.MessagingVoteCasting(pollId, pollVote));
                validateAttachment(transaction);

            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingVoteCasting attachment = (Attachment.MessagingVoteCasting)transaction.getAttachment();
                Poll poll = Poll.getPoll(attachment.getPollId());
                if (poll != null) {
                    Vote vote = Vote.addVote(transaction.getId(), attachment.getPollId(), transaction.getSenderId(),
                            attachment.getPollVote());
                    poll.addVoter(transaction.getSenderId(), vote.getId());
                }
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                throw new UndoNotSupportedException(transaction, "Reversal of vote casting not supported");
            }

            @Override
            void validateAttachment(TransactionImpl transaction) throws NxtException.ValidationException {
                if (Nxt.getBlockchain().getLastBlock().getHeight() < Constants.VOTING_SYSTEM_BLOCK) {
                    throw new NotYetEnabledException("Voting System not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
                }
                Attachment.MessagingVoteCasting attachment = (Attachment.MessagingVoteCasting)transaction.getAttachment();
                if (attachment.getPollId() == null || attachment.getPollVote() == null) {
                    throw new NxtException.ValidationException("Invalid vote casting attachment: " + attachment.getJSONObject());
                }
                if (transaction.getAmountNQT() != 0 || ! Genesis.CREATOR_ID.equals(transaction.getRecipientId())) {
                    throw new NxtException.ValidationException("Invalid vote casting amount or recipient");
                }
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
            void loadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                int nameLength = buffer.get();
                if (nameLength > 3 * Constants.MAX_ASSET_NAME_LENGTH) {
                    throw new NxtException.ValidationException("Max asset name length exceeded");
                }
                byte[] name = new byte[nameLength];
                buffer.get(name);
                int descriptionLength = buffer.getShort();
                if (descriptionLength > 3 * Constants.MAX_ASSET_DESCRIPTION_LENGTH) {
                    throw new NxtException.ValidationException("Max asset description length exceeded");
                }
                byte[] description = new byte[descriptionLength];
                buffer.get(description);
                long quantityQNT = buffer.getLong();
                byte decimals = buffer.get();
                try {
                    transaction.setAttachment(new Attachment.ColoredCoinsAssetIssuance(new String(name, "UTF-8").intern(),
                            new String(description, "UTF-8").intern(), quantityQNT, decimals));
                    validateAttachment(transaction);
                } catch (RuntimeException|UnsupportedEncodingException e) {
                    throw new NxtException.ValidationException("Error in asset issuance", e);
                }
            }

            @Override
            void loadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                String name = (String)attachmentData.get("name");
                String description = (String)attachmentData.get("description");
                long quantityQNT = (Long)attachmentData.get("quantityQNT");
                byte decimals = ((Long)attachmentData.get("decimals")).byteValue();
                transaction.setAttachment(new Attachment.ColoredCoinsAssetIssuance(name.trim(), description.trim(),
                        quantityQNT, decimals));
                validateAttachment(transaction);
            }

            @Override
            boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                return true;
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsAssetIssuance attachment = (Attachment.ColoredCoinsAssetIssuance)transaction.getAttachment();
                Long assetId = transaction.getId();
                Asset.addAsset(assetId, transaction.getSenderId(), attachment.getName(), attachment.getDescription(),
                        attachment.getQuantityQNT(), attachment.getDecimals());
                senderAccount.addToAssetAndUnconfirmedAssetBalanceQNT(assetId, attachment.getQuantityQNT());
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsAssetIssuance attachment = (Attachment.ColoredCoinsAssetIssuance)transaction.getAttachment();
                Long assetId = transaction.getId();
                senderAccount.addToAssetAndUnconfirmedAssetBalanceQNT(assetId, -attachment.getQuantityQNT());
                Asset.removeAsset(assetId);
            }

            @Override
            void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {}

            @Override
            void updateTotals(Transaction transaction, Map<Long, Long> accumulatedAmounts,
                             Map<Long, Map<Long, Long>> accumulatedAssetQuantities, Long accumulatedAmount) {}

            @Override
            void validateAttachment(TransactionImpl transaction) throws NxtException.ValidationException {
                if (Nxt.getBlockchain().getLastBlock().getHeight() < Constants.ASSET_EXCHANGE_BLOCK) {
                    throw new NotYetEnabledException("Asset Exchange not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
                }
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
            void loadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                Long assetId = Convert.zeroToNull(buffer.getLong());
                int quantity = buffer.getInt();
                int commentLength = buffer.getShort();
                if (commentLength > 3 * Constants.MAX_ASSET_TRANSFER_COMMENT_LENGTH) {
                    throw new NxtException.ValidationException("Max asset comment length exceeded");
                }
                byte[] comment = new byte[commentLength];
                buffer.get(comment);
                try {
                    transaction.setAttachment(new Attachment.ColoredCoinsAssetTransfer(assetId, quantity, new String(comment, "UTF-8").intern()));
                    validateAttachment(transaction);
                } catch (UnsupportedEncodingException e) {
                    throw new NxtException.ValidationException("Error in asset transfer", e);
                }
            }

            @Override
            void loadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                Long assetId = Convert.parseUnsignedLong((String) attachmentData.get("asset"));
                int quantity = ((Long)attachmentData.get("quantity")).intValue();
                String comment = (String)attachmentData.get("comment");
                transaction.setAttachment(new Attachment.ColoredCoinsAssetTransfer(assetId, quantity, comment));
                validateAttachment(transaction);
            }

            @Override
            boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer)transaction.getAttachment();
                Long unconfirmedAssetBalance = senderAccount.getUnconfirmedAssetBalanceQNT(attachment.getAssetId());
                if (unconfirmedAssetBalance != null && unconfirmedAssetBalance >= attachment.getQuantityQNT()) {
                    senderAccount.addToUnconfirmedAssetBalanceQNT(attachment.getAssetId(), -attachment.getQuantityQNT());
                    return true;
                }
                return false;
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer)transaction.getAttachment();
                senderAccount.addToAssetBalanceQNT(attachment.getAssetId(), -attachment.getQuantityQNT());
                recipientAccount.addToAssetAndUnconfirmedAssetBalanceQNT(attachment.getAssetId(), attachment.getQuantityQNT());
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer)transaction.getAttachment();
                senderAccount.addToAssetBalanceQNT(attachment.getAssetId(), attachment.getQuantityQNT());
                recipientAccount.addToAssetAndUnconfirmedAssetBalanceQNT(attachment.getAssetId(), -attachment.getQuantityQNT());
            }

            @Override
            void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer)transaction.getAttachment();
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
            void validateAttachment(TransactionImpl transaction) throws NxtException.ValidationException {
                if (Nxt.getBlockchain().getLastBlock().getHeight() < Constants.ASSET_EXCHANGE_BLOCK) {
                    throw new NotYetEnabledException("Asset Exchange not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
                }
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
            final void loadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                Long assetId = Convert.zeroToNull(buffer.getLong());
                long quantityQNT = buffer.getLong();
                long priceNQT = buffer.getLong();
                transaction.setAttachment(makeAttachment(assetId, quantityQNT, priceNQT));
                validateAttachment(transaction);
            }

            @Override
            final void loadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                Long assetId = Convert.parseUnsignedLong((String) attachmentData.get("asset"));
                long quantityQNT = (Long)attachmentData.get("quantityQNT");
                long priceNQT = (Long)attachmentData.get("priceNQT");
                transaction.setAttachment(makeAttachment(assetId, quantityQNT, priceNQT));
                validateAttachment(transaction);
            }

            @Override
            final void validateAttachment(TransactionImpl transaction) throws NxtException.ValidationException {
                if (Nxt.getBlockchain().getLastBlock().getHeight() < Constants.ASSET_EXCHANGE_BLOCK) {
                    throw new NotYetEnabledException("Asset Exchange not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
                }
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
                Attachment.ColoredCoinsAskOrderPlacement attachment = (Attachment.ColoredCoinsAskOrderPlacement)transaction.getAttachment();
                Long unconfirmedAssetBalance = senderAccount.getUnconfirmedAssetBalanceQNT(attachment.getAssetId());
                if (unconfirmedAssetBalance != null && unconfirmedAssetBalance >= attachment.getQuantityQNT()) {
                    senderAccount.addToUnconfirmedAssetBalanceQNT(attachment.getAssetId(), -attachment.getQuantityQNT());
                    return true;
                }
                return false;
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsAskOrderPlacement attachment = (Attachment.ColoredCoinsAskOrderPlacement)transaction.getAttachment();
                if (Asset.getAsset(attachment.getAssetId()) != null) {
                    Order.Ask.addOrder(transaction.getId(), senderAccount, attachment.getAssetId(),
                            attachment.getQuantityQNT(), attachment.getPriceNQT());
                }
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                Attachment.ColoredCoinsAskOrderPlacement attachment = (Attachment.ColoredCoinsAskOrderPlacement)transaction.getAttachment();
                Order.Ask askOrder = Order.Ask.removeOrder(transaction.getId());
                if (askOrder == null || askOrder.getQuantityQNT() != attachment.getQuantityQNT()
                        || ! askOrder.getAssetId().equals(attachment.getAssetId())) {
                    //undoing of partially filled orders not supported yet
                    throw new UndoNotSupportedException(transaction, "Ask order already filled");
                }
            }

            @Override
            void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.ColoredCoinsAskOrderPlacement attachment = (Attachment.ColoredCoinsAskOrderPlacement)transaction.getAttachment();
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
                    senderAccount.addToUnconfirmedBalanceNQT(- Convert.safeMultiply(attachment.getQuantityQNT(), attachment.getPriceNQT()));
                    return true;
                }
                return false;
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsBidOrderPlacement attachment = (Attachment.ColoredCoinsBidOrderPlacement)transaction.getAttachment();
                if (Asset.getAsset(attachment.getAssetId()) != null) {
                    Order.Bid.addOrder(transaction.getId(), senderAccount, attachment.getAssetId(),
                            attachment.getQuantityQNT(), attachment.getPriceNQT());
                }
            }

            @Override
            void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                Attachment.ColoredCoinsBidOrderPlacement attachment = (Attachment.ColoredCoinsBidOrderPlacement)transaction.getAttachment();
                Order.Bid bidOrder = Order.Bid.removeOrder(transaction.getId());
                if (bidOrder == null || bidOrder.getQuantityQNT() != attachment.getQuantityQNT()
                        || ! bidOrder.getAssetId().equals(attachment.getAssetId())) {
                    //undoing of partially filled orders not supported yet
                    throw new UndoNotSupportedException(transaction, "Bid order already filled");
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
            final void validateAttachment(TransactionImpl transaction) throws NxtException.ValidationException {
                if (Nxt.getBlockchain().getLastBlock().getHeight() < Constants.ASSET_EXCHANGE_BLOCK) {
                    throw new NotYetEnabledException("Asset Exchange not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
                }
                if (! Genesis.CREATOR_ID.equals(transaction.getRecipientId()) || transaction.getAmountNQT() != 0) {
                    throw new NxtException.ValidationException("Invalid asset order cancellation amount or recipient");
                }
                Attachment.ColoredCoinsOrderCancellation attachment = (Attachment.ColoredCoinsOrderCancellation)transaction.getAttachment();
                if (attachment.getOrderId() == null) {
                    throw new NxtException.ValidationException("Invalid order cancellation attachment: " + attachment.getJSONObject());
                }

            }

            @Override
            final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                return true;
            }

            @Override
            final void updateTotals(Transaction transaction, Map<Long, Long> accumulatedAmounts,
                              Map<Long, Map<Long, Long>> accumulatedAssetQuantities, Long accumulatedAmount) {}

            @Override
            final void undoAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                throw new UndoNotSupportedException(transaction, "Reversal of order cancellation not supported");
            }

            @Override
            final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {}

        }

        public static final TransactionType ASK_ORDER_CANCELLATION = new ColoredCoins.ColoredCoinsOrderCancellation() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION;
            }

            @Override
            void loadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                transaction.setAttachment(new Attachment.ColoredCoinsAskOrderCancellation(Convert.zeroToNull(buffer.getLong())));
                validateAttachment(transaction);
            }

            @Override
            void loadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                transaction.setAttachment(new Attachment.ColoredCoinsAskOrderCancellation(
                        Convert.parseUnsignedLong((String) attachmentData.get("order"))));
                validateAttachment(transaction);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsAskOrderCancellation attachment = (Attachment.ColoredCoinsAskOrderCancellation)transaction.getAttachment();
                Order order = Order.Ask.removeOrder(attachment.getOrderId());
                if (order != null) {
                    senderAccount.addToUnconfirmedAssetBalanceQNT(order.getAssetId(), order.getQuantityQNT());
                }
            }

        };

        public static final TransactionType BID_ORDER_CANCELLATION = new ColoredCoins.ColoredCoinsOrderCancellation() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION;
            }

            @Override
            void loadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                transaction.setAttachment(new Attachment.ColoredCoinsBidOrderCancellation(Convert.zeroToNull(buffer.getLong())));
                validateAttachment(transaction);
            }

            @Override
            void loadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                transaction.setAttachment(new Attachment.ColoredCoinsBidOrderCancellation(
                        Convert.parseUnsignedLong((String) attachmentData.get("order"))));
                validateAttachment(transaction);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsBidOrderCancellation attachment = (Attachment.ColoredCoinsBidOrderCancellation)transaction.getAttachment();
                Order order = Order.Bid.removeOrder(attachment.getOrderId());
                if (order != null) {
                    senderAccount.addToUnconfirmedBalanceNQT(Convert.safeMultiply(order.getQuantityQNT(), order.getPriceNQT()));
                }
            }

        };
    }


    public static final class UndoNotSupportedException extends NxtException {

        private final Transaction transaction;

        UndoNotSupportedException(Transaction transaction, String message) {
            super(message);
            this.transaction = transaction;
        }

        public Transaction getTransaction() {
            return transaction;
        }

    }

    public static final class NotYetEnabledException extends NxtException.ValidationException {

        NotYetEnabledException(String message) {
            super(message);
        }

    }

}
