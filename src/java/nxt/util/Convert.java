package nxt.util;

import nxt.Constants;

import java.math.BigInteger;
import java.util.Date;

public final class Convert {

    private static final char[] hexChars = { '0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f' };

    public static final BigInteger two64 = new BigInteger("18446744073709551616");

    private Convert() {} //never

    public static byte[] parseHexString(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int char1 = hex.charAt(i * 2);
            char1 = char1 > 0x60 ? char1 - 0x57 : char1 - 0x30;
            int char2 = hex.charAt(i * 2 + 1);
            char2 = char2 > 0x60 ? char2 - 0x57 : char2 - 0x30;
            if (char1 < 0 || char2 < 0 || char1 > 15 || char2 > 15) {
                throw new NumberFormatException("Invalid hex number: " + hex);
            }
            bytes[i] = (byte)((char1 << 4) + char2);
        }
        return bytes;
    }

    public static String toHexString(byte[] bytes) {
        char[] chars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            chars[i * 2] = hexChars[((bytes[i] >> 4) & 0xF)];
            chars[i * 2 + 1] = hexChars[(bytes[i] & 0xF)];
        }
        return String.valueOf(chars);
    }

    public static String toUnsignedLong(long objectId) {
        if (objectId >= 0) {
            return String.valueOf(objectId);
        }
        BigInteger id = BigInteger.valueOf(objectId).add(two64);
        return id.toString();
    }

    public static String toUnsignedLong(Long objectId) {
        return toUnsignedLong(nullToZero(objectId));
    }

    public static Long parseUnsignedLong(String number) {
        if (number == null) {
            throw new IllegalArgumentException("trying to parse null");
        }
        BigInteger bigInt = new BigInteger(number.trim());
        if (bigInt.signum() < 0 || bigInt.compareTo(two64) != -1) {
            throw new IllegalArgumentException("overflow: " + number);
        }
        return zeroToNull(bigInt.longValue());
    }

    public static int getEpochTime() {
        return (int)((System.currentTimeMillis() - Constants.EPOCH_BEGINNING + 500) / 1000);
    }

    public static Date fromEpochTime(int epochTime) {
        return new Date(epochTime * 1000L + Constants.EPOCH_BEGINNING - 500L);
    }

    public static Long zeroToNull(long l) {
        return l == 0 ? null : l;
    }

    public static long nullToZero(Long l) {
        return l == null ? 0 : l;
    }

    public static int nullToZero(Integer i) {
        return i == null ? 0 : i;
    }

    public static String emptyToNull(String s) {
        return s == null || s.length() == 0 ? null : s;
    }

    public static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    public static String truncate(String s, String replaceNull, int limit, boolean dots) {
        return s == null ? replaceNull : s.length() > limit ? (s.substring(0, dots ? limit - 3 : limit) + (dots ? "..." : "")) : s;
    }

    /*
    public static String toNXT(long nqt) {
        long wholePart = nqt / Constants.ONE_NXT;
        long fractionalPart = nqt % Constants.ONE_NXT;
        if (fractionalPart == 0) {
            return String.valueOf(wholePart);
        }
        StringBuilder buf = new StringBuilder();
        buf.append(wholePart);
        buf.append('.');
        String fractionalPartString = String.valueOf(fractionalPart);
        for (int i = fractionalPartString.length(); i < 8; i++) {
            buf.append('0');
        }
        buf.append(fractionalPartString);
        return buf.toString();
    }
    */

    public static long parseNXT(String nxt) {
        String[] s = nxt.trim().split("\\.");
        if (s.length == 0 || s.length > 2) {
            throw new NumberFormatException("Invalid amount: " + nxt);
        }
        long wholePart = Long.parseLong(s[0]);
        if (wholePart > Constants.MAX_BALANCE_NXT) {
            throw new IllegalArgumentException("Value of " + nxt + " exceeds maximum possible NXT balance");
        }
        if (s.length == 1) {
            return wholePart * Constants.ONE_NXT;
        }
        long fractionalPart = Long.parseLong(s[1]);
        if (fractionalPart >= Constants.ONE_NXT || s[1].length() > 8) {
            throw new IllegalArgumentException("Fractional part of " + nxt + " exceeds maximum NXT divisibility");
        }
        for (int i = s[1].length(); i < 8; i++) {
            fractionalPart *= 10;
        }
        return wholePart * Constants.ONE_NXT + fractionalPart;
    }

    // overflow checking based on https://www.securecoding.cert.org/confluence/display/java/NUM00-J.+Detect+or+prevent+integer+overflow
    public static long safeAdd(long left, long right)
            throws ArithmeticException {
        if (right > 0 ? left > Long.MAX_VALUE - right
                : left < Long.MIN_VALUE - right) {
            throw new ArithmeticException("Integer overflow");
        }
        return left + right;
    }

    public static long safeSubtract(long left, long right)
            throws ArithmeticException {
        if (right > 0 ? left < Long.MIN_VALUE + right
                : left > Long.MAX_VALUE + right) {
            throw new ArithmeticException("Integer overflow");
        }
        return left - right;
    }

    public static long safeMultiply(long left, long right)
            throws ArithmeticException {
        if (right > 0 ? left > Long.MAX_VALUE/right
                || left < Long.MIN_VALUE/right
                : (right < -1 ? left > Long.MIN_VALUE/right
                || left < Long.MAX_VALUE/right
                : right == -1
                && left == Long.MIN_VALUE) ) {
            throw new ArithmeticException("Integer overflow");
        }
        return left * right;
    }

    public static long safeDivide(long left, long right)
            throws ArithmeticException {
        if ((left == Long.MIN_VALUE) && (right == -1)) {
            throw new ArithmeticException("Integer overflow");
        }
        return left / right;
    }

    public static long safeNegate(long a) throws ArithmeticException {
        if (a == Long.MIN_VALUE) {
            throw new ArithmeticException("Integer overflow");
        }
        return -a;
    }

    public static long safeAbs(long a) throws ArithmeticException {
        if (a == Long.MIN_VALUE) {
            throw new ArithmeticException("Integer overflow");
        }
        return Math.abs(a);
    }

}
