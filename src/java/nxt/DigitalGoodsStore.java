package nxt;

import nxt.crypto.EncryptedData;
import nxt.db.Db;
import nxt.db.DbKey;
import nxt.db.VersionedEntityDbTable;
import nxt.db.VersionedValuesDbTable;
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

        private static final DbKey.LongKeyFactory<Goods> goodsDbKeyFactory = new DbKey.LongKeyFactory<Goods>("id") {

            @Override
            public DbKey newKey(Goods goods) {
                return newKey(goods.getId());
            }

        };

        private static final VersionedEntityDbTable<Goods> goodsTable = new VersionedEntityDbTable<Goods>(goodsDbKeyFactory) {

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
                goods.save(con);
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

        private void save(Connection con) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO goods (id, seller_id, name, "
                    + "description, tags, timestamp, quantity, price, delisted, height, latest) KEY (id, height) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
                int i = 0;
                pstmt.setLong(++i, this.getId());
                pstmt.setLong(++i, this.getSellerId());
                pstmt.setString(++i, this.getName());
                pstmt.setString(++i, this.getDescription());
                pstmt.setString(++i, this.getTags());
                pstmt.setInt(++i, this.getTimestamp());
                pstmt.setInt(++i, this.getQuantity());
                pstmt.setLong(++i, this.getPriceNQT());
                pstmt.setBoolean(++i, this.isDelisted());
                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
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
            if (!name.equals(other.name)) {
                return name.compareTo(other.name);
            }
            if (!description.equals(other.description)) {
                return description.compareTo(other.description);
            }
            return Long.compare(id, other.id);
        }
        */

    }

    public static final class Purchase {

        private static final DbKey.LongKeyFactory<Purchase> purchaseDbKeyFactory = new DbKey.LongKeyFactory<Purchase>("id") {

            @Override
            public DbKey newKey(Purchase purchase) {
                return newKey(purchase.getId());
            }

        };

        private static final VersionedEntityDbTable<Purchase> purchaseTable = new VersionedEntityDbTable<Purchase>(purchaseDbKeyFactory) {

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
                purchase.save(con);
            }

        };

        private static final DbKey.LongKeyFactory<Purchase> feedbackDbKeyFactory = new DbKey.LongKeyFactory<Purchase>("id") {

            @Override
            public DbKey newKey(Purchase purchase) {
                return newKey(purchase.getId());
            }

        };

        private static final VersionedValuesDbTable<Purchase, EncryptedData> feedbackTable = new VersionedValuesDbTable<Purchase, EncryptedData>(feedbackDbKeyFactory) {

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

        private static final DbKey.LongKeyFactory<Purchase> publicFeedbackDbKeyFactory = new DbKey.LongKeyFactory<Purchase>("id") {

            @Override
            public DbKey newKey(Purchase purchase) {
                return newKey(purchase.getId());
            }

        };

        private static final VersionedValuesDbTable<Purchase, String> publicFeedbackTable = new VersionedValuesDbTable<Purchase, String>(publicFeedbackDbKeyFactory) {

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

        private void save(Connection con) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO purchase (id, buyer_id, goods_id, seller_id, "
                    + "quantity, price, deadline, note, nonce, timestamp, pending, goods, goods_nonce, refund_note, "
                    + "refund_nonce, has_feedback_notes, has_public_feedbacks, discount, refund, height, latest) KEY (id, height) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
                int i = 0;
                pstmt.setLong(++i, this.getId());
                pstmt.setLong(++i, this.getBuyerId());
                pstmt.setLong(++i, this.getGoodsId());
                pstmt.setLong(++i, this.getSellerId());
                pstmt.setInt(++i, this.getQuantity());
                pstmt.setLong(++i, this.getPriceNQT());
                pstmt.setInt(++i, this.getDeliveryDeadlineTimestamp());
                setEncryptedData(pstmt, this.getNote(), ++i);
                ++i;
                pstmt.setInt(++i, this.getTimestamp());
                pstmt.setBoolean(++i, this.isPending());
                setEncryptedData(pstmt, this.getEncryptedGoods(), ++i);
                ++i;
                setEncryptedData(pstmt, this.getRefundNote(), ++i);
                ++i;
                pstmt.setBoolean(++i, this.getFeedbackNotes() != null && this.getFeedbackNotes().size() > 0);
                pstmt.setBoolean(++i, this.getPublicFeedback() != null && this.getPublicFeedback().size() > 0);
                pstmt.setLong(++i, this.getDiscountNQT());
                pstmt.setLong(++i, this.getRefundNQT());
                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
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
            feedbackNotes = feedbackTable.get(feedbackDbKeyFactory.newKey(id));
            return feedbackNotes;
        }

        private void addFeedbackNote(EncryptedData feedbackNote) {
            if (feedbackNotes == null) {
                feedbackNotes = new ArrayList<>();
            }
            feedbackNotes.add(feedbackNote);
            this.hasFeedbackNotes = true;
            purchaseTable.insert(this);
            feedbackTable.insert(this, feedbackNote);
		}

        public List<String> getPublicFeedback() {
            if (!hasPublicFeedbacks) {
                return null;
            }
            publicFeedbacks = publicFeedbackTable.get(publicFeedbackDbKeyFactory.newKey(id));
            return publicFeedbacks;
        }

        private void addPublicFeedback(String publicFeedback) {
            if (publicFeedbacks == null) {
                publicFeedbacks = new ArrayList<>();
            }
            publicFeedbacks.add(publicFeedback);
            this.hasPublicFeedbacks = true;
            purchaseTable.insert(this);
            publicFeedbackTable.insert(this, publicFeedback);
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
        return Goods.goodsTable.get(Goods.goodsDbKeyFactory.newKey(goodsId));
    }

    public static List<Goods> getAllGoods() {
        return Goods.goodsTable.getAll();
    }

    public static List<Goods> getGoodsInStock() {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM goods WHERE "
                     + "latest = TRUE AND delisted = FALSE AND quantity > 0 "
                     + "ORDER BY timestamp DESC")) {
            return Goods.goodsTable.getManyBy(con, pstmt, true);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static List<Goods> getSellerGoods(Long sellerId, boolean inStockOnly) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM goods WHERE seller_id = ? "
                     + "AND latest = TRUE " + (inStockOnly ? "AND delisted = FALSE AND quantity > 0" : "")
                     + " ORDER BY name ASC, description ASC, id ASC")) {
            pstmt.setLong(1, sellerId);
            return Goods.goodsTable.getManyBy(con, pstmt, true);
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
            return Purchase.purchaseTable.getManyBy(con, pstmt, true);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static List<Purchase> getBuyerPurchases(Long buyerId) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM purchase WHERE buyer_id = ? "
                     + "AND latest = TRUE ORDER BY timestamp DESC, id ASC")) {
            pstmt.setLong(1, buyerId);
            return Purchase.purchaseTable.getManyBy(con, pstmt, true);
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
            return Purchase.purchaseTable.getManyBy(con, pstmt, true);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static Purchase getPurchase(Long purchaseId) {
        return Purchase.purchaseTable.get(Purchase.purchaseDbKeyFactory.newKey(purchaseId));
    }

    public static List<Purchase> getPendingSellerPurchases(Long sellerId) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM purchase WHERE seller_id = ? "
                     + "AND pending = TRUE AND latest = TRUE ORDER BY timestamp DESC, id ASC")) {
            pstmt.setLong(1, sellerId);
            return Purchase.purchaseTable.getManyBy(con, pstmt, true);
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
            return Purchase.purchaseTable.getManyBy(con, pstmt, true);
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
            return Purchase.purchaseTable.getManyBy(con, pstmt, true);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    private static void addPurchase(Transaction transaction,  Attachment.DigitalGoodsPurchase attachment, Long sellerId) {
        Purchase purchase = new Purchase(transaction, attachment, sellerId);
        Purchase.purchaseTable.insert(purchase);
        purchaseListeners.notify(purchase, Event.PURCHASE);
    }

    static void listGoods(Transaction transaction, Attachment.DigitalGoodsListing attachment) {
        Goods goods = new Goods(transaction, attachment);
        Goods.goodsTable.insert(goods);
        goodsListeners.notify(goods, Event.GOODS_LISTED);
    }

    static void delistGoods(Long goodsId) {
        Goods goods = Goods.goodsTable.get(Goods.goodsDbKeyFactory.newKey(goodsId));
        if (! goods.isDelisted()) {
            goods.setDelisted(true);
            goodsListeners.notify(goods, Event.GOODS_DELISTED);
        } else {
            throw new IllegalStateException("Goods already delisted");
        }
    }

    static void changePrice(Long goodsId, long priceNQT) {
        Goods goods = Goods.goodsTable.get(Goods.goodsDbKeyFactory.newKey(goodsId));
        if (! goods.isDelisted()) {
            goods.changePrice(priceNQT);
            goodsListeners.notify(goods, Event.GOODS_PRICE_CHANGE);
        } else {
            throw new IllegalStateException("Can't change price of delisted goods");
        }
    }

    static void changeQuantity(Long goodsId, int deltaQuantity) {
        Goods goods = Goods.goodsTable.get(Goods.goodsDbKeyFactory.newKey(goodsId));
        if (! goods.isDelisted()) {
            goods.changeQuantity(deltaQuantity);
            goodsListeners.notify(goods, Event.GOODS_QUANTITY_CHANGE);
        } else {
            throw new IllegalStateException("Can't change quantity of delisted goods");
        }
    }

    static void purchase(Transaction transaction,  Attachment.DigitalGoodsPurchase attachment) {
        Goods goods = Goods.goodsTable.get(Goods.goodsDbKeyFactory.newKey(attachment.getGoodsId()));
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

    static void refund(Long sellerId, Long purchaseId, long refundNQT, Appendix.EncryptedMessage encryptedMessage) {
        Purchase purchase = Purchase.purchaseTable.get(Purchase.purchaseDbKeyFactory.newKey(purchaseId));
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

    static void feedback(Long purchaseId, Appendix.EncryptedMessage encryptedMessage, Appendix.Message message) {
        Purchase purchase = Purchase.purchaseTable.get(Purchase.purchaseDbKeyFactory.newKey(purchaseId));
        if (encryptedMessage != null) {
            purchase.addFeedbackNote(encryptedMessage.getEncryptedData());
        }
        if (message != null) {
            purchase.addPublicFeedback(Convert.toString(message.getMessage()));
        }
        purchaseListeners.notify(purchase, Event.FEEDBACK);
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
