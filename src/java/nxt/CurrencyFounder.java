package nxt;

import nxt.db.DbKey;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
* Created by lyaf on 9/6/2014.
*/
public class CurrencyFounder {
    private final DbKey dbKey;
    private final long currencyId;
    private final long accountId;
    private final long value;

    CurrencyFounder(long currencyId, long accountId, long value) {
        this.currencyId = currencyId;
        this.dbKey = NonIssuedCurrency.currencyFounderDbKeyFactory.newKey(currencyId);
        this.accountId = accountId;
        this.value = value;
    }

    CurrencyFounder(ResultSet rs) throws SQLException {
        this.currencyId = rs.getLong("id");
        this.dbKey = NonIssuedCurrency.currencyFounderDbKeyFactory.newKey(currencyId);
        this.accountId = rs.getLong("account_id");
        this.value = rs.getLong("value");
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO currency_founder (id, account_id, value)"
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

    public static void addFounder(Long currencyId, Long accountId, long value) {
        NonIssuedCurrency.currencyFounderTable.insert(new CurrencyFounder(currencyId, accountId, value));
    }
}
