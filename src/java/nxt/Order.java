package nxt;

import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class Order {

    private static final SortedSet emptySortedSet = Collections.unmodifiableSortedSet(new TreeSet<>());

    static void clear() {
        Ask.askOrders.clear();
        Ask.sortedAskOrders.clear();
        Bid.bidOrders.clear();
        Bid.sortedBidOrders.clear();
    }

    // called only from Blockchain.apply(Block) which is already synchronized on Blockchain.class
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

            int quantity = ((Order)askOrder).quantity < ((Order)bidOrder).quantity ? ((Order)askOrder).quantity : ((Order)bidOrder).quantity;
            long price = askOrder.getHeight() < bidOrder.getHeight() || (askOrder.getHeight() == bidOrder.getHeight() && askOrder.getId() < bidOrder.getId()) ? askOrder.getPrice() : bidOrder.getPrice();

            if ((((Order)askOrder).quantity -= quantity) == 0) {

                Ask.removeOrder(askOrder.getId());

            }

            askOrder.getAccount().addToBalanceAndUnconfirmedBalance(quantity * price);

            if ((((Order)bidOrder).quantity -= quantity) == 0) {

                Bid.removeOrder(bidOrder.getId());

            }

            bidOrder.getAccount().addToAssetAndUnconfirmedAssetBalance(assetId, quantity);

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
        this.height = Blockchain.getLastBlock().getHeight();
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

        public static ConcurrentMap<Long, SortedSet<Ask>> getSortedAskOrders() {
            return sortedAskOrders;
        }

        public static Collection<Ask> getAllAskOrders() {
            return allAskOrders;
        }

        public static Ask getAskOrder(Long orderId) {
            return askOrders.get(orderId);
        }

        public static SortedSet<Ask> getSortedOrders(Long assetId) {
            SortedSet<Ask> sortedOrders = sortedAskOrders.get(assetId);
            return sortedOrders == null ? emptySortedSet : Collections.unmodifiableSortedSet(sortedOrders);
        }

        static void addOrder(Long transactionId, Account senderAccount, Long assetId, int quantity, long price) {
            senderAccount.addToAssetAndUnconfirmedAssetBalance(assetId, -quantity);
            Ask order = new Ask(transactionId, senderAccount, assetId, quantity, price);
            askOrders.put(order.getId(), order);
            sortedAskOrders.get(assetId).add(order);
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

        @Override
        public boolean equals(Object o) {
            return o instanceof Ask && this.getId().equals(((Ask)o).getId());
        }

        @Override
        public int hashCode() {
            return getId().hashCode();
        }

    }

    public static final class Bid extends Order implements Comparable<Bid> {

        private static final ConcurrentMap<Long, Bid> bidOrders = new ConcurrentHashMap<>();
        private static final ConcurrentMap<Long, SortedSet<Bid>> sortedBidOrders = new ConcurrentHashMap<>();

        private static final Collection<Bid> allBidOrders = Collections.unmodifiableCollection(bidOrders.values());

        public static ConcurrentMap<Long, SortedSet<Bid>> getSortedBidOrders() {
            return sortedBidOrders;
        }

        public static Collection<Bid> getAllBidOrders() {
            return allBidOrders;
        }

        public static Bid getBidOrder(Long orderId) {
            return bidOrders.get(orderId);
        }

        public static SortedSet<Bid> getSortedOrders(Long assetId) {
            SortedSet<Bid> sortedOrders = sortedBidOrders.get(assetId);
            return sortedOrders == null ? emptySortedSet : Collections.unmodifiableSortedSet(sortedOrders);
        }

        static void addOrder(Long transactionId, Account senderAccount, Long assetId, int quantity, long price) {
            senderAccount.addToBalanceAndUnconfirmedBalance(-quantity * price);
            Bid order = new Bid(transactionId, senderAccount, assetId, quantity, price);
            bidOrders.put(order.getId(), order);
            sortedBidOrders.get(assetId).add(order);
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

        @Override
        public boolean equals(Object o) {
            return o instanceof Bid && this.getId().equals(((Bid)o).getId());
        }

        @Override
        public int hashCode() {
            return getId().hashCode();
        }

    }
}
