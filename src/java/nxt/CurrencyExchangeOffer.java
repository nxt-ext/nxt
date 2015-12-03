/******************************************************************************
 * Copyright © 2013-2015 The Nxt Core Developers.                             *
 *                                                                            *
 * See the AUTHORS.txt, DEVELOPER-AGREEMENT.txt and LICENSE.txt files at      *
 * the top-level directory of this distribution for the individual copyright  *
 * holder information and the developer policies on copyright and licensing.  *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * Nxt software, including this file, may be copied, modified, propagated,    *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

package nxt;

import nxt.AccountLedger.LedgerEvent;
import nxt.db.DbClause;
import nxt.db.DbIterator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public abstract class CurrencyExchangeOffer {

    public static final class AvailableOffers {

        private final long currencyId;
        private final long rateNQT;
        private final long units;
        private final long amountNQT;

        private AvailableOffers(long currencyId, long rateNQT, long units, long amountNQT) {
            this.currencyId = currencyId;
            this.rateNQT = rateNQT;
            this.units = units;
            this.amountNQT = amountNQT;
        }

        public long getCurrencyId() {
            return currencyId;
        }

        public long getRateNQT() {
            return rateNQT;
        }

        public long getUnits() {
            return units;
        }

        public long getAmountNQT() {
            return amountNQT;
        }

    }

    static {

        Nxt.getBlockchainProcessor().addListener(block -> {
            if (block.getHeight() <= Constants.MONETARY_SYSTEM_BLOCK) {
                return;
            }
            List<CurrencyBuyOffer> expired = new ArrayList<>();
            try (DbIterator<CurrencyBuyOffer> offers = CurrencyBuyOffer.getOffers(new DbClause.IntClause("expiration_height", block.getHeight()), 0, -1)) {
                for (CurrencyBuyOffer offer : offers) {
                    expired.add(offer);
                }
            }
            expired.forEach((offer) -> CurrencyExchangeOffer.removeOffer(LedgerEvent.CURRENCY_OFFER_EXPIRED, offer));
        }, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);

    }

    static void publishOffer(Transaction transaction, Attachment.MonetarySystemPublishExchangeOffer attachment) {
        CurrencyBuyOffer previousOffer = CurrencyBuyOffer.getOffer(attachment.getCurrencyId(), transaction.getSenderId());
        if (previousOffer != null) {
            CurrencyExchangeOffer.removeOffer(LedgerEvent.CURRENCY_OFFER_REPLACED, previousOffer);
        }
        CurrencyBuyOffer.addOffer(transaction, attachment);
        CurrencySellOffer.addOffer(transaction, attachment);
    }

    private static final class ValidOffersDbClause extends DbClause {

        private final long currencyId;
        private final long rateNQT;

        private ValidOffersDbClause(long currencyId, long rateNQT, boolean rateDescending) {
            super(rateDescending ? " currency_id = ? AND unit_limit <> 0 AND supply <> 0 AND rate >= ? "
                            : " currency_id = ? AND unit_limit <> 0 AND supply <> 0 AND rate <= ? ");
            this.currencyId = currencyId;
            this.rateNQT = rateNQT;
        }

        @Override
        protected int set(PreparedStatement pstmt, int index) throws SQLException {
            pstmt.setLong(index++, currencyId);
            pstmt.setLong(index++, rateNQT);
            return index;
        }

    }

    static final DbClause availableOnlyDbClause = new DbClause.LongClause("unit_limit", DbClause.Op.NE, 0)
            .and(new DbClause.LongClause("supply", DbClause.Op.NE, 0));

    public static AvailableOffers getAvailableToSell(final long currencyId, final long units) {
        long totalAmountNQT = 0;
        long remainingUnits = units;
        long rateNQT = 0;
        List<CurrencyBuyOffer> currencyBuyOffers = new ArrayList<>();
        DbClause dbClause = new DbClause.LongClause("currency_id", currencyId).and(availableOnlyDbClause);
        try (DbIterator<CurrencyBuyOffer> offers = CurrencyBuyOffer.getOffers(dbClause, 0, -1,
                " ORDER BY rate DESC, creation_height ASC, transaction_height ASC, transaction_index ASC ")) {
            for (CurrencyBuyOffer offer : offers) {
                currencyBuyOffers.add(offer);
            }
        }
        for (CurrencyBuyOffer offer : currencyBuyOffers) {
            if (remainingUnits == 0) {
                break;
            }
            rateNQT = offer.getRateNQT();
            long curUnits = Math.min(Math.min(remainingUnits, offer.getSupply()), offer.getLimit());
            long curAmountNQT = Math.multiplyExact(curUnits, offer.getRateNQT());
            totalAmountNQT = Math.addExact(totalAmountNQT, curAmountNQT);
            remainingUnits = Math.subtractExact(remainingUnits, curUnits);
        }
        return new AvailableOffers(currencyId, rateNQT, Math.subtractExact(units, remainingUnits), totalAmountNQT);
    }

    static void exchangeCurrencyForNXT(Transaction transaction, Account account, final long currencyId, final long rateNQT, final long units) {
        long totalAmountNQT = 0;
        long remainingUnits = units;

        List<CurrencyBuyOffer> currencyBuyOffers = new ArrayList<>();
        try (DbIterator<CurrencyBuyOffer> offers = CurrencyBuyOffer.getOffers(new ValidOffersDbClause(currencyId, rateNQT, true), 0, -1,
                " ORDER BY rate DESC, creation_height ASC, transaction_height ASC, transaction_index ASC ")) {
            for (CurrencyBuyOffer offer : offers) {
                currencyBuyOffers.add(offer);
            }
        }

        for (CurrencyBuyOffer offer : currencyBuyOffers) {
            if (remainingUnits == 0) {
                break;
            }
            long curUnits = Math.min(Math.min(remainingUnits, offer.getSupply()), offer.getLimit());
            long curAmountNQT = Math.multiplyExact(curUnits, offer.getRateNQT());

            totalAmountNQT = Math.addExact(totalAmountNQT, curAmountNQT);
            remainingUnits = Math.subtractExact(remainingUnits, curUnits);

            offer.decreaseLimitAndSupply(curUnits);
            long excess = offer.getCounterOffer().increaseSupply(curUnits);

            Account counterAccount = Account.getAccount(offer.getAccountId());
            counterAccount.addToBalanceNQT(LedgerEvent.CURRENCY_EXCHANGE, offer.getId(), -curAmountNQT);
            counterAccount.addToCurrencyUnits(LedgerEvent.CURRENCY_EXCHANGE, offer.getId(), currencyId, curUnits);
            counterAccount.addToUnconfirmedCurrencyUnits(LedgerEvent.CURRENCY_EXCHANGE, offer.getId(), currencyId, excess);
            Exchange.addExchange(transaction, currencyId, offer, account.getId(), offer.getAccountId(), curUnits);
        }
        long transactionId = transaction.getId();
        account.addToBalanceAndUnconfirmedBalanceNQT(LedgerEvent.CURRENCY_EXCHANGE, transactionId, totalAmountNQT);
        account.addToCurrencyUnits(LedgerEvent.CURRENCY_EXCHANGE, transactionId, currencyId, -(units - remainingUnits));
        account.addToUnconfirmedCurrencyUnits(LedgerEvent.CURRENCY_EXCHANGE, transactionId, currencyId, remainingUnits);
    }

    public static AvailableOffers getAvailableToBuy(final long currencyId, final long amountNQT) {
        long totalUnits = 0;
        long remainingAmountNQT = amountNQT;
        long rateNQT = 0;
        List<CurrencySellOffer> currencySellOffers = new ArrayList<>();
        DbClause dbClause = new DbClause.LongClause("currency_id", currencyId).and(availableOnlyDbClause);
        try (DbIterator<CurrencySellOffer> offers = CurrencySellOffer.getOffers(dbClause, 0, -1,
                " ORDER BY rate ASC, creation_height ASC, transaction_height ASC, transaction_index ASC ")) {
            for (CurrencySellOffer offer : offers) {
                currencySellOffers.add(offer);
            }
        }
        for (CurrencySellOffer offer : currencySellOffers) {
            if (remainingAmountNQT == 0) {
                break;
            }
            long curUnits = Math.min(Math.min(remainingAmountNQT / offer.getRateNQT(), offer.getSupply()), offer.getLimit());
            if (curUnits == 0) {
                continue;
            }
            rateNQT = offer.getRateNQT();
            long curAmountNQT = Math.multiplyExact(curUnits, offer.getRateNQT());
            totalUnits = Math.addExact(totalUnits, curUnits);
            remainingAmountNQT = Math.subtractExact(remainingAmountNQT, curAmountNQT);
        }
        return new AvailableOffers(currencyId, rateNQT, totalUnits, Math.subtractExact(amountNQT, remainingAmountNQT));
    }

    static void exchangeNXTForCurrency(Transaction transaction, Account account, final long currencyId, final long rateNQT, final long units) {
        long totalUnits = 0;
        long remainingAmountNQT = Math.multiplyExact(units, rateNQT);

        List<CurrencySellOffer> currencySellOffers = new ArrayList<>();
        try (DbIterator<CurrencySellOffer> offers = CurrencySellOffer.getOffers(new ValidOffersDbClause(currencyId, rateNQT, false), 0, -1,
                " ORDER BY rate ASC, creation_height ASC, transaction_height ASC, transaction_index ASC ")) {
            for (CurrencySellOffer offer : offers) {
                currencySellOffers.add(offer);
            }
        }

        for (CurrencySellOffer offer : currencySellOffers) {
            if (remainingAmountNQT == 0) {
                break;
            }
            long curUnits = Math.min(Math.min(remainingAmountNQT / offer.getRateNQT(), offer.getSupply()), offer.getLimit());
            if (curUnits == 0) {
                continue;
            }
            long curAmountNQT = Math.multiplyExact(curUnits, offer.getRateNQT());

            totalUnits = Math.addExact(totalUnits, curUnits);
            remainingAmountNQT = Math.subtractExact(remainingAmountNQT, curAmountNQT);

            offer.decreaseLimitAndSupply(curUnits);
            long excess = offer.getCounterOffer().increaseSupply(curUnits);

            Account counterAccount = Account.getAccount(offer.getAccountId());
            counterAccount.addToBalanceNQT(LedgerEvent.CURRENCY_EXCHANGE, offer.getId(), curAmountNQT);
            counterAccount.addToUnconfirmedBalanceNQT(LedgerEvent.CURRENCY_EXCHANGE, offer.getId(),
                        Math.addExact(Math.multiplyExact(curUnits - excess, offer.getRateNQT() - offer.getCounterOffer().getRateNQT()), Math.multiplyExact(excess, offer.getRateNQT())));
            counterAccount.addToCurrencyUnits(LedgerEvent.CURRENCY_EXCHANGE, offer.getId(), currencyId, -curUnits);
            Exchange.addExchange(transaction, currencyId, offer, offer.getAccountId(), account.getId(), curUnits);
        }
        long transactionId = transaction.getId();
        account.addToCurrencyAndUnconfirmedCurrencyUnits(LedgerEvent.CURRENCY_EXCHANGE, transactionId,
                                                         currencyId, totalUnits);
        account.addToBalanceNQT(LedgerEvent.CURRENCY_EXCHANGE, transactionId,
                                -(Math.multiplyExact(units, rateNQT) - remainingAmountNQT));
        account.addToUnconfirmedBalanceNQT(LedgerEvent.CURRENCY_EXCHANGE, transactionId, remainingAmountNQT);
    }

    static void removeOffer(LedgerEvent event, CurrencyBuyOffer buyOffer) {
        CurrencySellOffer sellOffer = buyOffer.getCounterOffer();

        CurrencyBuyOffer.remove(buyOffer);
        CurrencySellOffer.remove(sellOffer);

        Account account = Account.getAccount(buyOffer.getAccountId());
        account.addToUnconfirmedBalanceNQT(event, buyOffer.getId(), Math.multiplyExact(buyOffer.getSupply(), buyOffer.getRateNQT()));
        account.addToUnconfirmedCurrencyUnits(event, buyOffer.getId(), buyOffer.getCurrencyId(), sellOffer.getSupply());
    }


    final long id;
    private final long currencyId;
    private final long accountId;
    private final long rateNQT;
    private long limit; // limit on the total sum of units for this offer across transactions
    private long supply; // total units supply for the offer
    private final int expirationHeight;
    private final int creationHeight;
    private final short transactionIndex;
    private final int transactionHeight;

    CurrencyExchangeOffer(long id, long currencyId, long accountId, long rateNQT, long limit, long supply,
                          int expirationHeight, int transactionHeight, short transactionIndex) {
        this.id = id;
        this.currencyId = currencyId;
        this.accountId = accountId;
        this.rateNQT = rateNQT;
        this.limit = limit;
        this.supply = supply;
        this.expirationHeight = expirationHeight;
        this.creationHeight = Nxt.getBlockchain().getHeight();
        this.transactionIndex = transactionIndex;
        this.transactionHeight = transactionHeight;
    }

    CurrencyExchangeOffer(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.currencyId = rs.getLong("currency_id");
        this.accountId = rs.getLong("account_id");
        this.rateNQT = rs.getLong("rate");
        this.limit = rs.getLong("unit_limit");
        this.supply = rs.getLong("supply");
        this.expirationHeight = rs.getInt("expiration_height");
        this.creationHeight = rs.getInt("creation_height");
        this.transactionIndex = rs.getShort("transaction_index");
        this.transactionHeight = rs.getInt("transaction_height");
    }

    void save(Connection con, String table) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO " + table + " (id, currency_id, account_id, "
                + "rate, unit_limit, supply, expiration_height, creation_height, transaction_index, transaction_height, height, latest) "
                + "KEY (id, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, this.id);
            pstmt.setLong(++i, this.currencyId);
            pstmt.setLong(++i, this.accountId);
            pstmt.setLong(++i, this.rateNQT);
            pstmt.setLong(++i, this.limit);
            pstmt.setLong(++i, this.supply);
            pstmt.setInt(++i, this.expirationHeight);
            pstmt.setInt(++i, this.creationHeight);
            pstmt.setShort(++i, this.transactionIndex);
            pstmt.setInt(++i, this.transactionHeight);
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }

    public long getId() {
        return id;
    }

    public long getCurrencyId() {
        return currencyId;
    }

    public long getAccountId() {
        return accountId;
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

    public int getHeight() {
        return creationHeight;
    }

    public abstract CurrencyExchangeOffer getCounterOffer();

    long increaseSupply(long delta) {
        long excess = Math.max(Math.addExact(supply, Math.subtractExact(delta, limit)), 0);
        supply += delta - excess;
        return excess;
    }

    void decreaseLimitAndSupply(long delta) {
        limit -= delta;
        supply -= delta;
    }
}
