package nxt;

import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONObject;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public interface Attachment {

    public int getSize();
    public byte[] getBytes();
    public JSONObject getJSONObject();

    Transaction.Type getTransactionType();


    public static class MessagingArbitraryMessage implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        final byte[] message;

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
        public JSONObject getJSONObject() {

            JSONObject attachment = new JSONObject();
            attachment.put("message", Convert.convert(message));

            return attachment;

        }

        @Override
        public Transaction.Type getTransactionType() {
            return Transaction.Type.Messaging.ARBITRARY_MESSAGE;
        }

    }

    public static class MessagingAliasAssignment implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        final String alias;
        final String uri;

        public MessagingAliasAssignment(String alias, String uri) {

            this.alias = alias;
            this.uri = uri;

        }

        @Override
        public int getSize() {
            try {
                return 1 + alias.getBytes("UTF-8").length + 2 + uri.getBytes("UTF-8").length;
            } catch (RuntimeException|UnsupportedEncodingException e) {
                Logger.logMessage("Error in getBytes", e);
                return 0;
            }
        }

        @Override
        public byte[] getBytes() {

            try {

                byte[] alias = this.alias.getBytes("UTF-8");
                byte[] uri = this.uri.getBytes("UTF-8");

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
        public JSONObject getJSONObject() {

            JSONObject attachment = new JSONObject();
            attachment.put("alias", alias);
            attachment.put("uri", uri);

            return attachment;

        }

        @Override
        public Transaction.Type getTransactionType() {
            return Transaction.Type.Messaging.ALIAS_ASSIGNMENT;
        }

    }

    public static class ColoredCoinsAssetIssuance implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        final String name;
        final String description;
        final int quantity;

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
        public JSONObject getJSONObject() {

            JSONObject attachment = new JSONObject();
            attachment.put("name", name);
            attachment.put("description", description);
            attachment.put("quantity", quantity);

            return attachment;

        }

        @Override
        public Transaction.Type getTransactionType() {
            return Transaction.Type.ColoredCoins.ASSET_ISSUANCE;
        }

    }

    public static class ColoredCoinsAssetTransfer implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        final long asset;
        final int quantity;

        public ColoredCoinsAssetTransfer(long asset, int quantity) {

            this.asset = asset;
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
            buffer.putLong(asset);
            buffer.putInt(quantity);

            return buffer.array();

        }

        @Override
        public JSONObject getJSONObject() {

            JSONObject attachment = new JSONObject();
            attachment.put("asset", Convert.convert(asset));
            attachment.put("quantity", quantity);

            return attachment;

        }

        @Override
        public Transaction.Type getTransactionType() {
            return Transaction.Type.ColoredCoins.ASSET_TRANSFER;
        }

    }

    abstract static class ColoredCoinsOrderPlacement implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        final long asset;
        final int quantity;
        final long price;

        private ColoredCoinsOrderPlacement(long asset, int quantity, long price) {

            this.asset = asset;
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
            buffer.putLong(asset);
            buffer.putInt(quantity);
            buffer.putLong(price);

            return buffer.array();

        }

        @Override
        public JSONObject getJSONObject() {

            JSONObject attachment = new JSONObject();
            attachment.put("asset", Convert.convert(asset));
            attachment.put("quantity", quantity);
            attachment.put("price", price);

            return attachment;

        }

    }

    public static class ColoredCoinsAskOrderPlacement extends ColoredCoinsOrderPlacement {

        static final long serialVersionUID = 0;

        public ColoredCoinsAskOrderPlacement(long asset, int quantity, long price) {
            super(asset, quantity, price);
        }

        @Override
        public Transaction.Type getTransactionType() {
            return Transaction.Type.ColoredCoins.ASK_ORDER_PLACEMENT;
        }

    }

    public static class ColoredCoinsBidOrderPlacement extends ColoredCoinsOrderPlacement {

        static final long serialVersionUID = 0;

        public ColoredCoinsBidOrderPlacement(long asset, int quantity, long price) {
            super(asset, quantity, price);
        }

        @Override
        public Transaction.Type getTransactionType() {
            return Transaction.Type.ColoredCoins.BID_ORDER_PLACEMENT;
        }

    }

    abstract static class ColoredCoinsOrderCancellation implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        final long order;

        private ColoredCoinsOrderCancellation(long order) {
            this.order = order;
        }

        @Override
        public int getSize() {
            return 8;
        }

        @Override
        public byte[] getBytes() {

            ByteBuffer buffer = ByteBuffer.allocate(getSize());
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putLong(order);

            return buffer.array();

        }

        @Override
        public JSONObject getJSONObject() {

            JSONObject attachment = new JSONObject();
            attachment.put("order", Convert.convert(order));

            return attachment;

        }

    }

    public static class ColoredCoinsAskOrderCancellation extends ColoredCoinsOrderCancellation {

        static final long serialVersionUID = 0;

        public ColoredCoinsAskOrderCancellation(long order) {
            super(order);
        }

        @Override
        public Transaction.Type getTransactionType() {
            return Transaction.Type.ColoredCoins.ASK_ORDER_CANCELLATION;
        }

    }

    public static class ColoredCoinsBidOrderCancellation extends ColoredCoinsOrderCancellation {

        static final long serialVersionUID = 0;

        public ColoredCoinsBidOrderCancellation(long order) {
            super(order);
        }

        @Override
        public Transaction.Type getTransactionType() {
            return Transaction.Type.ColoredCoins.BID_ORDER_CANCELLATION;
        }

    }

}
