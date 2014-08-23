package nxt;

import nxt.db.EntityDbTable;
import nxt.util.Convert;
import nxt.util.Listener;
import nxt.util.Listeners;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

public final class Trade {

    public static enum Event {
        TRADE
    }

    private static final Listeners<Trade,Event> listeners = new Listeners<>();

    private static final EntityDbTable<Trade> tradeTable = new EntityDbTable<Trade>() {

        @Override
        protected String table() {
            return "trade";
        }

        @Override
        protected Long getId(Trade trade) {
            //TODO
            return null;
        }

        @Override
        protected Trade load(Connection con, ResultSet rs) throws SQLException {
            return new Trade(rs);
        }

        @Override
        protected void save(Connection con, Trade trade) throws SQLException {
            trade.save(con);
        }

    };

    public static Collection<Trade> getAllTrades() {
        return tradeTable.getAll();
    }

    public static int getCount() {
        return tradeTable.getCount();
    }

    public static boolean addListener(Listener<Trade> listener, Event eventType) {
        return listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<Trade> listener, Event eventType) {
        return listeners.removeListener(listener, eventType);
    }

    public static List<Trade> getTrades(Long assetId) {
        return tradeTable.getManyBy("asset_id", assetId);
    }

    static Trade addTrade(Long assetId, Block block, Long askOrderId, Long bidOrderId, long quantityQNT, long priceNQT) {
        Trade trade = new Trade(assetId, block, askOrderId, bidOrderId, quantityQNT, priceNQT);
        tradeTable.insert(trade);
        listeners.notify(trade, Event.TRADE);
        return trade;
    }

    static void clear() {
        tradeTable.truncate();
    }

    private final int timestamp;
    private final Long assetId;
    private final Long blockId;
    private final int height;
    private final Long askOrderId, bidOrderId;
    private final long quantityQNT;
    private final long priceNQT;

    private Trade(Long assetId, Block block, Long askOrderId, Long bidOrderId, long quantityQNT, long priceNQT) {
        this.blockId = block.getId();
        this.height = block.getHeight();
        this.assetId = assetId;
        this.timestamp = block.getTimestamp();
        this.askOrderId = askOrderId;
        this.bidOrderId = bidOrderId;
        this.quantityQNT = quantityQNT;
        this.priceNQT = priceNQT;
    }

    private Trade(ResultSet rs) throws SQLException {
        this.assetId = rs.getLong("asset_id");
        this.blockId = rs.getLong("block_id");
        this.askOrderId = rs.getLong("ask_order_id");
        this.bidOrderId = rs.getLong("bid_order_id");
        this.quantityQNT = rs.getLong("quantity");
        this.priceNQT = rs.getLong("price");
        this.timestamp = rs.getInt("timestamp");
        this.height = rs.getInt("height");
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO trade (asset_id, block_id, "
                + "ask_order_id, bid_order_id, quantity, price, timestamp, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, this.getAssetId());
            pstmt.setLong(++i, this.getBlockId());
            pstmt.setLong(++i, this.getAskOrderId());
            pstmt.setLong(++i, this.getBidOrderId());
            pstmt.setLong(++i, this.getQuantityQNT());
            pstmt.setLong(++i, this.getPriceNQT());
            pstmt.setInt(++i, this.getTimestamp());
            pstmt.setInt(++i, this.getHeight());
            pstmt.executeUpdate();
        }
    }

    public Long getBlockId() { return blockId; }

    public Long getAskOrderId() { return askOrderId; }

    public Long getBidOrderId() { return bidOrderId; }

    public long getQuantityQNT() { return quantityQNT; }

    public long getPriceNQT() { return priceNQT; }
    
    public Long getAssetId() { return assetId; }
    
    public int getTimestamp() { return timestamp; }

    public int getHeight() {
        return height;
    }

    @Override
    public String toString() {
        return "Trade asset: " + Convert.toUnsignedLong(assetId) + " ask: " + Convert.toUnsignedLong(askOrderId)
                + " bid: " + Convert.toUnsignedLong(bidOrderId) + " price: " + priceNQT + " quantity: " + quantityQNT + " height: " + height;
    }

}
