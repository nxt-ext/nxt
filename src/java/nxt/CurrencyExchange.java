package nxt;

import nxt.db.DbIterator;
import nxt.util.Convert;
import nxt.util.Listener;

public final class CurrencyExchange {

    static {
        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
                removeExpiredOffers(block.getHeight());
            }
        }, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);

    }

    private static void removeExpiredOffers(int height) {
        DbIterator<CurrencyOffer> buyOffers = CurrencyBuy.getAll(0, CurrencyBuy.getCount() - 1);
        for (CurrencyOffer offer : buyOffers) {
            if (offer.getExpirationHeight() <= height) {
                removeOffer(offer.getCurrencyId(), offer);
            }
        }
    }

    static void publishOffer(long id, Account account, long currencyId, long buyRateNQT, long sellRateNQT, long totalBuyLimit, long totalSellLimit,
                             long initialBuySupply, long initialSellSupply, int expirationHeight) {
        removeOffer(currencyId, account.getId());

        int height = BlockchainImpl.getInstance().getHeight();
        CurrencyBuy.addOffer(new CurrencyBuy(id, currencyId, account.getId(), buyRateNQT, totalBuyLimit, initialBuySupply,
                expirationHeight, height));
        CurrencySell.addOffer(new CurrencySell(id, currencyId, account.getId(), sellRateNQT, totalSellLimit, initialSellSupply,
                expirationHeight, height));
    }

    static void exchangeCurrencyForNXT(Account account, long currencyId, long rateNQT, long units) {
        long extraAmountNQT = 0;
        long remainingUnits = units;

        DbIterator<CurrencyOffer> currencyBuyOffers = CurrencyBuy.getCurrencyOffers(currencyId);
        if (currencyBuyOffers != null) {
            for (CurrencyOffer offer : currencyBuyOffers) {
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
                Exchange.addExchange(currencyId, Nxt.getBlockchain().getLastBlock(),
                        offer.getId(), account.getId(), offer.getAccountId(), curUnits, offer.getRateNQT());
            }
        }

        account.addToBalanceAndUnconfirmedBalanceNQT(extraAmountNQT);
        account.addToCurrencyBalanceQNT(currencyId, -(units - remainingUnits));
        account.addToUnconfirmedCurrencyBalanceQNT(currencyId, remainingUnits);
    }

    static void exchangeNXTForCurrency(Account account, long currencyId, long rateNQT, long units) {
        long extraUnits = 0;
        long remainingAmountNQT = Convert.safeMultiply(units, rateNQT);

        DbIterator<CurrencyOffer> currencySellOffers = CurrencySell.getCurrencyOffers(currencyId);
        if (currencySellOffers != null) {
            for (CurrencyOffer offer : currencySellOffers) {
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
                Exchange.addExchange(currencyId, Nxt.getBlockchain().getLastBlock(),
                        offer.getId(), offer.getAccountId(), account.getId(), curUnits, offer.getRateNQT());
            }
        }

        account.addToCurrencyAndUnconfirmedCurrencyBalanceQNT(currencyId, extraUnits);
        account.addToBalanceNQT(-(Convert.safeMultiply(units, rateNQT) - remainingAmountNQT));
        account.addToUnconfirmedBalanceNQT(remainingAmountNQT);
    }

    private static void removeOffer(long currencyId, CurrencyOffer buyOffer) {
        CurrencyOffer sellOffer = buyOffer.getCounterOffer();

        CurrencyBuy.remove(buyOffer);
        CurrencySell.remove(sellOffer);

        Account account = Account.getAccount(buyOffer.getAccountId());
        account.addToUnconfirmedBalanceNQT(buyOffer.getSupply());
        account.addToUnconfirmedCurrencyBalanceQNT(currencyId, sellOffer.getSupply());
    }

    private static void removeOffer(long currencyId, long accountId) {
        DbIterator<CurrencyOffer> buyOffers = CurrencyBuy.getCurrencyOffers(currencyId);
        if (buyOffers == null) {
            return;
        }

        for (CurrencyOffer offer : buyOffers) {
            if (offer.getAccountId() == accountId) {
                removeOffer(currencyId, offer);
                return;
            }
        }
    }
}
