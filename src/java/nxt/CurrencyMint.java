package nxt;

import nxt.crypto.HashFunction;
import nxt.db.Db;
import nxt.db.DbKey;
import nxt.db.DbUtils;
import nxt.db.VersionedEntityDbTable;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

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

    public static final VersionedEntityDbTable<CurrencyMint> currencyMintTable = new VersionedEntityDbTable<CurrencyMint>("currency_mint", currencyMintDbKeyFactory) {

        @Override
        protected CurrencyMint load(Connection con, ResultSet rs) throws SQLException {
            return new CurrencyMint(rs);
        }

        @Override
        protected void save(Connection con, CurrencyMint currencyMint) throws SQLException {
            currencyMint.save(con);
        }

    };

    private final DbKey dbKey;
    private final long currencyId;
    private final long accountId;
    private final long counter;
    private final int height;

    CurrencyMint(long currencyId, long accountId, long counter) {
        this.currencyId = currencyId;
        this.accountId = accountId;
        this.dbKey = currencyMintDbKeyFactory.newKey(currencyId, accountId);
        this.counter = counter;
        this.height = Nxt.getBlockchain().getHeight();
    }

    CurrencyMint(ResultSet rs) throws SQLException {
        this.currencyId = rs.getLong("currency_id");
        this.accountId = rs.getLong("account_id");
        this.dbKey = currencyMintDbKeyFactory.newKey(currencyId, accountId);
        this.counter = rs.getLong("counter");
        this.height = rs.getInt("height");
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO currency_mint (currency_id, account_id, counter, height)"
                + "VALUES (?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, this.getCurrencyId());
            pstmt.setLong(++i, this.getAccountId());
            pstmt.setLong(++i, this.getCounter());
            pstmt.setInt(++i, this.getHeight());
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

    public int getHeight() {
        return height;
    }

    static void mintMoney(Account account, long nonce, long currencyId, long units, long counter) {
        Connection con = null;
        CurrencyMint currencyMint;
        try {
            con = Db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM currency_mint WHERE currency_id = ? AND account_id = ?");
            pstmt.setLong(1, currencyId);
            pstmt.setLong(2, account.getId());
            currencyMint = currencyMintTable.get(con, pstmt);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }

        if (currencyMint != null && counter <= currencyMint.getCounter()) {
            return;
        }

        Currency currency = Currency.getCurrency(currencyId);
        byte[] hash = getHash(currency.getAlgorithm(), nonce, currencyId, units, counter, account.getId());
        byte[] target = getTarget(currency.getMinDifficulty(), currency.getMaxDifficulty(),
                units, currency.getCurrentSupply(), currency.getTotalSupply());
        if (meetsTarget(hash, target)) {
            currencyMintTable.insert(new CurrencyMint(currencyId, account.getId(), counter));
            units = Math.min(units, currency.getTotalSupply() - currency.getCurrentSupply());
            account.addToCurrencyAndUnconfirmedCurrencyBalanceQNT(currencyId, units);
            currency.increaseSupply(units);
        }
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
        ByteBuffer buffer = ByteBuffer.allocate(8 + 8 + 8 + 8 + 8);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(nonce);
        buffer.putLong(currencyId);
        buffer.putLong(units);
        buffer.putLong(counter);
        buffer.putLong(accountId);
        return HashFunction.getHashFunction(algorithm).hash(buffer.array());
    }

    public static byte[] getTarget(byte min, byte max, long units, long currentSupply, long totalSupply) {
        BigInteger targetNum = getNumericTarget(min, max, units, currentSupply, totalSupply);
        byte[] targetRowBytes = targetNum.toByteArray();
        if (targetRowBytes.length == 32) {
            return reverse(targetRowBytes);
        }
        byte[] targetBytes = new byte[32];
        Arrays.fill(targetBytes, 0, 32 - targetRowBytes.length, (byte) 0);
        System.arraycopy(targetRowBytes, 0, targetBytes, 32 - targetRowBytes.length, targetRowBytes.length);
        return reverse(targetBytes);
    }

    public static BigInteger getNumericTarget(byte min, byte max, long units, float currentSupply, float totalSupply) {
        int exp = 256 - (min + Math.round((max - min) * (currentSupply / totalSupply)));
        return (BigInteger.valueOf(2).pow(exp)).divide(BigInteger.valueOf(units));
    }

    static byte[] reverse(byte[] b) {
        for(int i=0; i < b.length/2; i++) {
            byte temp = b[i];
            b[i] = b[b.length - i - 1];
            b[b.length - i - 1] = temp;
        }
        return b;
    }
}
