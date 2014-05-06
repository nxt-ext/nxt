package nxt.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.security.SecureRandom;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            gzip.write(plaintext);
            gzip.flush();
            gzip.close();
            byte[] compressedPlaintext = bos.toByteArray();
            byte[] nonce = new byte[32];
            secureRandom.get().nextBytes(nonce);
            byte[] data = Crypto.aesEncrypt(compressedPlaintext, myPrivateKey, theirPublicKey, nonce);
            return new EncryptedData(data, nonce);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public byte[] decrypt(byte[] myPrivateKey, byte[] theirPublicKey) {
        byte[] compressedPlaintext = Crypto.aesDecrypt(data, myPrivateKey, theirPublicKey, nonce);
        try (ByteArrayInputStream bis = new ByteArrayInputStream(compressedPlaintext);
             GZIPInputStream gzip = new GZIPInputStream(bis);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int nRead;
            while ((nRead = gzip.read(buffer, 0, buffer.length)) > 0) {
                bos.write(buffer, 0, nRead);
            }
            bos.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public byte[] getData() {
        return data;
    }

    public byte[] getNonce() {
        return nonce;
    }

}
