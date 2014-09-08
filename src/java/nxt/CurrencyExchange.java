package nxt;

import nxt.util.Convert;
import nxt.util.Listener;

import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

public final class CurrencyExchange {

    static {
        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
                removeExpiredOffers(block.getHeight());
            }
        }, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);

    }

    public static final class BuyingOffer implements Comparable<BuyingOffer> {

        private final long rateNQT;
        private long limit;
        private long supply;
        private final int expirationHeight;
        private final Long accountId;
        private final int publicationHeight;
        private SellingOffer counterOffer;

        public BuyingOffer(long rateNQT, long limit, long supply, int expirationHeight, Long accountId, int publicationHeight) {
            this.rateNQT = rateNQT;
            this.limit = limit;
            this.supply = supply;
            this.expirationHeight = expirationHeight;
            this.accountId = accountId;
            this.publicationHeight = publicationHeight;
        }

        public long getRateNQT() {
            return rateNQT;
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

        public int getPublicationHeight() {
            return publicationHeight;
        }

        public SellingOffer getCounterOffer() {
            return counterOffer;
        }

        public void increaseSupply(long delta) {
            supply += delta;
        }

        public void decreaseLimitAndSupply(long delta) {
            limit -= delta;
            supply -= delta;
        }

        public void setCounterOffer(SellingOffer counterOffer) {
            this.counterOffer = counterOffer;
        }

        @Override
        public int compareTo(BuyingOffer offer) {
            if (rateNQT > offer.getRateNQT()) {
                return -1;
            } else if (rateNQT < offer.getRateNQT()) {
                return 1;
            } else {
                if (publicationHeight < offer.getPublicationHeight()) {
                    return -1;
                } else if (publicationHeight > offer.getPublicationHeight()) {
                    return 1;
                } else {
                    if (accountId < offer.getAccountId()) {
                        return -1;
                    } else if (accountId > offer.getAccountId()) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
            }
        }

    }

    public static final class SellingOffer implements Comparable<SellingOffer> {

        private final long rateNQT;
        private long limit;
        private long supply;
        private final int expirationHeight;
        private final Long accountId;
        private final int publicationHeight;
        private BuyingOffer counterOffer;

        public SellingOffer(long rateNQT, long limit, long supply, int expirationHeight, Long accountId, int publicationHeight) {
            this.rateNQT = rateNQT;
            this.limit = limit;
            this.supply = supply;
            this.expirationHeight = expirationHeight;
            this.accountId = accountId;
            this.publicationHeight = publicationHeight;
        }

        public long getRateNQT() {
            return rateNQT;
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

        public int getPublicationHeight() {
            return publicationHeight;
        }

        public BuyingOffer getCounterOffer() {
            return counterOffer;
        }

        public void increaseSupply(long delta) {
            supply += delta;
        }

        public void decreaseLimitAndSupply(long delta) {
            limit -= delta;
            supply -= delta;
        }

        public void setCounterOffer(BuyingOffer counterOffer) {
            this.counterOffer = counterOffer;
        }

        @Override
        public int compareTo(SellingOffer offer) {
            if (rateNQT < offer.getRateNQT()) {
                return -1;
            } else if (rateNQT > offer.getRateNQT()) {
                return 1;
            } else {
                if (publicationHeight < offer.getPublicationHeight()) {
                    return -1;
                } else if (publicationHeight > offer.getPublicationHeight()) {
                    return 1;
                } else {
                    if (accountId < offer.getAccountId()) {
                        return -1;
                    } else if (accountId > offer.getAccountId()) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
            }
        }

    }

    private static final ConcurrentMap<Long, SortedSet<BuyingOffer>> buyingOffers = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Long, SortedSet<SellingOffer>> sellingOffers = new ConcurrentHashMap<>();

    static void clear() {
        buyingOffers.clear();
        sellingOffers.clear();
    }

    static void removeExpiredOffers(int height) {
        for (Map.Entry<Long, SortedSet<BuyingOffer>> buyingOffersEntry : CurrencyExchange.buyingOffers.entrySet()) {
            for (BuyingOffer offer : buyingOffersEntry.getValue()) {
                if (offer.getExpirationHeight() <= height) {
                    removeOffer(buyingOffersEntry.getKey(), offer);
                }
            }
        }
    }

    static void publishOffer(Account account, Long currencyId, long buyingRateNQT, long sellingRateNQT, long totalBuyingLimit, long totalSellingLimit,
                             long initialBuyingSupply, long initialSellingSupply, int expirationHeight) {
        removeOffer(currencyId, account.getId());

        int publicationHeight = BlockchainImpl.getInstance().getHeight();
        BuyingOffer buyingOffer = new BuyingOffer(buyingRateNQT, totalBuyingLimit, initialBuyingSupply, expirationHeight, account.getId(), publicationHeight);
        SellingOffer sellingOffer = new SellingOffer(sellingRateNQT, totalSellingLimit, initialSellingSupply, expirationHeight, account.getId(), publicationHeight);
        buyingOffer.setCounterOffer(sellingOffer);
        sellingOffer.setCounterOffer(buyingOffer);

        SortedSet<BuyingOffer> currencyBuyingOffers = buyingOffers.get(currencyId);
        if (currencyBuyingOffers == null) {
            currencyBuyingOffers = new ConcurrentSkipListSet<>();
            buyingOffers.put(currencyId, currencyBuyingOffers);
        }
        currencyBuyingOffers.add(buyingOffer);

        SortedSet<SellingOffer> currencySellingOffers = sellingOffers.get(currencyId);
        if (currencySellingOffers == null) {
            currencySellingOffers = new ConcurrentSkipListSet<>();
            sellingOffers.put(currencyId, currencySellingOffers);
        }
        currencySellingOffers.add(sellingOffer);
    }

    static void exchangeMoneyForNXT(Account account, Long currencyId, long rateNQT, long units) {
        long extraAmountNQT = 0;
        long remainingUnits = units;

        SortedSet<BuyingOffer> currencyBuyingOffers = buyingOffers.get(currencyId);
        if (currencyBuyingOffers != null && !currencyBuyingOffers.isEmpty()) {
            for (BuyingOffer offer : currencyBuyingOffers) {
                if (offer.getRateNQT() < rateNQT) {
                    break;
                }

                if (offer.getLimit() == 0 || offer.getSupply() == 0) {
                    continue;
                }

                long curUnits = Math.min(Math.min(remainingUnits, offer.getSupply()), offer.getLimit());
                long curAmountNQT = Convert.safeMultiply(curUnits, offer.getRateNQT());

                extraAmountNQT = Convert.safeAdd(extraAmountNQT, curAmountNQT);
                remainingUnits = Convert.safeSubtract(remainingUnits, curUnits);

                offer.decreaseLimitAndSupply(curUnits);
                offer.getCounterOffer().increaseSupply(curUnits);

                Account counterAccount = Account.getAccount(offer.getAccountId());
                counterAccount.addToBalanceNQT(-curAmountNQT);
                counterAccount.addToCurrencyBalanceQNT(currencyId, curUnits);
            }
        }

        account.addToBalanceAndUnconfirmedBalanceNQT(extraAmountNQT);
        account.addToCurrencyBalanceQNT(currencyId, -(units - remainingUnits));
        account.addToUnconfirmedCurrencyBalanceQNT(currencyId, remainingUnits);
    }

    static void exchangeNXTForMoney(Account account, Long currencyId, long rateNQT, long units) {
        long extraUnits = 0;
        long remainingAmountNQT = Convert.safeMultiply(units, rateNQT);

        SortedSet<SellingOffer> currencySellingOffers = sellingOffers.get(currencyId);
        if (currencySellingOffers != null && !currencySellingOffers.isEmpty()) {
            for (SellingOffer offer : currencySellingOffers) {
                if (offer.getRateNQT() > rateNQT) {
                    break;
                }

                if (offer.getLimit() == 0 || offer.getSupply() == 0) {
                    continue;
                }

                long curUnits = Math.min(Math.min(remainingAmountNQT / offer.getRateNQT(), offer.getSupply()), offer.getLimit());
                long curAmountNQT = Convert.safeMultiply(curUnits, offer.getRateNQT());

                extraUnits = Convert.safeAdd(extraUnits, curUnits);
                remainingAmountNQT = Convert.safeSubtract(remainingAmountNQT, curAmountNQT);

                offer.decreaseLimitAndSupply(curUnits);
                offer.getCounterOffer().increaseSupply(curUnits);

                Account counterAccount = Account.getAccount(offer.getAccountId());
                counterAccount.addToBalanceNQT(curAmountNQT);
                counterAccount.addToCurrencyBalanceQNT(currencyId, -curUnits);
            }
        }

        account.addToCurrencyAndUnconfirmedCurrencyBalanceQNT(currencyId, extraUnits);
        account.addToBalanceNQT(-(Convert.safeMultiply(units, rateNQT) - remainingAmountNQT));
        account.addToUnconfirmedBalanceNQT(remainingAmountNQT);
    }

    static void removeOffer(Long currencyId, BuyingOffer buyingOffer) {
        SellingOffer sellingOffer = buyingOffer.getCounterOffer();

        buyingOffers.get(currencyId).remove(buyingOffer);
        sellingOffers.get(currencyId).remove(sellingOffer);

        Account account = Account.getAccount(buyingOffer.getAccountId());
        account.addToUnconfirmedBalanceNQT(buyingOffer.getSupply());
        account.addToUnconfirmedCurrencyBalanceQNT(currencyId, sellingOffer.getSupply());
    }

    static void removeOffer(Long currencyId, Long accountId) {
        if (buyingOffers.get(currencyId) == null) {
            return;
        }

        for (BuyingOffer offer : buyingOffers.get(currencyId)) {
            if (offer.getAccountId().equals(accountId)) {
                removeOffer(currencyId, offer);
                return;
            }
        }
    }
}
