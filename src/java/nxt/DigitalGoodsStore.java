package nxt;

import nxt.crypto.EncryptedData;
import nxt.util.Convert;
import nxt.util.Listener;
import nxt.util.Listeners;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

public final class DigitalGoodsStore {

    public static enum Event {
        GOODS_LISTED, GOODS_DELISTED, GOODS_PRICE_CHANGE, GOODS_QUANTITY_CHANGE,
        PURCHASE, DELIVERY, REFUND, FEEDBACK
    }

    static {
        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
                for (Map.Entry<Long, Purchase> pendingPurchaseEntry : pendingPurchasesMap.entrySet()) {
                    Purchase purchase = pendingPurchaseEntry.getValue();
                    if (block.getTimestamp() > purchase.getDeliveryDeadlineTimestamp()) {
                        Account buyer = Account.getAccount(purchase.getBuyerId());
                        buyer.addToUnconfirmedBalanceNQT(Convert.safeMultiply(purchase.getQuantity(), purchase.getPriceNQT()));
                        getGoods(purchase.getGoodsId()).changeQuantity(purchase.getQuantity());
                        pendingPurchasesMap.remove(pendingPurchaseEntry.getKey());
                    }
                }
            }
        }, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);

        // reverse any pending purchase expiration that was caused by the block that got popped off
        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
                Block previousBlock = Nxt.getBlockchain().getLastBlock();
                for (Map.Entry<Long, Purchase> purchaseEntry : purchasesMap.entrySet()) {
                    Purchase purchase = purchaseEntry.getValue();
                    if (block.getTimestamp() > purchase.getDeliveryDeadlineTimestamp()
                            && previousBlock.getTimestamp() <= purchase.getDeliveryDeadlineTimestamp()) {
                        Account buyer = Account.getAccount(purchase.getBuyerId());
                        buyer.addToUnconfirmedBalanceNQT(- Convert.safeMultiply(purchase.getQuantity(), purchase.getPriceNQT()));
                        getGoods(purchase.getGoodsId()).changeQuantity(- purchase.getQuantity());
                        pendingPurchasesMap.put(purchaseEntry.getKey(), purchase);
                    }
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

    public static final class Goods implements Comparable<Goods> {
        private final Long id;
        private final Long sellerId;
        private final String name;
        private final String description;
        private final String tags;
        private volatile int quantity;
        private volatile long priceNQT;
        private volatile boolean delisted;

        private Goods(Long id, Long sellerId, String name, String description, String tags, int quantity, long priceNQT) {
            this.id = id;
            this.sellerId = sellerId;
            this.name = name;
            this.description = description;
            this.tags = tags;
            this.quantity = quantity;
            this.priceNQT = priceNQT;
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
        }

        public long getPriceNQT() {
            return priceNQT;
        }

        private void changePrice(long priceNQT) {
            this.priceNQT = priceNQT;
        }

        public boolean isDelisted() {
            return delisted;
        }

        private void setDelisted(boolean delisted) {
            this.delisted = delisted;
        }

        @Override
        public int compareTo(Goods other) {
            return name.compareTo(other.name);
        }

    }

    public static final class Purchase implements Comparable<Purchase> {
        private final Long id;
        private final Long buyerId;
        private final Long goodsId;
        private final Long sellerId;
        private final int quantity;
        private final long priceNQT;
        private final int deliveryDeadlineTimestamp;
        private final EncryptedData note;
        private final int timestamp;
        private volatile EncryptedData encryptedGoods;
        private volatile EncryptedData refundNote;
        private volatile EncryptedData feedbackNote;
        private volatile long discountNQT;
        private volatile long refundNQT;

        private Purchase(Long id, Long buyerId, Long goodsId, Long sellerId, int quantity, long priceNQT,
                         int deliveryDeadlineTimestamp, EncryptedData note, int timestamp) {
            this.id = id;
            this.buyerId = buyerId;
            this.goodsId = goodsId;
            this.sellerId = sellerId;
            this.quantity = quantity;
            this.priceNQT = priceNQT;
            this.deliveryDeadlineTimestamp = deliveryDeadlineTimestamp;
            this.note = note;
            this.timestamp = timestamp;
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
            return deliveryDeadlineTimestamp;
        }

        public EncryptedData getNote() {
            return note;
        }

        public boolean isPending() {
            return pendingPurchasesMap.containsKey(id);
        }

        public int getTimestamp() {
            return timestamp;
        }

        public EncryptedData getEncryptedGoods() {
            return encryptedGoods;
        }

        private void setEncryptedGoods(EncryptedData encryptedGoods) {
            this.encryptedGoods = encryptedGoods;
        }

        public EncryptedData getRefundNote() {
            return refundNote;
        }

        private void setRefundNote(EncryptedData refundNote) {
            this.refundNote = refundNote;
        }

        public EncryptedData getFeedbackNote() {
            return feedbackNote;
        }

        private void setFeedbackNote(EncryptedData feedbackNote) {
            this.feedbackNote = feedbackNote;
        }

        public long getDiscountNQT() {
            return discountNQT;
        }

        public void setDiscountNQT(long discountNQT) {
            this.discountNQT = discountNQT;
        }

        public long getRefundNQT() {
            return refundNQT;
        }

        public void setRefundNQT(long refundNQT) {
            this.refundNQT = refundNQT;
        }

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

    }

    private static final Map<Long, Goods> goodsMap = Collections.synchronizedMap(new LinkedHashMap<Long, Goods>());
    private static final ConcurrentMap<Long, Purchase> purchasesMap = new ConcurrentHashMap<>();
    private static final Collection<Goods> allGoods = Collections.unmodifiableCollection(goodsMap.values());
    private static final Collection<Purchase> allPurchases = Collections.unmodifiableCollection(purchasesMap.values());
    private static final ConcurrentMap<Long, Purchase> pendingPurchasesMap = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Long, SortedSet<Goods>> sellerGoodsMap = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Long, SortedSet<Purchase>> sellerPurchasesMap = new ConcurrentHashMap<>();

    private static final SortedSet emptySortedSet = Collections.unmodifiableSortedSet(new TreeSet());

    public static Collection<Goods> getAllGoods() {
        return allGoods;
    }

    public static SortedSet<Goods> getSellerGoods(Long sellerId) {
        SortedSet<Goods> set = sellerGoodsMap.get(sellerId);
        if (set == null || set.isEmpty()) {
            return emptySortedSet;
        }
        return Collections.unmodifiableSortedSet(set);
    }

    public static Goods getGoods(Long goodsId) {
        return goodsMap.get(goodsId);
    }

    public static Collection<Purchase> getAllPurchases() {
        return allPurchases;
    }

    public static SortedSet<Purchase> getSellerPurchases(Long sellerId) {
        SortedSet<Purchase> set = sellerPurchasesMap.get(sellerId);
        if (set == null || set.isEmpty()) {
            return emptySortedSet;
        }
        return Collections.unmodifiableSortedSet(set);
    }

    public static SortedSet<Purchase> getBuyerPurchases(Long buyerId) {
        SortedSet<Purchase> set = new TreeSet<>();
        for (Purchase purchase : allPurchases) {
            if (purchase.getBuyerId().equals(buyerId)) {
                set.add(purchase);
            }
        }
        return set;
    }

    public static SortedSet<Purchase> getSellerBuyerPurchases(Long sellerId, Long buyerId) {
        SortedSet<Purchase> set = new TreeSet<>();
        for (Purchase purchase : getSellerPurchases(sellerId)) {
            if (purchase.getBuyerId().equals(buyerId)) {
                set.add(purchase);
            }
        }
        return set;
    }

    public static Purchase getPurchase(Long purchaseId) {
        return purchasesMap.get(purchaseId);
    }

    public static SortedSet<Purchase> getPendingSellerPurchases(Long sellerId) {
        SortedSet<Purchase> set = sellerPurchasesMap.get(sellerId);
        if (set == null || set.isEmpty()) {
            return emptySortedSet;
        }
        SortedSet<Purchase> result = new TreeSet<>();
        for (Purchase purchase : set) {
            if (pendingPurchasesMap.containsKey(purchase.getId())) {
                result.add(purchase);
            }
        }
        return result;
    }

    static Purchase getPendingPurchase(Long purchaseId) {
        return pendingPurchasesMap.get(purchaseId);
    }

    private static void addPurchase(Long purchaseId, Long buyerId, Long goodsId, Long sellerId, int quantity, long priceNQT,
                                   int deliveryDeadlineTimestamp, EncryptedData note, int timestamp) {
        Purchase purchase = new Purchase(purchaseId, buyerId, goodsId, sellerId, quantity, priceNQT,
                deliveryDeadlineTimestamp, note, timestamp);
        purchasesMap.put(purchaseId, purchase);
        pendingPurchasesMap.put(purchaseId, purchase);
        SortedSet<Purchase> set = sellerPurchasesMap.get(sellerId);
        if (set == null) {
            set = new ConcurrentSkipListSet<>();
            sellerPurchasesMap.put(sellerId, set);
        }
        set.add(purchase);
        purchaseListeners.notify(purchase, Event.PURCHASE);
    }

    static void clear() {
        goodsMap.clear();
        purchasesMap.clear();
        pendingPurchasesMap.clear();
        sellerGoodsMap.clear();
        sellerPurchasesMap.clear();
    }

    static void listGoods(Long goodsId, Long sellerId, String name, String description, String tags,
                                 int quantity, long priceNQT) {
        Goods goods = new Goods(goodsId, sellerId, name, description, tags, quantity, priceNQT);
        goodsMap.put(goodsId, goods);
        SortedSet<Goods> set = sellerGoodsMap.get(sellerId);
        if (set == null) {
            set = new ConcurrentSkipListSet<>();
            sellerGoodsMap.put(sellerId, set);
        }
        set.add(goods);
        goodsListeners.notify(goods, Event.GOODS_LISTED);
    }

    static void undoListGoods(Long goodsId) {
        Goods goods = goodsMap.remove(goodsId);
        SortedSet<Goods> set = sellerGoodsMap.get(goods.getSellerId());
        set.remove(goods);
        if (set.isEmpty()) {
            sellerGoodsMap.remove(goods.getSellerId());
        }
    }

    static void delistGoods(Long goodsId) {
        Goods goods = getGoods(goodsId);
        if (! goods.isDelisted()) {
            goods.setDelisted(true);
            goodsListeners.notify(goods, Event.GOODS_DELISTED);
        } else {
            throw new IllegalStateException("Goods already delisted");
        }
    }

    static void undoDelistGoods(Long goodsId) {
        Goods goods = getGoods(goodsId);
        if (goods.isDelisted()) {
            goods.setDelisted(false);
        } else {
            throw new IllegalStateException("Goods were not delisted");
        }
    }

    static void changePrice(Long goodsId, long priceNQT) {
        Goods goods = getGoods(goodsId);
        if (! goods.isDelisted()) {
            goods.changePrice(priceNQT);
            goodsListeners.notify(goods, Event.GOODS_PRICE_CHANGE);
        } else {
            throw new IllegalStateException("Can't change price of delisted goods");
        }
    }

    static void changeQuantity(Long goodsId, int deltaQuantity) {
        Goods goods = getGoods(goodsId);
        if (! goods.isDelisted()) {
            goods.changeQuantity(deltaQuantity);
            goodsListeners.notify(goods, Event.GOODS_QUANTITY_CHANGE);
        } else {
            throw new IllegalStateException("Can't change quantity of delisted goods");
        }
    }

    static void purchase(Long purchaseId, Long buyerId, Long goodsId, int quantity, long priceNQT,
                                int deliveryDeadlineTimestamp, EncryptedData note, int timestamp) {
        Goods goods = getGoods(goodsId);
        if (! goods.isDelisted() && quantity <= goods.getQuantity() && priceNQT == goods.getPriceNQT()
                && deliveryDeadlineTimestamp > Nxt.getBlockchain().getLastBlock().getHeight()) {
            goods.changeQuantity(-quantity);
            addPurchase(purchaseId, buyerId, goodsId, goods.getSellerId(), quantity, priceNQT,
                    deliveryDeadlineTimestamp, note, timestamp);
        } else {
            Account buyer = Account.getAccount(buyerId);
            buyer.addToUnconfirmedBalanceNQT(Convert.safeMultiply(quantity, priceNQT));
            // restoring the unconfirmed balance if purchase not successful, however buyer still lost the transaction fees
        }
    }

    static void undoPurchase(Long purchaseId, Long buyerId, int quantity, long priceNQT) {
        Purchase purchase = purchasesMap.remove(purchaseId);
        if (purchase != null) {
            pendingPurchasesMap.remove(purchaseId);
            getGoods(purchase.getGoodsId()).changeQuantity(purchase.getQuantity());
            SortedSet<Purchase> set = sellerPurchasesMap.get(purchase.getSellerId());
            set.remove(purchase);
            if (set.isEmpty()) {
                sellerPurchasesMap.remove(purchase.getSellerId());
            }
        } else {
            Account buyer = Account.getAccount(buyerId);
            buyer.addToUnconfirmedBalanceNQT(-Convert.safeMultiply(quantity, priceNQT));
        }
    }

    static void deliver(Long sellerId, Long purchaseId, long discountNQT, EncryptedData encryptedGoods) {
        Purchase purchase = pendingPurchasesMap.remove(purchaseId);
        if (purchase != null) {
            long totalWithoutDiscount = Convert.safeMultiply(purchase.getQuantity(), purchase.getPriceNQT());
            Account buyer = Account.getAccount(purchase.getBuyerId());
            buyer.addToBalanceNQT(Convert.safeSubtract(discountNQT, totalWithoutDiscount));
            buyer.addToUnconfirmedBalanceNQT(discountNQT);
            Account seller = Account.getAccount(sellerId);
            seller.addToBalanceAndUnconfirmedBalanceNQT(Convert.safeSubtract(totalWithoutDiscount, discountNQT));
            purchase.setEncryptedGoods(encryptedGoods);
            purchase.setDiscountNQT(discountNQT);
            purchaseListeners.notify(purchase, Event.DELIVERY);
        }
    }

    static void undoDeliver(Long sellerId, Long purchaseId, long discountNQT) {
        Purchase purchase = purchasesMap.get(purchaseId);
        if (purchase != null) {
            pendingPurchasesMap.put(purchaseId, purchase);
            long totalWithoutDiscount = Convert.safeMultiply(purchase.getQuantity(), purchase.getPriceNQT());
            Account buyer = Account.getAccount(purchase.getBuyerId());
            buyer.addToBalanceNQT(Convert.safeSubtract(totalWithoutDiscount, discountNQT));
            buyer.addToUnconfirmedBalanceNQT(- discountNQT);
            Account seller = Account.getAccount(sellerId);
            seller.addToBalanceAndUnconfirmedBalanceNQT(Convert.safeSubtract(discountNQT, totalWithoutDiscount));
            purchase.setEncryptedGoods(null);
            purchase.setDiscountNQT(0);
        }
    }

    static void refund(Long sellerId, Long purchaseId, long refundNQT, EncryptedData encryptedNote) {
        Purchase purchase = purchasesMap.get(purchaseId);
        Account seller = Account.getAccount(sellerId);
        seller.addToBalanceNQT(-refundNQT);
        Account buyer = Account.getAccount(purchase.getBuyerId());
        buyer.addToBalanceAndUnconfirmedBalanceNQT(refundNQT);
        purchase.setRefundNote(encryptedNote);
        purchase.setRefundNQT(refundNQT);
        pendingPurchasesMap.remove(purchaseId);
        purchaseListeners.notify(purchase, Event.REFUND);
    }

    static void undoRefund(Long sellerId, Long purchaseId, long refundNQT) {
        Purchase purchase = purchasesMap.get(purchaseId);
        Account seller = Account.getAccount(sellerId);
        seller.addToBalanceNQT(refundNQT);
        Account buyer = Account.getAccount(purchase.getBuyerId());
        buyer.addToBalanceAndUnconfirmedBalanceNQT(-refundNQT);
        purchase.setRefundNote(null);
        purchase.setRefundNQT(0);
        if (purchase.encryptedGoods == null) {
            pendingPurchasesMap.put(purchaseId, purchase);
        }
    }

    static void feedback(Long purchaseId, EncryptedData encryptedNote) {
        Purchase purchase = purchasesMap.get(purchaseId);
        purchase.setFeedbackNote(encryptedNote);
        purchaseListeners.notify(purchase, Event.FEEDBACK);
    }

    static void undoFeedback(Long purchaseId) {
        Purchase purchase = purchasesMap.get(purchaseId);
        purchase.setFeedbackNote(null);
    }

}
