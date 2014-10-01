package nxt;

import nxt.db.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Each CurrencyFounder instance represents a single founder contribution for a non issued currency
 * Once the currency is issued all founder contributions are removed
 * In case the currency is not issued because of insufficient funding, all funds are returned to the founders
 */
public class CurrencyFounder {

    private static final DbKey.LinkKeyFactory<CurrencyFounder> currencyFounderDbKeyFactory = new DbKey.LinkKeyFactory<CurrencyFounder>("currency_id", "account_id") {

            @Override
        public DbKey newKey(CurrencyFounder currencyFounder) {
            return currencyFounder.dbKey;
        }

    };

    public static final VersionedEntityDbTable<CurrencyFounder> currencyFounderTable = new VersionedEntityDbTable<CurrencyFounder>("currency_founder", currencyFounderDbKeyFactory) {

        @Override
        protected CurrencyFounder load(Connection con, ResultSet rs) throws SQLException {
            return new CurrencyFounder(rs);
        }

        @Override
        protected void save(Connection con, CurrencyFounder currencyFounder) throws SQLException {
            currencyFounder.save(con);
        }

    };

    private final DbKey dbKey;
    private final long currencyId;
    private final long accountId;
    private long value;

    CurrencyFounder(long currencyId, long accountId, long value) {
        this.currencyId = currencyId;
        this.dbKey = currencyFounderDbKeyFactory.newKey(currencyId, accountId);
        this.accountId = accountId;
        this.value = value;
    }

    CurrencyFounder(ResultSet rs) throws SQLException {
        this.currencyId = rs.getLong("currency_id");
        this.accountId = rs.getLong("account_id");
        this.dbKey = currencyFounderDbKeyFactory.newKey(currencyId, accountId);
        this.value = rs.getLong("value");
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO currency_founder (currency_id, account_id, value, height)"
                + "VALUES (?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, this.getCurrencyId());
            pstmt.setLong(++i, this.getAccountId());
            pstmt.setLong(++i, this.getValue());
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

    public long getValue() {
        return value;
    }

    public static void addOrUpdateFounder(long currencyId, long accountId, long value) {
        CurrencyFounder founder = getFounder(currencyId, accountId);
        if (founder == null) {
            founder = new CurrencyFounder(currencyId, accountId, value);
        } else {
            founder.value += value;
        }
        currencyFounderTable.insert(founder);
    }

    public static CurrencyFounder getFounder(long currencyId, long accountId) {
        return currencyFounderTable.get(currencyFounderDbKeyFactory.newKey(currencyId, accountId));
    }

    public static DbIterator<CurrencyFounder> getCurrencyFounders(long currencyId, int from, int to) {
        return currencyFounderTable.getManyBy("currency_id", currencyId, from, to);
    }

    public static void remove(long currencyId) {
        for (CurrencyFounder founder : CurrencyFounder.getCurrencyFounders(currencyId, 0, Integer.MAX_VALUE)) {
            currencyFounderTable.delete(founder);
        }
    }
}
