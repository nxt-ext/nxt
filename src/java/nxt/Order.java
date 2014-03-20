package nxt;

import nxt.util.Convert;

import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

public abstract class Order {

    private static final SortedSet<? extends Order> emptySortedSet = Collections.unmodifiableSortedSet(new ConcurrentSkipListSet<Order>());

    static void clear() {
        Ask.askOrders.clear();
        Ask.sortedAskOrders.clear();
        Bid.bidOrders.clear();
        Bid.sortedBidOrders.clear();
    }

    private static void matchOrders(Long assetId) {

        SortedSet<Ask> sortedAssetAskOrders = Ask.sortedAskOrders.get(assetId);
        SortedSet<Bid> sortedAssetBidOrders = Bid.sortedBidOrders.get(assetId);

        if (sortedAssetAskOrders == null || sortedAssetBidOrders == null) {
            return;
        }

        while (!sortedAssetAskOrders.isEmpty() && !sortedAssetBidOrders.isEmpty()) {

            Ask askOrder = sortedAssetAskOrders.first();
            Bid bidOrder = sortedAssetBidOrders.first();

            if (askOrder.getPrice() > bidOrder.getPrice()) {
                break;
            }

            int quantity = Math.min(((Order)askOrder).quantity, ((Order)bidOrder).quantity);
            long price = askOrder.getHeight() < bidOrder.getHeight() || (askOrder.getHeight() == bidOrder.getHeight() && askOrder.getId() < bidOrder.getId()) ? askOrder.getPrice() : bidOrder.getPrice();

            Block lastBlock=Nxt.getBlockchain().getLastBlock();
            int timeStamp=lastBlock.getTimestamp();
            
            Trade.addTrade(assetId, timeStamp, lastBlock.getId(), askOrder.getId(), bidOrder.getId(), quantity, price);

            if ((((Order)askOrder).quantity -= quantity) == 0) {
                Ask.removeOrder(askOrder.getId());
            }
            askOrder.getAccount().addToBalanceAndUnconfirmedBalance(quantity * price);
            askOrder.getAccount().addToAssetBalance(assetId, -quantity);

            if ((((Order)bidOrder).quantity -= quantity) == 0) {
                Bid.removeOrder(bidOrder.getId());
            }
            bidOrder.getAccount().addToAssetAndUnconfirmedAssetBalance(assetId, quantity);
            bidOrder.getAccount().addToBalance(-quantity * price);
            bidOrder.getAccount().addToUnconfirmedBalance(quantity * (bidOrder.getPrice() - price));

        }

    }

    private final Long id;
    private final Account account;
    private final Long assetId;
    private final long price;
    private final long height;

    private volatile int quantity;

    private Order(Long id, Account account, Long assetId, int quantity, long price) {
        this.id = id;
        this.account = account;
        this.assetId = assetId;
        this.quantity = quantity;
        this.price = price;
        this.height = Nxt.getBlockchain().getLastBlock().getHeight();
    }

    public Long getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public Long getAssetId() {
        return assetId;
    }

    public long getPrice() {
        return price;
    }

    public final int getQuantity() {
        return quantity;
    }

    public long getHeight() {
        return height;
    }

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

    public static final class Ask extends Order implements Comparable<Ask> {

        private static final ConcurrentMap<Long, Ask> askOrders = new ConcurrentHashMap<>();
        private static final ConcurrentMap<Long, SortedSet<Ask>> sortedAskOrders = new ConcurrentHashMap<>();

        private static final Collection<Ask> allAskOrders = Collections.unmodifiableCollection(askOrders.values());

        public static Collection<Ask> getAllAskOrders() {
            return allAskOrders;
        }

        public static Ask getAskOrder(Long orderId) {
            return askOrders.get(orderId);
        }

        public static SortedSet<Ask> getSortedOrders(Long assetId) {
            SortedSet<Ask> sortedOrders = sortedAskOrders.get(assetId);
            return sortedOrders == null ? (SortedSet<Ask>)emptySortedSet : Collections.unmodifiableSortedSet(sortedOrders);
        }

        static void addOrder(Long transactionId, Account senderAccount, Long assetId, int quantity, long price) {
            Ask order = new Ask(transactionId, senderAccount, assetId, quantity, price);
            if (askOrders.putIfAbsent(order.getId(), order) != null) {
                throw new IllegalStateException("Ask order id " + Convert.toUnsignedLong(order.getId()) + " already exists");
            }
            SortedSet<Ask> sortedAssetAskOrders = sortedAskOrders.get(assetId);
            if (sortedAssetAskOrders == null) {
                sortedAssetAskOrders = new ConcurrentSkipListSet<>();
                sortedAskOrders.put(assetId,sortedAssetAskOrders);
            }
            sortedAssetAskOrders.add(order);
            matchOrders(assetId);
        }

        static Ask removeOrder(Long orderId) {
            Ask askOrder = askOrders.remove(orderId);
            if (askOrder != null) {
                sortedAskOrders.get(askOrder.getAssetId()).remove(askOrder);
            }
            return askOrder;
        }

        private Ask(Long orderId, Account account, Long assetId, int quantity, long price) {
            super(orderId, account, assetId, quantity, price);
        }

        @Override
        public int compareTo(Ask o) {
            if (this.getPrice() < o.getPrice()) {
                return -1;
            } else if (this.getPrice() > o.getPrice()) {
                return 1;
            } else {
                return super.compareTo(o);
            }
        }

    }

    public static final class Bid extends Order implements Comparable<Bid> {

        private static final ConcurrentMap<Long, Bid> bidOrders = new ConcurrentHashMap<>();
        private static final ConcurrentMap<Long, SortedSet<Bid>> sortedBidOrders = new ConcurrentHashMap<>();

        private static final Collection<Bid> allBidOrders = Collections.unmodifiableCollection(bidOrders.values());

        public static Collection<Bid> getAllBidOrders() {
            return allBidOrders;
        }

        public static Bid getBidOrder(Long orderId) {
            return bidOrders.get(orderId);
        }

        public static SortedSet<Bid> getSortedOrders(Long assetId) {
            SortedSet<Bid> sortedOrders = sortedBidOrders.get(assetId);
            return sortedOrders == null ? (SortedSet<Bid>)emptySortedSet : Collections.unmodifiableSortedSet(sortedOrders);
        }

        static void addOrder(Long transactionId, Account senderAccount, Long assetId, int quantity, long price) {
            Bid order = new Bid(transactionId, senderAccount, assetId, quantity, price);
            if (bidOrders.putIfAbsent(order.getId(), order) != null) {
                throw new IllegalStateException("Bid order id " + Convert.toUnsignedLong(order.getId()) + " already exists");
            }
            SortedSet<Bid> sortedAssetBidOrders = sortedBidOrders.get(assetId);
            if (sortedAssetBidOrders == null) {
                sortedAssetBidOrders = new ConcurrentSkipListSet<>();
                sortedBidOrders.put(assetId,sortedAssetBidOrders);
            }
            sortedAssetBidOrders.add(order);
            matchOrders(assetId);
        }

        static Bid removeOrder(Long orderId) {
            Bid bidOrder = bidOrders.remove(orderId);
            if (bidOrder != null) {
                sortedBidOrders.get(bidOrder.getAssetId()).remove(bidOrder);
            }
            return bidOrder;
        }

        private Bid(Long orderId, Account account, Long assetId, int quantity, long price) {
            super(orderId, account, assetId, quantity, price);
        }

        @Override
        public int compareTo(Bid o) {
            if (this.getPrice() > o.getPrice()) {
                return -1;
            } else if (this.getPrice() < o.getPrice()) {
                return 1;
            } else {
                return super.compareTo(o);
            }
        }

    }
}
