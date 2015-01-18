package nxt;

import nxt.db.DbClause;
import nxt.db.DbIterator;
import nxt.util.Convert;
import nxt.util.Listener;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public abstract class CurrencyExchangeOffer {

    static {

        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {

            @Override
            public void notify(Block block) {
                if (block.getHeight() <= Constants.MONETARY_SYSTEM_BLOCK) {
                    return;
                }
                List<CurrencyBuyOffer> expired = new ArrayList<>();
                try (DbIterator<CurrencyBuyOffer> offers = CurrencyBuyOffer.getOffers(new DbClause.IntClause("expiration_height", block.getHeight()), 0, -1)) {
                    for (CurrencyBuyOffer offer : offers) {
                        expired.add(offer);
                    }
                }
                for (CurrencyBuyOffer offer : expired) {
                    removeOffer(offer);
                }
            }

        }, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);

    }

    static void publishOffer(Transaction transaction, Attachment.MonetarySystemPublishExchangeOffer attachment) {
        CurrencyBuyOffer previousOffer = CurrencyBuyOffer.getOffer(attachment.getCurrencyId(), transaction.getSenderId());
        if (previousOffer != null) {
            removeOffer(previousOffer);
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

    static final DbClause availableOnlyDbClause = new DbClause.FixedClause(" unit_limit <> 0 AND supply <> 0 ");

    static void exchangeCurrencyForNXT(Transaction transaction, Account account, final long currencyId, final long rateNQT, long units) {
        long extraAmountNQT = 0;
        long remainingUnits = units;

        List<CurrencyBuyOffer> currencyBuyOffers = new ArrayList<>();
        try (DbIterator<CurrencyBuyOffer> offers = CurrencyBuyOffer.getOffers(new ValidOffersDbClause(currencyId, rateNQT, true), 0, -1,
                " ORDER BY rate DESC, creation_height ASC, transaction_index ASC ")) {
            for (CurrencyBuyOffer offer : offers) {
                currencyBuyOffers.add(offer);
            }
        }

        for (CurrencyBuyOffer offer : currencyBuyOffers) {
            if (remainingUnits == 0) {
                break;
            }
            long curUnits = Math.min(Math.min(remainingUnits, offer.getSupply()), offer.getLimit());
            long curAmountNQT = Convert.safeMultiply(curUnits, offer.getRateNQT());

            extraAmountNQT = Convert.safeAdd(extraAmountNQT, curAmountNQT);
            remainingUnits = Convert.safeSubtract(remainingUnits, curUnits);

            offer.decreaseLimitAndSupply(curUnits);
            long excess = offer.getCounterOffer().increaseSupply(curUnits);

            Account counterAccount = Account.getAccount(offer.getAccountId());
            counterAccount.addToBalanceNQT(-curAmountNQT);
            counterAccount.addToCurrencyUnits(currencyId, curUnits);
            counterAccount.addToUnconfirmedCurrencyUnits(currencyId, excess);
            Exchange.addExchange(transaction, currencyId, offer, account.getId(), offer.getAccountId(), curUnits);
        }

        account.addToBalanceAndUnconfirmedBalanceNQT(extraAmountNQT);
        account.addToCurrencyUnits(currencyId, -(units - remainingUnits));
        account.addToUnconfirmedCurrencyUnits(currencyId, remainingUnits);
    }

    static void exchangeNXTForCurrency(Transaction transaction, Account account, final long currencyId, final long rateNQT, long units) {
        long extraUnits = 0;
        long remainingAmountNQT = Convert.safeMultiply(units, rateNQT);

        List<CurrencySellOffer> currencySellOffers = new ArrayList<>();
        try (DbIterator<CurrencySellOffer> offers = CurrencySellOffer.getOffers(new ValidOffersDbClause(currencyId, rateNQT, false), 0, -1,
                " ORDER BY rate ASC, creation_height ASC, transaction_index ASC ")) {
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
            long curAmountNQT = Convert.safeMultiply(curUnits, offer.getRateNQT());

            extraUnits = Convert.safeAdd(extraUnits, curUnits);
            remainingAmountNQT = Convert.safeSubtract(remainingAmountNQT, curAmountNQT);

            offer.decreaseLimitAndSupply(curUnits);
            long excess = offer.getCounterOffer().increaseSupply(curUnits);

            Account counterAccount = Account.getAccount(offer.getAccountId());
            counterAccount.addToBalanceNQT(curAmountNQT);
            counterAccount.addToUnconfirmedBalanceNQT(Convert.safeAdd(
                    Convert.safeMultiply(curUnits - excess, offer.getRateNQT() - offer.getCounterOffer().getRateNQT()),
                    Convert.safeMultiply(excess, offer.getRateNQT())));
            counterAccount.addToCurrencyUnits(currencyId, -curUnits);
            Exchange.addExchange(transaction, currencyId, offer, offer.getAccountId(), account.getId(), curUnits);
        }

        account.addToCurrencyAndUnconfirmedCurrencyUnits(currencyId, extraUnits);
        account.addToBalanceNQT(-(Convert.safeMultiply(units, rateNQT) - remainingAmountNQT));
        account.addToUnconfirmedBalanceNQT(remainingAmountNQT);
    }

    static void removeOffer(CurrencyBuyOffer buyOffer) {
        CurrencySellOffer sellOffer = buyOffer.getCounterOffer();

        CurrencyBuyOffer.remove(buyOffer);
        CurrencySellOffer.remove(sellOffer);

        Account account = Account.getAccount(buyOffer.getAccountId());
        account.addToUnconfirmedBalanceNQT(Convert.safeMultiply(buyOffer.getSupply(), buyOffer.getRateNQT()));
        account.addToUnconfirmedCurrencyUnits(buyOffer.getCurrencyId(), sellOffer.getSupply());
    }


    protected final long id;
    protected final long currencyId;
    protected final long accountId;
    protected final long rateNQT;
    protected long limit; // limit on the total sum of units for this offer across transactions
    protected long supply; // total units supply for the offer
    protected final int expirationHeight;
    protected final int creationHeight;
    protected final short transactionIndex;

    protected CurrencyExchangeOffer(long id, long currencyId, long accountId, long rateNQT, long limit, long supply,
                                    int expirationHeight, int creationHeight, short transactionIndex) {
        this.id = id;
        this.currencyId = currencyId;
        this.accountId = accountId;
        this.rateNQT = rateNQT;
        this.limit = limit;
        this.supply = supply;
        this.expirationHeight = expirationHeight;
        this.creationHeight = creationHeight;
        this.transactionIndex = transactionIndex;
    }

    protected CurrencyExchangeOffer(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.currencyId = rs.getLong("currency_id");
        this.accountId = rs.getLong("account_id");
        this.rateNQT = rs.getLong("rate");
        this.limit = rs.getLong("unit_limit");
        this.supply = rs.getLong("supply");
        this.expirationHeight = rs.getInt("expiration_height");
        this.creationHeight = rs.getInt("creation_height");
        this.transactionIndex = rs.getShort("transaction_index");
    }

    protected void save(Connection con, String table) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO " + table + " (id, currency_id, account_id, "
                + "rate, unit_limit, supply, expiration_height, creation_height, transaction_index, height, latest) "
                + "KEY (id, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, this.getId());
            pstmt.setLong(++i, this.getCurrencyId());
            pstmt.setLong(++i, this.getAccountId());
            pstmt.setLong(++i, this.getRateNQT());
            pstmt.setLong(++i, this.getLimit());
            pstmt.setLong(++i, this.getSupply());
            pstmt.setInt(++i, this.getExpirationHeight());
            pstmt.setInt(++i, this.getHeight());
            pstmt.setShort(++i, this.transactionIndex);
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
        long excess = Math.max(Convert.safeAdd(supply, Convert.safeSubtract(delta, limit)), 0);
        supply += delta - excess;
        return excess;
    }

    void decreaseLimitAndSupply(long delta) {
        limit -= delta;
        supply -= delta;
    }
}
