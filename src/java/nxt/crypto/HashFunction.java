package nxt.crypto;

import org.bouncycastle.jcajce.provider.digest.SHA3;

public enum HashFunction {

    SHA256((byte)2) {
        public byte[] hash(byte[] input) {
            return Crypto.sha256().digest(input);
        }
    }, SHA3((byte)3) {
        public byte[] hash(byte[] input) {
            return new SHA3.DigestSHA3(256).digest(input);
        }
    }, Keccak25((byte)25) {
        public byte[] hash(byte[] input) {
            return KNV25.hash(input);
        }
    };

    private final byte id;

    HashFunction(byte id) {
        this.id = id;
    }

    public static HashFunction getHashFunction(byte id) {
        for (HashFunction function : values()) {
            if (function.id == id) {
                return function;
            }
        }
        throw new IllegalArgumentException(String.format("illegal algorithm %d", id));
    }

    public byte getId() {
        return id;
    }

    public abstract byte[] hash(byte[] input);
}
