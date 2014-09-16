package nxt;

import nxt.db.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class CurrencyBuy extends CurrencyOffer {

    private static final DbKey.LongKeyFactory<CurrencyOffer> buyOfferDbKeyFactory = new DbKey.LongKeyFactory<CurrencyOffer>("id") {

        @Override
        public DbKey newKey(CurrencyOffer offer) {
            return offer.dbKey;
        }

    };

    private static final VersionedEntityDbTable<CurrencyOffer> buyOfferTable = new VersionedEntityDbTable<CurrencyOffer>(buyOfferDbKeyFactory) {

        @Override
        protected String table() {
            return "buy_offer";
        }

        @Override
        protected CurrencyBuy load(Connection con, ResultSet rs) throws SQLException {
            return new CurrencyBuy(rs);
        }

        @Override
        protected void save(Connection con, CurrencyOffer buy) throws SQLException {
            buy.save(con, table());
        }

    };

    public static int getCount() {
        return buyOfferTable.getCount();
    }

    public static CurrencyOffer getBuyOffer(Long offerId) {
        return buyOfferTable.get(buyOfferDbKeyFactory.newKey(offerId));
    }

    public static DbIterator<CurrencyOffer> getAll(int from, int to) {
        return buyOfferTable.getAll(from, to);
    }

    static void init() {}

    public CurrencyBuy(long id, long currencyId, Long accountId, long rateNQT, long limit, long supply, int expirationHeight, int publicationHeight) {
        super(id, currencyId, accountId, rateNQT, limit, supply, expirationHeight, publicationHeight);
        this.dbKey = buyOfferDbKeyFactory.newKey(id);
    }

    private CurrencyBuy(ResultSet rs) throws SQLException {
        super(rs);
        this.dbKey = buyOfferDbKeyFactory.newKey(super.id);
    }

    protected void save(Connection con, String table) throws SQLException {
        super.save(con, table);
    }

    @Override
    public CurrencyOffer getCounterOffer() {
        return CurrencySell.getSellOffer(id);
    }

    public static void addOffer(CurrencyOffer buyOffer) {
        buyOfferTable.insert(buyOffer);
    }

    public static void remove(CurrencyOffer buyOffer) {
        buyOfferTable.delete(buyOffer);
    }

    public static DbIterator<CurrencyOffer> getCurrencyOffers(long currencyId) {
        Connection con = null;
        try {
            con = Db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM buy_offer WHERE currency_id = ? "
                    + "AND latest = TRUE ORDER BY rate DESC, height ASC, id ASC");
            pstmt.setLong(1, currencyId);
            return buyOfferTable.getManyBy(con, pstmt, true);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public void increaseSupply(long delta) {
        super.increaseSupply(delta);
        buyOfferTable.insert(this);
    }

    public void decreaseLimitAndSupply(long delta) {
        super.decreaseLimitAndSupply(delta);
        buyOfferTable.insert(this);
    }

}
