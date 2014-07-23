package nxt;

import nxt.crypto.EncryptedData;
import nxt.util.Convert;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

public interface Appendix {

    int getSize(byte transactionVersion);
    void putBytes(ByteBuffer buffer, byte transactionVersion);
    JSONObject getJSONObject();
    byte getVersion();


    static abstract class AbstractAppendix implements Appendix {

        private final byte version;

        AbstractAppendix(JSONObject attachmentData, byte transactionVersion) {
            if (transactionVersion == 0) {
                version = 0;
            } else {
                version = ((Long) attachmentData.get("version")).byteValue();
            }
        }

        AbstractAppendix(ByteBuffer buffer, byte transactionVersion) {
            if (transactionVersion == 0) {
                version = 0;
            } else {
                version = buffer.get();
            }
        }

        AbstractAppendix(int version) {
            this.version = (byte) version;
        }

        @Override
        public final int getSize(byte transactionVersion) {
            return getMySize() + (transactionVersion > 0 ? 1 : 0);
        }

        abstract int getMySize();

        @Override
        public final void putBytes(ByteBuffer buffer, byte transactionVersion) {
            if (transactionVersion > 0) {
                buffer.put(version);
            }
            putMyBytes(buffer);
        }

        abstract void putMyBytes(ByteBuffer buffer);

        @Override
        public final JSONObject getJSONObject() {
            JSONObject json = new JSONObject();
            json.put("version", version);
            putMyJSON(json);
            return json;
        }

        abstract void putMyJSON(JSONObject json);

        @Override
        public final byte getVersion() {
            return version;
        }

    }

    public static class Message extends AbstractAppendix {

        static Message parse(JSONObject attachmentData, byte transactionVersion) throws NxtException.ValidationException {
            if (attachmentData.get("message") == null) {
                return null;
            }
            return new Message(attachmentData, transactionVersion);
        }

        private final byte[] message;
        private final boolean isText;

        Message(ByteBuffer buffer, byte transactionVersion) throws NxtException.ValidationException {
            super(buffer, transactionVersion);
            int messageLength = buffer.getInt();
            this.isText = messageLength < 0; // ugly hack
            if (messageLength < 0) {
                if (Nxt.getBlockchain().getHeight() < Constants.DIGITAL_GOODS_STORE_BLOCK) {
                    throw new TransactionType.NotYetEnabledException("Text messages not yet enabled");
                }
                messageLength ^= Integer.MIN_VALUE;
            }
            if (messageLength > Constants.MAX_ARBITRARY_MESSAGE_LENGTH) {
                throw new NxtException.ValidationException("Invalid arbitrary message length: " + messageLength);
            }
            this.message = new byte[messageLength];
            buffer.get(this.message);
        }

        Message(JSONObject attachmentData, byte transactionVersion) throws NxtException.ValidationException {
            super(attachmentData, transactionVersion);
            String messageString = (String)attachmentData.get("message");
            this.isText = Boolean.TRUE.equals((Boolean)attachmentData.get("messageIsText"));
            if (this.isText && Nxt.getBlockchain().getHeight() < Constants.DIGITAL_GOODS_STORE_BLOCK) {
                throw new TransactionType.NotYetEnabledException("Text messages not yet enabled");
            }
            this.message = isText ? Convert.toBytes(messageString) : Convert.parseHexString(messageString);
            if (message.length > Constants.MAX_ARBITRARY_MESSAGE_LENGTH) {
                throw new NxtException.ValidationException("Invalid arbitrary message length: " + message.length);
            }
        }

        public Message(byte[] message) {
            super(0);
            this.message = message;
            this.isText = false;
        }

        public Message(String string) {
            super(0);
            this.message = Convert.toBytes(string);
            this.isText = true;
        }

        @Override
        int getMySize() {
            return 4 + message.length;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putInt(isText ? (message.length | Integer.MIN_VALUE) : message.length);
            buffer.put(message);
        }

        @Override
        void putMyJSON(JSONObject json) {
            json.put("message", isText ? Convert.toString(message) : Convert.toHexString(message));
            json.put("messageIsText", isText);
        }

        public byte[] getMessage() {
            return message;
        }

        public boolean isText() {
            return isText;
        }
    }

    public static class EncryptedMessage extends AbstractAppendix {

        static EncryptedMessage parse(JSONObject attachmentData, byte transactionVersion) throws NxtException.ValidationException {
            JSONObject encryptedMessageJSON = (JSONObject)attachmentData.get("encryptedMessage");
            if (encryptedMessageJSON == null ) {
                return null;
            }
            return new EncryptedMessage(encryptedMessageJSON, transactionVersion);
        }

        private final EncryptedData encryptedData;
        private final boolean isText;

        EncryptedMessage(ByteBuffer buffer, byte transactionVersion) throws NxtException.ValidationException {
            super(buffer, transactionVersion);
            int length = buffer.getInt();
            this.isText = length < 0;
            if (length < 0) {
                length ^= Integer.MIN_VALUE;
            }
            this.encryptedData = EncryptedData.readEncryptedData(buffer, length, Constants.MAX_ENCRYPTED_MESSAGE_LENGTH);
        }

        EncryptedMessage(JSONObject attachmentData, byte transactionVersion) throws NxtException.ValidationException {
            super(attachmentData, transactionVersion);
            byte[] data = Convert.parseHexString((String)attachmentData.get("data"));
            if (data.length > Constants.MAX_ENCRYPTED_MESSAGE_LENGTH) {
                throw new NxtException.ValidationException("Max encrypted message length exceeded");
            }
            byte[] nonce = Convert.parseHexString((String)attachmentData.get("nonce"));
            if ((nonce.length != 32 && data.length > 0) || (nonce.length != 0 && data.length == 0)) {
                throw new NxtException.ValidationException("Invalid nonce length " + nonce.length);
            }
            this.encryptedData = new EncryptedData(data, nonce);
            this.isText = Boolean.TRUE.equals((Boolean)attachmentData.get("isText"));
        }

        public EncryptedMessage(EncryptedData encryptedData, boolean isText) {
            super(0);
            this.encryptedData = encryptedData;
            this.isText = isText;
        }

        @Override
        int getMySize() {
            return 4 + encryptedData.getSize();
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putInt(isText ? encryptedData.getData().length | Integer.MIN_VALUE : encryptedData.getData().length);
            buffer.put(encryptedData.getData());
            buffer.put(encryptedData.getNonce());
        }

        @Override
        void putMyJSON(JSONObject json) {
            JSONObject encryptedMessageJSON = new JSONObject();
            encryptedMessageJSON.put("data", Convert.toHexString(encryptedData.getData()));
            encryptedMessageJSON.put("nonce", Convert.toHexString(encryptedData.getNonce()));
            encryptedMessageJSON.put("isText", isText);
            json.put("encryptedMessage", encryptedMessageJSON);
        }

        public EncryptedData getEncryptedData() {
            return encryptedData;
        }

        public boolean isText() {
            return isText;
        }

    }

}
