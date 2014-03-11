package nxt;

import nxt.crypto.XoredData;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;

public interface Attachment {

    public int getSize();
    public byte[] getBytes();
    public JSONStreamAware getJSON();

    TransactionType getTransactionType();


    public final static class MessagingArbitraryMessage implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        private final byte[] message;

        public MessagingArbitraryMessage(byte[] message) {

            this.message = message;

        }

        @Override
        public int getSize() {
            return 4 + message.length;
        }

        @Override
        public byte[] getBytes() {

            ByteBuffer buffer = ByteBuffer.allocate(getSize());
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(message.length);
            buffer.put(message);

            return buffer.array();

        }

        @Override
        public JSONStreamAware getJSON() {

            JSONObject attachment = new JSONObject();
            attachment.put("message", Convert.toHexString(message));

            return attachment;

        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ARBITRARY_MESSAGE;
        }

        public byte[] getMessage() {
            return message;
        }
    }

    public final static class MessagingAliasAssignment implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        private final String aliasName;
        private final String aliasURI;

        public MessagingAliasAssignment(String aliasName, String aliasURI) {

            this.aliasName = aliasName.trim().intern();
            this.aliasURI = aliasURI.trim().intern();

        }

        @Override
        public int getSize() {
            try {
                return 1 + aliasName.getBytes("UTF-8").length + 2 + aliasURI.getBytes("UTF-8").length;
            } catch (RuntimeException|UnsupportedEncodingException e) {
                Logger.logMessage("Error in getBytes", e);
                return 0;
            }
        }

        @Override
        public byte[] getBytes() {

            try {

                byte[] alias = this.aliasName.getBytes("UTF-8");
                byte[] uri = this.aliasURI.getBytes("UTF-8");

                ByteBuffer buffer = ByteBuffer.allocate(1 + alias.length + 2 + uri.length);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.put((byte)alias.length);
                buffer.put(alias);
                buffer.putShort((short)uri.length);
                buffer.put(uri);

                return buffer.array();

            } catch (RuntimeException|UnsupportedEncodingException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;

            }

        }

        @Override
        public JSONStreamAware getJSON() {

            JSONObject attachment = new JSONObject();
            attachment.put("alias", aliasName);
            attachment.put("uri", aliasURI);

            return attachment;

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

    public final static class MessagingPollCreation implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        private final String pollName;
        private final String pollDescription;
        private final String[] pollOptions;
        private final byte minNumberOfOptions, maxNumberOfOptions;
        private final boolean optionsAreBinary;

        public MessagingPollCreation(String pollName, String pollDescription, String[] pollOptions, byte minNumberOfOptions, byte maxNumberOfOptions, boolean optionsAreBinary) {

            this.pollName = pollName;
            this.pollDescription = pollDescription;
            this.pollOptions = pollOptions;
            this.minNumberOfOptions = minNumberOfOptions;
            this.maxNumberOfOptions = maxNumberOfOptions;
            this.optionsAreBinary = optionsAreBinary;

        }

        @Override
        public int getSize() {
            try {
                int size = 2 + pollName.getBytes("UTF-8").length + 2 + pollDescription.getBytes("UTF-8").length + 1;
                for (String pollOption : pollOptions) {
                    size += 2 + pollOption.getBytes("UTF-8").length;
                }
                size +=  1 + 1 + 1;
                return size;
            } catch (RuntimeException|UnsupportedEncodingException e) {
                Logger.logMessage("Error in getBytes", e);
                return 0;
            }
        }

        @Override
        public byte[] getBytes() {

            try {

                byte[] name = this.pollName.getBytes("UTF-8");
                byte[] description = this.pollDescription.getBytes("UTF-8");
                byte[][] options = new byte[this.pollOptions.length][];
                for (int i = 0; i < this.pollOptions.length; i++) {
                    options[i] = this.pollOptions[i].getBytes("UTF-8");
                }

                ByteBuffer buffer = ByteBuffer.allocate(getSize());
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.putShort((short)name.length);
                buffer.put(name);
                buffer.putShort((short)description.length);
                buffer.put(description);
                buffer.put((byte)options.length);
                for (byte[] option : options) {
                    buffer.putShort((short) option.length);
                    buffer.put(option);
                }
                buffer.put(this.minNumberOfOptions);
                buffer.put(this.maxNumberOfOptions);
                buffer.put(this.optionsAreBinary ? (byte)1 : (byte)0);

                return buffer.array();

            } catch (RuntimeException | UnsupportedEncodingException e) {

                Logger.logMessage("Error in getBytes", e);
                return null;

            }

        }

        @Override
        public JSONStreamAware getJSON() {

            JSONObject attachment = new JSONObject();
            attachment.put("name", this.pollName);
            attachment.put("description", this.pollDescription);
            JSONArray options = new JSONArray();
            Collections.addAll(options, this.pollOptions);
            attachment.put("options", options);
            attachment.put("minNumberOfOptions", this.minNumberOfOptions);
            attachment.put("maxNumberOfOptions", this.maxNumberOfOptions);
            attachment.put("optionsAreBinary", this.optionsAreBinary);

            return attachment;

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

    public final static class MessagingVoteCasting implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        private final Long pollId;
        private final byte[] pollVote;

        public MessagingVoteCasting(Long pollId, byte[] pollVote) {

            this.pollId = pollId;
            this.pollVote = pollVote;

        }

        @Override
        public int getSize() {
            return 8 + 1 + this.pollVote.length;
        }

        @Override
        public byte[] getBytes() {

            ByteBuffer buffer = ByteBuffer.allocate(getSize());
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putLong(this.pollId);
            buffer.put((byte)this.pollVote.length);
            buffer.put(this.pollVote);

            return buffer.array();

        }

        @Override
        public JSONStreamAware getJSON() {

            JSONObject attachment = new JSONObject();
            attachment.put("pollId", Convert.toUnsignedLong(this.pollId));
            JSONArray vote = new JSONArray();
            for (byte aPollVote : this.pollVote) {
                vote.add(aPollVote);
            }
            attachment.put("vote", vote);

            return attachment;

        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.VOTE_CASTING;
        }

        public Long getPollId() { return pollId; }

        public byte[] getPollVote() { return pollVote; }

    }

    public final static class MessagingHubTerminalAnnouncement implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        private final String[] uris;

        public MessagingHubTerminalAnnouncement(String[] uris) {
            this.uris = uris;
        }

        @Override
        public int getSize() {
            try {
                int size = 2;
                for (String uri : uris) {
                    size += 2 + uri.getBytes("UTF-8").length;
                }
                return size;
            } catch (RuntimeException|UnsupportedEncodingException e) {
                Logger.logMessage("Error in getBytes", e);
                return 0;
            }
        }

        @Override
        public byte[] getBytes() {

            try {
                ByteBuffer buffer = ByteBuffer.allocate(getSize());
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.putShort((short)uris.length);
                for (String uri : uris) {
                    byte[] uriBytes = uri.getBytes("UTF-8");
                    buffer.putShort((short)uriBytes.length);
                    buffer.put(uriBytes);
                }
                return buffer.array();
            } catch (RuntimeException|UnsupportedEncodingException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;
            }

        }

        @Override
        public JSONStreamAware getJSON() {

            JSONObject attachment = new JSONObject();
            JSONArray uris = new JSONArray();
            for (String uri : this.uris) {
                uris.add(uri);
            }
            attachment.put("uris", uris);
            return attachment;

        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.HUB_TERMINAL_ANNOUNCEMENT;
        }

    }

    public final static class ColoredCoinsAssetIssuance implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        private final String name;
        private final String description;
        private final int quantity;

        public ColoredCoinsAssetIssuance(String name, String description, int quantity) {

            this.name = name;
            this.description = description == null ? "" : description;
            this.quantity = quantity;

        }

        @Override
        public int getSize() {
            try {
                return 1 + name.getBytes("UTF-8").length + 2 + description.getBytes("UTF-8").length + 4;
            } catch (RuntimeException|UnsupportedEncodingException e) {
                Logger.logMessage("Error in getBytes", e);
                return 0;
            }
        }

        @Override
        public byte[] getBytes() {

            try {
                byte[] name = this.name.getBytes("UTF-8");
                byte[] description = this.description.getBytes("UTF-8");

                ByteBuffer buffer = ByteBuffer.allocate(1 + name.length + 2 + description.length + 4);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.put((byte)name.length);
                buffer.put(name);
                buffer.putShort((short)description.length);
                buffer.put(description);
                buffer.putInt(quantity);

                return buffer.array();
            } catch (RuntimeException|UnsupportedEncodingException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;
            }

        }

        @Override
        public JSONStreamAware getJSON() {

            JSONObject attachment = new JSONObject();
            attachment.put("name", name);
            attachment.put("description", description);
            attachment.put("quantity", quantity);

            return attachment;

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

        public int getQuantity() {
            return quantity;
        }
    }

    public final static class ColoredCoinsAssetTransfer implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        private final Long assetId;
        private final int quantity;

        public ColoredCoinsAssetTransfer(Long assetId, int quantity) {

            this.assetId = assetId;
            this.quantity = quantity;

        }

        @Override
        public int getSize() {
            return 8 + 4;
        }

        @Override
        public byte[] getBytes() {

            ByteBuffer buffer = ByteBuffer.allocate(getSize());
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putLong(Convert.nullToZero(assetId));
            buffer.putInt(quantity);

            return buffer.array();

        }

        @Override
        public JSONStreamAware getJSON() {

            JSONObject attachment = new JSONObject();
            attachment.put("asset", Convert.toUnsignedLong(assetId));
            attachment.put("quantity", quantity);

            return attachment;

        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.ASSET_TRANSFER;
        }

        public Long getAssetId() {
            return assetId;
        }

        public int getQuantity() {
            return quantity;
        }
    }

    abstract static class ColoredCoinsOrderPlacement implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        private final Long assetId;
        private final int quantity;
        private final long price;

        private ColoredCoinsOrderPlacement(Long assetId, int quantity, long price) {

            this.assetId = assetId;
            this.quantity = quantity;
            this.price = price;

        }

        @Override
        public int getSize() {
            return 8 + 4 + 8;
        }

        @Override
        public byte[] getBytes() {

            ByteBuffer buffer = ByteBuffer.allocate(getSize());
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putLong(Convert.nullToZero(assetId));
            buffer.putInt(quantity);
            buffer.putLong(price);

            return buffer.array();

        }

        @Override
        public JSONStreamAware getJSON() {

            JSONObject attachment = new JSONObject();
            attachment.put("asset", Convert.toUnsignedLong(assetId));
            attachment.put("quantity", quantity);
            attachment.put("price", price);

            return attachment;

        }

        public Long getAssetId() {
            return assetId;
        }

        public int getQuantity() {
            return quantity;
        }

        public long getPrice() {
            return price;
        }
    }

    public final static class ColoredCoinsAskOrderPlacement extends ColoredCoinsOrderPlacement {

        static final long serialVersionUID = 0;

        public ColoredCoinsAskOrderPlacement(Long assetId, int quantity, long price) {
            super(assetId, quantity, price);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.ASK_ORDER_PLACEMENT;
        }

    }

    public final static class ColoredCoinsBidOrderPlacement extends ColoredCoinsOrderPlacement {

        static final long serialVersionUID = 0;

        public ColoredCoinsBidOrderPlacement(Long assetId, int quantity, long price) {
            super(assetId, quantity, price);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.BID_ORDER_PLACEMENT;
        }

    }

    abstract static class ColoredCoinsOrderCancellation implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        private final Long orderId;

        private ColoredCoinsOrderCancellation(Long orderId) {
            this.orderId = orderId;
        }

        @Override
        public int getSize() {
            return 8;
        }

        @Override
        public byte[] getBytes() {

            ByteBuffer buffer = ByteBuffer.allocate(getSize());
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putLong(Convert.nullToZero(orderId));

            return buffer.array();

        }

        @Override
        public JSONStreamAware getJSON() {

            JSONObject attachment = new JSONObject();
            attachment.put("order", Convert.toUnsignedLong(orderId));

            return attachment;

        }

        public Long getOrderId() {
            return orderId;
        }
    }

    public final static class ColoredCoinsAskOrderCancellation extends ColoredCoinsOrderCancellation {

        static final long serialVersionUID = 0;

        public ColoredCoinsAskOrderCancellation(Long orderId) {
            super(orderId);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.ASK_ORDER_CANCELLATION;
        }

    }

    public final static class ColoredCoinsBidOrderCancellation extends ColoredCoinsOrderCancellation {

        static final long serialVersionUID = 0;

        public ColoredCoinsBidOrderCancellation(Long orderId) {
            super(orderId);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.BID_ORDER_CANCELLATION;
        }

    }

    public final static class DigitalGoodsListing implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        private final String name;
        private final String description;
        private final String tags;
        private final int quantity;
        private final long price;

        public DigitalGoodsListing(String name, String description, String tags, int quantity, long price) {
            this.name = name;
            this.description = description;
            this.tags = tags;
            this.quantity = quantity;
            this.price = price;
        }

        @Override
        public int getSize() {
            return 0;
        }

        @Override
        public byte[] getBytes() {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(getSize());
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                return buffer.array();
            } catch (RuntimeException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;
            }
        }

        @Override
        public JSONStreamAware getJSON() {
            JSONObject attachment = new JSONObject();
            return attachment;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.LISTING;
        }

    }

    public final static class DigitalGoodsDelisting implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        private final Long goodsId;

        public DigitalGoodsDelisting(Long goodsId) {
            this.goodsId = goodsId;
        }

        @Override
        public int getSize() {
            return 0;
        }

        @Override
        public byte[] getBytes() {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(getSize());
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                return buffer.array();
            } catch (RuntimeException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;
            }
        }

        @Override
        public JSONStreamAware getJSON() {
            JSONObject attachment = new JSONObject();
            return attachment;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.DELISTING;
        }

    }

    public final static class DigitalGoodsPriceChange implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        private final Long goodsId;
        private final long price;

        public DigitalGoodsPriceChange(Long goodsId, long price) {
            this.goodsId = goodsId;
            this.price = price;
        }

        @Override
        public int getSize() {
            return 0;
        }

        @Override
        public byte[] getBytes() {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(getSize());
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                return buffer.array();
            } catch (RuntimeException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;
            }
        }

        @Override
        public JSONStreamAware getJSON() {
            JSONObject attachment = new JSONObject();
            return attachment;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.PRICE_CHANGE;
        }

    }

    public final static class DigitalGoodsQuantityChange implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        private final Long goodsId;
        private final int deltaQuantity;

        public DigitalGoodsQuantityChange(Long goodsId, int deltaQuantity) {
            this.goodsId = goodsId;
            this.deltaQuantity = deltaQuantity;
        }

        @Override
        public int getSize() {
            return 0;
        }

        @Override
        public byte[] getBytes() {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(getSize());
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                return buffer.array();
            } catch (RuntimeException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;
            }
        }

        @Override
        public JSONStreamAware getJSON() {
            JSONObject attachment = new JSONObject();
            return attachment;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.QUANTITY_CHANGE;
        }

    }

    public final static class DigitalGoodsPurchase implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        private final Long goodsId;
        private final int quantity;
        private final long price;
        private final XoredData note;

        public DigitalGoodsPurchase(Long goodsId, int quantity, long price, XoredData note) {
            this.goodsId = goodsId;
            this.quantity = quantity;
            this.price = price;
            this.note = note;
        }

        @Override
        public int getSize() {
            return 0;
        }

        @Override
        public byte[] getBytes() {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(getSize());
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                return buffer.array();
            } catch (RuntimeException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;
            }
        }

        @Override
        public JSONStreamAware getJSON() {
            JSONObject attachment = new JSONObject();
            return attachment;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.PURCHASE;
        }

    }

    public final static class DigitalGoodsDelivery implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        private final Long purchaseId;
        private final XoredData goods;
        private final long discount;

        public DigitalGoodsDelivery(Long purchaseId, XoredData goods, long discount) {
            this.purchaseId = purchaseId;
            this.goods = goods;
            this.discount = discount;
        }

        @Override
        public int getSize() {
            return 0;
        }

        @Override
        public byte[] getBytes() {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(getSize());
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                return buffer.array();
            } catch (RuntimeException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;
            }
        }

        @Override
        public JSONStreamAware getJSON() {
            JSONObject attachment = new JSONObject();
            return attachment;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.DELIVERY;
        }

    }

    public final static class DigitalGoodsRating implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        private final Long purchaseId;
        private final byte deltaRating;
        private final String comment;

        public DigitalGoodsRating(Long purchaseId, byte deltaRating, String comment) {
            this.purchaseId = purchaseId;
            this.deltaRating = deltaRating;
            this.comment = comment;
        }

        @Override
        public int getSize() {
            return 0;
        }

        @Override
        public byte[] getBytes() {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(getSize());
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                return buffer.array();
            } catch (RuntimeException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;
            }
        }

        @Override
        public JSONStreamAware getJSON() {
            JSONObject attachment = new JSONObject();
            return attachment;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.RATING;
        }

    }

    public final static class DigitalGoodsRefund implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        private final Long purchaseId;
        private final long refund;
        private final String note;

        public DigitalGoodsRefund(Long purchaseId, long refund, String note) {
            this.purchaseId = purchaseId;
            this.refund = refund;
            this.note = note;
        }

        @Override
        public int getSize() {
            return 0;
        }

        @Override
        public byte[] getBytes() {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(getSize());
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                return buffer.array();
            } catch (RuntimeException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;
            }
        }

        @Override
        public JSONStreamAware getJSON() {
            JSONObject attachment = new JSONObject();
            return attachment;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.REFUND;
        }

    }

}
