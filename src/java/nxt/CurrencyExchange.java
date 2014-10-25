package nxt;

import nxt.db.DbClause;
import nxt.db.DbIterator;
import nxt.util.Convert;
import nxt.util.Listener;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class CurrencyExchange {

    static {

        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {

            final DbClause dbClause = new DbClause(" expiration_height <= ? ") {
                @Override
                protected int set(PreparedStatement pstmt, int index) throws SQLException {
                    pstmt.setInt(index++, Nxt.getBlockchain().getHeight());
                    return index;
                }
            };

            @Override
            public void notify(Block block) {
                try (DbIterator<CurrencyOffer> expiredBuyOffers = CurrencyBuyOffer.getOffers(dbClause, 0, -1)) {
                    for (CurrencyOffer offer : expiredBuyOffers) {
                        removeOffer(offer); // TODO: move out of the iterator loop
                    }
                }
            }

        }, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);

    }

    static void publishOffer(Transaction transaction, Attachment.MonetarySystemPublishExchangeOffer attachment) {
        removeOffer(CurrencyBuyOffer.getCurrencyOffer(attachment.getCurrencyId(), transaction.getSenderId()));
        CurrencyBuyOffer.addOffer(transaction, attachment);
        CurrencySellOffer.addOffer(transaction, attachment);
    }

    static void exchangeCurrencyForNXT(Transaction transaction, Account account, long currencyId, long rateNQT, long units) {
        long extraAmountNQT = 0;
        long remainingUnits = units;

        //TODO: use a custom sql query to filter off offers with 0 limit or 0 supply
        try (DbIterator<CurrencyOffer> currencyBuyOffers = CurrencyBuyOffer.getCurrencyOffers(currencyId)) {
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
                Exchange.addExchange(transaction, currencyId, offer, account.getId(), offer.getAccountId(), curUnits);
            }
        }

        account.addToBalanceAndUnconfirmedBalanceNQT(extraAmountNQT);
        account.addToCurrencyUnits(currencyId, -(units - remainingUnits));
        account.addToUnconfirmedCurrencyUnits(currencyId, remainingUnits);
    }

    static void exchangeNXTForCurrency(Transaction transaction, Account account, long currencyId, long rateNQT, long units) {
        long extraUnits = 0;
        long remainingAmountNQT = Convert.safeMultiply(units, rateNQT);

        //TODO: use a custom sql query to filter off offers with 0 limit or 0 supply
        try (DbIterator<CurrencyOffer> currencySellOffers = CurrencySellOffer.getCurrencyOffers(currencyId)) {
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
                Exchange.addExchange(transaction, currencyId, offer, offer.getAccountId(), account.getId(), curUnits);
            }
        }

        account.addToCurrencyAndUnconfirmedCurrencyUnits(currencyId, extraUnits);
        account.addToBalanceNQT(-(Convert.safeMultiply(units, rateNQT) - remainingAmountNQT));
        account.addToUnconfirmedBalanceNQT(remainingAmountNQT);
    }

    private static void removeOffer(CurrencyOffer buyOffer) {
        CurrencyOffer sellOffer = buyOffer.getCounterOffer();

        CurrencyBuyOffer.remove(buyOffer);
        CurrencySellOffer.remove(sellOffer);

        Account account = Account.getAccount(buyOffer.getAccountId());
        account.addToUnconfirmedBalanceNQT(buyOffer.getSupply());
        account.addToUnconfirmedCurrencyUnits(buyOffer.getCurrencyId(), sellOffer.getSupply());
    }

}
