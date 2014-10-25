package nxt;

import nxt.db.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class CurrencySellOffer extends CurrencyOffer {

    private static final DbKey.LongKeyFactory<CurrencyOffer> sellOfferDbKeyFactory = new DbKey.LongKeyFactory<CurrencyOffer>("id") {

        @Override
        public DbKey newKey(CurrencyOffer sell) {
            return sell.dbKey;
        }

    };

    private static final VersionedEntityDbTable<CurrencyOffer> sellOfferTable = new VersionedEntityDbTable<CurrencyOffer>("sell_offer", sellOfferDbKeyFactory) {

        @Override
        protected CurrencySellOffer load(Connection con, ResultSet rs) throws SQLException {
            return new CurrencySellOffer(rs);
        }

        @Override
        protected void save(Connection con, CurrencyOffer sell) throws SQLException {
            sell.save(con, table);
        }

    };

    public static int getCount() {
        return sellOfferTable.getCount();
    }

    public static CurrencyOffer getSellOffer(long id) {
        return sellOfferTable.get(sellOfferDbKeyFactory.newKey(id));
    }

    public static DbIterator<CurrencyOffer> getAll(int from, int to) {
        return sellOfferTable.getAll(from, to);
    }

    static void init() {}

    private CurrencySellOffer(Transaction transaction, Attachment.MonetarySystemPublishExchangeOffer attachment) {
        super(transaction.getId(), attachment.getCurrencyId(), transaction.getSenderId(), attachment.getSellRateNQT(),
                attachment.getTotalSellLimit(), attachment.getInitialSellSupply(), attachment.getExpirationHeight(), transaction.getHeight());
        this.dbKey = sellOfferDbKeyFactory.newKey(id);
    }

    private CurrencySellOffer(ResultSet rs) throws SQLException {
        super(rs);
        this.dbKey = sellOfferDbKeyFactory.newKey(super.id);
    }

    protected void save(Connection con, String table) throws SQLException {
        super.save(con, table);
    }

    @Override
    public CurrencyOffer getCounterOffer() {
        return CurrencyBuyOffer.getBuyOffer(id);
    }

    static void addOffer(Transaction transaction, Attachment.MonetarySystemPublishExchangeOffer attachment) {
        sellOfferTable.insert(new CurrencySellOffer(transaction, attachment));
    }

    static void remove(CurrencyOffer sellOffer) {
        sellOfferTable.delete(sellOffer);
    }

    //TODO: shouldn't the ordering be rate ASC? add index to the table?
    public static DbIterator<CurrencyOffer> getCurrencyOffers(long currencyId) {
        return sellOfferTable.getManyBy(new DbClause.LongClause("currency_id", currencyId), 0, -1, " ORDER BY rate DESC, height ASC, id ASC ");
    }

    void increaseSupply(long delta) {
        super.increaseSupply(delta);
        sellOfferTable.insert(this);
    }

    void decreaseLimitAndSupply(long delta) {
        super.decreaseLimitAndSupply(delta);
        sellOfferTable.insert(this);
    }
}
