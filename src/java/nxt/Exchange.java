package nxt;

import nxt.db.DbClause;
import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.DbUtils;
import nxt.db.EntityDbTable;
import nxt.db.FilteringIterator;
import nxt.util.Convert;
import nxt.util.Filter;
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

    private static final DbKey.LinkKeyFactory<Exchange> exchangeDbKeyFactory = new DbKey.LinkKeyFactory<Exchange>("transaction_id", "offer_id") {

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
            con = Db.db.getConnection();
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
            con = Db.db.getConnection();
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

    public static FilteringIterator<? extends Transaction> getAccountCurrencyExchangeRequests(final long accountId, final long currencyId, int from, int to) {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM transaction where sender_id = ? AND type = ? AND (subtype = ? OR subtype = ?) " +
                    " ORDER BY block_timestamp DESC, transaction_index DESC ");
            int i = 0;
            pstmt.setLong(++i, accountId);
            pstmt.setByte(++i, MonetarySystem.EXCHANGE_BUY.getType());
            pstmt.setByte(++i, MonetarySystem.EXCHANGE_BUY.getSubtype());
            pstmt.setByte(++i, MonetarySystem.EXCHANGE_SELL.getSubtype());
            return new FilteringIterator<>(BlockchainImpl.getInstance().getTransactions(con, pstmt),
                    new Filter<TransactionImpl>() {
                        @Override
                        public boolean ok(TransactionImpl transaction) {
                            return ((Attachment.MonetarySystemAttachment)transaction.getAttachment()).getCurrencyId() == currencyId;
                        }
                    }, from, to);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static DbIterator<Exchange> getExchanges(long transactionId) {
        return exchangeTable.getManyBy(new DbClause.LongClause("transaction_id", transactionId), 0, -1, " ORDER BY height DESC ");
    }

    public static DbIterator<Exchange> getOfferExchanges(long offerId, int from, int to) {
        return exchangeTable.getManyBy(new DbClause.LongClause("offer_id", offerId), from, to, " ORDER BY height DESC ");
    }

    public static int getExchangeCount(long currencyId) {
        return exchangeTable.getCount(new DbClause.LongClause("currency_id", currencyId));
    }

    static Exchange addExchange(Transaction transaction, long currencyId, CurrencyExchangeOffer offer, long sellerId, long buyerId, long units) {
        Exchange exchange = new Exchange(transaction, currencyId, offer, sellerId, buyerId, units);
        exchangeTable.insert(exchange);
        listeners.notify(exchange, Event.EXCHANGE);
        return exchange;
    }

    static void init() {}


    private final long transactionId;
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

    private Exchange(Transaction transaction, long currencyId, CurrencyExchangeOffer offer, long sellerId, long buyerId, long units) {
        this.transactionId = transaction.getId();
        this.blockId = transaction.getBlockId();
        this.height = transaction.getHeight();
        this.currencyId = currencyId;
        this.timestamp = transaction.getBlockTimestamp();
        this.offerId = offer.getId();
        this.sellerId = sellerId;
        this.buyerId = buyerId;
        this.dbKey = exchangeDbKeyFactory.newKey(this.transactionId, this.offerId);
        this.units = units;
        this.rate = offer.getRateNQT();
    }

    private Exchange(ResultSet rs) throws SQLException {
        this.transactionId = rs.getLong("transaction_id");
        this.currencyId = rs.getLong("currency_id");
        this.blockId = rs.getLong("block_id");
        this.offerId = rs.getLong("offer_id");
        this.sellerId = rs.getLong("seller_id");
        this.buyerId = rs.getLong("buyer_id");
        this.dbKey = exchangeDbKeyFactory.newKey(this.transactionId, this.offerId);
        this.units = rs.getLong("units");
        this.rate = rs.getLong("rate");
        this.timestamp = rs.getInt("timestamp");
        this.height = rs.getInt("height");
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO exchange (transaction_id, currency_id, block_id, "
                + "offer_id, seller_id, buyer_id, units, rate, timestamp, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, this.getTransactionId());
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

    public long getTransactionId() {
        return transactionId;
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
                + " rate: " + rate + " units: " + units + " height: " + height + " transaction: " + Convert.toUnsignedLong(transactionId);
    }

}
