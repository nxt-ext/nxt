package nxt.crypto;

import nxt.NxtException;
import nxt.util.Convert;

import java.nio.ByteBuffer;
import java.security.SecureRandom;

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
        byte[] compressedPlaintext = Convert.compress(plaintext);
        byte[] nonce = new byte[32];
        secureRandom.get().nextBytes(nonce);
        byte[] data = Crypto.aesEncrypt(compressedPlaintext, myPrivateKey, theirPublicKey, nonce);
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
        byte[] noteBytes = new byte[length];
        buffer.get(noteBytes);
        byte[] noteNonceBytes = new byte[32];
        buffer.get(noteNonceBytes);
        return new EncryptedData(noteBytes, noteNonceBytes);
    }

    public static EncryptedData readEncryptedData(ByteBuffer buffer, int length, int maxLength, long nonce)
            throws NxtException.NotValidException {
        if (length == 0) {
            return EMPTY_DATA;
        }
        if (length > maxLength) {
            throw new NxtException.NotValidException("Max encrypted data length exceeded: " + length);
        }
        byte[] noteBytes = new byte[length];
        buffer.get(noteBytes);
        return new EncryptedData(noteBytes, ByteBuffer.allocate(8).putLong(nonce).array());
    }

    private final byte[] data;
    private final byte[] nonce;

    public EncryptedData(byte[] data, byte[] nonce) {
        this.data = data;
        this.nonce = nonce;
    }

    public EncryptedData(byte[] data, long nonce) {
        this.data = data;
        this.nonce = ByteBuffer.allocate(8).putLong(nonce).array();
    }

    public byte[] decrypt(byte[] myPrivateKey, byte[] theirPublicKey) {
        if (data.length == 0) {
            return data;
        }
        return Convert.uncompress(Crypto.aesDecrypt(data, myPrivateKey, theirPublicKey, nonce));
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

}
