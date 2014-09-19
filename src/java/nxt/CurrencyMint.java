package nxt;

import nxt.crypto.KNV;
import nxt.db.Db;
import nxt.db.DbKey;
import nxt.db.DbUtils;
import nxt.db.EntityDbTable;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Manages currency proof of work minting
 */
public final class CurrencyMint {


    private static final DbKey.LongKeyFactory<CurrencyMint> currencyMintDbKeyFactory = new DbKey.LongKeyFactory<CurrencyMint>("currency_id") {

        @Override
        public DbKey newKey(CurrencyMint currencyMint) {
            return currencyMint.dbKey;
        }

    };

    public static final EntityDbTable<CurrencyMint> currencyMintTable = new EntityDbTable<CurrencyMint>("currency_mint", currencyMintDbKeyFactory) {

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

    CurrencyMint(long currencyId, long accountId, long counter) {
        this.currencyId = currencyId;
        this.dbKey = currencyMintDbKeyFactory.newKey(currencyId);
        this.accountId = accountId;
        this.counter = counter;
    }

    CurrencyMint(ResultSet rs) throws SQLException {
        this.currencyId = rs.getLong("currency_id");
        this.dbKey = currencyMintDbKeyFactory.newKey(currencyId);
        this.accountId = rs.getLong("account_id");
        this.counter = rs.getLong("counter");
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO currency_mint (currency_id, account_id, counter)"
                + "VALUES (?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, this.getCurrencyId());
            pstmt.setLong(++i, this.getAccountId());
            pstmt.setLong(++i, this.getCounter());
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


    static void mintMoney(Account account, long nonce, Long currencyId, long units, long counter) {
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

        ByteBuffer buffer = ByteBuffer.allocate(8 + 8 + 8 + 8 + 8);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(nonce);
        buffer.putLong(currencyId);
        buffer.putLong(units);
        buffer.putLong(counter);
        buffer.putLong(account.getId());
        byte[] hash = new byte[32];
        KNV.hash(buffer.array(), 0, buffer.array().length, hash, 0);

        if (new BigInteger(hash).compareTo(getCurrencyDifficulty(currencyId)) >= 0) {
            currencyMintTable.insert(new CurrencyMint(currencyId, account.getId(), counter));
            Currency currency = Currency.getCurrency(currencyId);
            units = Math.min(units, currency.getTotalSupply() - currency.getCurrentSupply());
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
