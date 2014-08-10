package nxt;

import nxt.crypto.EncryptedData;
import nxt.db.Db;
import nxt.db.VersioningDbTable;
import nxt.db.VersioningValuesDbTable;
import nxt.util.Convert;
import nxt.util.Listener;
import nxt.util.Listeners;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public final class DigitalGoodsStore {

    public static enum Event {
        GOODS_LISTED, GOODS_DELISTED, GOODS_PRICE_CHANGE, GOODS_QUANTITY_CHANGE,
        PURCHASE, DELIVERY, REFUND, FEEDBACK
    }

    static {
        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
                for (Purchase purchase : getExpiredPendingPurchases(block.getTimestamp())) {
                    Account buyer = Account.getAccount(purchase.getBuyerId());
                    buyer.addToUnconfirmedBalanceNQT(Convert.safeMultiply(purchase.getQuantity(), purchase.getPriceNQT()));
                    getGoods(purchase.getGoodsId()).changeQuantity(purchase.getQuantity());
                    purchase.setPending(false);
                }
            }
        }, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);

        // reverse any pending purchase expiration that was caused by the block that got popped off
        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
                Block previousBlock = Nxt.getBlockchain().getLastBlock();
                for (Purchase purchase : getPurchasesExpiredBetween(previousBlock.getTimestamp(), block.getTimestamp())) {
                    Account buyer = Account.getAccount(purchase.getBuyerId());
                    buyer.addToUnconfirmedBalanceNQT(- Convert.safeMultiply(purchase.getQuantity(), purchase.getPriceNQT()));
                    getGoods(purchase.getGoodsId()).changeQuantity(- purchase.getQuantity());
                    purchase.setPending(true);
                }
            }
        }, BlockchainProcessor.Event.BLOCK_POPPED);
    }

    private static final Listeners<Goods,Event> goodsListeners = new Listeners<>();

    private static final Listeners<Purchase,Event> purchaseListeners = new Listeners<>();

    public static boolean addGoodsListener(Listener<Goods> listener, Event eventType) {
        return goodsListeners.addListener(listener, eventType);
    }

    public static boolean removeGoodsListener(Listener<Goods> listener, Event eventType) {
        return goodsListeners.removeListener(listener, eventType);
    }

    public static boolean addPurchaseListener(Listener<Purchase> listener, Event eventType) {
        return purchaseListeners.addListener(listener, eventType);
    }

    public static boolean removePurchaseListener(Listener<Purchase> listener, Event eventType) {
        return purchaseListeners.removeListener(listener, eventType);
    }

    public static final class Goods {

        private static final VersioningDbTable<Goods> goodsTable = new VersioningDbTable<Goods>() {

            @Override
            protected Long getId(Goods goods) {
                return goods.getId();
            }

            @Override
            protected String table() {
                return "goods";
            }

            @Override
            protected Goods load(Connection con, ResultSet rs) throws SQLException {
                return new Goods(rs);
            }

            @Override
            protected void save(Connection con, Goods goods) throws SQLException {
                try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO goods (id, seller_id, name, "
                        + "description, tags, timestamp, quantity, price, delisted, height, latest) KEY (id, height) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
                    int i = 0;
                    pstmt.setLong(++i, goods.getId());
                    pstmt.setLong(++i, goods.getSellerId());
                    pstmt.setString(++i, goods.getName());
                    pstmt.setString(++i, goods.getDescription());
                    pstmt.setString(++i, goods.getTags());
                    pstmt.setInt(++i, goods.getTimestamp());
                    pstmt.setInt(++i, goods.getQuantity());
                    pstmt.setLong(++i, goods.getPriceNQT());
                    pstmt.setBoolean(++i, goods.isDelisted());
                    pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                    pstmt.executeUpdate();
                }
            }

        };

        private final Long id;
        private final Long sellerId;
        private final String name;
        private final String description;
        private final String tags;
        private final int timestamp;
        private int quantity;
        private long priceNQT;
        private boolean delisted;

        private Goods(Transaction transaction, Attachment.DigitalGoodsListing attachment) {
            this.id = transaction.getId();
            this.sellerId = transaction.getSenderId();
            this.name = attachment.getName();
            this.description = attachment.getDescription();
            this.tags = attachment.getTags();
            this.quantity = attachment.getQuantity();
            this.priceNQT = attachment.getPriceNQT();
            this.delisted = false;
            this.timestamp = transaction.getTimestamp();
        }

        private Goods(ResultSet rs) throws SQLException {
            this.id = rs.getLong("id");
            this.sellerId = rs.getLong("seller_id");
            this.name = rs.getString("name");
            this.description = rs.getString("description");
            this.tags = rs.getString("tags");
            this.quantity = rs.getInt("quantity");
            this.priceNQT = rs.getLong("price");
            this.delisted = rs.getBoolean("delisted");
            this.timestamp = rs.getInt("timestamp");
        }

        public Long getId() {
            return id;
        }

        public Long getSellerId() {
            return sellerId;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getTags() {
            return tags;
        }

        public int getTimestamp() {
            return timestamp;
        }

        public int getQuantity() {
            return quantity;
        }

        private void changeQuantity(int deltaQuantity) {
            quantity += deltaQuantity;
            if (quantity < 0) {
                quantity = 0;
            } else if (quantity > Constants.MAX_DGS_LISTING_QUANTITY) {
                quantity = Constants.MAX_DGS_LISTING_QUANTITY;
            }
            goodsTable.insert(this);
        }

        public long getPriceNQT() {
            return priceNQT;
        }

        private void changePrice(long priceNQT) {
            this.priceNQT = priceNQT;
            goodsTable.insert(this);
        }

        public boolean isDelisted() {
            return delisted;
        }

        private void setDelisted(boolean delisted) {
            this.delisted = delisted;
            goodsTable.insert(this);
        }

        /*
        @Override
        public int compareTo(Goods other) {
            return name.compareTo(other.name);
        }
        */

    }

    public static final class Purchase {

        private static final VersioningDbTable<Purchase> purchaseTable = new VersioningDbTable<Purchase>() {

            @Override
            protected Long getId(Purchase purchase) {
                return purchase.getId();
            }

            @Override
            protected String table() {
                return "purchase";
            }

            @Override
            protected Purchase load(Connection con, ResultSet rs) throws SQLException {
                return new Purchase(rs);
            }

            @Override
            protected void save(Connection con, Purchase purchase) throws SQLException {
                try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO purchase (id, buyer_id, goods_id, seller_id, "
                        + "quantity, price, deadline, note, nonce, timestamp, pending, goods, goods_nonce, refund_note, "
                        + "refund_nonce, has_feedback_notes, has_public_feedbacks, discount, refund, height, latest) KEY (id, height) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
                    int i = 0;
                    pstmt.setLong(++i, purchase.getId());
                    pstmt.setLong(++i, purchase.getBuyerId());
                    pstmt.setLong(++i, purchase.getGoodsId());
                    pstmt.setLong(++i, purchase.getSellerId());
                    pstmt.setInt(++i, purchase.getQuantity());
                    pstmt.setLong(++i, purchase.getPriceNQT());
                    pstmt.setInt(++i, purchase.getDeliveryDeadlineTimestamp());
                    setEncryptedData(pstmt, purchase.getNote(), ++i);
                    ++i;
                    pstmt.setInt(++i, purchase.getTimestamp());
                    pstmt.setBoolean(++i, purchase.isPending());
                    setEncryptedData(pstmt, purchase.getEncryptedGoods(), ++i);
                    ++i;
                    setEncryptedData(pstmt, purchase.getRefundNote(), ++i);
                    ++i;
                    pstmt.setBoolean(++i, purchase.getFeedbackNotes() != null && purchase.getFeedbackNotes().size() > 0);
                    pstmt.setBoolean(++i, purchase.getPublicFeedback() != null && purchase.getPublicFeedback().size() > 0);
                    pstmt.setLong(++i, purchase.getDiscountNQT());
                    pstmt.setLong(++i, purchase.getRefundNQT());
                    pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                    pstmt.executeUpdate();
                }
            }

        };

        private static final VersioningValuesDbTable<Purchase, EncryptedData> purchaseFeedbackNotesTable = new VersioningValuesDbTable<Purchase, EncryptedData>() {

            @Override
            protected Long getId(Purchase purchase) {
                return purchase.getId();
            }

            @Override
            protected String table() {
                return "purchase_feedback";
            }

            @Override
            protected EncryptedData load(Connection con, ResultSet rs) throws SQLException {
                byte[] data = rs.getBytes("feedback_data");
                byte[] nonce = rs.getBytes("feedback_nonce");
                return new EncryptedData(data, nonce);
            }

            @Override
            protected void save(Connection con, Purchase purchase, EncryptedData encryptedData) throws SQLException {
                try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO purchase_feedback (id, feedback_data, feedback_nonce, "
                        + "height, latest) VALUES (?, ?, ?, ?, TRUE)")) {
                    int i = 0;
                    pstmt.setLong(++i, purchase.getId());
                    setEncryptedData(pstmt, encryptedData, ++i);
                    ++i;
                    pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                    pstmt.executeUpdate();
                }
            }

        };

        private static final VersioningValuesDbTable<Purchase, String> purchasePublicFeedbackTable = new VersioningValuesDbTable<Purchase, String>() {

            @Override
            protected Long getId(Purchase purchase) {
                return purchase.getId();
            }

            @Override
            protected String table() {
                return "purchase_public_feedback";
            }

            @Override
            protected String load(Connection con, ResultSet rs) throws SQLException {
                return rs.getString("public_feedback");
            }

            @Override
            protected void save(Connection con, Purchase purchase, String publicFeedback) throws SQLException {
                try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO purchase_public_feedback (id, public_feedback, "
                        + "height, latest) VALUES (?, ?, ?, TRUE)")) {
                    int i = 0;
                    pstmt.setLong(++i, purchase.getId());
                    pstmt.setString(++i, publicFeedback);
                    pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                    pstmt.executeUpdate();
                }
            }

        };

        private final Long id;
        private final Long buyerId;
        private final Long goodsId;
        private final Long sellerId;
        private final int quantity;
        private final long priceNQT;
        private final int deadline;
        private final EncryptedData note;
        private final int timestamp;
        private boolean isPending;
        private EncryptedData encryptedGoods;
		private boolean goodsIsText;
        private EncryptedData refundNote;
        private boolean hasFeedbackNotes;
        private List<EncryptedData> feedbackNotes;
        private boolean hasPublicFeedbacks;
        private List<String> publicFeedbacks;
        private long discountNQT;
        private long refundNQT;

        private Purchase(Transaction transaction, Attachment.DigitalGoodsPurchase attachment, Long sellerId) {
            this.id = transaction.getId();
            this.buyerId = transaction.getSenderId();
            this.goodsId = attachment.getGoodsId();
            this.sellerId = sellerId;
            this.quantity = attachment.getQuantity();
            this.priceNQT = attachment.getPriceNQT();
            this.deadline = attachment.getDeliveryDeadlineTimestamp();
            this.note = transaction.getEncryptedMessage() == null ? null : transaction.getEncryptedMessage().getEncryptedData();
            this.timestamp = transaction.getTimestamp();
            this.isPending = true;
        }

        private Purchase(ResultSet rs) throws SQLException {
            this.id = rs.getLong("id");
            this.buyerId = rs.getLong("buyer_id");
            this.goodsId = rs.getLong("goods_id");
            this.sellerId = rs.getLong("seller_id");
            this.quantity = rs.getInt("quantity");
            this.priceNQT = rs.getLong("price");
            this.deadline = rs.getInt("deadline");
            this.note = loadEncryptedData(rs, "note", "nonce");
            this.timestamp = rs.getInt("timestamp");
            this.isPending = rs.getBoolean("pending");
            this.encryptedGoods = loadEncryptedData(rs, "goods", "goods_nonce");
            this.refundNote = loadEncryptedData(rs, "refund_note", "refund_nonce");
            this.hasFeedbackNotes = rs.getBoolean("has_feedback_notes");
            this.hasPublicFeedbacks = rs.getBoolean("has_public_feedbacks");
            this.discountNQT = rs.getLong("discount");
            this.refundNQT = rs.getLong("refund");
        }

        public Long getId() {
            return id;
        }

        public Long getBuyerId() {
            return buyerId;
        }

        public Long getGoodsId() {
            return goodsId;
        }

        public Long getSellerId() { return sellerId; }

        public int getQuantity() {
            return quantity;
        }

        public long getPriceNQT() {
            return priceNQT;
        }

        public int getDeliveryDeadlineTimestamp() {
            return deadline;
        }

        public EncryptedData getNote() {
            return note;
        }

        public boolean isPending() {
            return isPending;
        }

        private void setPending(boolean isPending) {
            this.isPending = isPending;
            purchaseTable.insert(this);
        }

        public int getTimestamp() {
            return timestamp;
        }

        public String getName() {
            return getGoods(goodsId).getName();
        }

        public EncryptedData getEncryptedGoods() {
            return encryptedGoods;
        }

        public boolean goodsIsText() {
            return goodsIsText;
        }

        private void setEncryptedGoods(EncryptedData encryptedGoods, boolean goodsIsText) {
            this.encryptedGoods = encryptedGoods;
            this.goodsIsText = goodsIsText;
            purchaseTable.insert(this);
        }

        public EncryptedData getRefundNote() {
            return refundNote;
        }

        private void setRefundNote(EncryptedData refundNote) {
            this.refundNote = refundNote;
            purchaseTable.insert(this);
        }

        public List<EncryptedData> getFeedbackNotes() {
            if (!hasFeedbackNotes) {
                return null;
            }
            feedbackNotes = purchaseFeedbackNotesTable.get(id);
            return feedbackNotes;
        }

        private void addFeedbackNote(EncryptedData feedbackNote) {
            if (feedbackNotes == null) {
                feedbackNotes = new ArrayList<>();
            }
            feedbackNotes.add(feedbackNote);
            this.hasFeedbackNotes = true;
            purchaseTable.insert(this);
            purchaseFeedbackNotesTable.insert(this, feedbackNote);
		}

        public List<String> getPublicFeedback() {
            if (!hasPublicFeedbacks) {
                return null;
            }
            publicFeedbacks = purchasePublicFeedbackTable.get(id);
            return publicFeedbacks;
        }

        private void addPublicFeedback(String publicFeedback) {
            if (publicFeedbacks == null) {
                publicFeedbacks = new ArrayList<>();
            }
            publicFeedbacks.add(publicFeedback);
            this.hasPublicFeedbacks = true;
            purchaseTable.insert(this);
            purchasePublicFeedbackTable.insert(this, publicFeedback);
        }

        public long getDiscountNQT() {
            return discountNQT;
        }

        public void setDiscountNQT(long discountNQT) {
            this.discountNQT = discountNQT;
            purchaseTable.insert(this);
        }

        public long getRefundNQT() {
            return refundNQT;
        }

        public void setRefundNQT(long refundNQT) {
            this.refundNQT = refundNQT;
            purchaseTable.insert(this);
        }

        /*
        @Override
        public int compareTo(Purchase other) {
            if (this.timestamp < other.timestamp) {
                return 1;
            }
            if (this.timestamp > other.timestamp) {
                return -1;
            }
            return Long.compare(this.id, other.id);
        }
        */

    }

    public static Goods getGoods(Long goodsId) {
        return Goods.goodsTable.get(goodsId);
    }

    public static List<Goods> getAllGoods() {
        return Goods.goodsTable.getAll();
    }

    public static List<Goods> getGoodsInStock() {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM goods WHERE "
                     + "latest = TRUE AND delisted = FALSE AND quantity > 0 "
                     + "ORDER BY timestamp DESC")) {
            return Goods.goodsTable.getManyBy(con, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static List<Goods> getSellerGoods(Long sellerId, boolean inStockOnly) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM goods WHERE seller_id = ? "
                     + "AND latest = TRUE " + (inStockOnly ? "AND delisted = FALSE AND quantity > 0" : "")
                     + " ORDER BY name ASC")) {
            pstmt.setLong(1, sellerId);
            return Goods.goodsTable.getManyBy(con, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static List<Purchase> getAllPurchases() {
        return Purchase.purchaseTable.getAll();
    }

    public static List<Purchase> getSellerPurchases(Long sellerId) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM purchase WHERE seller_id = ? "
                     + "AND latest = TRUE ORDER BY timestamp DESC, id ASC")) {
            pstmt.setLong(1, sellerId);
            return Purchase.purchaseTable.getManyBy(con, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static List<Purchase> getBuyerPurchases(Long buyerId) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM purchase WHERE buyer_id = ? "
                     + "AND latest = TRUE ORDER BY timestamp DESC, id ASC")) {
            pstmt.setLong(1, buyerId);
            return Purchase.purchaseTable.getManyBy(con, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static List<Purchase> getSellerBuyerPurchases(Long sellerId, Long buyerId) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM purchase WHERE seller_id = ? "
                     + "AND buyer_id = ? AND latest = TRUE ORDER BY timestamp DESC, id ASC")) {
            pstmt.setLong(1, sellerId);
            pstmt.setLong(2, buyerId);
            return Purchase.purchaseTable.getManyBy(con, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static Purchase getPurchase(Long purchaseId) {
        return Purchase.purchaseTable.get(purchaseId);
    }

    public static List<Purchase> getPendingSellerPurchases(Long sellerId) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM purchase WHERE seller_id = ? "
                     + "AND pending = TRUE AND latest = TRUE ORDER BY timestamp DESC, id ASC")) {
            pstmt.setLong(1, sellerId);
            return Purchase.purchaseTable.getManyBy(con, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    static Purchase getPendingPurchase(Long purchaseId) {
        Purchase purchase = getPurchase(purchaseId);
        return purchase.isPending() ? purchase : null;
    }

    private static List<Purchase> getExpiredPendingPurchases(int timestamp) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM purchase WHERE deadline < ? "
                     + "AND pending = TRUE AND latest = TRUE ORDER BY timestamp DESC, id ASC")) {
            pstmt.setLong(1, timestamp);
            return Purchase.purchaseTable.getManyBy(con, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
		}
	}
	
    private static List<Purchase> getPurchasesExpiredBetween(int previousTimestamp, int currentTimestamp) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM purchase WHERE deadline >= ? AND deadline < ? "
                     + "AND latest = TRUE ORDER BY timestamp DESC, id ASC")) {
            pstmt.setLong(1, previousTimestamp);
            pstmt.setLong(2, currentTimestamp);
            return Purchase.purchaseTable.getManyBy(con, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    private static void addPurchase(Transaction transaction,  Attachment.DigitalGoodsPurchase attachment, Long sellerId) {
        Purchase purchase = new Purchase(transaction, attachment, sellerId);
        Purchase.purchaseTable.insert(purchase);
        purchaseListeners.notify(purchase, Event.PURCHASE);
    }

    static void clear() {
        Goods.goodsTable.truncate();
        Purchase.purchaseTable.truncate();
    }

    static void listGoods(Transaction transaction, Attachment.DigitalGoodsListing attachment) {
        Goods goods = new Goods(transaction, attachment);
        Goods.goodsTable.insert(goods);
        goodsListeners.notify(goods, Event.GOODS_LISTED);
    }

    static void undoListGoods(Long goodsId) {
        Goods.goodsTable.rollbackTo(goodsId, Nxt.getBlockchain().getHeight());
    }

    static void delistGoods(Long goodsId) {
        Goods goods = Goods.goodsTable.get(goodsId);
        if (! goods.isDelisted()) {
            goods.setDelisted(true);
            goodsListeners.notify(goods, Event.GOODS_DELISTED);
        } else {
            throw new IllegalStateException("Goods already delisted");
        }
    }

    static void undoDelistGoods(Long goodsId) {
        Goods goods = Goods.goodsTable.get(goodsId);
        if (goods.isDelisted()) {
            goods.setDelisted(false);
        } else {
            throw new IllegalStateException("Goods were not delisted");
        }
    }

    static void changePrice(Long goodsId, long priceNQT) {
        Goods goods = Goods.goodsTable.get(goodsId);
        if (! goods.isDelisted()) {
            goods.changePrice(priceNQT);
            goodsListeners.notify(goods, Event.GOODS_PRICE_CHANGE);
        } else {
            throw new IllegalStateException("Can't change price of delisted goods");
        }
    }

    static void changeQuantity(Long goodsId, int deltaQuantity) {
        Goods goods = Goods.goodsTable.get(goodsId);
        if (! goods.isDelisted()) {
            goods.changeQuantity(deltaQuantity);
            goodsListeners.notify(goods, Event.GOODS_QUANTITY_CHANGE);
        } else {
            throw new IllegalStateException("Can't change quantity of delisted goods");
        }
    }

    static void purchase(Transaction transaction,  Attachment.DigitalGoodsPurchase attachment) {
        Goods goods = Goods.goodsTable.get(attachment.getGoodsId());
        if (! goods.isDelisted() && attachment.getQuantity() <= goods.getQuantity() && attachment.getPriceNQT() == goods.getPriceNQT()
                && attachment.getDeliveryDeadlineTimestamp() > Nxt.getBlockchain().getLastBlock().getTimestamp()) {
            goods.changeQuantity(-attachment.getQuantity());
            addPurchase(transaction, attachment, goods.getSellerId());
        } else {
            Account buyer = Account.getAccount(transaction.getSenderId());
            buyer.addToUnconfirmedBalanceNQT(Convert.safeMultiply(attachment.getQuantity(), attachment.getPriceNQT()));
            // restoring the unconfirmed balance if purchase not successful, however buyer still lost the transaction fees
        }
    }

    static void undoPurchase(Long purchaseId, Long buyerId, int quantity, long priceNQT) {
        Purchase purchase = Purchase.purchaseTable.get(purchaseId);
        Purchase.purchaseTable.rollbackTo(purchaseId, Nxt.getBlockchain().getHeight());
        if (purchase != null) {
            Goods.goodsTable.rollbackTo(purchase.getGoodsId(), Nxt.getBlockchain().getHeight());
        } else {
            Account buyer = Account.getAccount(buyerId);
            buyer.addToUnconfirmedBalanceNQT(-Convert.safeMultiply(quantity, priceNQT));
        }
    }

    static void deliver(Transaction transaction, Attachment.DigitalGoodsDelivery attachment) {
        Purchase purchase = getPendingPurchase(attachment.getPurchaseId());
        purchase.setPending(false);
        long totalWithoutDiscount = Convert.safeMultiply(purchase.getQuantity(), purchase.getPriceNQT());
        Account buyer = Account.getAccount(purchase.getBuyerId());
        buyer.addToBalanceNQT(Convert.safeSubtract(attachment.getDiscountNQT(), totalWithoutDiscount));
        buyer.addToUnconfirmedBalanceNQT(attachment.getDiscountNQT());
        Account seller = Account.getAccount(transaction.getSenderId());
        seller.addToBalanceAndUnconfirmedBalanceNQT(Convert.safeSubtract(totalWithoutDiscount, attachment.getDiscountNQT()));
        purchase.setEncryptedGoods(attachment.getGoods(), attachment.goodsIsText());
        purchase.setDiscountNQT(attachment.getDiscountNQT());
        purchaseListeners.notify(purchase, Event.DELIVERY);
    }

    static void undoDeliver(Long sellerId, Long purchaseId, long discountNQT) {
        Purchase purchase = Purchase.purchaseTable.get(purchaseId);
        purchase.setPending(true);
        long totalWithoutDiscount = Convert.safeMultiply(purchase.getQuantity(), purchase.getPriceNQT());
        Account buyer = Account.getAccount(purchase.getBuyerId());
        buyer.addToBalanceNQT(Convert.safeSubtract(totalWithoutDiscount, discountNQT));
        buyer.addToUnconfirmedBalanceNQT(- discountNQT);
        Account seller = Account.getAccount(sellerId);
        seller.addToBalanceAndUnconfirmedBalanceNQT(Convert.safeSubtract(discountNQT, totalWithoutDiscount));
        purchase.setEncryptedGoods(null, false);
        purchase.setDiscountNQT(0);
    }

    static void refund(Long sellerId, Long purchaseId, long refundNQT, Appendix.EncryptedMessage encryptedMessage) {
        Purchase purchase = Purchase.purchaseTable.get(purchaseId);
        Account seller = Account.getAccount(sellerId);
        seller.addToBalanceNQT(-refundNQT);
        Account buyer = Account.getAccount(purchase.getBuyerId());
        buyer.addToBalanceAndUnconfirmedBalanceNQT(refundNQT);
        if (encryptedMessage != null) {
            purchase.setRefundNote(encryptedMessage.getEncryptedData());
        }
        purchase.setRefundNQT(refundNQT);
        purchaseListeners.notify(purchase, Event.REFUND);
    }

    static void undoRefund(Long sellerId, Long purchaseId, long refundNQT) {
        Purchase purchase = Purchase.purchaseTable.get(purchaseId);
        Account seller = Account.getAccount(sellerId);
        seller.addToBalanceNQT(refundNQT);
        Account buyer = Account.getAccount(purchase.getBuyerId());
        buyer.addToBalanceAndUnconfirmedBalanceNQT(-refundNQT);
        purchase.setRefundNote(null);
        purchase.setRefundNQT(0);
    }

    static void feedback(Long purchaseId, Appendix.EncryptedMessage encryptedMessage, Appendix.Message message) {
        Purchase purchase = Purchase.purchaseTable.get(purchaseId);
        if (encryptedMessage != null) {
            purchase.addFeedbackNote(encryptedMessage.getEncryptedData());
        }
        if (message != null) {
            purchase.addPublicFeedback(Convert.toString(message.getMessage()));
        }
        purchaseListeners.notify(purchase, Event.FEEDBACK);
    }

    static void undoFeedback(Long purchaseId, Appendix.EncryptedMessage encryptedMessage, Appendix.Message message) {
        Purchase purchase = Purchase.purchaseTable.get(purchaseId);
        // TODO: this may not be needed
        /*
        if (encryptedMessage != null) {
            purchase.removeFeedbackNote();
        }
        if (message != null) {
            purchase.removePublicFeedback();
        }
        */
    }

    private static EncryptedData loadEncryptedData(ResultSet rs, String dataColumn, String nonceColumn) throws SQLException {
        byte[] data = rs.getBytes(dataColumn);
        if (data == null) {
            return null;
        }
        return new EncryptedData(data, rs.getBytes(nonceColumn));
    }

    private static void setEncryptedData(PreparedStatement pstmt, EncryptedData encryptedData, int i) throws SQLException {
        if (encryptedData == null) {
            pstmt.setNull(i, Types.VARBINARY);
            pstmt.setNull(i + 1, Types.VARBINARY);
        } else {
            pstmt.setBytes(i, encryptedData.getData());
            pstmt.setBytes(i + 1, encryptedData.getNonce());
        }
    }

}
