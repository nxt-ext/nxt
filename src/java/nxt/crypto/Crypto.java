package nxt.crypto;

import nxt.util.Logger;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public final class Crypto {

    private Crypto() {} //never

    public static MessageDigest getMessageDigest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            Logger.logMessage("Missing message digest algorithm: " + algorithm);
            System.exit(1);
            return null;
        }
    }

    public static MessageDigest sha256() {
        return getMessageDigest("SHA-256");
    }

    public static byte[] getPublicKey(String secretPhrase) {

        try {

            byte[] publicKey = new byte[32];
            Curve25519.keygen(publicKey, null, Crypto.sha256().digest(secretPhrase.getBytes("UTF-8")));

            return publicKey;

        } catch (RuntimeException|UnsupportedEncodingException e) {
            Logger.logMessage("Error getting public key", e);
            return null;
        }

    }

    public static byte[] sign(byte[] message, String secretPhrase) {

        try {

            byte[] P = new byte[32];
            byte[] s = new byte[32];
            MessageDigest digest = Crypto.sha256();
            Curve25519.keygen(P, s, digest.digest(secretPhrase.getBytes("UTF-8")));

            byte[] m = digest.digest(message);

            digest.update(m);
            byte[] x = digest.digest(s);

            byte[] Y = new byte[32];
            Curve25519.keygen(Y, null, x);

            digest.update(m);
            byte[] h = digest.digest(Y);

            byte[] v = new byte[32];
            Curve25519.sign(v, h, x, s);

            byte[] signature = new byte[64];
            System.arraycopy(v, 0, signature, 0, 32);
            System.arraycopy(h, 0, signature, 32, 32);

            return signature;

        } catch (RuntimeException|UnsupportedEncodingException e) {
            Logger.logMessage("Error in signing message", e);
            return null;
        }

    }

    public static boolean verify(byte[] signature, byte[] message, byte[] publicKey) {

        try {

            byte[] Y = new byte[32];
            byte[] v = new byte[32];
            System.arraycopy(signature, 0, v, 0, 32);
            byte[] h = new byte[32];
            System.arraycopy(signature, 32, h, 0, 32);
            Curve25519.verify(Y, v, h, publicKey);

            MessageDigest digest = Crypto.sha256();
            byte[] m = digest.digest(message);
            digest.update(m);
            byte[] h2 = digest.digest(Y);

            return Arrays.equals(h, h2);

        } catch (RuntimeException e) {
            Logger.logMessage("Error in Crypto verify", e);
            return false;
        }

    }

}
