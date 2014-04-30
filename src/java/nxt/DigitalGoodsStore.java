package nxt;

import nxt.crypto.XoredData;
import nxt.util.Convert;
import nxt.util.Listener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class DigitalGoodsStore {

    static {
        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
                for (Map.Entry<Long, Purchase> pendingPurchaseEntry : pendingPurchases.entrySet()) {
                    Purchase purchase = pendingPurchaseEntry.getValue();
                    if (block.getTimestamp() > purchase.getDeliveryDeadlineTimestamp()) {
                        Account buyer = Account.getAccount(purchase.getBuyerId());
                        buyer.addToUnconfirmedBalanceNQT(Convert.safeMultiply(purchase.getQuantity(), purchase.getPriceNQT()));
                        getGoods(purchase.getGoodsId()).changeQuantity(purchase.getQuantity());
                        pendingPurchases.remove(pendingPurchaseEntry.getKey());
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
                        pendingPurchases.put(purchaseEntry.getKey(), purchase);
                    }
                }
            }
        }, BlockchainProcessor.Event.BLOCK_POPPED);
    }

    public static final class Goods {
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

    }

    public static final class Purchase {
        private final Long id;
        private final Long buyerId;
        private final Long goodsId;
        private final Long sellerId;
        private final int quantity;
        private final long priceNQT;
        private final int deliveryDeadlineTimestamp;
        private final XoredData note;
        private final int timestamp;

        private Purchase(Long id, Long buyerId, Long goodsId, Long sellerId, int quantity, long priceNQT,
                         int deliveryDeadlineTimestamp, XoredData note, int timestamp) {
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

        public XoredData getNote() {
            return note;
        }

        public boolean isPending() {
            return pendingPurchases.containsKey(id);
        }

        public int getTimestamp() {
            return timestamp;
        }

    }

    private static final ConcurrentMap<Long, Goods> goodsMap = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Long, Purchase> purchasesMap = new ConcurrentHashMap<>();
    private static final Collection<Goods> allGoods = Collections.unmodifiableCollection(goodsMap.values());
    private static final Collection<Purchase> allPurchases = Collections.unmodifiableCollection(purchasesMap.values());
    private static final ConcurrentMap<Long, Purchase> pendingPurchases = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Long, ConcurrentMap<Long, Goods>> sellerGoodsMap = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Long, ConcurrentMap<Long, Purchase>> sellerPurchasesMap = new ConcurrentHashMap<>();

    public static Collection<Goods> getAllGoods() {
        return allGoods;
    }

    public static Collection<Goods> getSellerGoods(Long sellerId) {
        Map<Long,Goods> map = sellerGoodsMap.get(sellerId);
        if (map == null || map.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableCollection(map.values());
    }

    public static Goods getGoods(Long goodsId) {
        return goodsMap.get(goodsId);
    }

    public static Collection<Purchase> getAllPurchases() {
        return allPurchases;
    }

    public static Collection<Purchase> getSellerPurchases(Long sellerId) {
        Map<Long,Purchase> map = sellerPurchasesMap.get(sellerId);
        if (map == null || map.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableCollection(map.values());
    }

    public static Collection<Purchase> getBuyerPurchases(Long buyerId) {
        List<Purchase> list = new ArrayList<>();
        for (Purchase purchase : allPurchases) {
            if (purchase.getBuyerId().equals(buyerId)) {
                list.add(purchase);
            }
        }
        return list;
    }

    public static Collection<Purchase> getSellerBuyerPurchases(Long sellerId, Long buyerId) {
        List<Purchase> list = new ArrayList<>();
        for (Purchase purchase : getSellerPurchases(sellerId)) {
            if (purchase.getBuyerId().equals(buyerId)) {
                list.add(purchase);
            }
        }
        return list;
    }

    public static Purchase getPurchase(Long purchaseId) {
        return purchasesMap.get(purchaseId);
    }

    public static Collection<Purchase> getPendingSellerPurchases(Long sellerId) {
        Map<Long,Purchase> map = sellerPurchasesMap.get(sellerId);
        if (map == null || map.isEmpty()) {
            return Collections.emptySet();
        }
        List<Purchase> list = new ArrayList<>();
        for (Map.Entry<Long,Purchase> entry : map.entrySet()) {
            if (pendingPurchases.containsKey(entry.getKey())) {
                list.add(entry.getValue());
            }
        }
        return list;
    }

    static Purchase getPendingPurchase(Long purchaseId) {
        return pendingPurchases.get(purchaseId);
    }

    private static void addPurchase(Long purchaseId, Long buyerId, Long goodsId, Long sellerId, int quantity, long priceNQT,
                                   int deliveryDeadlineTimestamp, XoredData note, int timestamp) {
        Purchase purchase = new Purchase(purchaseId, buyerId, goodsId, sellerId, quantity, priceNQT,
                deliveryDeadlineTimestamp, note, timestamp);
        purchasesMap.put(purchaseId, purchase);
        pendingPurchases.put(purchaseId, purchase);
        ConcurrentMap<Long, Purchase> map = sellerPurchasesMap.get(sellerId);
        if (map == null) {
            map = new ConcurrentHashMap<>();
            sellerPurchasesMap.put(sellerId, map);
        }
        map.put(purchaseId, purchase);
    }

    static void clear() {
        goodsMap.clear();
        purchasesMap.clear();
        sellerGoodsMap.clear();
        sellerPurchasesMap.clear();
    }

    static void listGoods(Long goodsId, Long sellerId, String name, String description, String tags,
                                 int quantity, long priceNQT) {
        Goods goods = new Goods(goodsId, sellerId, name, description, tags, quantity, priceNQT);
        goodsMap.put(goodsId, goods);
        ConcurrentMap<Long, Goods> map = sellerGoodsMap.get(sellerId);
        if (map == null) {
            map = new ConcurrentHashMap<>();
            sellerGoodsMap.put(sellerId, map);
        }
        map.put(goodsId, goods);
    }

    static void undoListGoods(Long goodsId) {
        Goods goods = goodsMap.remove(goodsId);
        Map<Long, Goods> map = sellerGoodsMap.get(goods.getSellerId());
        map.remove(goodsId);
        if (map.isEmpty()) {
            sellerGoodsMap.remove(goods.getSellerId());
        }
    }

    static void delistGoods(Long goodsId) {
        Goods goods = getGoods(goodsId);
        if (! goods.isDelisted()) {
            goods.setDelisted(true);
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
        } else {
            throw new IllegalStateException("Can't change price of delisted goods");
        }
    }

    static void changeQuantity(Long goodsId, int deltaQuantity) {
        Goods goods = getGoods(goodsId);
        if (! goods.isDelisted()) {
            goods.changeQuantity(deltaQuantity);
        } else {
            throw new IllegalStateException("Can't change quantity of delisted goods");
        }
    }

    static void purchase(Long purchaseId, Long buyerId, Long goodsId, int quantity, long priceNQT,
                                int deliveryDeadlineTimestamp, XoredData note, int timestamp) {
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
            pendingPurchases.remove(purchaseId);
            getGoods(purchase.getGoodsId()).changeQuantity(purchase.getQuantity());
            Map<Long, Purchase> map = sellerPurchasesMap.get(purchase.getSellerId());
            map.remove(purchaseId);
            if (map.isEmpty()) {
                sellerPurchasesMap.remove(purchase.getSellerId());
            }
        } else {
            Account buyer = Account.getAccount(buyerId);
            buyer.addToUnconfirmedBalanceNQT(-Convert.safeMultiply(quantity, priceNQT));
        }
    }

    static void deliver(Long sellerId, Long purchaseId, long discountNQT) {
        Purchase purchase = pendingPurchases.remove(purchaseId);
        if (purchase != null) {
            long totalWithoutDiscount = Convert.safeMultiply(purchase.getQuantity(), purchase.getPriceNQT());
            Account buyer = Account.getAccount(purchase.getBuyerId());
            buyer.addToBalanceNQT(Convert.safeSubtract(discountNQT, totalWithoutDiscount));
            buyer.addToUnconfirmedBalanceNQT(discountNQT);
            Account seller = Account.getAccount(sellerId);
            seller.addToBalanceAndUnconfirmedBalanceNQT(Convert.safeSubtract(totalWithoutDiscount, discountNQT));
        }
    }

    static void undoDeliver(Long sellerId, Long purchaseId, long discountNQT) {
        Purchase purchase = purchasesMap.get(purchaseId);
        if (purchase != null) {
            pendingPurchases.put(purchaseId, purchase);
            long totalWithoutDiscount = Convert.safeMultiply(purchase.getQuantity(), purchase.getPriceNQT());
            Account buyer = Account.getAccount(purchase.getBuyerId());
            buyer.addToBalanceNQT(Convert.safeSubtract(totalWithoutDiscount, discountNQT));
            buyer.addToUnconfirmedBalanceNQT(- discountNQT);
            Account seller = Account.getAccount(sellerId);
            seller.addToBalanceAndUnconfirmedBalanceNQT(Convert.safeSubtract(discountNQT, totalWithoutDiscount));
        }
    }

    static void refund(Long sellerId, Long purchaseId, long refundNQT) {
        Purchase purchase = getPurchase(purchaseId);
        Account seller = Account.getAccount(sellerId);
        seller.addToBalanceNQT(-refundNQT);
        Account buyer = Account.getAccount(purchase.getBuyerId());
        buyer.addToBalanceAndUnconfirmedBalanceNQT(refundNQT);
    }

    static void undoRefund(Long sellerId, Long purchaseId, long refundNQT) {
        Purchase purchase = getPurchase(purchaseId);
        Account seller = Account.getAccount(sellerId);
        seller.addToBalanceNQT(refundNQT);
        Account buyer = Account.getAccount(purchase.getBuyerId());
        buyer.addToBalanceAndUnconfirmedBalanceNQT(-refundNQT);
    }

}
