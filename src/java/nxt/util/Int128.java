package nxt.util;

public final class Int128 {

    // cfb: This code works with numbers in [0x0..0x0FFF'FFFF'FFFF'FFFF] range only, I didn't add validation to keep the code as fast as possible

    private final long leastSignificantHalf, mostSignificantHalf;

    private Int128(long leastSignificantHalf, long mostSignificantHalf) {
        this.leastSignificantHalf = leastSignificantHalf;
        this.mostSignificantHalf = mostSignificantHalf;
    }

    public Int128(long int64) {
        this.leastSignificantHalf = int64;
        this.mostSignificantHalf = 0;
    }

    public static Int128 add(Int128 augend, Int128 addend) {
        long total1stPart = augend.leastSignificantHalf + addend.leastSignificantHalf;
        long total2ndPart = augend.mostSignificantHalf + addend.mostSignificantHalf;

        return new Int128(total1stPart & 0x0FFFFFFFFFFFFFFFL, (total1stPart >> 60) + total2ndPart);
    }

    public static Int128 multiply(long multiplicand, long multiplier) {
        long multiplicandMantissa0 = multiplicand & 0x3FFFFFFF, multiplicandMantissa30 = multiplicand >> 30;
        long multiplierMantissa0 = multiplier & 0x3FFFFFFF, multiplierMantissa30 = multiplier >> 30;

        long product1stPart = multiplicandMantissa0 * multiplierMantissa0;
        long product2ndPart = multiplicandMantissa0 * multiplierMantissa30 + multiplicandMantissa30 * multiplierMantissa0;
        long product3rdPart = multiplicandMantissa30 * multiplierMantissa30;

        return new Int128(product1stPart + ((product2ndPart & 0x3FFFFFFF) << 30), (product2ndPart >> 30) + product3rdPart);
    }

    public int compareTo(Int128 int128) {
        if (this.mostSignificantHalf < int128.mostSignificantHalf) {
            return -1;
        } else if (this.mostSignificantHalf > int128.mostSignificantHalf) {
            return 1;
        } else {
            if (this.leastSignificantHalf < int128.leastSignificantHalf) {
                return -1;
            } else if (this.leastSignificantHalf > int128.leastSignificantHalf) {
                return 1;
            } else {
                return 0;
            }
        }
    }

}
