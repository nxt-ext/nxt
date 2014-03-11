package nxt;

import nxt.crypto.XoredData;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class DigitalGoodsStore {

    private static final class Goods {
        private final String name;
        private final String description;
        private final String tags;
        private int quantity;
        private long price;

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

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public long getPrice() {
            return price;
        }

        public void setPrice(long price) {
            this.price = price;
        }
    }

    private static final class Purchase {
        private final Long goodsId;
        private final int quantity;
        private final long price;
        private final int deliveryDeadline;
        private final XoredData note;

        public Purchase(Long goodsId, int quantity, long price, int deliveryDeadline, XoredData note) {
            this.goodsId = goodsId;
            this.quantity = quantity;
            this.price = price;
            this.deliveryDeadline = deliveryDeadline;
            this.note = note;
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

    public static void addGoods(Long goodsId, String name, String description, String tags, int quantity, long price) {
        goods.put(goodsId, new Goods(name, description, tags, quantity, price));
    }

    public static void addPurchase(Long purchaseId, Long goodsId, int quantity, long price, int deliveryDeadline, XoredData note) {
        purchases.put(purchaseId, new Purchase(goodsId, quantity, price, deliveryDeadline, note));
    }

    public static void clear() {
        goods.clear();
        purchases.clear();
    }

}
