package nxt;

import nxt.crypto.KNV;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class CurrencyMint {

    static void clear() {
    }

    static void mintMoney(Account account, long nonce, Long currencyId, int units, int counter) {
        if (counter <= account.getMintingCounter(currencyId)) {
            return;
        }

        ByteBuffer buffer = ByteBuffer.allocate(8 + 8 + 4 + 4 + 8);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(nonce);
        buffer.putLong(currencyId);
        buffer.putInt(units);
        buffer.putInt(counter);
        buffer.putLong(account.getId());
        byte[] hash = new byte[32];
        KNV.hash(buffer.array(), 0, buffer.array().length, hash, 0);

        if ((new BigInteger(hash)).compareTo(getCurrencyDifficulty(currencyId)) >= 0) {
            account.setMintingCounter(currencyId, counter);

            Currency currency = Currency.getCurrency(currencyId);
            units = (int)Math.min((long)units, currency.getTotalSupply() - currency.getCurrentSupply());
            account.addToCurrencyAndUnconfirmedCurrencyBalanceQNT(currencyId, units);

            currency.increaseSupply(units);
        }
    }

    static BigInteger getCurrencyDifficulty(Long currencyId) {
        Currency currency = Currency.getCurrency(currencyId);
        BigInteger minDifficulty = BigInteger.valueOf(2).pow(currency.getMinDifficulty() & 0xFF);
        BigInteger maxDifficulty = BigInteger.valueOf(2).pow(currency.getMaxDifficulty() & 0xFF);
        return minDifficulty.add(maxDifficulty.subtract(minDifficulty).multiply(BigInteger.valueOf(currency.getCurrentSupply())).divide(BigInteger.valueOf(currency.getTotalSupply()))); // minDifficulty + (maxDifficulty - minDifficulty) * currentSupply / totalSupply
    }

}
