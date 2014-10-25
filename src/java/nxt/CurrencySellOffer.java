package nxt;

import nxt.db.DbClause;
import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.VersionedEntityDbTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class CurrencySellOffer extends CurrencyExchangeOffer {

    private static final DbKey.LongKeyFactory<CurrencySellOffer> sellOfferDbKeyFactory = new DbKey.LongKeyFactory<CurrencySellOffer>("id") {

        @Override
        public DbKey newKey(CurrencySellOffer sell) {
            return sell.dbKey;
        }

    };

    private static final VersionedEntityDbTable<CurrencySellOffer> sellOfferTable = new VersionedEntityDbTable<CurrencySellOffer>("sell_offer", sellOfferDbKeyFactory) {

        @Override
        protected CurrencySellOffer load(Connection con, ResultSet rs) throws SQLException {
            return new CurrencySellOffer(rs);
        }

        @Override
        protected void save(Connection con, CurrencySellOffer sell) throws SQLException {
            sell.save(con, table);
        }

    };

    public static int getCount() {
        return sellOfferTable.getCount();
    }

    public static CurrencyExchangeOffer getSellOffer(long id) {
        return sellOfferTable.get(sellOfferDbKeyFactory.newKey(id));
    }

    public static DbIterator<CurrencySellOffer> getAll(int from, int to) {
        return sellOfferTable.getAll(from, to);
    }

    //TODO: shouldn't the ordering be rate ASC? add index to the table?
    public static DbIterator<CurrencySellOffer> getCurrencyOffers(long currencyId) {
        return sellOfferTable.getManyBy(new DbClause.LongClause("currency_id", currencyId), 0, -1, " ORDER BY rate DESC, height ASC, id ASC ");
    }

    public static CurrencySellOffer getCurrencyOffer(final long currencyId, final long accountId) {
        DbClause dbClause = new DbClause(" currency_id = ? AND account_id = ? ") {
            @Override
            protected int set(PreparedStatement pstmt, int index) throws SQLException {
                pstmt.setLong(index++, currencyId);
                pstmt.setLong(index++, accountId);
                return index;
            }
        };
        return sellOfferTable.getBy(dbClause);
    }

    static void addOffer(Transaction transaction, Attachment.MonetarySystemPublishExchangeOffer attachment) {
        sellOfferTable.insert(new CurrencySellOffer(transaction, attachment));
    }

    static void remove(CurrencySellOffer sellOffer) {
        sellOfferTable.delete(sellOffer);
    }

    static void init() {}

    protected final DbKey dbKey;

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
    public CurrencyExchangeOffer getCounterOffer() {
        return CurrencyBuyOffer.getBuyOffer(id);
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
