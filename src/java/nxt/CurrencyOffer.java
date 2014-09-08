package nxt;

import nxt.db.DbKey;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class CurrencyOffer {

    protected final Long id;
    protected DbKey dbKey;
    protected final Long currencyId;
    protected final Long accountId;
    protected final long rateNQT;
    protected long limit;
    protected long supply;
    protected final int expirationHeight;
    protected final int height;

    public CurrencyOffer(long id, long currencyId, Long accountId, long rateNQT, long limit, long supply, int expirationHeight, int height) {
        this.id = id;
        this.currencyId = currencyId;
        this.accountId = accountId;
        this.rateNQT = rateNQT;
        this.limit = limit;
        this.supply = supply;
        this.expirationHeight = expirationHeight;
        this.height = height;
    }

    protected CurrencyOffer(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.currencyId = rs.getLong("currency_id");
        this.accountId = rs.getLong("account_id");
        this.rateNQT = rs.getLong("rate");
        this.limit = rs.getLong("limit");
        this.supply = rs.getLong("supply");
        this.expirationHeight = rs.getInt("expiration_height");
        this.height = rs.getInt("height");
    }

    protected void save(Connection con, String table) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO " + table + " (id, currency_id, account_id, "
                + "rate, limit, supply, expiration_height, height, latest) KEY (id, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, this.getId());
            pstmt.setLong(++i, this.getCurrencyId());
            pstmt.setLong(++i, this.getAccountId());
            pstmt.setLong(++i, this.getRateNQT());
            pstmt.setLong(++i, this.getLimit());
            pstmt.setLong(++i, this.getSupply());
            pstmt.setInt(++i, this.getExpirationHeight());
            pstmt.setInt(++i, this.getHeight());
            pstmt.executeUpdate();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getCurrencyId() {
        return currencyId;
    }

    public Long getAccountId() {
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
        return height;
    }

    public abstract CurrencyOffer getCounterOffer();

    public void increaseSupply(long delta) {
        supply += delta;
    }

    public void decreaseLimitAndSupply(long delta) {
        limit -= delta;
        supply -= delta;
    }
}
