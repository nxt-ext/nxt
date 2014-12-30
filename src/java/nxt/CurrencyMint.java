package nxt;

import nxt.crypto.HashFunction;
import nxt.db.DbClause;
import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.VersionedEntityDbTable;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Manages currency proof of work minting
 */
public final class CurrencyMint {


    private static final DbKey.LinkKeyFactory<CurrencyMint> currencyMintDbKeyFactory = new DbKey.LinkKeyFactory<CurrencyMint>("currency_id", "account_id") {

        @Override
        public DbKey newKey(CurrencyMint currencyMint) {
            return currencyMint.dbKey;
        }

    };

    private static final VersionedEntityDbTable<CurrencyMint> currencyMintTable = new VersionedEntityDbTable<CurrencyMint>("currency_mint", currencyMintDbKeyFactory) {

        @Override
        protected CurrencyMint load(Connection con, ResultSet rs) throws SQLException {
            return new CurrencyMint(rs);
        }

        @Override
        protected void save(Connection con, CurrencyMint currencyMint) throws SQLException {
            currencyMint.save(con);
        }

    };

    static void init() {}

    private final DbKey dbKey;
    private final long currencyId;
    private final long accountId;
    private long counter;

    private CurrencyMint(long currencyId, long accountId, long counter) {
        this.currencyId = currencyId;
        this.accountId = accountId;
        this.dbKey = currencyMintDbKeyFactory.newKey(this.currencyId, this.accountId);
        this.counter = counter;
    }

    private CurrencyMint(ResultSet rs) throws SQLException {
        this.currencyId = rs.getLong("currency_id");
        this.accountId = rs.getLong("account_id");
        this.dbKey = currencyMintDbKeyFactory.newKey(this.currencyId, this.accountId);
        this.counter = rs.getLong("counter");
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO currency_mint (currency_id, account_id, counter, height, latest) "
                + "KEY (currency_id, account_id, height) VALUES (?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, this.getCurrencyId());
            pstmt.setLong(++i, this.getAccountId());
            pstmt.setLong(++i, this.getCounter());
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }

    public long getCurrencyId() {
        return currencyId;
    }

    public long getAccountId() {
        return accountId;
    }

    public long getCounter() {
        return counter;
    }

    static void mintCurrency(final Account account, final Attachment.MonetarySystemCurrencyMinting attachment) {
        CurrencyMint currencyMint = currencyMintTable.get(currencyMintDbKeyFactory.newKey(attachment.getCurrencyId(), account.getId()));
        if (currencyMint != null && attachment.getCounter() <= currencyMint.getCounter()) {
            return;
        }
        Currency currency = Currency.getCurrency(attachment.getCurrencyId());
        if (meetsTarget(account.getId(), currency, attachment)) {
            if (currencyMint == null) {
                currencyMint = new CurrencyMint(attachment.getCurrencyId(), account.getId(), attachment.getCounter());
            } else {
                currencyMint.counter = attachment.getCounter();
            }
            currencyMintTable.insert(currencyMint);
            long units = Math.min(attachment.getUnits(), currency.getMaxSupply() - currency.getCurrentSupply());
            account.addToCurrencyAndUnconfirmedCurrencyUnits(currency.getId(), units);
            currency.increaseSupply(units);
        }
    }

    public static long getCounter(long currencyId, long accountId) {
        CurrencyMint currencyMint = currencyMintTable.get(currencyMintDbKeyFactory.newKey(currencyId, accountId));
        if (currencyMint != null) {
            return currencyMint.getCounter();
        } else {
            return 0;
        }
    }

    static void deleteCurrency(Currency currency) {
        List<CurrencyMint> currencyMints = new ArrayList<>();
        try (DbIterator<CurrencyMint> mints = currencyMintTable.getManyBy(new DbClause.LongClause("currency_id", currency.getId()), 0, -1)) {
            while (mints.hasNext()) {
                currencyMints.add(mints.next());
            }
        }
        for (CurrencyMint mint : currencyMints) {
            currencyMintTable.delete(mint);
        }
    }

    static boolean meetsTarget(long accountId, Currency currency, Attachment.MonetarySystemCurrencyMinting attachment) {
        byte[] hash = getHash(currency.getAlgorithm(), attachment.getNonce(), attachment.getCurrencyId(), attachment.getUnits(),
                attachment.getCounter(), accountId);
        byte[] target = getTarget(currency.getMinDifficulty(), currency.getMaxDifficulty(),
                attachment.getUnits(), currency.getCurrentSupply() - currency.getReserveSupply(), currency.getMaxSupply() - currency.getReserveSupply());
        return meetsTarget(hash, target);
    }

    public static boolean meetsTarget(byte[] hash, byte[] target) {
        for (int i = hash.length - 1; i >= 0; i--) {
            if ((hash[i] & 0xff) > (target[i] & 0xff)) {
                return false;
            }
            if ((hash[i] & 0xff) < (target[i] & 0xff)) {
                return true;
            }
        }
        return true;
    }

    public static byte[] getHash(byte algorithm, long nonce, long currencyId, long units, long counter, long accountId) {
        HashFunction hashFunction = HashFunction.getHashFunction(algorithm);
        return getHash(hashFunction, nonce, currencyId, units, counter, accountId);
    }

    public static byte[] getHash(HashFunction hashFunction, long nonce, long currencyId, long units, long counter, long accountId) {
        ByteBuffer buffer = ByteBuffer.allocate(8 + 8 + 8 + 8 + 8);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(nonce);
        buffer.putLong(currencyId);
        buffer.putLong(units);
        buffer.putLong(counter);
        buffer.putLong(accountId);
        return hashFunction.hash(buffer.array());
    }

    public static byte[] getTarget(int min, int max, long units, long currentMintableSupply, long totalMintableSupply) {
        return getTarget(getNumericTarget(min, max, units, currentMintableSupply, totalMintableSupply));
    }

    public static byte[] getTarget(BigInteger numericTarget) {
        byte[] targetRowBytes = numericTarget.toByteArray();
        if (targetRowBytes.length == 32) {
            return reverse(targetRowBytes);
        }
        byte[] targetBytes = new byte[32];
        Arrays.fill(targetBytes, 0, 32 - targetRowBytes.length, (byte) 0);
        System.arraycopy(targetRowBytes, 0, targetBytes, 32 - targetRowBytes.length, targetRowBytes.length);
        return reverse(targetBytes);
    }

    public static BigInteger getNumericTarget(Currency currency, long units) {
        return getNumericTarget(currency.getMinDifficulty(), currency.getMaxDifficulty(), units,
                currency.getCurrentSupply() - currency.getReserveSupply(), currency.getMaxSupply() - currency.getReserveSupply());
    }

    public static BigInteger getNumericTarget(int min, int max, long units, long currentMintableSupply, long totalMintableSupply) {
        if (min < 1 || max > 255) {
            throw new IllegalArgumentException(String.format("Min: %d, Max: %d, allowed range is 1 to 255", min, max));
        }
        int exp = (int)(256 - min - ((max - min) * currentMintableSupply) / totalMintableSupply);
        return BigInteger.valueOf(2).pow(exp).subtract(BigInteger.ONE).divide(BigInteger.valueOf(units));
    }

    private static byte[] reverse(byte[] b) {
        for(int i=0; i < b.length/2; i++) {
            byte temp = b[i];
            b[i] = b[b.length - i - 1];
            b[b.length - i - 1] = temp;
        }
        return b;
    }
}
