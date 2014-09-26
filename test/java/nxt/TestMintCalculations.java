package nxt;

import nxt.util.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class TestMintCalculations {

    @Test
    public void targetCalculation() {
        byte[] target = CurrencyMint.getTarget((byte) 4, (byte) 32, 1, 0, 100000);
        Logger.logDebugMessage("initial target: " + Arrays.toString(target));
        Assert.assertEquals(32, target.length);
        Assert.assertArrayEquals(new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16}, target);

        target = CurrencyMint.getTarget((byte) 4, (byte) 32, 1, 50000, 100000);
        Logger.logDebugMessage("midway target: " + Arrays.toString(target));
        Assert.assertEquals(32, target.length);
        Assert.assertArrayEquals(new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 64, 0, 0}, target);

        target = CurrencyMint.getTarget((byte) 4, (byte) 32, 1, 100000, 100000);
        Logger.logDebugMessage("final target: " + Arrays.toString(target));
        Assert.assertEquals(32, target.length);
        Assert.assertArrayEquals(new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0}, target);

        target = CurrencyMint.getTarget((byte) 4, (byte) 32, 100, 100000, 100000);
        Logger.logDebugMessage("final target for 100 units: " + Arrays.toString(target));
        Assert.assertEquals(32, target.length);
        Assert.assertArrayEquals(new byte[]{92, -113, -62, -11, 40, 92, -113, -62, -11, 40, 92, -113, -62, -11, 40, 92, -113, -62, -11, 40, 92, -113, -62, -11, 40, 92, -113, 2, 0, 0, 0, 0}, target);
    }

    @Test
    public void hashing() {
        long nonce;
        for (nonce=0; nonce < Long.MAX_VALUE; nonce++) {
            if (CurrencyMint.meetsTarget(CurrencyMint.getHash(nonce, 123, 1, 1, 987),
                    CurrencyMint.getTarget((byte) 8, (byte) 16, 1, 0, 100000))) {
                break;
            }
        }
        Assert.assertEquals(149, nonce);

        for (nonce=0; nonce < Long.MAX_VALUE; nonce++) {
            if (CurrencyMint.meetsTarget(CurrencyMint.getHash(nonce, 123, 1, 1, 987),
                    CurrencyMint.getTarget((byte) 8, (byte) 16, 1, 100000, 100000))) {
                break;
            }
        }
        Assert.assertEquals(120597, nonce);

        for (nonce=0; nonce < Long.MAX_VALUE; nonce++) {
            if (CurrencyMint.meetsTarget(CurrencyMint.getHash(nonce, 123, 100, 1, 987),
                    CurrencyMint.getTarget((byte) 8, (byte) 16, 100, 0, 100000))) {
                break;
            }
        }
        Assert.assertEquals(5123, nonce);
    }
}
