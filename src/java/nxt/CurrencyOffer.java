package nxt;

import nxt.db.DbKey;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class CurrencyOffer {

    protected final long id;
    protected DbKey dbKey;
    protected final long currencyId;
    protected final long accountId;
    protected final long rateNQT;
    protected long limit; // limit on the total sum of units for this offer across transactions
    protected long supply; // total units supply for the offer
    protected final int expirationHeight;
    protected final int creationHeight;

    protected CurrencyOffer(long id, long currencyId, long accountId, long rateNQT, long limit, long supply, int expirationHeight, int creationHeight) {
        this.id = id;
        this.currencyId = currencyId;
        this.accountId = accountId;
        this.rateNQT = rateNQT;
        this.limit = limit;
        this.supply = supply;
        this.expirationHeight = expirationHeight;
        this.creationHeight = creationHeight;
    }

    protected CurrencyOffer(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.currencyId = rs.getLong("currency_id");
        this.accountId = rs.getLong("account_id");
        this.rateNQT = rs.getLong("rate");
        this.limit = rs.getLong("unit_limit");
        this.supply = rs.getLong("supply");
        this.expirationHeight = rs.getInt("expiration_height");
        this.creationHeight = rs.getInt("creation_height");
    }

    protected void save(Connection con, String table) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO " + table + " (id, currency_id, account_id, "
                + "rate, unit_limit, supply, expiration_height, creation_height, height, latest) KEY (id, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, this.getId());
            pstmt.setLong(++i, this.getCurrencyId());
            pstmt.setLong(++i, this.getAccountId());
            pstmt.setLong(++i, this.getRateNQT());
            pstmt.setLong(++i, this.getLimit());
            pstmt.setLong(++i, this.getSupply());
            pstmt.setInt(++i, this.getExpirationHeight());
            pstmt.setInt(++i, this.getHeight());
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }

    public long getId() {
        return id;
    }

    public long getCurrencyId() {
        return currencyId;
    }

    public long getAccountId() {
        return accountId;
    }

    public long getRateNQT() {
        return rateNQT;
    }

    public long getLimit() {
        return limit;
    }

    public long getSupply() {
        return supply;
    }

    public int getExpirationHeight() {
        return expirationHeight;
    }

    public int getHeight() {
        return creationHeight;
    }

    public abstract CurrencyOffer getCounterOffer();

    void increaseSupply(long delta) {
        supply += delta;
    }

    void decreaseLimitAndSupply(long delta) {
        limit -= delta;
        supply -= delta;
    }
}
