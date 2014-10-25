package nxt;

import nxt.db.DbClause;
import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.VersionedEntityDbTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class CurrencyBuyOffer extends CurrencyExchangeOffer {

    private static final DbKey.LongKeyFactory<CurrencyBuyOffer> buyOfferDbKeyFactory = new DbKey.LongKeyFactory<CurrencyBuyOffer>("id") {

        @Override
        public DbKey newKey(CurrencyBuyOffer offer) {
            return offer.dbKey;
        }

    };

    private static final VersionedEntityDbTable<CurrencyBuyOffer> buyOfferTable = new VersionedEntityDbTable<CurrencyBuyOffer>("buy_offer", buyOfferDbKeyFactory) {

        @Override
        protected CurrencyBuyOffer load(Connection con, ResultSet rs) throws SQLException {
            return new CurrencyBuyOffer(rs);
        }

        @Override
        protected void save(Connection con, CurrencyBuyOffer buy) throws SQLException {
            buy.save(con, table);
        }

    };

    public static int getCount() {
        return buyOfferTable.getCount();
    }

    public static CurrencyExchangeOffer getBuyOffer(long offerId) {
        return buyOfferTable.get(buyOfferDbKeyFactory.newKey(offerId));
    }

    public static DbIterator<CurrencyBuyOffer> getAll(int from, int to) {
        return buyOfferTable.getAll(from, to);
    }

    //TODO: add index on rate DESC, height ASC, id ASC to buy_offer table?
    public static DbIterator<CurrencyBuyOffer> getCurrencyOffers(long currencyId) {
        return buyOfferTable.getManyBy(new DbClause.LongClause("currency_id", currencyId), 0, -1, " ORDER BY rate DESC, height ASC, id ASC ");
    }

    public static CurrencyBuyOffer getCurrencyOffer(final long currencyId, final long accountId) {
        DbClause dbClause = new DbClause(" currency_id = ? AND account_id = ? ") {
            @Override
            protected int set(PreparedStatement pstmt, int index) throws SQLException {
                pstmt.setLong(index++, currencyId);
                pstmt.setLong(index++, accountId);
                return index;
            }
        };
        return buyOfferTable.getBy(dbClause);
    }

    static DbIterator<CurrencyBuyOffer> getOffers(DbClause dbClause, int from, int to) {
        return buyOfferTable.getManyBy(dbClause, from, to);
    }

    static void addOffer(Transaction transaction, Attachment.MonetarySystemPublishExchangeOffer attachment) {
        buyOfferTable.insert(new CurrencyBuyOffer(transaction, attachment));
    }

    static void remove(CurrencyBuyOffer buyOffer) {
        buyOfferTable.delete(buyOffer);
    }

    static void init() {}

    protected final DbKey dbKey;

    private CurrencyBuyOffer(Transaction transaction, Attachment.MonetarySystemPublishExchangeOffer attachment) {
        super(transaction.getId(), attachment.getCurrencyId(), transaction.getSenderId(), attachment.getBuyRateNQT(),
                attachment.getTotalBuyLimit(), attachment.getInitialBuySupply(), attachment.getExpirationHeight(), transaction.getHeight());
        this.dbKey = buyOfferDbKeyFactory.newKey(id);
    }

    private CurrencyBuyOffer(ResultSet rs) throws SQLException {
        super(rs);
        this.dbKey = buyOfferDbKeyFactory.newKey(super.id);
    }

    protected void save(Connection con, String table) throws SQLException {
        super.save(con, table);
    }

    @Override
    public CurrencyExchangeOffer getCounterOffer() {
        return CurrencySellOffer.getSellOffer(id);
    }

    void increaseSupply(long delta) {
        super.increaseSupply(delta);
        buyOfferTable.insert(this);
    }

    void decreaseLimitAndSupply(long delta) {
        super.decreaseLimitAndSupply(delta);
        buyOfferTable.insert(this);
    }

}
