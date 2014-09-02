package nxt;

import nxt.crypto.EncryptedData;
import nxt.util.Convert;
import nxt.util.SuperComplexNumber;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

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
            aliasName = Convert.readString(buffer, buffer.get(), Constants.MAX_ALIAS_LENGTH).trim().intern();
            aliasURI = Convert.readString(buffer, buffer.getShort(), Constants.MAX_ALIAS_URI_LENGTH).trim().intern();
        }

        MessagingAliasAssignment(JSONObject attachmentData) {
            super(attachmentData);
            aliasName = (Convert.nullToEmpty((String) attachmentData.get("alias"))).trim().intern();
            aliasURI = (Convert.nullToEmpty((String) attachmentData.get("uri"))).trim().intern();
        }

        public MessagingAliasAssignment(String aliasName, String aliasURI) {
            this.aliasName = aliasName.trim().intern();
            this.aliasURI = aliasURI.trim().intern();
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

        private final Long pollId;
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

        public MessagingVoteCasting(Long pollId, byte[] pollVote) {
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

        public Long getPollId() { return pollId; }

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

        MessagingAccountInfo(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            super(buffer, transactionVersion);
            this.name = Convert.readString(buffer, buffer.get(), Constants.MAX_ACCOUNT_NAME_LENGTH);
            this.description = Convert.readString(buffer, buffer.getShort(), Constants.MAX_ACCOUNT_DESCRIPTION_LENGTH);
        }

        MessagingAccountInfo(JSONObject attachmentData) {
            super(attachmentData);
            this.name = Convert.nullToEmpty((String) attachmentData.get("name"));
            this.description = Convert.nullToEmpty((String) attachmentData.get("description"));
        }

        public MessagingAccountInfo(String name, String description) {
            this.name = name;
            this.description = description;
        }

        @Override
        String getAppendixName() {
            return "AccountInfo";
        }

        @Override
        int getMySize() {
            return 1 + Convert.toBytes(name).length + 2 + Convert.toBytes(description).length;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            byte[] name = Convert.toBytes(this.name);
            byte[] description = Convert.toBytes(this.description);
            buffer.put((byte)name.length);
            buffer.put(name);
            buffer.putShort((short) description.length);
            buffer.put(description);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("name", name);
            attachment.put("description", description);
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

        private final Long assetId;
        private final long quantityQNT;
        private final String comment;

        ColoredCoinsAssetTransfer(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            super(buffer, transactionVersion);
            this.assetId = Convert.zeroToNull(buffer.getLong());
            this.quantityQNT = buffer.getLong();
            this.comment = getVersion() == 0 ? Convert.readString(buffer, buffer.getShort(), Constants.MAX_ASSET_TRANSFER_COMMENT_LENGTH) : null;
        }

        ColoredCoinsAssetTransfer(JSONObject attachmentData) {
            super(attachmentData);
            this.assetId = Convert.parseUnsignedLong((String) attachmentData.get("asset"));
            this.quantityQNT = Convert.parseLong(attachmentData.get("quantityQNT"));
            this.comment = getVersion() == 0 ? Convert.nullToEmpty((String) attachmentData.get("comment")) : null;
        }

        public ColoredCoinsAssetTransfer(Long assetId, long quantityQNT, String comment) {
            this.assetId = assetId;
            this.quantityQNT = quantityQNT;
            this.comment = getVersion() == 0 ? Convert.nullToEmpty(comment) : null;
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
            buffer.putLong(Convert.nullToZero(assetId));
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

        public Long getAssetId() {
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

        private final Long assetId;
        private final long quantityQNT;
        private final long priceNQT;

        private ColoredCoinsOrderPlacement(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.assetId = Convert.zeroToNull(buffer.getLong());
            this.quantityQNT = buffer.getLong();
            this.priceNQT = buffer.getLong();
        }

        private ColoredCoinsOrderPlacement(JSONObject attachmentData) {
            super(attachmentData);
            this.assetId = Convert.parseUnsignedLong((String) attachmentData.get("asset"));
            this.quantityQNT = Convert.parseLong(attachmentData.get("quantityQNT"));
            this.priceNQT = Convert.parseLong(attachmentData.get("priceNQT"));
        }

        private ColoredCoinsOrderPlacement(Long assetId, long quantityQNT, long priceNQT) {
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
            buffer.putLong(Convert.nullToZero(assetId));
            buffer.putLong(quantityQNT);
            buffer.putLong(priceNQT);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("asset", Convert.toUnsignedLong(assetId));
            attachment.put("quantityQNT", quantityQNT);
            attachment.put("priceNQT", priceNQT);
        }

        public Long getAssetId() {
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

        public ColoredCoinsAskOrderPlacement(Long assetId, long quantityQNT, long priceNQT) {
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

        public ColoredCoinsBidOrderPlacement(Long assetId, long quantityQNT, long priceNQT) {
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

        private final Long orderId;

        private ColoredCoinsOrderCancellation(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.orderId = Convert.zeroToNull(buffer.getLong());
        }

        private ColoredCoinsOrderCancellation(JSONObject attachmentData) {
            super(attachmentData);
            this.orderId = Convert.parseUnsignedLong((String) attachmentData.get("order"));
        }

        private ColoredCoinsOrderCancellation(Long orderId) {
            this.orderId = orderId;
        }

        @Override
        int getMySize() {
            return 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(Convert.nullToZero(orderId));
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("order", Convert.toUnsignedLong(orderId));
        }

        public Long getOrderId() {
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

        public ColoredCoinsAskOrderCancellation(Long orderId) {
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

        public ColoredCoinsBidOrderCancellation(Long orderId) {
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

        private final Long goodsId;

        DigitalGoodsDelisting(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.goodsId = buffer.getLong();
        }

        DigitalGoodsDelisting(JSONObject attachmentData) {
            super(attachmentData);
            this.goodsId = Convert.parseUnsignedLong((String)attachmentData.get("goods"));
        }

        public DigitalGoodsDelisting(Long goodsId) {
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

        public Long getGoodsId() { return goodsId; }

    }

    public final static class DigitalGoodsPriceChange extends AbstractAttachment {

        private final Long goodsId;
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

        public DigitalGoodsPriceChange(Long goodsId, long priceNQT) {
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

        public Long getGoodsId() { return goodsId; }

        public long getPriceNQT() { return priceNQT; }

    }

    public final static class DigitalGoodsQuantityChange extends AbstractAttachment {

        private final Long goodsId;
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

        public DigitalGoodsQuantityChange(Long goodsId, int deltaQuantity) {
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

        public Long getGoodsId() { return goodsId; }

        public int getDeltaQuantity() { return deltaQuantity; }

    }

    public final static class DigitalGoodsPurchase extends AbstractAttachment {

        private final Long goodsId;
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

        public DigitalGoodsPurchase(Long goodsId, int quantity, long priceNQT, int deliveryDeadlineTimestamp) {
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

        public Long getGoodsId() { return goodsId; }

        public int getQuantity() { return quantity; }

        public long getPriceNQT() { return priceNQT; }

        public int getDeliveryDeadlineTimestamp() { return deliveryDeadlineTimestamp; }

    }

    public final static class DigitalGoodsDelivery extends AbstractAttachment {

        private final Long purchaseId;
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

        public DigitalGoodsDelivery(Long purchaseId, EncryptedData goods, boolean goodsIsText, long discountNQT) {
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

        public Long getPurchaseId() { return purchaseId; }

        public EncryptedData getGoods() { return goods; }

        public long getDiscountNQT() { return discountNQT; }

        public boolean goodsIsText() {
            return goodsIsText;
        }

    }

    public final static class DigitalGoodsFeedback extends AbstractAttachment {

        private final Long purchaseId;

        DigitalGoodsFeedback(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.purchaseId = buffer.getLong();
        }

        DigitalGoodsFeedback(JSONObject attachmentData) {
            super(attachmentData);
            this.purchaseId = Convert.parseUnsignedLong((String)attachmentData.get("purchase"));
        }

        public DigitalGoodsFeedback(Long purchaseId) {
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

        public Long getPurchaseId() { return purchaseId; }

    }

    public final static class DigitalGoodsRefund extends AbstractAttachment {

        private final Long purchaseId;
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

        public DigitalGoodsRefund(Long purchaseId, long refundNQT) {
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

        public Long getPurchaseId() { return purchaseId; }

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

    public final static class MonetarySystemCurrencyIssuance extends AbstractAttachment {

        private final String name;
        private final String code;
        private final String description;
        private final byte type;
        private final long totalSupply;
        private final int issuanceHeight;
        private final long minReservePerUnitNQT;
        private final byte minDifficulty;
        private final byte maxDifficulty;
        private final byte ruleset;

        MonetarySystemCurrencyIssuance(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            super(buffer, transactionVersion);
            this.name = Convert.readString(buffer, buffer.get(), Constants.MAX_CURRENCY_NAME_LENGTH);
            byte[] codeBytes = new byte[Constants.CURRENCY_CODE_LENGTH];
            buffer.get(codeBytes);
            this.code = Convert.toString(codeBytes);
            this.description = Convert.readString(buffer, buffer.getShort(), Constants.MAX_CURRENCY_DESCRIPTION_LENGTH);
            this.type = buffer.get();
            this.totalSupply = buffer.getLong();
            this.issuanceHeight = buffer.getInt();
            this.minReservePerUnitNQT = buffer.getLong();
            this.minDifficulty = buffer.get();
            this.maxDifficulty = buffer.get();
            this.ruleset = buffer.get();
        }

        MonetarySystemCurrencyIssuance(JSONObject attachmentData) throws NxtException.NotValidException {
            super(attachmentData);
            this.name = (String)attachmentData.get("name");
            this.code = (String)attachmentData.get("code");
            this.description = (String)attachmentData.get("description");
            this.type = ((Long)attachmentData.get("type")).byteValue();
            this.totalSupply = (Long)attachmentData.get("totalSupply");
            this.issuanceHeight = ((Long)attachmentData.get("issuanceHeight")).intValue();
            this.minReservePerUnitNQT = (Long)attachmentData.get("minReservePerUnitNQT");
            this.minDifficulty = ((Long)attachmentData.get("minDifficulty")).byteValue();
            this.maxDifficulty = ((Long)attachmentData.get("maxDifficulty")).byteValue();
            this.ruleset = ((Long)attachmentData.get("ruleset")).byteValue();
        }

        public MonetarySystemCurrencyIssuance(String name, String code, String description, byte type, long totalSupply,
                                              int issuanceHeight, long minReservePerUnitNQT, byte minDifficulty, byte maxDifficulty, byte ruleset) {
            this.name = name;
            this.code = code;
            this.description = description;
            this.type = type;
            this.totalSupply = totalSupply;
            this.issuanceHeight = issuanceHeight;
            this.minReservePerUnitNQT = minReservePerUnitNQT;
            this.minDifficulty = minDifficulty;
            this.maxDifficulty = maxDifficulty;
            this.ruleset = ruleset;
        }

        @Override
        String getAppendixName() {
            return "CurrencyIssuance";
        }

        @Override
        int getMySize() {
            return 1 + Convert.toBytes(name).length + Constants.CURRENCY_CODE_LENGTH + 2 +
                    Convert.toBytes(description).length + 1 + 8 + 4 + 8 + 1 + 1 + 1;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            byte[] name = Convert.toBytes(this.name);
            byte[] description = Convert.toBytes(this.description);
            buffer.put((byte)name.length);
            buffer.put(name);
            buffer.put(Convert.toBytes(code));
            buffer.putShort((short) description.length);
            buffer.put(description);
            buffer.put(type);
            buffer.putLong(totalSupply);
            buffer.putInt(issuanceHeight);
            buffer.putLong(minReservePerUnitNQT);
            buffer.put(minDifficulty);
            buffer.put(maxDifficulty);
            buffer.put(ruleset);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("name", name);
            attachment.put("code", code);
            attachment.put("description", description);
            attachment.put("type", type);
            attachment.put("totalSupply", totalSupply);
            attachment.put("issuanceHeight", issuanceHeight);
            attachment.put("minReservePerUnitNQT", minReservePerUnitNQT);
            attachment.put("minDifficulty", minDifficulty & 0xFF);
            attachment.put("maxDifficulty", maxDifficulty & 0xFF);
            attachment.put("ruleset", ruleset & 0xFF);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.MonetarySystem.CURRENCY_ISSUANCE;
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

        public long getTotalSupply() {
            return totalSupply;
        }

        public int getIssuanceHeight() {
            return issuanceHeight;
        }

        public long getMinReservePerUnitNQT() {
            return minReservePerUnitNQT;
        }

        public byte getMinDifficulty() {
            return minDifficulty;
        }

        public byte getMaxDifficulty() {
            return maxDifficulty;
        }

        public byte getRuleset() {
            return ruleset;
        }

    }

    public final static class MonetarySystemReserveIncrease extends AbstractAttachment {

        private final Long currencyId;
        private final long amountNQT;

        MonetarySystemReserveIncrease(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.currencyId = buffer.getLong();
            this.amountNQT = buffer.getLong();
        }

        MonetarySystemReserveIncrease(JSONObject attachmentData) {
            super(attachmentData);
            this.currencyId = (Long)attachmentData.get("currency");
            this.amountNQT = (Long)attachmentData.get("amountNQT");
        }

        public MonetarySystemReserveIncrease(Long currencyId, long amountNQT) {
            this.currencyId = currencyId;
            this.amountNQT = amountNQT;
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
            buffer.putLong(Convert.nullToZero(currencyId));
            buffer.putLong(amountNQT);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("currency", Convert.toUnsignedLong(currencyId));
            attachment.put("amountNQT", amountNQT);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.MonetarySystem.RESERVE_INCREASE;
        }

        public Long getCurrencyId() {
            return currencyId;
        }

        public long getAmountNQT() {
            return amountNQT;
        }

    }

    public final static class MonetarySystemReserveClaim extends AbstractAttachment {

        private final Long currencyId;
        private final long units;

        MonetarySystemReserveClaim(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.currencyId = buffer.getLong();
            this.units = buffer.getLong();
        }

        MonetarySystemReserveClaim(JSONObject attachmentData) {
            super(attachmentData);
            this.currencyId = (Long)attachmentData.get("currency");
            this.units = (Long)attachmentData.get("units");
        }

        public MonetarySystemReserveClaim(Long currencyId, long units) {
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
            return TransactionType.MonetarySystem.RESERVE_CLAIM;
        }

        public Long getCurrencyId() {
            return currencyId;
        }

        public long getUnits() {
            return units;
        }

    }

    public final static class MonetarySystemMoneyTransfer extends AbstractAttachment {

        public static final class Entry {

            private final Long recipientId;
            private final Long currencyId;
            private final long units;

            public Entry(Long recipientId, Long currencyId, long units) {
                this.recipientId = recipientId;
                this.currencyId = currencyId;
                this.units = units;
            }

            public Long getRecipientId() {
                return recipientId;
            }

            public Long getCurrencyId() {
                return currencyId;
            }

            public long getUnits() {
                return units;
            }

        }

        private final List<Entry> entries;
        private final String comment;

        MonetarySystemMoneyTransfer(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            super(buffer, transactionVersion);
            this.entries = new LinkedList<>();
            short numberOfEntries = buffer.getShort();
            for (int i = 0; i < numberOfEntries; i++) {
                Long recipientId = buffer.getLong();
                Long currencyId = buffer.getLong();
                long units = buffer.getLong();
                entries.add(new Attachment.MonetarySystemMoneyTransfer.Entry(recipientId, currencyId, units));
            }
            this.comment = Convert.readString(buffer, buffer.getShort(), Constants.MAX_MONEY_TRANSFER_COMMENT_LENGTH);
        }

        MonetarySystemMoneyTransfer(JSONObject attachmentData) {
            super(attachmentData);
            this.entries = new LinkedList<>();
            JSONArray entriesArray = (JSONArray)attachmentData.get("transfers");
            for (int i = 0; i < entriesArray.size(); i++) {
                JSONObject entryObject = (JSONObject)entriesArray.get(i);
                Long recipientId = (Long)entryObject.get("recipient");
                Long currencyId = (Long)entryObject.get("currency");
                long units = (Long)entryObject.get("units");
                entries.add(new Attachment.MonetarySystemMoneyTransfer.Entry(recipientId, currencyId, units));
            }
            this.comment = (String)attachmentData.get("comment");
        }

        public MonetarySystemMoneyTransfer(List<Entry> entries, String comment) {
            this.entries = entries;
            this.comment = Convert.nullToEmpty(comment);
        }

        @Override
        String getAppendixName() {
            return "MoneyTransfer";
        }

        @Override
        int getMySize() {
            return 2 + entries.size() * (8 + 8 + 8) + 2 + Convert.toBytes(comment).length;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            byte[] comment = Convert.toBytes(this.comment);
            buffer.putShort((short)entries.size());
            for (Entry entry : entries) {
                buffer.putLong(entry.getRecipientId());
                buffer.putLong(entry.getCurrencyId());
                buffer.putLong(entry.getUnits());
            }
            buffer.putShort((short)comment.length);
            buffer.put(comment);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            JSONArray entriesArray = new JSONArray();
            for (Entry entry : entries) {
                JSONObject entryObject = new JSONObject();
                entryObject.put("recipient", Convert.toUnsignedLong(entry.getRecipientId()));
                entryObject.put("currency", Convert.toUnsignedLong(entry.getCurrencyId()));
                entryObject.put("units", entry.getUnits());
                entriesArray.add(entryObject);
            }
            attachment.put("transfers", entriesArray);
            attachment.put("comment", comment);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.MonetarySystem.MONEY_TRANSFER;
        }

        public List<Entry> getEntries() {
            return entries;
        }

        public int getNumberOfEntries() {
            return entries.size();
        }

        public Entry getEntry(int index) {
            return entries.get(index);
        }

        public String getComment() {
            return comment;
        }

        public SuperComplexNumber getTransfer() {
            SuperComplexNumber superComplexNumber = new SuperComplexNumber();
            for (Entry entry : entries) {
                superComplexNumber.add(entry.getCurrencyId(), entry.getUnits());
            }
            return superComplexNumber;
        }

    }

    public final static class MonetarySystemExchangeOfferPublication extends AbstractAttachment {

        private final Long currencyId;
        private final long buyingRateNQT;
        private final long sellingRateNQT;
        private final long totalBuyingLimit;
        private final long totalSellingLimit;
        private final long initialBuyingSupply;
        private final long initialSellingSupply;
        private final int expirationHeight;

        MonetarySystemExchangeOfferPublication(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.currencyId = buffer.getLong();
            this.buyingRateNQT = buffer.getLong();
            this.sellingRateNQT = buffer.getLong();
            this.totalBuyingLimit = buffer.getLong();
            this.totalSellingLimit = buffer.getLong();
            this.initialBuyingSupply = buffer.getLong();
            this.initialSellingSupply = buffer.getLong();
            this.expirationHeight = buffer.getInt();
        }

        MonetarySystemExchangeOfferPublication(JSONObject attachmentData) {
            super(attachmentData);
            this.currencyId = (Long)attachmentData.get("currency");
            this.buyingRateNQT = (Long)attachmentData.get("buyingRateNQT");
            this.sellingRateNQT = (Long)attachmentData.get("sellingRateNQT");
            this.totalBuyingLimit = (Long)attachmentData.get("totalBuyingLimit");
            this.totalSellingLimit = (Long)attachmentData.get("totalSellingLimit");
            this.initialBuyingSupply = (Long)attachmentData.get("initialBuyingSupply");
            this.initialSellingSupply = (Long)attachmentData.get("initialSellingSupply");
            this.expirationHeight = ((Long)attachmentData.get("expirationHeight")).intValue();
        }

        public MonetarySystemExchangeOfferPublication(Long currencyId, long buyingRateNQT, long sellingRateNQT, long totalBuyingLimit,
                                                      long totalSellingLimit, long initialBuyingSupply, long initialSellingSupply, int expirationHeight) {
            this.currencyId = currencyId;
            this.buyingRateNQT = buyingRateNQT;
            this.sellingRateNQT = sellingRateNQT;
            this.totalBuyingLimit = totalBuyingLimit;
            this.totalSellingLimit = totalSellingLimit;
            this.initialBuyingSupply = initialBuyingSupply;
            this.initialSellingSupply = initialSellingSupply;
            this.expirationHeight = expirationHeight;
        }

        @Override
        String getAppendixName() {
            return "ExchangeOfferPublication";
        }

        @Override
        int getMySize() {
            return 8 + 8 + 8 + 8 + 8 + 8 + 8 + 4;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(currencyId);
            buffer.putLong(buyingRateNQT);
            buffer.putLong(sellingRateNQT);
            buffer.putLong(totalBuyingLimit);
            buffer.putLong(totalSellingLimit);
            buffer.putLong(initialBuyingSupply);
            buffer.putLong(initialSellingSupply);
            buffer.putInt(expirationHeight);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("currency", Convert.toUnsignedLong(currencyId));
            attachment.put("buyingRateNQT", buyingRateNQT);
            attachment.put("sellingRateNQT", sellingRateNQT);
            attachment.put("totalBuyingLimit", totalBuyingLimit);
            attachment.put("totalSellingLimit", totalSellingLimit);
            attachment.put("initialBuyingSupply", initialBuyingSupply);
            attachment.put("initialSellingSupply", initialSellingSupply);
            attachment.put("expirationHeight", expirationHeight);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.MonetarySystem.EXCHANGE_OFFER_PUBLICATION;
        }

        public Long getCurrencyId() {
            return currencyId;
        }

        public long getBuyingRateNQT() {
            return buyingRateNQT;
        }

        public long getSellingRateNQT() {
            return sellingRateNQT;
        }

        public long getTotalBuyingLimit() {
            return totalBuyingLimit;
        }

        public long getTotalSellingLimit() {
            return totalSellingLimit;
        }

        public long getInitialBuyingSupply() {
            return initialBuyingSupply;
        }

        public long getInitialSellingSupply() {
            return initialSellingSupply;
        }

        public int getExpirationHeight() {
            return expirationHeight;
        }

    }

    public final static class MonetarySystemExchange extends AbstractAttachment {

        private final Long currencyId;
        private final long rateNQT;
        private final long units;

        MonetarySystemExchange(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.currencyId = buffer.getLong();
            this.rateNQT = buffer.getLong();
            this.units = buffer.getLong();
        }

        MonetarySystemExchange(JSONObject attachmentData) {
            super(attachmentData);
            this.currencyId = (Long)attachmentData.get("currency");
            this.rateNQT = (Long)attachmentData.get("rateNQT");
            this.units = (Long)attachmentData.get("units");
        }

        public MonetarySystemExchange(Long currencyId, long rateNQT, long units) {
            this.currencyId = currencyId;
            this.rateNQT = rateNQT;
            this.units = units;
        }

        @Override
        String getAppendixName() {
            return "Exchange";
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
        public TransactionType getTransactionType() {
            return TransactionType.MonetarySystem.EXCHANGE;
        }

        public Long getCurrencyId() {
            return currencyId;
        }

        public long getRateNQT() {
            return rateNQT;
        }

        public long getUnits() {
            return units;
        }

        public boolean isPurchase() {
            return units > 0;
        }

    }

    public final static class MonetarySystemMoneyMinting extends AbstractAttachment {

        private final long nonce;
        private final Long currencyId;
        private final int units;
        private final int counter;

        MonetarySystemMoneyMinting(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.nonce = buffer.getLong();
            this.currencyId = buffer.getLong();
            this.units = buffer.getInt();
            this.counter = buffer.getInt();
        }

        MonetarySystemMoneyMinting(JSONObject attachmentData) {
            super(attachmentData);
            this.nonce = (Long)attachmentData.get("nonce");
            this.currencyId = (Long)attachmentData.get("currency");
            this.units = ((Long)attachmentData.get("units")).intValue();
            this.counter = ((Long)attachmentData.get("counter")).intValue();
        }

        public MonetarySystemMoneyMinting(long nonce, Long currencyId, int units, int counter) {
            this.nonce = nonce;
            this.currencyId = currencyId;
            this.units = units;
            this.counter = counter;
        }

        @Override
        String getAppendixName() {
            return "MoneyMinting";
        }

        @Override
        int getMySize() {
            return 8 + 8 + 4 + 4;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(nonce);
            buffer.putLong(currencyId);
            buffer.putInt(units);
            buffer.putInt(counter);
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
            return TransactionType.MonetarySystem.MONEY_MINTING;
        }

        public long getNonce() {
            return nonce;
        }

        public Long getCurrencyId() {
            return currencyId;
        }

        public int getUnits() {
            return units;
        }

        public int getCounter() {
            return counter;
        }

    }

    public final static class MonetarySystemShufflingInitiation extends AbstractAttachment {

        private final Long currencyId;
        private final long amount;
        private final byte numberOfParticipants;
        private final short maxInitiationDelay;
        private final short maxContinuationDelay;
        private final short maxFinalizationDelay;
        private final short maxCancellationDelay;

        MonetarySystemShufflingInitiation(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.currencyId = buffer.getLong();
            this.amount = buffer.getLong();
            this.numberOfParticipants = buffer.get();
            this.maxInitiationDelay = buffer.getShort();
            this.maxContinuationDelay = buffer.getShort();
            this.maxFinalizationDelay = buffer.getShort();
            this.maxCancellationDelay = buffer.getShort();
        }

        MonetarySystemShufflingInitiation(JSONObject attachmentData) {
            super(attachmentData);
            this.currencyId = Convert.parseUnsignedLong((String)attachmentData.get("currency"));
            this.amount = (Long)attachmentData.get("amount");
            this.numberOfParticipants = ((Long)attachmentData.get("numberOfParticipants")).byteValue();
            this.maxInitiationDelay = ((Long)attachmentData.get("maxInitiationDelay")).shortValue();
            this.maxContinuationDelay = ((Long)attachmentData.get("maxContinuationDelay")).shortValue();
            this.maxFinalizationDelay = ((Long)attachmentData.get("maxFinalizationDelay")).shortValue();
            this.maxCancellationDelay = ((Long)attachmentData.get("maxCancellationDelay")).shortValue();
        }

        MonetarySystemShufflingInitiation(Long currencyId, long amount, byte numberOfParticipants, short maxInitiationDelay, short maxContinuationDelay, short maxFinalizationDelay, short maxCancellationDelay) {
            this.currencyId = currencyId;
            this.amount = amount;
            this.numberOfParticipants = numberOfParticipants;
            this.maxInitiationDelay = maxInitiationDelay;
            this.maxContinuationDelay = maxContinuationDelay;
            this.maxFinalizationDelay = maxFinalizationDelay;
            this.maxCancellationDelay = maxCancellationDelay;
        }

        @Override
        String getAppendixName() {
            return "ShufflingInitiation";
        }

        @Override
        int getMySize() {
            return 8 + 8 + 1 + 2 + 2 + 2 + 2;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(currencyId);
            buffer.putLong(amount);
            buffer.put(numberOfParticipants);
            buffer.putShort(maxInitiationDelay);
            buffer.putShort(maxContinuationDelay);
            buffer.putShort(maxFinalizationDelay);
            buffer.putShort(maxCancellationDelay);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("currency", Convert.toUnsignedLong(currencyId));
            attachment.put("amount", amount);
            attachment.put("numberOfParticipants", numberOfParticipants);
            attachment.put("maxInitiationDelay", maxInitiationDelay);
            attachment.put("maxContinuationDelay", maxContinuationDelay);
            attachment.put("maxFinalizationDelay", maxFinalizationDelay);
            attachment.put("maxCancellationDelay", maxCancellationDelay);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.MonetarySystem.SHUFFLING_INITIATION;
        }

        public Long getCurrencyId() {
            return currencyId;
        }

        public long getAmount() {
            return amount;
        }

        public byte getNumberOfParticipants() {
            return numberOfParticipants;
        }

        public short getMaxInitiationDelay() {
            return maxInitiationDelay;
        }

        public short getMaxContinuationDelay() {
            return maxContinuationDelay;
        }

        public short getMaxFinalizationDelay() {
            return maxFinalizationDelay;
        }

        public short getMaxCancellationDelay() {
            return maxCancellationDelay;
        }
    }

    public final static class MonetarySystemShufflingContinuation extends AbstractAttachment {

        private final Long shufflingId;
        private final EncryptedData recipients;

        MonetarySystemShufflingContinuation(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            super(buffer, transactionVersion);
            this.shufflingId = buffer.getLong();
            this.recipients = EncryptedData.readEncryptedData(buffer, buffer.getInt(), Constants.MAX_SHUFFLING_RECIPIENTS_LENGTH, this.shufflingId);
        }

        MonetarySystemShufflingContinuation(JSONObject attachmentData) {
            super(attachmentData);
            this.shufflingId = Convert.parseUnsignedLong((String)attachmentData.get("shuffling"));
            this.recipients = new EncryptedData(Convert.parseHexString((String)attachmentData.get("recipients")), this.shufflingId);
        }

        MonetarySystemShufflingContinuation(Long shufflingId, EncryptedData recipients) {
            this.shufflingId = shufflingId;
            this.recipients = recipients;
        }

        @Override
        String getAppendixName() {
            return "ShufflingContinuation";
        }

        @Override
        int getMySize() {
            return 8 + 4 + recipients.getData().length;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(shufflingId);
            buffer.putInt(recipients.getData().length);
            buffer.put(recipients.getData());
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("shuffling", Convert.toUnsignedLong(shufflingId));
            attachment.put("recipients", Convert.toHexString(recipients.getData()));

        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.MonetarySystem.SHUFFLING_CONTINUATION;
        }

        public Long getShufflingId() {
            return shufflingId;
        }

        public EncryptedData getRecipients() {
            return recipients;
        }
    }

    public final static class MonetarySystemShufflingFinalization extends AbstractAttachment {

        private final Long shufflingId;
        private final Long[] recipients;

        MonetarySystemShufflingFinalization(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.shufflingId = buffer.getLong();
            byte numberOfEntries = buffer.get();
            this.recipients = new Long[numberOfEntries];
            for (int i = 0; i < this.recipients.length; i++) {
                this.recipients[i] = buffer.getLong();
            }
            Arrays.sort(this.recipients);
        }

        MonetarySystemShufflingFinalization(JSONObject attachmentData) {
            super(attachmentData);
            this.shufflingId = Convert.parseUnsignedLong((String)attachmentData.get("shuffling"));
            JSONArray recipientsData = (JSONArray)attachmentData.get("recipients");
            this.recipients = new Long[recipientsData.size()];
            for (int i = 0; i < this.recipients.length; i++) {
                this.recipients[i] = Convert.parseUnsignedLong((String)recipientsData.get(i));
            }
            Arrays.sort(this.recipients);
        }

        MonetarySystemShufflingFinalization(Long shufflingId, Long[] recipients) {
            this.shufflingId = shufflingId;
            this.recipients = recipients;
        }

        @Override
        String getAppendixName() {
            return "ShufflingFinalization";
        }

        @Override
        int getMySize() {
            return 8 + 1 + recipients.length * 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(shufflingId);
            buffer.put((byte)recipients.length);
            for (Long recipient : recipients) {
                buffer.putLong(recipient);
            }
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("shuffling", Convert.toUnsignedLong(shufflingId));
            JSONArray recipientsData = new JSONArray();
            for (Long recipient : recipients) {
                recipientsData.add(Convert.toUnsignedLong(recipient));
            }
            attachment.put("recipients", recipientsData);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.MonetarySystem.SHUFFLING_FINALIZATION;
        }

        public Long getShufflingId() {
            return shufflingId;
        }

        public Long[] getRecipients() {
            return recipients;
        }
    }


    public final static class MonetarySystemShufflingCancellation extends AbstractAttachment {

        private final Long shufflingId;
        private final byte[] keys;

        MonetarySystemShufflingCancellation(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.shufflingId = buffer.getLong();
            this.keys = new byte[32 * buffer.get()];
            buffer.get(this.keys);
        }

        MonetarySystemShufflingCancellation(JSONObject attachmentData) {
            super(attachmentData);
            this.shufflingId = Convert.parseUnsignedLong((String)attachmentData.get("shuffling"));
            this.keys = Convert.parseHexString((String)attachmentData.get("keys"));
        }

        MonetarySystemShufflingCancellation(Long shufflingId, byte[] keys) {
            this.shufflingId = shufflingId;
            this.keys = keys;
        }

        @Override
        String getAppendixName() {
            return "ShufflingCancellation";
        }

        @Override
        int getMySize() {
            return 8 + 1 + keys.length;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(shufflingId);
            buffer.put((byte)(keys.length / 32));
            buffer.put(keys);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("shuffling", Convert.toUnsignedLong(shufflingId));
            attachment.put("keys", Convert.toHexString(keys));
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.MonetarySystem.SHUFFLING_CANCELLATION;
        }

        public Long getShufflingId() {
            return shufflingId;
        }

        public byte[] getKeys() {
            return keys;
        }
    }

}
