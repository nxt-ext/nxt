package nxt;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class Trade {

    private static final ConcurrentMap<Long, List<Trade>> trades = new ConcurrentHashMap<>();
    private static final Collection<List<Trade>> allTrades = Collections.unmodifiableCollection(trades.values());

    public static Collection<List<Trade>> getAllTrades() {
        return allTrades;
    }

    static void addTrade(Long assetId, int timeStamp, Long blockId, Long askOrderId, Long bidOrderId, int quantity, long price) {
        List<Trade> assetTrades = trades.get(assetId);
        if (assetTrades == null) {
            assetTrades = new CopyOnWriteArrayList<>();
            // cfb: CopyOnWriteArrayList requires a lot of resources to grow but this happens only when a new block is pushed/applied, I can't decide if we should replace it with another class
            trades.put(assetId, assetTrades);
        }
        assetTrades.add(new Trade(blockId, timeStamp, assetId, askOrderId, bidOrderId, quantity, price));
    }

    static void clear() {
        trades.clear();
    }

    private final int timeStamp;
    private final Long assetId;
    private final Long blockId;
    private final Long askOrderId, bidOrderId;
    private final int quantity;
    private final long price;

    private Trade(Long blockId, int timeStamp, Long assetId, Long askOrderId, Long bidOrderId, int quantity, long price) {

        this.blockId = blockId;
        this.assetId = assetId;
        this.timeStamp = timeStamp;
        this.askOrderId = askOrderId;
        this.bidOrderId = bidOrderId;
        this.quantity = quantity;
        this.price = price;

    }

    public Long getBlockId() { return blockId; }

    public Long getAskOrderId() { return askOrderId; }

    public Long getBidOrderId() { return bidOrderId; }

    public int getQuantity() { return quantity; }

    public long getPrice() { return price; }
    
    public long getAssetId() { return assetId; }
    
    public long getTimeStamp() { return timeStamp; }

    public static List<Trade> getTrades(Long assetId) {
        return Collections.unmodifiableList(trades.get(assetId));
    }

}
