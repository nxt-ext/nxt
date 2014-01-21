package nxt.util;

import nxt.Nxt;

import java.math.BigInteger;

public final class Convert {

    public static final String alphabet = "0123456789abcdefghijklmnopqrstuvwxyz";
    public static final BigInteger two64 = new BigInteger("18446744073709551616");

    private Convert() {} //never

    public static byte[] convert(String string) {

        byte[] bytes = new byte[string.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte)Integer.parseInt(string.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;

    }

    public static String convert(byte[] bytes) {

        StringBuilder string = new StringBuilder();
        for (byte b : bytes) {
            int number;
            string.append(alphabet.charAt((number = b & 0xFF) >> 4)).append(alphabet.charAt(number & 0xF));
        }
        return string.toString();

    }

    public static String convert(long objectId) {

        BigInteger id = BigInteger.valueOf(objectId);
        if (objectId < 0) {
            id = id.add(two64);
        }
        return id.toString();

    }

    public static long parseUnsignedLong(String number) {

        if (number == null) {
            throw new IllegalArgumentException("trying to parse null");
        }
        BigInteger bigInt = new BigInteger(number.trim());
        if (bigInt.signum() < 0 || bigInt.compareTo(two64) != -1) {
            throw new IllegalArgumentException("overflow: " + number);
        }
        return bigInt.longValue();

    }

    public static int getEpochTime() {

        return (int)((System.currentTimeMillis() - Nxt.epochBeginning + 500) / 1000);

    }
}
