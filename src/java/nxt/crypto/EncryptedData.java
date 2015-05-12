package nxt.crypto;

import nxt.NxtException;
import nxt.util.Convert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class EncryptedData {

    private static final ThreadLocal<SecureRandom> secureRandom = new ThreadLocal<SecureRandom>() {
        @Override
        protected SecureRandom initialValue() {
            return new SecureRandom();
        }
    };

    public static final EncryptedData EMPTY_DATA = new EncryptedData(new byte[0], new byte[0]);

    public static EncryptedData encrypt(byte[] plaintext, byte[] myPrivateKey, byte[] theirPublicKey) {
        if (plaintext.length == 0) {
            return EMPTY_DATA;
        }
        byte[] nonce = new byte[32];
        secureRandom.get().nextBytes(nonce);
        byte[] data = Crypto.aesEncrypt(plaintext, myPrivateKey, theirPublicKey, nonce);
        return new EncryptedData(data, nonce);
    }

    public static EncryptedData readEncryptedData(ByteBuffer buffer, int length, int maxLength)
            throws NxtException.NotValidException {
        if (length == 0) {
            return EMPTY_DATA;
        }
        if (length > maxLength) {
            throw new NxtException.NotValidException("Max encrypted data length exceeded: " + length);
        }
        byte[] data = new byte[length];
        buffer.get(data);
        byte[] nonce = new byte[32];
        buffer.get(nonce);
        return new EncryptedData(data, nonce);
    }

    /*
    public static EncryptedData readEncryptedData(ByteBuffer buffer, int length, int maxLength, long nonce)
            throws NxtException.NotValidException {
        if (length == 0) {
            return EMPTY_DATA;
        }
        if (length > maxLength) {
            throw new NxtException.NotValidException("Max encrypted data length exceeded: " + length);
        }
        byte[] data = new byte[length];
        buffer.get(data);
        return new EncryptedData(data, ByteBuffer.allocate(8).putLong(nonce).array());
    }
    */

    public static EncryptedData readEncryptedData(byte[] bytes) {
        if (bytes.length == 0) {
            return EMPTY_DATA;
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        try {
            return readEncryptedData(buffer, bytes.length - 32, Integer.MAX_VALUE);
        } catch (NxtException.NotValidException e) {
            throw new RuntimeException(e.toString(), e); // never
        }
    }

    private final byte[] data;
    private final byte[] nonce;

    public EncryptedData(byte[] data, byte[] nonce) {
        this.data = data;
        this.nonce = nonce;
    }

    /*
    public EncryptedData(byte[] data, long nonce) {
        this.data = data;
        this.nonce = ByteBuffer.allocate(8).putLong(nonce).array();
    }
    */

    public byte[] decrypt(byte[] myPrivateKey, byte[] theirPublicKey) {
        if (data.length == 0) {
            return data;
        }
        return Crypto.aesDecrypt(data, myPrivateKey, theirPublicKey, nonce);
    }

    public static byte[] marshalData(EncryptedData encryptedData) {
        ByteArrayOutputStream bytesStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(bytesStream);
        marshalData(dataOutputStream, encryptedData);
        return bytesStream.toByteArray();
    }

    public static void marshalData(DataOutputStream dataOutputStream, EncryptedData encryptedData) {
        try {
            dataOutputStream.writeInt(encryptedData.getData().length);
            dataOutputStream.write(encryptedData.getData());
            dataOutputStream.writeInt(encryptedData.getNonce().length);
            dataOutputStream.write(encryptedData.getNonce());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static EncryptedData unmarshalData(byte[] data) {
        return unmarshalData(new DataInputStream(new ByteArrayInputStream(data)));
    }

    public static EncryptedData unmarshalData(DataInputStream dataInputStream) {
        try {
            byte[] data = new byte[dataInputStream.readInt()];
            int rc = dataInputStream.read(data);
            if (rc != data.length) {
                throw new IllegalStateException("Error reading data");
            }
            int nonceLen = dataInputStream.readInt();
            byte[] nonce;
            if (nonceLen > 0) {
                nonce = new byte[nonceLen];
                rc = dataInputStream.read(nonce);
                if (rc != nonce.length) {
                    throw new IllegalStateException("Error reading nonce");
                }
            } else {
                // When creating EncryptedData for plain text data we set an empty nonce to signal this
                nonce = new byte[]{};
            }
            return new EncryptedData(data, nonce);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static List<EncryptedData> getUnmarshaledDataList(byte[] dataBytes) {
        DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(dataBytes));
        List<EncryptedData> inputDataList = new ArrayList<>();
        try {
            while (dataInputStream.available() > 0) {
                EncryptedData encryptedData = unmarshalData(dataInputStream);
                inputDataList.add(encryptedData);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return inputDataList;
    }

    public byte[] getData() {
        return data;
    }

    public byte[] getNonce() {
        return nonce;
    }

    public int getSize() {
        return data.length + nonce.length;
    }

    public static Random getSecureRandom() {
        return secureRandom.get();
    }

    public byte[] getBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(nonce.length + data.length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(data);
        buffer.put(nonce);
        return buffer.array();
    }

    @Override
    public String toString() {
        return "data: " + Convert.toHexString(data) + " nonce: " + Convert.toHexString(nonce);
    }

}
