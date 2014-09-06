package nxt;

import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.EntityDbTable;

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

    private static final DbKey.LongKeyFactory<CurrencyFounder> currencyFounderDbKeyFactory = new DbKey.LongKeyFactory<CurrencyFounder>("currency_id") {

        @Override
        public DbKey newKey(CurrencyFounder currencyFounder) {
            return currencyFounder.dbKey;
        }

    };

    public static final EntityDbTable<CurrencyFounder> currencyFounderTable = new EntityDbTable<CurrencyFounder>(currencyFounderDbKeyFactory) {

        @Override
        protected String table() {
            return "currency_founder";
        }

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
    private final long value;

    CurrencyFounder(long currencyId, long accountId, long value) {
        this.currencyId = currencyId;
        this.dbKey = currencyFounderDbKeyFactory.newKey(currencyId);
        this.accountId = accountId;
        this.value = value;
    }

    CurrencyFounder(ResultSet rs) throws SQLException {
        this.currencyId = rs.getLong("currency_id");
        this.dbKey = currencyFounderDbKeyFactory.newKey(currencyId);
        this.accountId = rs.getLong("account_id");
        this.value = rs.getLong("value");
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO currency_founder (currency_id, account_id, value)"
                + "VALUES (?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, this.getCurrencyId());
            pstmt.setLong(++i, this.getAccountId());
            pstmt.setLong(++i, this.getValue());
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

    public static void addFounder(long currencyId, Long accountId, long value) {
        currencyFounderTable.insert(new CurrencyFounder(currencyId, accountId, value));
    }

    public static DbIterator<CurrencyFounder> getCurrencyFounders(long currencyId) {
        return currencyFounderTable.getManyBy("currency_id", currencyId, 0, currencyFounderTable.getCount() - 1);
    }

    public static void remove(Long currencyId) {
        // Inefficient
        try (DbIterator<CurrencyFounder> founders = CurrencyFounder.getCurrencyFounders(currencyId)) {
            while (founders.hasNext()) {
                CurrencyFounder founderEntry = founders.next();
                currencyFounderTable.delete(founderEntry);
            }
        }
    }
}
