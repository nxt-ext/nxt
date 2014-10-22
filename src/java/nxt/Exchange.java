package nxt;

import nxt.db.*;
import nxt.util.Convert;
import nxt.util.Listener;
import nxt.util.Listeners;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class Exchange {

    public static enum Event {
        EXCHANGE
    }

    private static final Listeners<Exchange,Event> listeners = new Listeners<>();

    //TODO: shouldn't exchange have buy_offer_id and sell_offer_id instead of just offer_id?
    private static final DbKey.LongKeyFactory<Exchange> exchangeDbKeyFactory = new DbKey.LongKeyFactory<Exchange>("offer_id") {

        @Override
        public DbKey newKey(Exchange exchange) {
            return exchange.dbKey;
        }

    };

    private static final EntityDbTable<Exchange> exchangeTable = new EntityDbTable<Exchange>("exchange", exchangeDbKeyFactory) {

        @Override
        protected Exchange load(Connection con, ResultSet rs) throws SQLException {
            return new Exchange(rs);
        }

        @Override
        protected void save(Connection con, Exchange exchange) throws SQLException {
            exchange.save(con);
        }

    };

    public static DbIterator<Exchange> getAllExchanges(int from, int to) {
        return exchangeTable.getAll(from, to);
    }

    public static int getCount() {
        return exchangeTable.getCount();
    }

    public static boolean addListener(Listener<Exchange> listener, Event eventType) {
        return listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<Exchange> listener, Event eventType) {
        return listeners.removeListener(listener, eventType);
    }

    public static DbIterator<Exchange> getCurrencyExchanges(long currencyId, int from, int to) {
        return exchangeTable.getManyBy(new DbClause.LongClause("currency_id", currencyId), from, to);
    }

    public static DbIterator<Exchange> getAccountExchanges(long accountId, int from, int to) {
        Connection con = null;
        try {
            con = Db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM exchange WHERE seller_id = ?"
                    + " UNION ALL SELECT * FROM exchange WHERE buyer_id = ? AND seller_id <> ? ORDER BY height DESC"
                    + DbUtils.limitsClause(from, to));
            int i = 0;
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, accountId);
            DbUtils.setLimits(++i, pstmt, from, to);
            return exchangeTable.getManyBy(con, pstmt, false);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static DbIterator<Exchange> getAccountCurrencyExchanges(long accountId, long currencyId, int from, int to) {
        Connection con = null;
        try {
            con = Db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM exchange WHERE seller_id = ? AND currency_id = ?"
                    + " UNION ALL SELECT * FROM exchange WHERE buyer_id = ? AND seller_id <> ? AND currency_id = ? ORDER BY height DESC"
                    + DbUtils.limitsClause(from, to));
            int i = 0;
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, currencyId);
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, currencyId);
            DbUtils.setLimits(++i, pstmt, from, to);
            return exchangeTable.getManyBy(con, pstmt, false);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static int getExchangeCount(long currencyId) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT COUNT(*) FROM exchange WHERE currency_id = ?")) {
            pstmt.setLong(1, currencyId);
            try (ResultSet rs = pstmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    static Exchange addExchange(long currencyId, Block block, CurrencyOffer offer, long sellerId, long buyerId, long units) {
        Exchange exchange = new Exchange(currencyId, block, offer, sellerId, buyerId, units);
        exchangeTable.insert(exchange);
        listeners.notify(exchange, Event.EXCHANGE);
        return exchange;
    }

    static void init() {}


    private final int timestamp;
    private final long currencyId;
    private final long blockId;
    private final int height;
    private final long offerId;
    private final long sellerId;
    private final long buyerId;
    private final DbKey dbKey;
    private final long units;
    private final long rate;

    private Exchange(long currencyId, Block block, CurrencyOffer offer, long sellerId, long buyerId, long units) {
        this.blockId = block.getId();
        this.height = block.getHeight();
        this.currencyId = currencyId;
        this.timestamp = block.getTimestamp();
        this.offerId = offer.getId();
        this.sellerId = sellerId;
        this.buyerId = buyerId;
        this.dbKey = exchangeDbKeyFactory.newKey(this.offerId); // TODO, not unique
        this.units = units;
        this.rate = offer.getRateNQT();
    }

    private Exchange(ResultSet rs) throws SQLException {
        this.currencyId = rs.getLong("currency_id");
        this.blockId = rs.getLong("block_id");
        this.offerId = rs.getLong("offer_id");
        this.sellerId = rs.getLong("seller_id");
        this.buyerId = rs.getLong("buyer_id");
        this.dbKey = exchangeDbKeyFactory.newKey(this.offerId); // TODO, not unique
        this.units = rs.getLong("units");
        this.rate = rs.getLong("rate");
        this.timestamp = rs.getInt("timestamp");
        this.height = rs.getInt("height");
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO exchange (currency_id, block_id, "
                + "offer_id, seller_id, buyer_id, units, rate, timestamp, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, this.getCurrencyId());
            pstmt.setLong(++i, this.getBlockId());
            pstmt.setLong(++i, this.getOfferId());
            pstmt.setLong(++i, this.getSellerId());
            pstmt.setLong(++i, this.getBuyerId());
            pstmt.setLong(++i, this.getUnits());
            pstmt.setLong(++i, this.getRate());
            pstmt.setInt(++i, this.getTimestamp());
            pstmt.setInt(++i, this.getHeight());
            pstmt.executeUpdate();
        }
    }

    public long getBlockId() { return blockId; }

    public long getOfferId() { return offerId; }

    public long getSellerId() {
        return sellerId;
    }

    public long getBuyerId() {
        return buyerId;
    }

    public long getUnits() { return units; }

    public long getRate() { return rate; }
    
    public long getCurrencyId() { return currencyId; }
    
    public int getTimestamp() { return timestamp; }

    public int getHeight() {
        return height;
    }

    @Override
    public String toString() {
        return "Exchange currency: " + Convert.toUnsignedLong(currencyId) + " offer: " + Convert.toUnsignedLong(offerId)
                + " rate: " + rate + " units: " + units + " height: " + height;
    }

}
