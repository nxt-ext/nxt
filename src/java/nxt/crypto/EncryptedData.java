package nxt.crypto;

import java.io.Serializable;
import java.security.SecureRandom;

public final class EncryptedData implements Serializable {

    private static final ThreadLocal<SecureRandom> secureRandom = new ThreadLocal<SecureRandom>() {
        @Override
        protected SecureRandom initialValue() {
            return new SecureRandom();
        }
    };

    private final byte[] data;
    private final byte[] nonce;

    public EncryptedData() {
        this.data = null;
        this.nonce = null;
    }

    public EncryptedData(byte[] data, byte[] nonce) {
        this.data = data;
        this.nonce = nonce;
    }

    public static EncryptedData encrypt(byte[] plaintext, byte[] myPrivateKey, byte[] theirPublicKey) {
        byte[] nonce = new byte[32];
        secureRandom.get().nextBytes(nonce);
        byte[] data = Crypto.aesEncrypt(plaintext, myPrivateKey, theirPublicKey, nonce);
        return new EncryptedData(data, nonce);
    }

    public byte[] decrypt(byte[] myPrivateKey, byte[] theirPublicKey) {
        return Crypto.aesDecrypt(data, myPrivateKey, theirPublicKey, nonce);
    }

    public byte[] getData() {
        return data;
    }

    public byte[] getNonce() {
        return nonce;
    }

}
