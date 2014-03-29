package nxt;

import nxt.crypto.XoredData;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class DigitalGoodsStore {

    private static final class Goods {
        private final String name;
        private final String description;
        private final String tags;
        private int quantity;
        private long price;

        private boolean delisted;

        public Goods(String name, String description, String tags, int quantity, long price) {
            this.name = name;
            this.description = description;
            this.tags = tags;
            this.quantity = quantity;
            this.price = price;
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

        public void changeQuantity(int deltaQuantity) {
            quantity += deltaQuantity;
            if (quantity < 0) {
                quantity = 0;
            } else if (quantity > Constants.MAX_DIGITAL_GOODS_QUANTITY) {
                quantity = Constants.MAX_DIGITAL_GOODS_QUANTITY;
            }
        }

        public long getPrice() {
            return price;
        }

        public void changePrice(long price) {
            this.price = price;
        }

        public void delist() {
            delisted = true;
        }

        public boolean isDelisted() {
            return delisted;
        }
    }

    private static final class Purchase {
        private final Long accountId;
        private final Long goodsId;
        private final int quantity;
        private final long price;
        private final int deliveryDeadline;
        private final XoredData note;

        public Purchase(Long accountId, Long goodsId, int quantity, long price, int deliveryDeadline, XoredData note) {
            this.accountId = accountId;
            this.goodsId = goodsId;
            this.quantity = quantity;
            this.price = price;
            this.deliveryDeadline = deliveryDeadline;
            this.note = note;
        }

        public Long getAccountId() {
            return accountId;
        }

        public Long getGoodsId() {
            return goodsId;
        }

        public int getQuantity() {
            return quantity;
        }

        public long getPrice() {
            return price;
        }

        public int getDeliveryDeadline() {
            return deliveryDeadline;
        }

        public XoredData getNote() {
            return note;
        }
    }

    private static final ConcurrentMap<Long, Goods> goods = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Long, Purchase> purchases = new ConcurrentHashMap<>();
    private static final Collection<Goods> allGoods = Collections.unmodifiableCollection(goods.values());
    private static final Collection<Purchase> allPurchases = Collections.unmodifiableCollection(purchases.values());
    private static final ConcurrentMap<Long, Purchase> pendingPurchases = new ConcurrentHashMap<>();

    public static Collection<Goods> getAllGoods() {
        return allGoods;
    }

    public static Collection<Purchase> getAllPurchases() {
        return allPurchases;
    }

    public static Goods getGoods(Long goodsId) {
        return goods.get(goodsId);
    }

    public static Purchase getPurchase(Long purchaseId) {
        return purchases.get(purchaseId);
    }

    public static Purchase getPendingPurchase(Long purchaseId) {
        return pendingPurchases.get(purchaseId);
    }

    public static void addGoods(Long goodsId, String name, String description, String tags, int quantity, long price) {
        goods.put(goodsId, new Goods(name, description, tags, quantity, price));
    }

    public static void addPurchase(Long purchaseId, Long accountId, Long goodsId, int quantity, long price, int deliveryDeadline, XoredData note) {
        Purchase purchase = new Purchase(accountId, goodsId, quantity, price, deliveryDeadline, note);
        purchases.put(purchaseId, purchase);
        pendingPurchases.put(purchaseId, purchase);
    }

    public static void clear() {
        goods.clear();
        purchases.clear();
    }

    public static void listGoods(Long goodsId, String name, String description, String tags, int quantity, long price) {
        goods.put(goodsId, new Goods(name, description, tags, quantity, price));
    }

    public static void delistGoods(Long goodsId) {
        Goods goods = getGoods(goodsId);
        if (goods != null && !goods.isDelisted()) {
            goods.delist();
        }
    }

    public static void changePrice(Long goodsId, long price) {
        Goods goods = getGoods(goodsId);
        if (goods != null && !goods.isDelisted()) {
            goods.changePrice(price);
        }
    }

    public static void changeQuantity(Long goodsId, int deltaQuantity) {
        Goods goods = getGoods(goodsId);
        if (goods != null && !goods.isDelisted()) {
            goods.changeQuantity(deltaQuantity);
        }
    }

    public static void purchase(Long purchaseId, Long accountId, Long goodsId, int quantity, long price, int deliveryDeadline, XoredData note) {
        Goods goods = getGoods(goodsId);
        if (goods != null && !goods.isDelisted() && quantity <= goods.getQuantity() && price == goods.getPrice() && deliveryDeadline > Nxt.getBlockchain().getLastBlock().getHeight()) {
            Account account = Account.getAccount(accountId);
            if (account.addToLockedBalance(quantity * price)) {
                goods.changeQuantity(-quantity);
                addPurchase(purchaseId, accountId, goodsId, quantity, price, deliveryDeadline, note);
            }
        }
    }

    public static void deliver(Long accountId, Long purchaseId, XoredData goods, long discount) {
        Purchase purchase = getPendingPurchase(purchaseId);
        if (purchase != null) {
            if (Account.getAccount(purchase.getAccountId()).transferLockedBalance(purchase.getQuantity() * purchase.getPrice(), accountId, discount)) {
                pendingPurchases.remove(purchaseId);
            }
        }
    }

    public static void refund(Long purchaseId, long refund, XoredData note) {

    }

    public static void reviewAllPendingPurchases() {
        int height = Nxt.getBlockchain().getLastBlock().getHeight();
        for (Map.Entry<Long, Purchase> pendingPurchaseEntry : pendingPurchases.entrySet()) {
            Purchase purchase = pendingPurchaseEntry.getValue();
            if (height > purchase.getDeliveryDeadline()) {
                Account.getAccount(purchase.getAccountId()).addToLockedBalance(-purchase.getQuantity() * purchase.getPrice());
                getGoods(purchase.getGoodsId()).changeQuantity(purchase.getQuantity());
                pendingPurchases.remove(pendingPurchaseEntry.getKey());
            }
        }
    }

}
