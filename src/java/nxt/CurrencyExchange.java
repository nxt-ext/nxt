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
        //TODO: use a custom sql query to get only expired offers
        try (DbIterator<CurrencyOffer> buyOffers = CurrencyBuy.getAll(0, CurrencyBuy.getCount() - 1)) {
            for (CurrencyOffer offer : buyOffers) {
                if (offer.getExpirationHeight() <= height) {
                    removeOffer(offer); // TODO: move out of the iterator loop
                }
            }
        }
    }

    //TODO: also enforce uniqueness of (currency_id, account_id, height) using a unique index
    static void publishOffer(Transaction transaction, Attachment.MonetarySystemPublishExchangeOffer attachment) {
        removeOffer(attachment.getCurrencyId(), transaction.getSenderId());
        CurrencyBuy.addOffer(transaction, attachment);
        CurrencySell.addOffer(transaction, attachment);
    }

    static void exchangeCurrencyForNXT(Account account, long currencyId, long rateNQT, long units) {
        long extraAmountNQT = 0;
        long remainingUnits = units;

        try (DbIterator<CurrencyOffer> currencyBuyOffers = CurrencyBuy.getCurrencyOffers(currencyId)) {
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
                counterAccount.addToCurrencyUnits(currencyId, curUnits);
                Exchange.addExchange(currencyId, Nxt.getBlockchain().getLastBlock(),
                        offer, account.getId(), offer.getAccountId(), curUnits);
            }
        }

        account.addToBalanceAndUnconfirmedBalanceNQT(extraAmountNQT);
        account.addToCurrencyUnits(currencyId, -(units - remainingUnits));
        account.addToUnconfirmedCurrencyUnits(currencyId, remainingUnits);
    }

    static void exchangeNXTForCurrency(Account account, long currencyId, long rateNQT, long units) {
        long extraUnits = 0;
        long remainingAmountNQT = Convert.safeMultiply(units, rateNQT);

        try (DbIterator<CurrencyOffer> currencySellOffers = CurrencySell.getCurrencyOffers(currencyId)) {
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
                counterAccount.addToCurrencyUnits(currencyId, -curUnits);
                Exchange.addExchange(currencyId, Nxt.getBlockchain().getLastBlock(),
                        offer, offer.getAccountId(), account.getId(), curUnits);
            }
        }

        account.addToCurrencyAndUnconfirmedCurrencyUnits(currencyId, extraUnits);
        account.addToBalanceNQT(-(Convert.safeMultiply(units, rateNQT) - remainingAmountNQT));
        account.addToUnconfirmedBalanceNQT(remainingAmountNQT);
    }

    private static void removeOffer(CurrencyOffer buyOffer) {
        CurrencyOffer sellOffer = buyOffer.getCounterOffer();

        CurrencyBuy.remove(buyOffer);
        CurrencySell.remove(sellOffer);

        Account account = Account.getAccount(buyOffer.getAccountId());
        account.addToUnconfirmedBalanceNQT(buyOffer.getSupply());
        account.addToUnconfirmedCurrencyUnits(buyOffer.getCurrencyId(), sellOffer.getSupply());
    }

    private static void removeOffer(long currencyId, long accountId) {
        //TODO: optimize by using both currencyId and accountId in a custom sql query
        try (DbIterator<CurrencyOffer> buyOffers = CurrencyBuy.getCurrencyOffers(currencyId)) {
            for (CurrencyOffer offer : buyOffers) {
                if (offer.getAccountId() == accountId) {
                    removeOffer(offer);
                    return;
                }
            }
        }
    }
}
