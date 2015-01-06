package nxt;

import nxt.crypto.EncryptedData;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.regex.Pattern;

public interface Attachment extends Appendix {

    TransactionType getTransactionType();

    abstract static class AbstractAttachment extends AbstractAppendix implements Attachment {

        private AbstractAttachment(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
        }

        private AbstractAttachment(JSONObject attachmentData) {
            super(attachmentData);
        }

        private AbstractAttachment(int version) {
            super(version);
        }

        private AbstractAttachment() {}

        @Override
        final void validate(Transaction transaction) throws NxtException.ValidationException {
            getTransactionType().validateAttachment(transaction);
        }

        @Override
        final void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
            getTransactionType().apply(transaction, senderAccount, recipientAccount);
        }

    }

    abstract static class EmptyAttachment extends AbstractAttachment {

        private EmptyAttachment() {
            super(0);
        }

        @Override
        final int getMySize() {
            return 0;
        }

        @Override
        final void putMyBytes(ByteBuffer buffer) {
        }

        @Override
        final void putMyJSON(JSONObject json) {
        }

        @Override
        final boolean verifyVersion(byte transactionVersion) {
            return true;
        }

    }

    public final static EmptyAttachment ORDINARY_PAYMENT = new EmptyAttachment() {

        @Override
        String getAppendixName() {
            return "OrdinaryPayment";
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Payment.ORDINARY;
        }

    };

    // the message payload is in the Appendix
    public final static EmptyAttachment ARBITRARY_MESSAGE = new EmptyAttachment() {

        @Override
        String getAppendixName() {
            return "ArbitraryMessage";
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ARBITRARY_MESSAGE;
        }

    };

    public final static class MessagingAliasAssignment extends AbstractAttachment {

        private final String aliasName;
        private final String aliasURI;

        MessagingAliasAssignment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            super(buffer, transactionVersion);
            aliasName = Convert.readString(buffer, buffer.get(), Constants.MAX_ALIAS_LENGTH).trim();
            aliasURI = Convert.readString(buffer, buffer.getShort(), Constants.MAX_ALIAS_URI_LENGTH).trim();
        }

        MessagingAliasAssignment(JSONObject attachmentData) {
            super(attachmentData);
            aliasName = (Convert.nullToEmpty((String) attachmentData.get("alias"))).trim();
            aliasURI = (Convert.nullToEmpty((String) attachmentData.get("uri"))).trim();
        }

        public MessagingAliasAssignment(String aliasName, String aliasURI) {
            this.aliasName = aliasName.trim();
            this.aliasURI = aliasURI.trim();
        }

        @Override
        String getAppendixName() {
            return "AliasAssignment";
        }

        @Override
        int getMySize() {
            return 1 + Convert.toBytes(aliasName).length + 2 + Convert.toBytes(aliasURI).length;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            byte[] alias = Convert.toBytes(this.aliasName);
            byte[] uri = Convert.toBytes(this.aliasURI);
            buffer.put((byte)alias.length);
            buffer.put(alias);
            buffer.putShort((short) uri.length);
            buffer.put(uri);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("alias", aliasName);
            attachment.put("uri", aliasURI);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ALIAS_ASSIGNMENT;
        }

        public String getAliasName() {
            return aliasName;
        }

        public String getAliasURI() {
            return aliasURI;
        }
    }

    public final static class MessagingAliasSell extends AbstractAttachment {

        private final String aliasName;
        private final long priceNQT;

        MessagingAliasSell(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            super(buffer, transactionVersion);
            this.aliasName = Convert.readString(buffer, buffer.get(), Constants.MAX_ALIAS_LENGTH);
            this.priceNQT = buffer.getLong();
        }

        MessagingAliasSell(JSONObject attachmentData) {
            super(attachmentData);
            this.aliasName = Convert.nullToEmpty((String) attachmentData.get("alias"));
            this.priceNQT = Convert.parseLong(attachmentData.get("priceNQT"));
        }

        public MessagingAliasSell(String aliasName, long priceNQT) {
            this.aliasName = aliasName;
            this.priceNQT = priceNQT;
        }

        @Override
        String getAppendixName() {
            return "AliasSell";
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ALIAS_SELL;
        }

        @Override
        int getMySize() {
            return 1 + Convert.toBytes(aliasName).length + 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            byte[] aliasBytes = Convert.toBytes(aliasName);
            buffer.put((byte)aliasBytes.length);
            buffer.put(aliasBytes);
            buffer.putLong(priceNQT);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("alias", aliasName);
            attachment.put("priceNQT", priceNQT);
        }

        public String getAliasName(){
            return aliasName;
        }

        public long getPriceNQT(){
            return priceNQT;
        }
    }

    public final static class MessagingAliasBuy extends AbstractAttachment {

        private final String aliasName;

        MessagingAliasBuy(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            super(buffer, transactionVersion);
            this.aliasName = Convert.readString(buffer, buffer.get(), Constants.MAX_ALIAS_LENGTH);
        }

        MessagingAliasBuy(JSONObject attachmentData) {
            super(attachmentData);
            this.aliasName = Convert.nullToEmpty((String) attachmentData.get("alias"));
        }

        public MessagingAliasBuy(String aliasName) {
            this.aliasName = aliasName;
        }

        @Override
        String getAppendixName() {
            return "AliasBuy";
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ALIAS_BUY;
        }

        @Override
        int getMySize() {
            return 1 + Convert.toBytes(aliasName).length;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            byte[] aliasBytes = Convert.toBytes(aliasName);
            buffer.put((byte) aliasBytes.length);
            buffer.put(aliasBytes);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("alias", aliasName);
        }

        public String getAliasName(){
            return aliasName;
        }
    }

    public final static class MessagingAliasDelete extends AbstractAttachment {

        private final String aliasName;

        MessagingAliasDelete(final ByteBuffer buffer, final byte transactionVersion) throws NxtException.NotValidException {
            super(buffer, transactionVersion);
            this.aliasName = Convert.readString(buffer, buffer.get(), Constants.MAX_ALIAS_LENGTH);
        }

        MessagingAliasDelete(final JSONObject attachmentData) {
            super(attachmentData);
            this.aliasName = Convert.nullToEmpty((String) attachmentData.get("alias"));
        }

        public MessagingAliasDelete(final String aliasName) {
            this.aliasName = aliasName;
        }

        @Override
        String getAppendixName() {
            return "AliasDelete";
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ALIAS_DELETE;
        }

        @Override
        int getMySize() {
            return 1 + Convert.toBytes(aliasName).length;
        }

        @Override
        void putMyBytes(final ByteBuffer buffer) {
            byte[] aliasBytes = Convert.toBytes(aliasName);
            buffer.put((byte)aliasBytes.length);
            buffer.put(aliasBytes);
        }

        @Override
        void putMyJSON(final JSONObject attachment) {
            attachment.put("alias", aliasName);
        }

        public String getAliasName(){
            return aliasName;
        }
    }

    public final static class MessagingPollCreation extends AbstractAttachment {

        private final String pollName;
        private final String pollDescription;
        private final String[] pollOptions;
        private final byte minNumberOfOptions, maxNumberOfOptions;
        private final boolean optionsAreBinary;

        MessagingPollCreation(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            super(buffer, transactionVersion);
            this.pollName = Convert.readString(buffer, buffer.getShort(), Constants.MAX_POLL_NAME_LENGTH);
            this.pollDescription = Convert.readString(buffer, buffer.getShort(), Constants.MAX_POLL_DESCRIPTION_LENGTH);
            int numberOfOptions = buffer.get();
            if (numberOfOptions > Constants.MAX_POLL_OPTION_COUNT) {
                throw new NxtException.NotValidException("Invalid number of poll options: " + numberOfOptions);
            }
            this.pollOptions = new String[numberOfOptions];
            for (int i = 0; i < numberOfOptions; i++) {
                pollOptions[i] = Convert.readString(buffer, buffer.getShort(), Constants.MAX_POLL_OPTION_LENGTH);
            }
            this.minNumberOfOptions = buffer.get();
            this.maxNumberOfOptions = buffer.get();
            this.optionsAreBinary = buffer.get() != 0;
        }

        MessagingPollCreation(JSONObject attachmentData) {
            super(attachmentData);
            this.pollName = ((String) attachmentData.get("name")).trim();
            this.pollDescription = ((String) attachmentData.get("description")).trim();
            JSONArray options = (JSONArray) attachmentData.get("options");
            this.pollOptions = new String[options.size()];
            for (int i = 0; i < pollOptions.length; i++) {
                pollOptions[i] = ((String) options.get(i)).trim();
            }
            this.minNumberOfOptions = ((Long) attachmentData.get("minNumberOfOptions")).byteValue();
            this.maxNumberOfOptions = ((Long) attachmentData.get("maxNumberOfOptions")).byteValue();
            this.optionsAreBinary = (Boolean) attachmentData.get("optionsAreBinary");
        }

        public MessagingPollCreation(String pollName, String pollDescription, String[] pollOptions, byte minNumberOfOptions,
                                     byte maxNumberOfOptions, boolean optionsAreBinary) {
            this.pollName = pollName;
            this.pollDescription = pollDescription;
            this.pollOptions = pollOptions;
            this.minNumberOfOptions = minNumberOfOptions;
            this.maxNumberOfOptions = maxNumberOfOptions;
            this.optionsAreBinary = optionsAreBinary;
        }

        @Override
        String getAppendixName() {
            return "PollCreation";
        }

        @Override
        int getMySize() {
            int size = 2 + Convert.toBytes(pollName).length + 2 + Convert.toBytes(pollDescription).length + 1;
            for (String pollOption : pollOptions) {
                size += 2 + Convert.toBytes(pollOption).length;
            }
            size +=  1 + 1 + 1;
            return size;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            byte[] name = Convert.toBytes(this.pollName);
            byte[] description = Convert.toBytes(this.pollDescription);
            byte[][] options = new byte[this.pollOptions.length][];
            for (int i = 0; i < this.pollOptions.length; i++) {
                options[i] = Convert.toBytes(this.pollOptions[i]);
            }
            buffer.putShort((short)name.length);
            buffer.put(name);
            buffer.putShort((short)description.length);
            buffer.put(description);
            buffer.put((byte) options.length);
            for (byte[] option : options) {
                buffer.putShort((short) option.length);
                buffer.put(option);
            }
            buffer.put(this.minNumberOfOptions);
            buffer.put(this.maxNumberOfOptions);
            buffer.put(this.optionsAreBinary ? (byte)1 : (byte)0);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("name", this.pollName);
            attachment.put("description", this.pollDescription);
            JSONArray options = new JSONArray();
            if (this.pollOptions != null) {
                Collections.addAll(options, this.pollOptions);
            }
            attachment.put("options", options);
            attachment.put("minNumberOfOptions", this.minNumberOfOptions);
            attachment.put("maxNumberOfOptions", this.maxNumberOfOptions);
            attachment.put("optionsAreBinary", this.optionsAreBinary);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.POLL_CREATION;
        }

        public String getPollName() { return pollName; }

        public String getPollDescription() { return pollDescription; }

        public String[] getPollOptions() { return pollOptions; }

        public byte getMinNumberOfOptions() { return minNumberOfOptions; }

        public byte getMaxNumberOfOptions() { return maxNumberOfOptions; }

        public boolean isOptionsAreBinary() { return optionsAreBinary; }

    }

    public final static class MessagingVoteCasting extends AbstractAttachment {

        private final long pollId;
        private final byte[] pollVote;

        MessagingVoteCasting(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            super(buffer, transactionVersion);
            this.pollId = buffer.getLong();
            int numberOfOptions = buffer.get();
            if (numberOfOptions > Constants.MAX_POLL_OPTION_COUNT) {
                throw new NxtException.NotValidException("Error parsing vote casting parameters");
            }
            this.pollVote = new byte[numberOfOptions];
            buffer.get(pollVote);
        }

        MessagingVoteCasting(JSONObject attachmentData) {
            super(attachmentData);
            this.pollId = Convert.parseUnsignedLong((String)attachmentData.get("pollId"));
            JSONArray vote = (JSONArray)attachmentData.get("vote");
            this.pollVote = new byte[vote.size()];
            for (int i = 0; i < pollVote.length; i++) {
                pollVote[i] = ((Long) vote.get(i)).byteValue();
            }
        }

        public MessagingVoteCasting(long pollId, byte[] pollVote) {
            this.pollId = pollId;
            this.pollVote = pollVote;
        }

        @Override
        String getAppendixName() {
            return "VoteCasting";
        }

        @Override
        int getMySize() {
            return 8 + 1 + this.pollVote.length;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(this.pollId);
            buffer.put((byte) this.pollVote.length);
            buffer.put(this.pollVote);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("pollId", Convert.toUnsignedLong(this.pollId));
            JSONArray vote = new JSONArray();
            if (this.pollVote != null) {
                for (byte aPollVote : this.pollVote) {
                    vote.add(aPollVote);
                }
            }
            attachment.put("vote", vote);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.VOTE_CASTING;
        }

        public long getPollId() { return pollId; }

        public byte[] getPollVote() { return pollVote; }

    }

    public final static class MessagingHubAnnouncement extends AbstractAttachment {

        private final long minFeePerByteNQT;
        private final String[] uris;

        MessagingHubAnnouncement(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            super(buffer, transactionVersion);
            this.minFeePerByteNQT = buffer.getLong();
            int numberOfUris = buffer.get();
            if (numberOfUris > Constants.MAX_HUB_ANNOUNCEMENT_URIS) {
                throw new NxtException.NotValidException("Invalid number of URIs: " + numberOfUris);
            }
            this.uris = new String[numberOfUris];
            for (int i = 0; i < uris.length; i++) {
                uris[i] = Convert.readString(buffer, buffer.getShort(), Constants.MAX_HUB_ANNOUNCEMENT_URI_LENGTH);
            }
        }

        MessagingHubAnnouncement(JSONObject attachmentData) throws NxtException.NotValidException {
            super(attachmentData);
            this.minFeePerByteNQT = (Long) attachmentData.get("minFeePerByte");
            try {
                JSONArray urisData = (JSONArray) attachmentData.get("uris");
                this.uris = new String[urisData.size()];
                for (int i = 0; i < uris.length; i++) {
                    uris[i] = (String) urisData.get(i);
                }
            } catch (RuntimeException e) {
                throw new NxtException.NotValidException("Error parsing hub terminal announcement parameters", e);
            }
        }

        public MessagingHubAnnouncement(long minFeePerByteNQT, String[] uris) {
            this.minFeePerByteNQT = minFeePerByteNQT;
            this.uris = uris;
        }

        @Override
        String getAppendixName() {
            return "HubAnnouncement";
        }

        @Override
        int getMySize() {
            int size = 8 + 1;
            for (String uri : uris) {
                size += 2 + Convert.toBytes(uri).length;
            }
            return size;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(minFeePerByteNQT);
            buffer.put((byte) uris.length);
            for (String uri : uris) {
                byte[] uriBytes = Convert.toBytes(uri);
                buffer.putShort((short)uriBytes.length);
                buffer.put(uriBytes);
            }
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("minFeePerByteNQT", minFeePerByteNQT);
            JSONArray uris = new JSONArray();
            Collections.addAll(uris, this.uris);
            attachment.put("uris", uris);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.HUB_ANNOUNCEMENT;
        }

        public long getMinFeePerByteNQT() {
            return minFeePerByteNQT;
        }

        public String[] getUris() {
            return uris;
        }

    }

    public final static class MessagingAccountInfo extends AbstractAttachment {

        private final String name;
        private final String description;
        private final Pattern messagePattern;

        MessagingAccountInfo(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            super(buffer, transactionVersion);
            this.name = Convert.readString(buffer, buffer.get(), Constants.MAX_ACCOUNT_NAME_LENGTH);
            this.description = Convert.readString(buffer, buffer.getShort(), Constants.MAX_ACCOUNT_DESCRIPTION_LENGTH);
            if (getVersion() < 2) {
                this.messagePattern = null;
            } else {
                String regex = Convert.readString(buffer, buffer.getShort(), Constants.MAX_ACCOUNT_MESSAGE_PATTERN_LENGTH);
                if (regex.length() > 0) {
                    int flags = buffer.getInt();
                    this.messagePattern = Pattern.compile(regex, flags);
                } else {
                    this.messagePattern = null;
                }
            }
        }

        MessagingAccountInfo(JSONObject attachmentData) {
            super(attachmentData);
            this.name = Convert.nullToEmpty((String) attachmentData.get("name"));
            this.description = Convert.nullToEmpty((String) attachmentData.get("description"));
            if (getVersion() < 2) {
                this.messagePattern = null;
            } else {
                String regex = Convert.emptyToNull((String)attachmentData.get("messagePatternRegex"));
                if (regex != null) {
                    int flags = ((Long) attachmentData.get("messagePatternFlags")).intValue();
                    this.messagePattern = Pattern.compile(regex, flags);
                } else {
                    this.messagePattern = null;
                }
            }
        }

        public MessagingAccountInfo(String name, String description) {
            super(1);
            this.name = name;
            this.description = description;
            this.messagePattern = null;
        }

        /*
        public MessagingAccountInfo(String name, String description, Pattern messagePattern) {
            super(messagePattern == null ? 1 : 2);
            this.name = name;
            this.description = description;
            this.messagePattern = messagePattern;
        }
        */

        @Override
        String getAppendixName() {
            return "AccountInfo";
        }

        @Override
        int getMySize() {
            return 1 + Convert.toBytes(name).length + 2 + Convert.toBytes(description).length +
                    (getVersion() < 2 ? 0 : 2 + (messagePattern == null ? 0 : Convert.toBytes(messagePattern.pattern()).length + 4));
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            byte[] name = Convert.toBytes(this.name);
            byte[] description = Convert.toBytes(this.description);
            buffer.put((byte)name.length);
            buffer.put(name);
            buffer.putShort((short) description.length);
            buffer.put(description);
            if (getVersion() >=2 ) {
                if (messagePattern == null) {
                    buffer.putShort((short)0);
                } else {
                    byte[] regexBytes = Convert.toBytes(messagePattern.pattern());
                    buffer.putShort((short) regexBytes.length);
                    buffer.put(regexBytes);
                    buffer.putInt(messagePattern.flags());
                }
            }
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("name", name);
            attachment.put("description", description);
            if (messagePattern != null) {
                attachment.put("messagePatternRegex", messagePattern.pattern());
                attachment.put("messagePatternFlags", messagePattern.flags());
            }
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ACCOUNT_INFO;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public Pattern getMessagePattern() {
            return messagePattern;
        }

    }

    public final static class ColoredCoinsAssetIssuance extends AbstractAttachment {

        private final String name;
        private final String description;
        private final long quantityQNT;
        private final byte decimals;

        ColoredCoinsAssetIssuance(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            super(buffer, transactionVersion);
            this.name = Convert.readString(buffer, buffer.get(), Constants.MAX_ASSET_NAME_LENGTH);
            this.description = Convert.readString(buffer, buffer.getShort(), Constants.MAX_ASSET_DESCRIPTION_LENGTH);
            this.quantityQNT = buffer.getLong();
            this.decimals = buffer.get();
        }

        ColoredCoinsAssetIssuance(JSONObject attachmentData) {
            super(attachmentData);
            this.name = (String) attachmentData.get("name");
            this.description = Convert.nullToEmpty((String) attachmentData.get("description"));
            this.quantityQNT = Convert.parseLong(attachmentData.get("quantityQNT"));
            this.decimals = ((Long) attachmentData.get("decimals")).byteValue();
        }

        public ColoredCoinsAssetIssuance(String name, String description, long quantityQNT, byte decimals) {
            this.name = name;
            this.description = Convert.nullToEmpty(description);
            this.quantityQNT = quantityQNT;
            this.decimals = decimals;
        }

        @Override
        String getAppendixName() {
            return "AssetIssuance";
        }

        @Override
        int getMySize() {
            return 1 + Convert.toBytes(name).length + 2 + Convert.toBytes(description).length + 8 + 1;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            byte[] name = Convert.toBytes(this.name);
            byte[] description = Convert.toBytes(this.description);
            buffer.put((byte)name.length);
            buffer.put(name);
            buffer.putShort((short) description.length);
            buffer.put(description);
            buffer.putLong(quantityQNT);
            buffer.put(decimals);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("name", name);
            attachment.put("description", description);
            attachment.put("quantityQNT", quantityQNT);
            attachment.put("decimals", decimals);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.ASSET_ISSUANCE;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public long getQuantityQNT() {
            return quantityQNT;
        }

        public byte getDecimals() {
            return decimals;
        }
    }

    public final static class ColoredCoinsAssetTransfer extends AbstractAttachment {

        private final long assetId;
        private final long quantityQNT;
        private final String comment;

        ColoredCoinsAssetTransfer(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            super(buffer, transactionVersion);
            this.assetId = buffer.getLong();
            this.quantityQNT = buffer.getLong();
            this.comment = getVersion() == 0 ? Convert.readString(buffer, buffer.getShort(), Constants.MAX_ASSET_TRANSFER_COMMENT_LENGTH) : null;
        }

        ColoredCoinsAssetTransfer(JSONObject attachmentData) {
            super(attachmentData);
            this.assetId = Convert.parseUnsignedLong((String) attachmentData.get("asset"));
            this.quantityQNT = Convert.parseLong(attachmentData.get("quantityQNT"));
            this.comment = getVersion() == 0 ? Convert.nullToEmpty((String) attachmentData.get("comment")) : null;
        }

        public ColoredCoinsAssetTransfer(long assetId, long quantityQNT) {
            this.assetId = assetId;
            this.quantityQNT = quantityQNT;
            this.comment = null;
        }

        @Override
        String getAppendixName() {
            return "AssetTransfer";
        }

        @Override
        int getMySize() {
            return 8 + 8 + (getVersion() == 0 ? (2 + Convert.toBytes(comment).length) : 0);
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(assetId);
            buffer.putLong(quantityQNT);
            if (getVersion() == 0 && comment != null) {
                byte[] commentBytes = Convert.toBytes(this.comment);
                buffer.putShort((short) commentBytes.length);
                buffer.put(commentBytes);
            }
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("asset", Convert.toUnsignedLong(assetId));
            attachment.put("quantityQNT", quantityQNT);
            if (getVersion() == 0) {
                attachment.put("comment", comment);
            }
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.ASSET_TRANSFER;
        }

        public long getAssetId() {
            return assetId;
        }

        public long getQuantityQNT() {
            return quantityQNT;
        }

        public String getComment() {
            return comment;
        }

    }

    abstract static class ColoredCoinsOrderPlacement extends AbstractAttachment {

        private final long assetId;
        private final long quantityQNT;
        private final long priceNQT;

        private ColoredCoinsOrderPlacement(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.assetId = buffer.getLong();
            this.quantityQNT = buffer.getLong();
            this.priceNQT = buffer.getLong();
        }

        private ColoredCoinsOrderPlacement(JSONObject attachmentData) {
            super(attachmentData);
            this.assetId = Convert.parseUnsignedLong((String) attachmentData.get("asset"));
            this.quantityQNT = Convert.parseLong(attachmentData.get("quantityQNT"));
            this.priceNQT = Convert.parseLong(attachmentData.get("priceNQT"));
        }

        private ColoredCoinsOrderPlacement(long assetId, long quantityQNT, long priceNQT) {
            this.assetId = assetId;
            this.quantityQNT = quantityQNT;
            this.priceNQT = priceNQT;
        }

        @Override
        int getMySize() {
            return 8 + 8 + 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(assetId);
            buffer.putLong(quantityQNT);
            buffer.putLong(priceNQT);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("asset", Convert.toUnsignedLong(assetId));
            attachment.put("quantityQNT", quantityQNT);
            attachment.put("priceNQT", priceNQT);
        }

        public long getAssetId() {
            return assetId;
        }

        public long getQuantityQNT() {
            return quantityQNT;
        }

        public long getPriceNQT() {
            return priceNQT;
        }
    }

    public final static class ColoredCoinsAskOrderPlacement extends ColoredCoinsOrderPlacement {

        ColoredCoinsAskOrderPlacement(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
        }

        ColoredCoinsAskOrderPlacement(JSONObject attachmentData) {
            super(attachmentData);
        }

        public ColoredCoinsAskOrderPlacement(long assetId, long quantityQNT, long priceNQT) {
            super(assetId, quantityQNT, priceNQT);
        }

        @Override
        String getAppendixName() {
            return "AskOrderPlacement";
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.ASK_ORDER_PLACEMENT;
        }

    }

    public final static class ColoredCoinsBidOrderPlacement extends ColoredCoinsOrderPlacement {

        ColoredCoinsBidOrderPlacement(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
        }

        ColoredCoinsBidOrderPlacement(JSONObject attachmentData) {
            super(attachmentData);
        }

        public ColoredCoinsBidOrderPlacement(long assetId, long quantityQNT, long priceNQT) {
            super(assetId, quantityQNT, priceNQT);
        }

        @Override
        String getAppendixName() {
            return "BidOrderPlacement";
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.BID_ORDER_PLACEMENT;
        }

    }

    abstract static class ColoredCoinsOrderCancellation extends AbstractAttachment {

        private final long orderId;

        private ColoredCoinsOrderCancellation(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.orderId = buffer.getLong();
        }

        private ColoredCoinsOrderCancellation(JSONObject attachmentData) {
            super(attachmentData);
            this.orderId = Convert.parseUnsignedLong((String) attachmentData.get("order"));
        }

        private ColoredCoinsOrderCancellation(long orderId) {
            this.orderId = orderId;
        }

        @Override
        int getMySize() {
            return 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(orderId);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("order", Convert.toUnsignedLong(orderId));
        }

        public long getOrderId() {
            return orderId;
        }
    }

    public final static class ColoredCoinsAskOrderCancellation extends ColoredCoinsOrderCancellation {

        ColoredCoinsAskOrderCancellation(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
        }

        ColoredCoinsAskOrderCancellation(JSONObject attachmentData) {
            super(attachmentData);
        }

        public ColoredCoinsAskOrderCancellation(long orderId) {
            super(orderId);
        }

        @Override
        String getAppendixName() {
            return "AskOrderCancellation";
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.ASK_ORDER_CANCELLATION;
        }

    }

    public final static class ColoredCoinsBidOrderCancellation extends ColoredCoinsOrderCancellation {

        ColoredCoinsBidOrderCancellation(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
        }

        ColoredCoinsBidOrderCancellation(JSONObject attachmentData) {
            super(attachmentData);
        }

        public ColoredCoinsBidOrderCancellation(long orderId) {
            super(orderId);
        }

        @Override
        String getAppendixName() {
            return "BidOrderCancellation";
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.BID_ORDER_CANCELLATION;
        }

    }

    public final static class ColoredCoinsDividendPayment extends AbstractAttachment {

        private final long assetId;
        private final int height;
        private final long amountNQTPerQNT;

        ColoredCoinsDividendPayment(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.assetId = buffer.getLong();
            this.height = buffer.getInt();
            this.amountNQTPerQNT = buffer.getLong();
        }

        ColoredCoinsDividendPayment(JSONObject attachmentData) {
            super(attachmentData);
            this.assetId = Convert.parseUnsignedLong((String)attachmentData.get("asset"));
            this.height = ((Long)attachmentData.get("height")).intValue();
            this.amountNQTPerQNT = Convert.parseLong(attachmentData.get("amountNQTPerQNT"));
        }

        public ColoredCoinsDividendPayment(long assetId, int height, long amountNQTPerQNT) {
            this.assetId = assetId;
            this.height = height;
            this.amountNQTPerQNT = amountNQTPerQNT;
        }

        @Override
        String getAppendixName() {
            return "DividendPayment";
        }

        @Override
        int getMySize() {
            return 8 + 4 + 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(assetId);
            buffer.putInt(height);
            buffer.putLong(amountNQTPerQNT);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("asset", Convert.toUnsignedLong(assetId));
            attachment.put("height", height);
            attachment.put("amountNQTPerQNT", amountNQTPerQNT);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.DIVIDEND_PAYMENT;
        }

        public long getAssetId() {
            return assetId;
        }

        public int getHeight() {
            return height;
        }

        public long getAmountNQTPerQNT() {
            return amountNQTPerQNT;
        }

    }

    public final static class DigitalGoodsListing extends AbstractAttachment {

        private final String name;
        private final String description;
        private final String tags;
        private final int quantity;
        private final long priceNQT;

        DigitalGoodsListing(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            super(buffer, transactionVersion);
            this.name = Convert.readString(buffer, buffer.getShort(), Constants.MAX_DGS_LISTING_NAME_LENGTH);
            this.description = Convert.readString(buffer, buffer.getShort(), Constants.MAX_DGS_LISTING_DESCRIPTION_LENGTH);
            this.tags = Convert.readString(buffer, buffer.getShort(), Constants.MAX_DGS_LISTING_TAGS_LENGTH);
            this.quantity = buffer.getInt();
            this.priceNQT = buffer.getLong();
        }

        DigitalGoodsListing(JSONObject attachmentData) {
            super(attachmentData);
            this.name = (String) attachmentData.get("name");
            this.description = (String) attachmentData.get("description");
            this.tags = (String) attachmentData.get("tags");
            this.quantity = ((Long) attachmentData.get("quantity")).intValue();
            this.priceNQT = Convert.parseLong(attachmentData.get("priceNQT"));
        }

        public DigitalGoodsListing(String name, String description, String tags, int quantity, long priceNQT) {
            this.name = name;
            this.description = description;
            this.tags = tags;
            this.quantity = quantity;
            this.priceNQT = priceNQT;
        }

        @Override
        String getAppendixName() {
            return "DigitalGoodsListing";
        }

        @Override
        int getMySize() {
            return 2 + Convert.toBytes(name).length + 2 + Convert.toBytes(description).length + 2
                        + Convert.toBytes(tags).length + 4 + 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            byte[] nameBytes = Convert.toBytes(name);
            buffer.putShort((short) nameBytes.length);
            buffer.put(nameBytes);
            byte[] descriptionBytes = Convert.toBytes(description);
            buffer.putShort((short) descriptionBytes.length);
            buffer.put(descriptionBytes);
            byte[] tagsBytes = Convert.toBytes(tags);
            buffer.putShort((short) tagsBytes.length);
            buffer.put(tagsBytes);
            buffer.putInt(quantity);
            buffer.putLong(priceNQT);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("name", name);
            attachment.put("description", description);
            attachment.put("tags", tags);
            attachment.put("quantity", quantity);
            attachment.put("priceNQT", priceNQT);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.LISTING;
        }

        public String getName() { return name; }

        public String getDescription() { return description; }

        public String getTags() { return tags; }

        public int getQuantity() { return quantity; }

        public long getPriceNQT() { return priceNQT; }

    }

    public final static class DigitalGoodsDelisting extends AbstractAttachment {

        private final long goodsId;

        DigitalGoodsDelisting(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.goodsId = buffer.getLong();
        }

        DigitalGoodsDelisting(JSONObject attachmentData) {
            super(attachmentData);
            this.goodsId = Convert.parseUnsignedLong((String)attachmentData.get("goods"));
        }

        public DigitalGoodsDelisting(long goodsId) {
            this.goodsId = goodsId;
        }

        @Override
        String getAppendixName() {
            return "DigitalGoodsDelisting";
        }

        @Override
        int getMySize() {
            return 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(goodsId);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("goods", Convert.toUnsignedLong(goodsId));
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.DELISTING;
        }

        public long getGoodsId() { return goodsId; }

    }

    public final static class DigitalGoodsPriceChange extends AbstractAttachment {

        private final long goodsId;
        private final long priceNQT;

        DigitalGoodsPriceChange(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.goodsId = buffer.getLong();
            this.priceNQT = buffer.getLong();
        }

        DigitalGoodsPriceChange(JSONObject attachmentData) {
            super(attachmentData);
            this.goodsId = Convert.parseUnsignedLong((String)attachmentData.get("goods"));
            this.priceNQT = Convert.parseLong(attachmentData.get("priceNQT"));
        }

        public DigitalGoodsPriceChange(long goodsId, long priceNQT) {
            this.goodsId = goodsId;
            this.priceNQT = priceNQT;
        }

        @Override
        String getAppendixName() {
            return "DigitalGoodsPriceChange";
        }

        @Override
        int getMySize() {
            return 8 + 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(goodsId);
            buffer.putLong(priceNQT);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("goods", Convert.toUnsignedLong(goodsId));
            attachment.put("priceNQT", priceNQT);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.PRICE_CHANGE;
        }

        public long getGoodsId() { return goodsId; }

        public long getPriceNQT() { return priceNQT; }

    }

    public final static class DigitalGoodsQuantityChange extends AbstractAttachment {

        private final long goodsId;
        private final int deltaQuantity;

        DigitalGoodsQuantityChange(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.goodsId = buffer.getLong();
            this.deltaQuantity = buffer.getInt();
        }

        DigitalGoodsQuantityChange(JSONObject attachmentData) {
            super(attachmentData);
            this.goodsId = Convert.parseUnsignedLong((String)attachmentData.get("goods"));
            this.deltaQuantity = ((Long)attachmentData.get("deltaQuantity")).intValue();
        }

        public DigitalGoodsQuantityChange(long goodsId, int deltaQuantity) {
            this.goodsId = goodsId;
            this.deltaQuantity = deltaQuantity;
        }

        @Override
        String getAppendixName() {
            return "DigitalGoodsQuantityChange";
        }

        @Override
        int getMySize() {
            return 8 + 4;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(goodsId);
            buffer.putInt(deltaQuantity);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("goods", Convert.toUnsignedLong(goodsId));
            attachment.put("deltaQuantity", deltaQuantity);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.QUANTITY_CHANGE;
        }

        public long getGoodsId() { return goodsId; }

        public int getDeltaQuantity() { return deltaQuantity; }

    }

    public final static class DigitalGoodsPurchase extends AbstractAttachment {

        private final long goodsId;
        private final int quantity;
        private final long priceNQT;
        private final int deliveryDeadlineTimestamp;

        DigitalGoodsPurchase(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.goodsId = buffer.getLong();
            this.quantity = buffer.getInt();
            this.priceNQT = buffer.getLong();
            this.deliveryDeadlineTimestamp = buffer.getInt();
        }

        DigitalGoodsPurchase(JSONObject attachmentData) {
            super(attachmentData);
            this.goodsId = Convert.parseUnsignedLong((String)attachmentData.get("goods"));
            this.quantity = ((Long)attachmentData.get("quantity")).intValue();
            this.priceNQT = Convert.parseLong(attachmentData.get("priceNQT"));
            this.deliveryDeadlineTimestamp = ((Long)attachmentData.get("deliveryDeadlineTimestamp")).intValue();
        }

        public DigitalGoodsPurchase(long goodsId, int quantity, long priceNQT, int deliveryDeadlineTimestamp) {
            this.goodsId = goodsId;
            this.quantity = quantity;
            this.priceNQT = priceNQT;
            this.deliveryDeadlineTimestamp = deliveryDeadlineTimestamp;
        }

        @Override
        String getAppendixName() {
            return "DigitalGoodsPurchase";
        }

        @Override
        int getMySize() {
            return 8 + 4 + 8 + 4;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(goodsId);
            buffer.putInt(quantity);
            buffer.putLong(priceNQT);
            buffer.putInt(deliveryDeadlineTimestamp);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("goods", Convert.toUnsignedLong(goodsId));
            attachment.put("quantity", quantity);
            attachment.put("priceNQT", priceNQT);
            attachment.put("deliveryDeadlineTimestamp", deliveryDeadlineTimestamp);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.PURCHASE;
        }

        public long getGoodsId() { return goodsId; }

        public int getQuantity() { return quantity; }

        public long getPriceNQT() { return priceNQT; }

        public int getDeliveryDeadlineTimestamp() { return deliveryDeadlineTimestamp; }

    }

    public final static class DigitalGoodsDelivery extends AbstractAttachment {

        private final long purchaseId;
        private final EncryptedData goods;
        private final long discountNQT;
        private final boolean goodsIsText;

        DigitalGoodsDelivery(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            super(buffer, transactionVersion);
            this.purchaseId = buffer.getLong();
            int length = buffer.getInt();
            goodsIsText = length < 0;
            if (length < 0) {
                length &= Integer.MAX_VALUE;
            }
            this.goods = EncryptedData.readEncryptedData(buffer, length, Constants.MAX_DGS_GOODS_LENGTH);
            this.discountNQT = buffer.getLong();
        }

        DigitalGoodsDelivery(JSONObject attachmentData) {
            super(attachmentData);
            this.purchaseId = Convert.parseUnsignedLong((String) attachmentData.get("purchase"));
            this.goods = new EncryptedData(Convert.parseHexString((String)attachmentData.get("goodsData")),
                    Convert.parseHexString((String)attachmentData.get("goodsNonce")));
            this.discountNQT = Convert.parseLong(attachmentData.get("discountNQT"));
            this.goodsIsText = Boolean.TRUE.equals(attachmentData.get("goodsIsText"));
        }

        public DigitalGoodsDelivery(long purchaseId, EncryptedData goods, boolean goodsIsText, long discountNQT) {
            this.purchaseId = purchaseId;
            this.goods = goods;
            this.discountNQT = discountNQT;
            this.goodsIsText = goodsIsText;
        }

        @Override
        String getAppendixName() {
            return "DigitalGoodsDelivery";
        }

        @Override
        int getMySize() {
            return 8 + 4 + goods.getSize() + 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(purchaseId);
            buffer.putInt(goodsIsText ? goods.getData().length | Integer.MIN_VALUE : goods.getData().length);
            buffer.put(goods.getData());
            buffer.put(goods.getNonce());
            buffer.putLong(discountNQT);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("purchase", Convert.toUnsignedLong(purchaseId));
            attachment.put("goodsData", Convert.toHexString(goods.getData()));
            attachment.put("goodsNonce", Convert.toHexString(goods.getNonce()));
            attachment.put("discountNQT", discountNQT);
            attachment.put("goodsIsText", goodsIsText);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.DELIVERY;
        }

        public long getPurchaseId() { return purchaseId; }

        public EncryptedData getGoods() { return goods; }

        public long getDiscountNQT() { return discountNQT; }

        public boolean goodsIsText() {
            return goodsIsText;
        }

    }

    public final static class DigitalGoodsFeedback extends AbstractAttachment {

        private final long purchaseId;

        DigitalGoodsFeedback(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.purchaseId = buffer.getLong();
        }

        DigitalGoodsFeedback(JSONObject attachmentData) {
            super(attachmentData);
            this.purchaseId = Convert.parseUnsignedLong((String)attachmentData.get("purchase"));
        }

        public DigitalGoodsFeedback(long purchaseId) {
            this.purchaseId = purchaseId;
        }

        @Override
        String getAppendixName() {
            return "DigitalGoodsFeedback";
        }

        @Override
        int getMySize() {
            return 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(purchaseId);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("purchase", Convert.toUnsignedLong(purchaseId));
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.FEEDBACK;
        }

        public long getPurchaseId() { return purchaseId; }

    }

    public final static class DigitalGoodsRefund extends AbstractAttachment {

        private final long purchaseId;
        private final long refundNQT;

        DigitalGoodsRefund(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.purchaseId = buffer.getLong();
            this.refundNQT = buffer.getLong();
        }

        DigitalGoodsRefund(JSONObject attachmentData) {
            super(attachmentData);
            this.purchaseId = Convert.parseUnsignedLong((String)attachmentData.get("purchase"));
            this.refundNQT = Convert.parseLong(attachmentData.get("refundNQT"));
        }

        public DigitalGoodsRefund(long purchaseId, long refundNQT) {
            this.purchaseId = purchaseId;
            this.refundNQT = refundNQT;
        }

        @Override
        String getAppendixName() {
            return "DigitalGoodsRefund";
        }

        @Override
        int getMySize() {
            return 8 + 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(purchaseId);
            buffer.putLong(refundNQT);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("purchase", Convert.toUnsignedLong(purchaseId));
            attachment.put("refundNQT", refundNQT);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.REFUND;
        }

        public long getPurchaseId() { return purchaseId; }

        public long getRefundNQT() { return refundNQT; }

    }

    public final static class AccountControlEffectiveBalanceLeasing extends AbstractAttachment {

        private final short period;

        AccountControlEffectiveBalanceLeasing(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.period = buffer.getShort();
        }

        AccountControlEffectiveBalanceLeasing(JSONObject attachmentData) {
            super(attachmentData);
            this.period = ((Long) attachmentData.get("period")).shortValue();
        }

        public AccountControlEffectiveBalanceLeasing(short period) {
            this.period = period;
        }

        @Override
        String getAppendixName() {
            return "EffectiveBalanceLeasing";
        }

        @Override
        int getMySize() {
            return 2;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putShort(period);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("period", period);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.AccountControl.EFFECTIVE_BALANCE_LEASING;
        }

        public short getPeriod() {
            return period;
        }
    }

    public static interface MonetarySystemAttachment {

        long getCurrencyId();

    }

    public final static class MonetarySystemCurrencyIssuance extends AbstractAttachment {

        private final String name;
        private final String code;
        private final String description;
        private final byte type;
        private final long initialSupply;
        private final long reserveSupply;
        private final long maxSupply;
        private final int issuanceHeight;
        private final long minReservePerUnitNQT;
        private final int minDifficulty;
        private final int maxDifficulty;
        private final byte ruleset;
        private final byte algorithm;
        private final byte decimals;

        MonetarySystemCurrencyIssuance(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            super(buffer, transactionVersion);
            this.name = Convert.readString(buffer, buffer.get(), Constants.MAX_CURRENCY_NAME_LENGTH);
            this.code = Convert.readString(buffer, buffer.get(), Constants.MAX_CURRENCY_CODE_LENGTH);
            this.description = Convert.readString(buffer, buffer.getShort(), Constants.MAX_CURRENCY_DESCRIPTION_LENGTH);
            this.type = buffer.get();
            this.initialSupply = buffer.getLong();
            this.reserveSupply = buffer.getLong();
            this.maxSupply = buffer.getLong();
            this.issuanceHeight = buffer.getInt();
            this.minReservePerUnitNQT = buffer.getLong();
            this.minDifficulty = buffer.get() & 0xFF;
            this.maxDifficulty = buffer.get() & 0xFF;
            this.ruleset = buffer.get();
            this.algorithm = buffer.get();
            this.decimals = buffer.get();
        }

        MonetarySystemCurrencyIssuance(JSONObject attachmentData) {
            super(attachmentData);
            this.name = (String)attachmentData.get("name");
            this.code = (String)attachmentData.get("code");
            this.description = (String)attachmentData.get("description");
            this.type = ((Long)attachmentData.get("type")).byteValue();
            this.initialSupply = Convert.parseLong(attachmentData.get("initialSupply"));
            this.reserveSupply = Convert.parseLong(attachmentData.get("reserveSupply"));
            this.maxSupply = Convert.parseLong(attachmentData.get("maxSupply"));
            this.issuanceHeight = ((Long)attachmentData.get("issuanceHeight")).intValue();
            this.minReservePerUnitNQT = Convert.parseLong(attachmentData.get("minReservePerUnitNQT"));
            this.minDifficulty = ((Long)attachmentData.get("minDifficulty")).intValue();
            this.maxDifficulty = ((Long)attachmentData.get("maxDifficulty")).intValue();
            this.ruleset = ((Long)attachmentData.get("ruleset")).byteValue();
            this.algorithm = ((Long)attachmentData.get("algorithm")).byteValue();
            this.decimals = ((Long) attachmentData.get("decimals")).byteValue();
        }

        public MonetarySystemCurrencyIssuance(String name, String code, String description, byte type, long initialSupply, long reserveSupply,
                                              long maxSupply, int issuanceHeight, long minReservePerUnitNQT, int minDifficulty, int maxDifficulty,
                                              byte ruleset, byte algorithm, byte decimals) {
            this.name = name;
            this.code = code;
            this.description = description;
            this.type = type;
            this.initialSupply = initialSupply;
            this.reserveSupply = reserveSupply;
            this.maxSupply = maxSupply;
            this.issuanceHeight = issuanceHeight;
            this.minReservePerUnitNQT = minReservePerUnitNQT;
            this.minDifficulty = minDifficulty;
            this.maxDifficulty = maxDifficulty;
            this.ruleset = ruleset;
            this.algorithm = algorithm;
            this.decimals = decimals;
        }

        @Override
        String getAppendixName() {
            return "CurrencyIssuance";
        }

        @Override
        int getMySize() {
            return 1 + Convert.toBytes(name).length + 1 + Convert.toBytes(code).length + 2 +
                    Convert.toBytes(description).length + 1 + 8 + 8 + 8 + 4 + 8 + 1 + 1 + 1 + 1 + 1;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            byte[] name = Convert.toBytes(this.name);
            byte[] code = Convert.toBytes(this.code);
            byte[] description = Convert.toBytes(this.description);
            buffer.put((byte)name.length);
            buffer.put(name);
            buffer.put((byte)code.length);
            buffer.put(code);
            buffer.putShort((short) description.length);
            buffer.put(description);
            buffer.put(type);
            buffer.putLong(initialSupply);
            buffer.putLong(reserveSupply);
            buffer.putLong(maxSupply);
            buffer.putInt(issuanceHeight);
            buffer.putLong(minReservePerUnitNQT);
            buffer.put((byte)minDifficulty);
            buffer.put((byte)maxDifficulty);
            buffer.put(ruleset);
            buffer.put(algorithm);
            buffer.put(decimals);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("name", name);
            attachment.put("code", code);
            attachment.put("description", description);
            attachment.put("type", type);
            attachment.put("initialSupply", initialSupply);
            attachment.put("reserveSupply", reserveSupply);
            attachment.put("maxSupply", maxSupply);
            attachment.put("issuanceHeight", issuanceHeight);
            attachment.put("minReservePerUnitNQT", minReservePerUnitNQT);
            attachment.put("minDifficulty", minDifficulty);
            attachment.put("maxDifficulty", maxDifficulty);
            attachment.put("ruleset", ruleset);
            attachment.put("algorithm", algorithm);
            attachment.put("decimals", decimals);
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetarySystem.CURRENCY_ISSUANCE;
        }

        public String getName() {
            return name;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public byte getType() {
            return type;
        }

        public long getInitialSupply() {
            return initialSupply;
        }

        public long getReserveSupply() {
            return reserveSupply;
        }

        public long getMaxSupply() {
            return maxSupply;
        }

        public int getIssuanceHeight() {
            return issuanceHeight;
        }

        public long getMinReservePerUnitNQT() {
            return minReservePerUnitNQT;
        }

        public int getMinDifficulty() {
            return minDifficulty;
        }

        public int getMaxDifficulty() {
            return maxDifficulty;
        }

        public byte getRuleset() {
            return ruleset;
        }

        public byte getAlgorithm() {
            return algorithm;
        }

        public byte getDecimals() {
            return decimals;
        }
    }

    public final static class MonetarySystemReserveIncrease extends AbstractAttachment implements MonetarySystemAttachment {

        private final long currencyId;
        private final long amountPerUnitNQT;

        MonetarySystemReserveIncrease(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.currencyId = buffer.getLong();
            this.amountPerUnitNQT = buffer.getLong();
        }

        MonetarySystemReserveIncrease(JSONObject attachmentData) {
            super(attachmentData);
            this.currencyId = Convert.parseUnsignedLong((String)attachmentData.get("currency"));
            this.amountPerUnitNQT = Convert.parseLong(attachmentData.get("amountPerUnitNQT"));
        }

        public MonetarySystemReserveIncrease(long currencyId, long amountPerUnitNQT) {
            this.currencyId = currencyId;
            this.amountPerUnitNQT = amountPerUnitNQT;
        }

        @Override
        String getAppendixName() {
            return "ReserveIncrease";
        }

        @Override
        int getMySize() {
            return 8 + 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(currencyId);
            buffer.putLong(amountPerUnitNQT);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("currency", Convert.toUnsignedLong(currencyId));
            attachment.put("amountPerUnitNQT", amountPerUnitNQT);
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetarySystem.RESERVE_INCREASE;
        }

        @Override
        public long getCurrencyId() {
            return currencyId;
        }

        public long getAmountPerUnitNQT() {
            return amountPerUnitNQT;
        }

    }

    public final static class MonetarySystemReserveClaim extends AbstractAttachment implements MonetarySystemAttachment {

        private final long currencyId;
        private final long units;

        MonetarySystemReserveClaim(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.currencyId = buffer.getLong();
            this.units = buffer.getLong();
        }

        MonetarySystemReserveClaim(JSONObject attachmentData) {
            super(attachmentData);
            this.currencyId = Convert.parseUnsignedLong((String)attachmentData.get("currency"));
            this.units = Convert.parseLong(attachmentData.get("units"));
        }

        public MonetarySystemReserveClaim(long currencyId, long units) {
            this.currencyId = currencyId;
            this.units = units;
        }

        @Override
        String getAppendixName() {
            return "ReserveClaim";
        }

        @Override
        int getMySize() {
            return 8 + 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(currencyId);
            buffer.putLong(units);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("currency", Convert.toUnsignedLong(currencyId));
            attachment.put("units", units);
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetarySystem.RESERVE_CLAIM;
        }

        @Override
        public long getCurrencyId() {
            return currencyId;
        }

        public long getUnits() {
            return units;
        }

    }

    public final static class MonetarySystemCurrencyTransfer extends AbstractAttachment implements MonetarySystemAttachment {

        private final long currencyId;
        private final long units;

        MonetarySystemCurrencyTransfer(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.currencyId = buffer.getLong();
            this.units = buffer.getLong();
        }

        MonetarySystemCurrencyTransfer(JSONObject attachmentData) {
            super(attachmentData);
            this.currencyId = Convert.parseUnsignedLong((String)attachmentData.get("currency"));
            this.units = Convert.parseLong(attachmentData.get("units"));
        }

        public MonetarySystemCurrencyTransfer(long currencyId, long units) {
            this.currencyId = currencyId;
            this.units = units;
        }

        @Override
        String getAppendixName() {
            return "CurrencyTransfer";
        }

        @Override
        int getMySize() {
            return 8 + 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(currencyId);
            buffer.putLong(units);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("currency", Convert.toUnsignedLong(currencyId));
            attachment.put("units", units);
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetarySystem.CURRENCY_TRANSFER;
        }

        @Override
        public long getCurrencyId() {
            return currencyId;
        }

        public long getUnits() {
            return units;
        }
    }

    public final static class MonetarySystemPublishExchangeOffer extends AbstractAttachment implements MonetarySystemAttachment {

        private final long currencyId;
        private final long buyRateNQT;
        private final long sellRateNQT;
        private final long totalBuyLimit;
        private final long totalSellLimit;
        private final long initialBuySupply;
        private final long initialSellSupply;
        private final int expirationHeight;

        MonetarySystemPublishExchangeOffer(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.currencyId = buffer.getLong();
            this.buyRateNQT = buffer.getLong();
            this.sellRateNQT = buffer.getLong();
            this.totalBuyLimit = buffer.getLong();
            this.totalSellLimit = buffer.getLong();
            this.initialBuySupply = buffer.getLong();
            this.initialSellSupply = buffer.getLong();
            this.expirationHeight = buffer.getInt();
        }

        MonetarySystemPublishExchangeOffer(JSONObject attachmentData) {
            super(attachmentData);
            this.currencyId = Convert.parseUnsignedLong((String)attachmentData.get("currency"));
            this.buyRateNQT = Convert.parseLong(attachmentData.get("buyRateNQT"));
            this.sellRateNQT = Convert.parseLong(attachmentData.get("sellRateNQT"));
            this.totalBuyLimit = Convert.parseLong(attachmentData.get("totalBuyLimit"));
            this.totalSellLimit = Convert.parseLong(attachmentData.get("totalSellLimit"));
            this.initialBuySupply = Convert.parseLong(attachmentData.get("initialBuySupply"));
            this.initialSellSupply = Convert.parseLong(attachmentData.get("initialSellSupply"));
            this.expirationHeight = ((Long)attachmentData.get("expirationHeight")).intValue();
        }

        public MonetarySystemPublishExchangeOffer(long currencyId, long buyRateNQT, long sellRateNQT, long totalBuyLimit,
                                                  long totalSellLimit, long initialBuySupply, long initialSellSupply, int expirationHeight) {
            this.currencyId = currencyId;
            this.buyRateNQT = buyRateNQT;
            this.sellRateNQT = sellRateNQT;
            this.totalBuyLimit = totalBuyLimit;
            this.totalSellLimit = totalSellLimit;
            this.initialBuySupply = initialBuySupply;
            this.initialSellSupply = initialSellSupply;
            this.expirationHeight = expirationHeight;
        }

        @Override
        String getAppendixName() {
            return "PublishExchangeOffer";
        }

        @Override
        int getMySize() {
            return 8 + 8 + 8 + 8 + 8 + 8 + 8 + 4;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(currencyId);
            buffer.putLong(buyRateNQT);
            buffer.putLong(sellRateNQT);
            buffer.putLong(totalBuyLimit);
            buffer.putLong(totalSellLimit);
            buffer.putLong(initialBuySupply);
            buffer.putLong(initialSellSupply);
            buffer.putInt(expirationHeight);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("currency", Convert.toUnsignedLong(currencyId));
            attachment.put("buyRateNQT", buyRateNQT);
            attachment.put("sellRateNQT", sellRateNQT);
            attachment.put("totalBuyLimit", totalBuyLimit);
            attachment.put("totalSellLimit", totalSellLimit);
            attachment.put("initialBuySupply", initialBuySupply);
            attachment.put("initialSellSupply", initialSellSupply);
            attachment.put("expirationHeight", expirationHeight);
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetarySystem.PUBLISH_EXCHANGE_OFFER;
        }

        @Override
        public long getCurrencyId() {
            return currencyId;
        }

        public long getBuyRateNQT() {
            return buyRateNQT;
        }

        public long getSellRateNQT() {
            return sellRateNQT;
        }

        public long getTotalBuyLimit() {
            return totalBuyLimit;
        }

        public long getTotalSellLimit() {
            return totalSellLimit;
        }

        public long getInitialBuySupply() {
            return initialBuySupply;
        }

        public long getInitialSellSupply() {
            return initialSellSupply;
        }

        public int getExpirationHeight() {
            return expirationHeight;
        }

    }

    abstract static class MonetarySystemExchange extends AbstractAttachment implements MonetarySystemAttachment {

        private final long currencyId;
        private final long rateNQT;
        private final long units;

        private MonetarySystemExchange(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.currencyId = buffer.getLong();
            this.rateNQT = buffer.getLong();
            this.units = buffer.getLong();
        }

        private MonetarySystemExchange(JSONObject attachmentData) {
            super(attachmentData);
            this.currencyId = Convert.parseUnsignedLong((String)attachmentData.get("currency"));
            this.rateNQT = Convert.parseLong(attachmentData.get("rateNQT"));
            this.units = Convert.parseLong(attachmentData.get("units"));
        }

        private MonetarySystemExchange(long currencyId, long rateNQT, long units) {
            this.currencyId = currencyId;
            this.rateNQT = rateNQT;
            this.units = units;
        }

        @Override
        int getMySize() {
            return 8 + 8 + 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(currencyId);
            buffer.putLong(rateNQT);
            buffer.putLong(units);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("currency", Convert.toUnsignedLong(currencyId));
            attachment.put("rateNQT", rateNQT);
            attachment.put("units", units);
        }

        @Override
        public long getCurrencyId() {
            return currencyId;
        }

        public long getRateNQT() {
            return rateNQT;
        }

        public long getUnits() {
            return units;
        }

    }

    public final static class MonetarySystemExchangeBuy extends MonetarySystemExchange {

        MonetarySystemExchangeBuy(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
        }

        MonetarySystemExchangeBuy(JSONObject attachmentData) {
            super(attachmentData);
        }

        public MonetarySystemExchangeBuy(long currencyId, long rateNQT, long units) {
            super(currencyId, rateNQT, units);
        }

        @Override
        String getAppendixName() {
            return "ExchangeBuy";
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetarySystem.EXCHANGE_BUY;
        }

    }

    public final static class MonetarySystemExchangeSell extends MonetarySystemExchange {

        MonetarySystemExchangeSell(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
        }

        MonetarySystemExchangeSell(JSONObject attachmentData) {
            super(attachmentData);
        }

        public MonetarySystemExchangeSell(long currencyId, long rateNQT, long units) {
            super(currencyId, rateNQT, units);
        }

        @Override
        String getAppendixName() {
            return "ExchangeSell";
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetarySystem.EXCHANGE_SELL;
        }

    }

    public final static class MonetarySystemCurrencyMinting extends AbstractAttachment implements MonetarySystemAttachment {

        private final long nonce;
        private final long currencyId;
        private final long units;
        private final long counter;

        MonetarySystemCurrencyMinting(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.nonce = buffer.getLong();
            this.currencyId = buffer.getLong();
            this.units = buffer.getLong();
            this.counter = buffer.getLong();
        }

        MonetarySystemCurrencyMinting(JSONObject attachmentData) {
            super(attachmentData);
            this.nonce = Convert.parseLong(attachmentData.get("nonce"));
            this.currencyId = Convert.parseUnsignedLong((String)attachmentData.get("currency"));
            this.units = Convert.parseLong(attachmentData.get("units"));
            this.counter = Convert.parseLong(attachmentData.get("counter"));
        }

        public MonetarySystemCurrencyMinting(long nonce, long currencyId, long units, long counter) {
            this.nonce = nonce;
            this.currencyId = currencyId;
            this.units = units;
            this.counter = counter;
        }

        @Override
        String getAppendixName() {
            return "CurrencyMinting";
        }

        @Override
        int getMySize() {
            return 8 + 8 + 8 + 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(nonce);
            buffer.putLong(currencyId);
            buffer.putLong(units);
            buffer.putLong(counter);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("nonce", nonce);
            attachment.put("currency", Convert.toUnsignedLong(currencyId));
            attachment.put("units", units);
            attachment.put("counter", counter);
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetarySystem.CURRENCY_MINTING;
        }

        public long getNonce() {
            return nonce;
        }

        @Override
        public long getCurrencyId() {
            return currencyId;
        }

        public long getUnits() {
            return units;
        }

        public long getCounter() {
            return counter;
        }

    }

    public final static class MonetarySystemCurrencyDeletion extends AbstractAttachment implements MonetarySystemAttachment {

        private final long currencyId;

        MonetarySystemCurrencyDeletion(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.currencyId = buffer.getLong();
        }

        MonetarySystemCurrencyDeletion(JSONObject attachmentData) {
            super(attachmentData);
            this.currencyId = Convert.parseUnsignedLong((String)attachmentData.get("currency"));
        }

        public MonetarySystemCurrencyDeletion(long currencyId) {
            this.currencyId = currencyId;
        }

        @Override
        String getAppendixName() {
            return "CurrencyDeletion";
        }

        @Override
        int getMySize() {
            return 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(currencyId);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("currency", Convert.toUnsignedLong(currencyId));
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetarySystem.CURRENCY_DELETION;
        }

        @Override
        public long getCurrencyId() {
            return currencyId;
        }

    }

}
