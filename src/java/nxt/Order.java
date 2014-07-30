package nxt;

import nxt.db.VersioningDbTable;
import nxt.util.Convert;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public abstract class Order {

    static void clear() {
        Ask.askOrderTable.truncate();
        Bid.bidOrderTable.truncate();
    }

    private static void matchOrders(Long assetId) {

        Order.Ask askOrder;
        Order.Bid bidOrder;

        while ((askOrder = Ask.getNextOrder(assetId)) != null
                && (bidOrder = Bid.getNextOrder(assetId)) != null) {

            if (askOrder.getPriceNQT() > bidOrder.getPriceNQT()) {
                break;
            }

            long quantityQNT = Math.min(askOrder.getQuantityQNT(), bidOrder.getQuantityQNT());
            long priceNQT = askOrder.getHeight() < bidOrder.getHeight()
                    || (askOrder.getHeight() == bidOrder.getHeight()
                    && askOrder.getId() < bidOrder.getId())
                    ? askOrder.getPriceNQT() : bidOrder.getPriceNQT();

            Trade.addTrade(assetId, Nxt.getBlockchain().getLastBlock(), askOrder.getId(), bidOrder.getId(), quantityQNT, priceNQT);

            askOrder.updateQuantityQNT(Convert.safeSubtract(askOrder.getQuantityQNT(), quantityQNT));
            Account askAccount = Account.getAccount(askOrder.getAccountId());
            askAccount.addToBalanceAndUnconfirmedBalanceNQT(Convert.safeMultiply(quantityQNT, priceNQT));
            askAccount.addToAssetBalanceQNT(assetId, -quantityQNT);

            bidOrder.updateQuantityQNT(Convert.safeSubtract(bidOrder.getQuantityQNT(), quantityQNT));
            Account bidAccount = Account.getAccount(bidOrder.getAccountId());
            bidAccount.addToAssetAndUnconfirmedAssetBalanceQNT(assetId, quantityQNT);
            bidAccount.addToBalanceNQT(-Convert.safeMultiply(quantityQNT, priceNQT));
            bidAccount.addToUnconfirmedBalanceNQT(Convert.safeMultiply(quantityQNT, (bidOrder.getPriceNQT() - priceNQT)));

        }

    }

    private final Long id;
    private final Long accountId;
    private final Long assetId;
    private final long priceNQT;
    private final int height;

    private long quantityQNT;

    private Order(Transaction transaction, Attachment.ColoredCoinsOrderPlacement attachment) {
        this.id = transaction.getId();
        this.accountId = transaction.getSenderId();
        this.assetId = attachment.getAssetId();
        this.quantityQNT = attachment.getQuantityQNT();
        this.priceNQT = attachment.getPriceNQT();
        this.height = transaction.getHeight();
    }

    private Order(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.accountId = rs.getLong("account_id");
        this.assetId = rs.getLong("asset_id");
        this.priceNQT = rs.getLong("price");
        this.quantityQNT = rs.getLong("quantity");
        this.height = rs.getInt("height");
    }

    public Long getId() {
        return id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public Long getAssetId() {
        return assetId;
    }

    public long getPriceNQT() {
        return priceNQT;
    }

    public final long getQuantityQNT() {
        return quantityQNT;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " id: " + Convert.toUnsignedLong(id) + " account: " + Convert.toUnsignedLong(accountId)
                + " asset: " + Convert.toUnsignedLong(assetId) + " price: " + priceNQT + " quantity: " + quantityQNT + " height: " + height;
    }

    private void setQuantityQNT(long quantityQNT) {
        this.quantityQNT = quantityQNT;
    }

    /*
    private int compareTo(Order o) {
        if (height < o.height) {
            return -1;
        } else if (height > o.height) {
            return 1;
        } else {
            if (id < o.id) {
                return -1;
            } else if (id > o.id) {
                return 1;
            } else {
                return 0;
            }
        }

    }
    */

    public static final class Ask extends Order {

        private static final VersioningDbTable<Ask> askOrderTable = new VersioningDbTable<Ask>() {

            @Override
            protected Long getId(Ask ask) {
                return ask.getId();
            }

            @Override
            protected String table() {
                return "ask_order";
            }

            @Override
            protected Ask load(Connection con, ResultSet rs) throws SQLException {
                return new Ask(rs);
            }

            @Override
            protected void save(Connection con, Ask ask) throws SQLException {
                try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO ask_order (id, account_id, asset_id, "
                        + "price, quantity, height, latest) KEY (id, height) VALUES (?, ?, ?, ?, ?, ?, TRUE)")) {
                    int i = 0;
                    pstmt.setLong(++i, ask.getId());
                    pstmt.setLong(++i, ask.getAccountId());
                    pstmt.setLong(++i, ask.getAssetId());
                    pstmt.setLong(++i, ask.getPriceNQT());
                    pstmt.setLong(++i, ask.getQuantityQNT());
                    pstmt.setInt(++i, ask.getHeight());
                    pstmt.executeUpdate();
                }
            }

        };

        public static int getCount() {
            return askOrderTable.getCount();
        }

        public static Ask getAskOrder(Long orderId) {
            return askOrderTable.get(orderId);
        }

        public static List<Ask> getAll() {
            return askOrderTable.getAll();
        }

        public static List<Ask> getAskOrdersByAccount(Long accountId) {
            return askOrderTable.getManyBy("account_id", accountId);
        }

        public static List<Ask> getAskOrdersByAsset(Long assetId) {
            return askOrderTable.getManyBy("asset_id", assetId);
        }

        public static List<Ask> getAskOrdersByAccountAsset(Long accountId, Long assetId) {
            try (Connection con = Db.getConnection();
                 PreparedStatement pstmt = con.prepareStatement("SELECT * FROM ask_order WHERE account_id = ? "
                         + "AND asset_id = ? AND latest = TRUE ORDER BY height DESC")) {
                pstmt.setLong(1, accountId);
                pstmt.setLong(2, assetId);
                return askOrderTable.getManyBy(con, pstmt);
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }

        public static List<Ask> getSortedOrders(Long assetId) {
            try (Connection con = Db.getConnection();
                 PreparedStatement pstmt = con.prepareStatement("SELECT * FROM ask_order WHERE asset_id = ? "
                         + "AND latest = TRUE ORDER BY price ASC, height ASC, id ASC")) {
                pstmt.setLong(1, assetId);
                return askOrderTable.getManyBy(con, pstmt);
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }

        private static Ask getNextOrder(Long assetId) {
            try (Connection con = Db.getConnection();
                 PreparedStatement pstmt = con.prepareStatement("SELECT * FROM ask_order WHERE asset_id = ? "
                         + "AND latest = TRUE ORDER BY price ASC, height ASC, id ASC LIMIT 1")) {
                pstmt.setLong(1, assetId);
                List<Ask> result = askOrderTable.getManyBy(con, pstmt);
                return result.isEmpty() ? null : result.get(0);
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }

        static void addOrder(Transaction transaction, Attachment.ColoredCoinsAskOrderPlacement attachment) {
            Ask order = new Ask(transaction, attachment);
            askOrderTable.insert(order);
            matchOrders(attachment.getAssetId());
        }

        static void rollbackOrder(Long orderId) {
            askOrderTable.rollbackTo(orderId, Nxt.getBlockchain().getHeight());
        }

        static void removeOrder(Long orderId) {
            askOrderTable.delete(getAskOrder(orderId));
        }

        private Ask(Transaction transaction, Attachment.ColoredCoinsAskOrderPlacement attachment) {
            super(transaction, attachment);
        }

        private Ask(ResultSet rs) throws SQLException {
            super(rs);
        }

        private void updateQuantityQNT(long quantityQNT) {
            super.setQuantityQNT(quantityQNT);
            askOrderTable.insert(this);
            if (quantityQNT == 0) {
                askOrderTable.delete(this);
            }
        }

        /*
        @Override
        public int compareTo(Ask o) {
            if (this.getPriceNQT() < o.getPriceNQT()) {
                return -1;
            } else if (this.getPriceNQT() > o.getPriceNQT()) {
                return 1;
            } else {
                return super.compareTo(o);
            }
        }
        */

    }

    public static final class Bid extends Order {

        private static final VersioningDbTable<Bid> bidOrderTable = new VersioningDbTable<Bid>() {

            @Override
            protected Long getId(Bid bid) {
                return bid.getId();
            }

            @Override
            protected String table() {
                return "bid_order";
            }

            @Override
            protected Bid load(Connection con, ResultSet rs) throws SQLException {
                return new Bid(rs);
            }

            @Override
            protected void save(Connection con, Bid bid) throws SQLException {
                try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO bid_order (id, account_id, asset_id, "
                        + "price, quantity, height, latest) KEY (id, height) VALUES (?, ?, ?, ?, ?, ?, TRUE)")) {
                    int i = 0;
                    pstmt.setLong(++i, bid.getId());
                    pstmt.setLong(++i, bid.getAccountId());
                    pstmt.setLong(++i, bid.getAssetId());
                    pstmt.setLong(++i, bid.getPriceNQT());
                    pstmt.setLong(++i, bid.getQuantityQNT());
                    pstmt.setInt(++i, bid.getHeight());
                    pstmt.executeUpdate();
                }
            }
        };

        public static int getCount() {
            return bidOrderTable.getCount();
        }

        public static Bid getBidOrder(Long orderId) {
            return bidOrderTable.get(orderId);
        }

        public static List<Bid> getAll() {
            return bidOrderTable.getAll();
        }

        public static List<Bid> getBidOrdersByAccount(Long accountId) {
            return bidOrderTable.getManyBy("account_id", accountId);
        }

        public static List<Bid> getBidOrdersByAsset(Long assetId) {
            return bidOrderTable.getManyBy("asset_id", assetId);
        }

        public static List<Bid> getBidOrdersByAccountAsset(Long accountId, Long assetId) {
            try (Connection con = Db.getConnection();
                 PreparedStatement pstmt = con.prepareStatement("SELECT * FROM bid_order WHERE account_id = ? "
                         + "AND asset_id = ? AND latest = TRUE ORDER BY height DESC")) {
                pstmt.setLong(1, accountId);
                pstmt.setLong(2, assetId);
                return bidOrderTable.getManyBy(con, pstmt);
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }

        public static List<Bid> getSortedOrders(Long assetId) {
            try (Connection con = Db.getConnection();
                 PreparedStatement pstmt = con.prepareStatement("SELECT * FROM bid_order WHERE asset_id = ? "
                         + "AND latest = TRUE ORDER BY price DESC, height ASC, id ASC")) {
                pstmt.setLong(1, assetId);
                return bidOrderTable.getManyBy(con, pstmt);
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }

        private static Bid getNextOrder(Long assetId) {
            try (Connection con = Db.getConnection();
                 PreparedStatement pstmt = con.prepareStatement("SELECT * FROM bid_order WHERE asset_id = ? "
                         + "AND latest = TRUE ORDER BY price DESC, height ASC, id ASC LIMIT 1")) {
                pstmt.setLong(1, assetId);
                List<Bid> result = bidOrderTable.getManyBy(con, pstmt);
                return result.isEmpty() ? null : result.get(0);
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }

        static void addOrder(Transaction transaction, Attachment.ColoredCoinsBidOrderPlacement attachment) {
            Bid order = new Bid(transaction, attachment);
            bidOrderTable.insert(order);
            matchOrders(attachment.getAssetId());
        }

        static void rollbackOrder(Long orderId) {
            bidOrderTable.rollbackTo(orderId, Nxt.getBlockchain().getHeight());
        }

        static void removeOrder(Long orderId) {
            bidOrderTable.delete(getBidOrder(orderId));
        }

        private Bid(Transaction transaction, Attachment.ColoredCoinsBidOrderPlacement attachment) {
            super(transaction, attachment);
        }

        private Bid(ResultSet rs) throws SQLException {
            super(rs);
        }

        private void updateQuantityQNT(long quantityQNT) {
            super.setQuantityQNT(quantityQNT);
            bidOrderTable.insert(this);
            if (quantityQNT == 0) {
                bidOrderTable.delete(this);
            }
        }

        /*
        @Override
        public int compareTo(Bid o) {
            if (this.getPriceNQT() > o.getPriceNQT()) {
                return -1;
            } else if (this.getPriceNQT() < o.getPriceNQT()) {
                return 1;
            } else {
                return super.compareTo(o);
            }
        }
        */
    }
}
