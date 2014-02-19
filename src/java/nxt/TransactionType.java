package nxt;

import nxt.util.Convert;
import nxt.util.Logger;
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

    abstract boolean loadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException;

    abstract boolean loadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException;

    // return true iff double spending
    final boolean isDoubleSpending(Transaction transaction, Account senderAccount, int totalAmount) {
        if (senderAccount.getUnconfirmedBalance() < totalAmount * 100L) {
            return true;
        }
        senderAccount.addToUnconfirmedBalance(- totalAmount * 100L);
        return checkDoubleSpending(transaction, senderAccount, totalAmount);
    }

    abstract boolean checkDoubleSpending(Transaction transaction, Account senderAccount, int totalAmount);

    abstract void apply(Transaction transaction, Account senderAccount, Account recipientAccount);

    abstract void undo(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException;

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
            final boolean loadAttachment(TransactionImpl transaction, ByteBuffer buffer) {
                return validateAttachment(transaction);
            }

            @Override
            final boolean loadAttachment(TransactionImpl transaction, JSONObject attachmentData) {
                return validateAttachment(transaction);
            }

            @Override
            void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
                recipientAccount.addToBalanceAndUnconfirmedBalance(transaction.getAmount() * 100L);
            }

            @Override
            void undo(Transaction transaction, Account senderAccount, Account recipientAccount) {
                recipientAccount.addToBalanceAndUnconfirmedBalance(-transaction.getAmount() * 100L);
            }

            @Override
            void updateTotals(Transaction transaction, Map<Long, Long> accumulatedAmounts,
                              Map<Long, Map<Long, Long>> accumulatedAssetQuantities, Long accumulatedAmount) {}

            @Override
            boolean checkDoubleSpending(Transaction transaction, Account senderAccount, int totalAmount) {
                return false;
            }

            private boolean validateAttachment(Transaction transaction) {
                return transaction.getAmount() > 0 && transaction.getAmount() < Nxt.MAX_BALANCE;
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
        boolean checkDoubleSpending(Transaction transaction, Account senderAccount, int totalAmount) {
            return false;
        }

        @Override
        void updateTotals(Transaction transaction, Map<Long, Long> accumulatedAmounts,
                          Map<Long, Map<Long, Long>> accumulatedAssetQuantities, Long accumulatedAmount) {}

        public final static TransactionType ARBITRARY_MESSAGE = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ARBITRARY_MESSAGE;
            }

            @Override
            boolean loadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                int messageLength = buffer.getInt();
                if (messageLength <= Nxt.MAX_ARBITRARY_MESSAGE_LENGTH) {
                    byte[] message = new byte[messageLength];
                    buffer.get(message);
                    transaction.setAttachment(new Attachment.MessagingArbitraryMessage(message));
                    return validateAttachment(transaction);
                }
                return false;
            }

            @Override
            boolean loadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                String message = (String)attachmentData.get("message");
                transaction.setAttachment(new Attachment.MessagingArbitraryMessage(Convert.parseHexString(message)));
                return validateAttachment(transaction);
            }

            @Override
            void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {}

            @Override
            void undo(Transaction transaction, Account senderAccount, Account recipientAccount) {}

            private boolean validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                if (Blockchain.getLastBlock().getHeight() < Nxt.ARBITRARY_MESSAGES_BLOCK) {
                    throw new NotYetEnabledException("Arbitrary messages not yet enabled at height " + Blockchain.getLastBlock().getHeight());
                }
                try {
                    Attachment.MessagingArbitraryMessage attachment = (Attachment.MessagingArbitraryMessage)transaction.getAttachment();
                    return transaction.getAmount() == 0 && attachment.getMessage().length <= Nxt.MAX_ARBITRARY_MESSAGE_LENGTH;
                } catch (RuntimeException e) {
                    Logger.logDebugMessage("Error validating arbitrary message", e);
                    return false;
                }
            }

        };

        public static final TransactionType ALIAS_ASSIGNMENT = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT;
            }

            @Override
            boolean loadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                int aliasLength = buffer.get();
                if (aliasLength > Nxt.MAX_ALIAS_LENGTH * 3) {
                    throw new NxtException.ValidationException("Max alias length exceeded");
                }
                byte[] alias = new byte[aliasLength];
                buffer.get(alias);
                int uriLength = buffer.getShort();
                if (uriLength > Nxt.MAX_ALIAS_URI_LENGTH * 3) {
                    throw new NxtException.ValidationException("Max alias URI length exceeded");
                }
                byte[] uri = new byte[uriLength];
                buffer.get(uri);
                try {
                    transaction.setAttachment(new Attachment.MessagingAliasAssignment(new String(alias, "UTF-8"),
                            new String(uri, "UTF-8")));
                    return validateAttachment(transaction);
                } catch (RuntimeException|UnsupportedEncodingException e) {
                    Logger.logDebugMessage("Error parsing alias assignment", e);
                }
                return false;
            }

            @Override
            boolean loadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                String alias = (String)attachmentData.get("alias");
                String uri = (String)attachmentData.get("uri");
                transaction.setAttachment(new Attachment.MessagingAliasAssignment(alias, uri));
                return validateAttachment(transaction);
            }

            @Override
            void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingAliasAssignment attachment = (Attachment.MessagingAliasAssignment)transaction.getAttachment();
                Block block = transaction.getBlock();
                Alias.addOrUpdateAlias(senderAccount, transaction.getId(), attachment.getAliasName(), attachment.getAliasURI(), block.getTimestamp());
            }

            @Override
            void undo(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
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

            private boolean validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                if (Blockchain.getLastBlock().getHeight() < Nxt.ALIAS_SYSTEM_BLOCK) {
                    throw new NotYetEnabledException("Aliases not yet enabled at height " + Blockchain.getLastBlock().getHeight());
                }
                try {
                    Attachment.MessagingAliasAssignment attachment = (Attachment.MessagingAliasAssignment)transaction.getAttachment();
                    if (! Genesis.CREATOR_ID.equals(transaction.getRecipientId()) || transaction.getAmount() != 0 || attachment.getAliasName().length() == 0
                            || attachment.getAliasName().length() > Nxt.MAX_ALIAS_LENGTH || attachment.getAliasURI().length() > Nxt.MAX_ALIAS_URI_LENGTH) {
                        return false;
                    } else {
                        String normalizedAlias = attachment.getAliasName().toLowerCase();
                        for (int i = 0; i < normalizedAlias.length(); i++) {
                            if (Nxt.alphabet.indexOf(normalizedAlias.charAt(i)) < 0) {
                                return false;
                            }
                        }
                        Alias alias = Alias.getAlias(normalizedAlias);
                        return alias == null || Arrays.equals(alias.getAccount().getPublicKey(), transaction.getSenderPublicKey());
                    }
                } catch (RuntimeException e) {
                    Logger.logDebugMessage("Error in alias assignment validation", e);
                    return false;
                }
            }

        };

        public final static TransactionType POLL_CREATION = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_POLL_CREATION;
            }

            @Override
            boolean loadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NotYetEnabledException {

                String pollName, pollDescription;
                String[] pollOptions;
                byte minNumberOfOptions, maxNumberOfOptions;
                boolean optionsAreBinary;

                try {
                    int pollNameBytesLength = buffer.getShort();
                    if (pollNameBytesLength > 4 * Nxt.MAX_POLL_NAME_LENGTH) {
                        Logger.logDebugMessage("Error parsing poll name");
                        return false;
                    }
                    byte[] pollNameBytes = new byte[pollNameBytesLength];
                    buffer.get(pollNameBytes);
                    pollName = (new String(pollNameBytes, "UTF-8")).trim();
                } catch (RuntimeException | UnsupportedEncodingException e) {
                    Logger.logDebugMessage("Error parsing poll name", e);
                    return false;
                }

                try {
                    int pollDescriptionBytesLength = buffer.getShort();
                    if (pollDescriptionBytesLength > 4 * Nxt.MAX_POLL_DESCRIPTION_LENGTH) {
                        Logger.logDebugMessage("Error parsing poll description");
                        return false;
                    }
                    byte[] pollDescriptionBytes = new byte[pollDescriptionBytesLength];
                    buffer.get(pollDescriptionBytes);
                    pollDescription = (new String(pollDescriptionBytes, "UTF-8")).trim();
                } catch (RuntimeException | UnsupportedEncodingException e) {
                    Logger.logDebugMessage("Error parsing poll description", e);
                    return false;
                }

                try {
                    int numberOfOptions = buffer.get();
                    pollOptions = new String[numberOfOptions];
                    for (int i = 0; i < numberOfOptions; i++) {
                        int pollOptionBytesLength = buffer.getShort();
                        if (pollOptionBytesLength > 4 * Nxt.MAX_POLL_OPTION_LENGTH) {
                            Logger.logDebugMessage("Error parsing poll options");
                            return false;
                        }
                        byte[] pollOptionBytes = new byte[pollOptionBytesLength];
                        buffer.get(pollOptionBytes);
                        pollOptions[i] = (new String(pollOptionBytes, "UTF-8")).trim();
                    }
                } catch (RuntimeException | UnsupportedEncodingException e) {
                    Logger.logDebugMessage("Error parsing poll options", e);
                    return false;
                }

                try {
                    minNumberOfOptions = buffer.get();
                    maxNumberOfOptions = buffer.get();
                    optionsAreBinary = buffer.get() != 0;
                } catch (RuntimeException e) {
                    Logger.logDebugMessage("Error parsing poll creation parameters", e);
                    return false;
                }

                transaction.setAttachment(new Attachment.MessagingPollCreation(pollName, pollDescription, pollOptions,
                        minNumberOfOptions, maxNumberOfOptions, optionsAreBinary));
                return validateAttachment(transaction);

            }

            @Override
            boolean loadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NotYetEnabledException {

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
                return validateAttachment(transaction);

            }

            @Override
            void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingPollCreation attachment = (Attachment.MessagingPollCreation)transaction.getAttachment();
                Poll.addPoll(transaction.getId(), attachment.getPollName(), attachment.getPollDescription(), attachment.getPollOptions(),
                        attachment.getMinNumberOfOptions(), attachment.getMaxNumberOfOptions(), attachment.isOptionsAreBinary());
            }

            @Override
            void undo(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                throw new UndoNotSupportedException(transaction, "Reversal of poll creation not supported");
            }

            private boolean validateAttachment(Transaction transaction) throws NotYetEnabledException {
                if (Blockchain.getLastBlock().getHeight() < Nxt.VOTING_SYSTEM_BLOCK) {
                    throw new NotYetEnabledException("Voting System not yet enabled at height " + Blockchain.getLastBlock().getHeight());
                }
                try {
                    Attachment.MessagingPollCreation attachment = (Attachment.MessagingPollCreation)transaction.getAttachment();
                    for (int i = 0; i < attachment.getPollOptions().length; i++) {
                        if (attachment.getPollOptions()[i].length() > Nxt.MAX_POLL_OPTION_LENGTH) {
                            return false;
                        }
                    }
                    return attachment.getPollName().length() <= Nxt.MAX_POLL_NAME_LENGTH && attachment.getPollDescription().length() <= Nxt.MAX_POLL_DESCRIPTION_LENGTH
                            && attachment.getPollOptions().length <= 100 && transaction.getAmount() == 0 && Genesis.CREATOR_ID.equals(transaction.getRecipientId());
                } catch (RuntimeException e) {
                    Logger.logDebugMessage("Error validating poll creation", e);
                    return false;
                }
            }

        };

        public final static TransactionType VOTE_CASTING = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_VOTE_CASTING;
            }

            @Override
            boolean loadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {

                Long pollId;
                byte[] pollVote;

                try {
                    pollId = buffer.getLong();
                    int numberOfOptions = buffer.get();
                    if (numberOfOptions > 100) {
                        Logger.logDebugMessage("Error parsing vote casting parameters");
                        return false;
                    }
                    pollVote = new byte[numberOfOptions];
                    buffer.get(pollVote);
                } catch (RuntimeException e) {
                    Logger.logDebugMessage("Error parsing vote casting parameters", e);
                    return false;
                }

                transaction.setAttachment(new Attachment.MessagingVoteCasting(pollId, pollVote));
                return validateAttachment(transaction);

            }

            @Override
            boolean loadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {

                Long pollId = (Long)attachmentData.get("pollId");
                JSONArray vote = (JSONArray)attachmentData.get("vote");
                byte[] pollVote = new byte[vote.size()];
                for (int i = 0; i < pollVote.length; i++) {
                    pollVote[i] = ((Long)vote.get(i)).byteValue();
                }

                transaction.setAttachment(new Attachment.MessagingVoteCasting(pollId, pollVote));
                return validateAttachment(transaction);

            }

            @Override
            void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingVoteCasting attachment = (Attachment.MessagingVoteCasting)transaction.getAttachment();
                Poll poll = Poll.getPoll(attachment.getPollId());
                if (poll != null) {
                    Vote vote = Vote.addVote(transaction.getId(), attachment.getPollId(), transaction.getSenderId(), attachment.getPollVote());
                    poll.addVoter(transaction.getSenderId(), vote.getId());
                }
            }

            @Override
            void undo(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                throw new UndoNotSupportedException(transaction, "Reversal of vote casting not supported");
            }

            private boolean validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                if (Blockchain.getLastBlock().getHeight() < Nxt.VOTING_SYSTEM_BLOCK) {
                    throw new NotYetEnabledException("Voting System not yet enabled at height " + Blockchain.getLastBlock().getHeight());
                }
                try {
                    return transaction.getAmount() == 0 && Genesis.CREATOR_ID.equals(transaction.getRecipientId());
                } catch (RuntimeException e) {
                    Logger.logDebugMessage("Error validating vote casting", e);
                    return false;
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
            boolean loadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                int nameLength = buffer.get();
                if (nameLength > 30) {
                    throw new NxtException.ValidationException("Max asset name length exceeded");
                }
                byte[] name = new byte[nameLength];
                buffer.get(name);
                int descriptionLength = buffer.getShort();
                if (descriptionLength > 300) {
                    throw new NxtException.ValidationException("Max asset description length exceeded");
                }
                byte[] description = new byte[descriptionLength];
                buffer.get(description);
                int quantity = buffer.getInt();
                try {
                    transaction.setAttachment(new Attachment.ColoredCoinsAssetIssuance(new String(name, "UTF-8").intern(),
                            new String(description, "UTF-8").intern(), quantity));
                    return validateAttachment(transaction);
                } catch (RuntimeException|UnsupportedEncodingException e) {
                    Logger.logDebugMessage("Error in asset issuance", e);
                }
                return false;
            }

            @Override
            boolean loadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                String name = (String)attachmentData.get("name");
                String description = (String)attachmentData.get("description");
                int quantity = ((Long)attachmentData.get("quantity")).intValue();
                transaction.setAttachment(new Attachment.ColoredCoinsAssetIssuance(name.trim(), description.trim(), quantity));
                return validateAttachment(transaction);
            }

            @Override
            boolean checkDoubleSpending(Transaction transaction, Account senderAccount, int totalAmount) {
                return false;
            }

            @Override
            void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsAssetIssuance attachment = (Attachment.ColoredCoinsAssetIssuance)transaction.getAttachment();
                Long assetId = transaction.getId();
                Asset.addAsset(assetId, transaction.getSenderId(), attachment.getName(), attachment.getDescription(), attachment.getQuantity());
                senderAccount.addToAssetAndUnconfirmedAssetBalance(assetId, attachment.getQuantity());
            }

            @Override
            void undo(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsAssetIssuance attachment = (Attachment.ColoredCoinsAssetIssuance)transaction.getAttachment();
                Long assetId = transaction.getId();
                senderAccount.addToAssetAndUnconfirmedAssetBalance(assetId, -attachment.getQuantity());
                Asset.removeAsset(assetId);
            }

            @Override
            void updateTotals(Transaction transaction, Map<Long, Long> accumulatedAmounts,
                             Map<Long, Map<Long, Long>> accumulatedAssetQuantities, Long accumulatedAmount) {}

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Set<String>> duplicates) {
                Set<String> myDuplicates = duplicates.get(this);
                if (myDuplicates == null) {
                    myDuplicates = new HashSet<>();
                    duplicates.put(this, myDuplicates);
                }
                Attachment.ColoredCoinsAssetIssuance attachment = (Attachment.ColoredCoinsAssetIssuance)transaction.getAttachment();
                return ! myDuplicates.add(attachment.getName().toLowerCase());
            }

            private boolean validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                if (Blockchain.getLastBlock().getHeight() < Nxt.ASSET_EXCHANGE_BLOCK) {
                    throw new NotYetEnabledException("Asset Exchange not yet enabled at height " + Blockchain.getLastBlock().getHeight());
                }
                try {
                    Attachment.ColoredCoinsAssetIssuance attachment = (Attachment.ColoredCoinsAssetIssuance)transaction.getAttachment();
                    if (!Genesis.CREATOR_ID.equals(transaction.getRecipientId()) || transaction.getAmount() != 0 || transaction.getFee() < Nxt.ASSET_ISSUANCE_FEE
                            || attachment.getName().length() < 3 || attachment.getName().length() > 10 || attachment.getDescription().length() > 1000
                            || attachment.getQuantity() <= 0 || attachment.getQuantity() > Nxt.MAX_ASSET_QUANTITY) {
                        return false;
                    } else {
                        String normalizedName = attachment.getName().toLowerCase();
                        for (int i = 0; i < normalizedName.length(); i++) {
                            if (Nxt.alphabet.indexOf(normalizedName.charAt(i)) < 0) {
                                return false;
                            }
                        }
                        return Asset.getAsset(normalizedName) == null;
                    }
                } catch (RuntimeException e) {
                    Logger.logDebugMessage("Error validating colored coins asset issuance", e);
                    return false;
                }
            }

        };

        public static final TransactionType ASSET_TRANSFER = new ColoredCoins() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COLORED_COINS_ASSET_TRANSFER;
            }

            @Override
            boolean loadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                Long assetId = Convert.zeroToNull(buffer.getLong());
                int quantity = buffer.getInt();
                transaction.setAttachment(new Attachment.ColoredCoinsAssetTransfer(assetId, quantity));
                return validateAttachment(transaction);
            }

            @Override
            boolean loadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                Long assetId = Convert.parseUnsignedLong((String) attachmentData.get("asset"));
                int quantity = ((Long)attachmentData.get("quantity")).intValue();
                transaction.setAttachment(new Attachment.ColoredCoinsAssetTransfer(assetId, quantity));
                return validateAttachment(transaction);
            }

            @Override
            boolean checkDoubleSpending(Transaction transaction, Account account, int totalAmount) {
                Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer)transaction.getAttachment();
                Integer unconfirmedAssetBalance = account.getUnconfirmedAssetBalance(attachment.getAssetId());
                if (unconfirmedAssetBalance == null || unconfirmedAssetBalance < attachment.getQuantity()) {
                    account.addToUnconfirmedBalance(totalAmount * 100L);
                    return true;
                } else {
                    account.addToUnconfirmedAssetBalance(attachment.getAssetId(), -attachment.getQuantity());
                    return false;
                }
            }

            @Override
            void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer)transaction.getAttachment();
                senderAccount.addToAssetAndUnconfirmedAssetBalance(attachment.getAssetId(), -attachment.getQuantity());
                recipientAccount.addToAssetAndUnconfirmedAssetBalance(attachment.getAssetId(), attachment.getQuantity());
            }

            @Override
            void undo(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer)transaction.getAttachment();
                senderAccount.addToAssetAndUnconfirmedAssetBalance(attachment.getAssetId(), attachment.getQuantity());
                recipientAccount.addToAssetAndUnconfirmedAssetBalance(attachment.getAssetId(), -attachment.getQuantity());
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
                accountAccumulatedAssetQuantities.put(attachment.getAssetId(), assetAccumulatedAssetQuantities + attachment.getQuantity());
            }

            private boolean validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                if (Blockchain.getLastBlock().getHeight() < Nxt.ASSET_EXCHANGE_BLOCK) {
                    throw new NotYetEnabledException("Asset Exchange not yet enabled at height " + Blockchain.getLastBlock().getHeight());
                }
                Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer)transaction.getAttachment();
                return transaction.getAmount() == 0 && attachment.getQuantity() > 0 && attachment.getQuantity() <= Nxt.MAX_ASSET_QUANTITY;
            }

        };

        abstract static class ColoredCoinsOrderPlacement extends ColoredCoins {

            abstract Attachment.ColoredCoinsOrderPlacement makeAttachment(Long asset, int quantity, long price);

            @Override
            final boolean loadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                Long assetId = Convert.zeroToNull(buffer.getLong());
                int quantity = buffer.getInt();
                long price = buffer.getLong();
                transaction.setAttachment(makeAttachment(assetId, quantity, price));
                return validateAttachment(transaction);
            }

            @Override
            final boolean loadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                Long assetId = Convert.parseUnsignedLong((String) attachmentData.get("asset"));
                int quantity = ((Long)attachmentData.get("quantity")).intValue();
                long price = (Long)attachmentData.get("price");
                transaction.setAttachment(makeAttachment(assetId, quantity, price));
                return validateAttachment(transaction);
            }

            private boolean validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                if (Blockchain.getLastBlock().getHeight() < Nxt.ASSET_EXCHANGE_BLOCK) {
                    throw new NotYetEnabledException("Asset Exchange not yet enabled at height " + Blockchain.getLastBlock().getHeight());
                }
                Attachment.ColoredCoinsOrderPlacement attachment = (Attachment.ColoredCoinsOrderPlacement)transaction.getAttachment();
                return Genesis.CREATOR_ID.equals(transaction.getRecipientId()) && transaction.getAmount() == 0
                        && attachment.getQuantity() > 0 && attachment.getQuantity() <= Nxt.MAX_ASSET_QUANTITY
                        && attachment.getPrice() > 0 && attachment.getPrice() <= Nxt.MAX_BALANCE * 100L;
            }

        }

        public static final TransactionType ASK_ORDER_PLACEMENT = new ColoredCoins.ColoredCoinsOrderPlacement() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT;
            }

            final Attachment.ColoredCoinsOrderPlacement makeAttachment(Long assetId, int quantity, long price) {
                return new Attachment.ColoredCoinsAskOrderPlacement(assetId, quantity, price);
            }

            @Override
            boolean checkDoubleSpending(Transaction transaction, Account account, int totalAmount) {
                Attachment.ColoredCoinsAskOrderPlacement attachment = (Attachment.ColoredCoinsAskOrderPlacement)transaction.getAttachment();
                Integer unconfirmedAssetBalance = account.getUnconfirmedAssetBalance(attachment.getAssetId());
                if (unconfirmedAssetBalance == null || unconfirmedAssetBalance < attachment.getQuantity()) {
                    account.addToUnconfirmedBalance(totalAmount * 100L);
                    return true;
                } else {
                    account.addToUnconfirmedAssetBalance(attachment.getAssetId(), -attachment.getQuantity());
                    return false;
                }
            }

            @Override
            void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsAskOrderPlacement attachment = (Attachment.ColoredCoinsAskOrderPlacement)transaction.getAttachment();
                if (Asset.getAsset(attachment.getAssetId()) != null) {
                    Order.Ask.addOrder(transaction.getId(), senderAccount, attachment.getAssetId(), attachment.getQuantity(), attachment.getPrice());
                }
            }

            @Override
            void undo(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                Attachment.ColoredCoinsAskOrderPlacement attachment = (Attachment.ColoredCoinsAskOrderPlacement)transaction.getAttachment();
                Order.Ask askOrder = Order.Ask.removeOrder(transaction.getId());
                if (askOrder == null || askOrder.getQuantity() != attachment.getQuantity() || ! askOrder.getAssetId().equals(attachment.getAssetId())) {
                    //undoing of partially filled orders not supported yet
                    throw new UndoNotSupportedException(transaction, "Ask order already filled");
                }
                senderAccount.addToAssetAndUnconfirmedAssetBalance(attachment.getAssetId(), attachment.getQuantity());
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
                accountAccumulatedAssetQuantities.put(attachment.getAssetId(), assetAccumulatedAssetQuantities + attachment.getQuantity());
            }

        };

        public final static TransactionType BID_ORDER_PLACEMENT = new ColoredCoins.ColoredCoinsOrderPlacement() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT;
            }

            final Attachment.ColoredCoinsOrderPlacement makeAttachment(Long asset, int quantity, long price) {
                return new Attachment.ColoredCoinsBidOrderPlacement(asset, quantity, price);
            }

            @Override
            boolean checkDoubleSpending(Transaction transaction, Account account, int totalAmount) {
                Attachment.ColoredCoinsBidOrderPlacement attachment = (Attachment.ColoredCoinsBidOrderPlacement) transaction.getAttachment();
                if (account.getUnconfirmedBalance() < attachment.getQuantity() * attachment.getPrice()) {
                    account.addToUnconfirmedBalance(totalAmount * 100L);
                    return true;
                } else {
                    account.addToUnconfirmedBalance(-attachment.getQuantity() * attachment.getPrice());
                    return false;
                }
            }

            @Override
            void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsBidOrderPlacement attachment = (Attachment.ColoredCoinsBidOrderPlacement)transaction.getAttachment();
                if (Asset.getAsset(attachment.getAssetId()) != null) {
                    Order.Bid.addOrder(transaction.getId(), senderAccount, attachment.getAssetId(), attachment.getQuantity(), attachment.getPrice());
                }
            }

            @Override
            void undo(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                Attachment.ColoredCoinsBidOrderPlacement attachment = (Attachment.ColoredCoinsBidOrderPlacement)transaction.getAttachment();
                Order.Bid bidOrder = Order.Bid.removeOrder(transaction.getId());
                if (bidOrder == null || bidOrder.getQuantity() != attachment.getQuantity() || ! bidOrder.getAssetId().equals(attachment.getAssetId())) {
                    //undoing of partially filled orders not supported yet
                    throw new UndoNotSupportedException(transaction, "Bid order already filled");
                }
                senderAccount.addToBalanceAndUnconfirmedBalance(attachment.getQuantity() * attachment.getPrice());
            }

            @Override
            void updateTotals(Transaction transaction, Map<Long, Long> accumulatedAmounts,
                             Map<Long, Map<Long, Long>> accumulatedAssetQuantities, Long accumulatedAmount) {
                Attachment.ColoredCoinsBidOrderPlacement attachment = (Attachment.ColoredCoinsBidOrderPlacement) transaction.getAttachment();
                accumulatedAmounts.put(transaction.getSenderId(), accumulatedAmount + attachment.getQuantity() * attachment.getPrice());
            }

        };

        abstract static class ColoredCoinsOrderCancellation extends ColoredCoins {

            final boolean validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                if (Blockchain.getLastBlock().getHeight() < Nxt.ASSET_EXCHANGE_BLOCK) {
                    throw new NotYetEnabledException("Asset Exchange not yet enabled at height " + Blockchain.getLastBlock().getHeight());
                }
                return Genesis.CREATOR_ID.equals(transaction.getRecipientId()) && transaction.getAmount() == 0;
            }

            @Override
            final boolean checkDoubleSpending(Transaction transaction, Account senderAccount, int totalAmount) {
                return false;
            }

            @Override
            final void updateTotals(Transaction transaction, Map<Long, Long> accumulatedAmounts,
                              Map<Long, Map<Long, Long>> accumulatedAssetQuantities, Long accumulatedAmount) {}

            @Override
            final void undo(Transaction transaction, Account senderAccount, Account recipientAccount) throws UndoNotSupportedException {
                throw new UndoNotSupportedException(transaction, "Reversal of order cancellation not supported");
            }

        }

        public static final TransactionType ASK_ORDER_CANCELLATION = new ColoredCoins.ColoredCoinsOrderCancellation() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION;
            }

            @Override
            boolean loadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                transaction.setAttachment(new Attachment.ColoredCoinsAskOrderCancellation(Convert.zeroToNull(buffer.getLong())));
                return validateAttachment(transaction);
            }

            @Override
            boolean loadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                transaction.setAttachment(new Attachment.ColoredCoinsAskOrderCancellation(Convert.parseUnsignedLong((String) attachmentData.get("order"))));
                return validateAttachment(transaction);
            }

            @Override
            void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsAskOrderCancellation attachment = (Attachment.ColoredCoinsAskOrderCancellation)transaction.getAttachment();
                Order order = Order.Ask.removeOrder(attachment.getOrderId());
                if (order != null) {
                    senderAccount.addToAssetAndUnconfirmedAssetBalance(order.getAssetId(), order.getQuantity());
                }
            }

        };

        public static final TransactionType BID_ORDER_CANCELLATION = new ColoredCoins.ColoredCoinsOrderCancellation() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION;
            }

            @Override
            boolean loadAttachment(TransactionImpl transaction, ByteBuffer buffer) throws NxtException.ValidationException {
                transaction.setAttachment(new Attachment.ColoredCoinsBidOrderCancellation(Convert.zeroToNull(buffer.getLong())));
                return validateAttachment(transaction);
            }

            @Override
            boolean loadAttachment(TransactionImpl transaction, JSONObject attachmentData) throws NxtException.ValidationException {
                transaction.setAttachment(new Attachment.ColoredCoinsBidOrderCancellation(Convert.parseUnsignedLong((String) attachmentData.get("order"))));
                return validateAttachment(transaction);
            }

            @Override
            void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsBidOrderCancellation attachment = (Attachment.ColoredCoinsBidOrderCancellation)transaction.getAttachment();
                Order order = Order.Bid.removeOrder(attachment.getOrderId());
                if (order != null) {
                    senderAccount.addToBalanceAndUnconfirmedBalance(order.getQuantity() * order.getPrice());
                }
            }

        };
    }


    public static final class UndoNotSupportedException extends NxtException {

        private final Transaction transaction;

        public UndoNotSupportedException(Transaction transaction, String message) {
            super(message);
            this.transaction = transaction;
        }

        public Transaction getTransaction() {
            return transaction;
        }

    }

    public static final class NotYetEnabledException extends NxtException.ValidationException {

        public NotYetEnabledException(String message) {
            super(message);
        }

        public NotYetEnabledException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
