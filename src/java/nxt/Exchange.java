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

    private static final DbKey.LinkKeyFactory<Exchange> exchangeDbKeyFactory = new DbKey.LinkKeyFactory<Exchange>("buy_offer_id", "sell_offer_id") {

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

    public static DbIterator<Exchange> getCurrencyExchanges(Long currencyId, int from, int to) {
        return exchangeTable.getManyBy("currency_id", currencyId, from, to);
    }

    public static DbIterator<Exchange> getAccountExchanges(Long accountId, int from, int to) {
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

    public static DbIterator<Exchange> getAccountCurrencyExchanges(Long accountId, Long currencyId, int from, int to) {
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

    public static int getExchangeCount(Long currencyId) {
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

    static Exchange addExchange(Long currencyId, Block block, CurrencyBuy currencyBuy, CurrencySell currencySell, long quantityQNT, long priceNQT) {
        Exchange exchange = new Exchange(currencyId, block, currencyBuy, currencySell, quantityQNT, priceNQT);
        exchangeTable.insert(exchange);
        listeners.notify(exchange, Event.EXCHANGE);
        return exchange;
    }

    static void init() {}


    private final int timestamp;
    private final Long currencyId;
    private final Long blockId;
    private final int height;
    private final Long buyOfferId;
    private final Long sellOfferId;
    private final Long sellerId;
    private final Long buyerId;
    private final DbKey dbKey;
    private final long quantityQNT;
    private final long priceNQT;

    // @TODO still work in progress need to define database table, insert exchanges, exchange APIs
    private Exchange(Long currencyId, Block block, CurrencyBuy currencyBuy, CurrencySell currencySell, long quantityQNT, long priceNQT) {
        this.blockId = block.getId();
        this.height = block.getHeight();
        this.currencyId = currencyId;
        this.timestamp = block.getTimestamp();
        this.buyOfferId = currencyBuy.getId();
        this.sellOfferId = currencySell.getId();
        this.sellerId = currencyBuy.getAccountId();
        this.buyerId = currencySell.getAccountId();
        this.dbKey = exchangeDbKeyFactory.newKey(this.buyOfferId, this.sellOfferId);
        this.quantityQNT = quantityQNT;
        this.priceNQT = priceNQT;
    }

    private Exchange(ResultSet rs) throws SQLException {
        this.currencyId = rs.getLong("currency_id");
        this.blockId = rs.getLong("block_id");
        this.buyOfferId = rs.getLong("buy_offer_id");
        this.sellOfferId = rs.getLong("sell_offer_id");
        this.sellerId = rs.getLong("seller_id");
        this.buyerId = rs.getLong("buyer_id");
        this.dbKey = exchangeDbKeyFactory.newKey(this.buyOfferId, this.sellOfferId);
        this.quantityQNT = rs.getLong("quantity");
        this.priceNQT = rs.getLong("price");
        this.timestamp = rs.getInt("timestamp");
        this.height = rs.getInt("height");
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO exchange (currency_id, block_id, "
                + "buy_offer_id, sell_offer_id, seller_id, buyer_id, quantity, price, timestamp, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, this.getCurrencyId());
            pstmt.setLong(++i, this.getBlockId());
            pstmt.setLong(++i, this.getBuyOfferId());
            pstmt.setLong(++i, this.getSellOfferId());
            pstmt.setLong(++i, this.getSellerId());
            pstmt.setLong(++i, this.getBuyerId());
            pstmt.setLong(++i, this.getQuantityQNT());
            pstmt.setLong(++i, this.getPriceNQT());
            pstmt.setInt(++i, this.getTimestamp());
            pstmt.setInt(++i, this.getHeight());
            pstmt.executeUpdate();
        }
    }

    public Long getBlockId() { return blockId; }

    public Long getBuyOfferId() { return buyOfferId; }

    public Long getSellOfferId() { return sellOfferId; }

    public Long getSellerId() {
        return sellerId;
    }

    public Long getBuyerId() {
        return buyerId;
    }

    public long getQuantityQNT() { return quantityQNT; }

    public long getPriceNQT() { return priceNQT; }
    
    public Long getCurrencyId() { return currencyId; }
    
    public int getTimestamp() { return timestamp; }

    public int getHeight() {
        return height;
    }

    @Override
    public String toString() {
        return "Exchange currency: " + Convert.toUnsignedLong(currencyId) + " buy: " + Convert.toUnsignedLong(buyOfferId)
                + " sell: " + Convert.toUnsignedLong(sellOfferId) + " price: " + priceNQT + " quantity: " + quantityQNT + " height: " + height;
    }

}
