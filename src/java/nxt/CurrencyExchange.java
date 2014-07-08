package nxt;

import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class CurrencyExchange {

    public static final class Offer implements Comparable<Offer> {

        private final long rate;
        private long limit;
        private long supply;
        private final int expirationHeight;
        private final Long accountId;
        private Offer counterOffer;

        public Offer(long rate, long limit, long supply, int expirationHeight, Long accountId) {
            this.rate = rate;
            this.limit = limit;
            this.supply = supply;
            this.expirationHeight = expirationHeight;
            this.accountId = accountId;
        }

        public long getRate() {
            return rate;
        }

        public long getLimit() {
            return limit;
        }

        public long getSupply() {
            return supply;
        }

        public int getExpirationHeight() {
            return expirationHeight;
        }

        public Long getAccountId() {
            return accountId;
        }

        public Offer getCounterOffer() {
            return counterOffer;
        }

        public void lowerLimit(long delta) {
            limit -= delta;
        }

        public void lowerSupply(long delta) {
            supply -= delta;
        }

        public void setCounterOffer(Offer counterOffer) {
            this.counterOffer = counterOffer;
        }

        @Override
        public int compareTo(Offer offer) {
            return 0;
        }

    }

    private static final ConcurrentMap<Long, SortedSet<Offer>> buyingOffers = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Long, SortedSet<Offer>> sellingOffers = new ConcurrentHashMap<>();

    static void clear() {
        buyingOffers.clear();
        sellingOffers.clear();
    }

    static void publicateExchangeOffer(Account account, Long currencyId, long buyingRateNQT, long sellingRateNQT, long totalBuyingLimitNQT, long totalSellingLimit, long initialNXTSupplyNQT, long initialCurrencySupply, int expirationHeight) {
        // TODO: Add implementation!
    }

    static void exchange(Account account, Long currencyId, long amountNQT, long units) {
        // TODO: Add implementation!
    }

}
