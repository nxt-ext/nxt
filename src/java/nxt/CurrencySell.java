package nxt;

import nxt.db.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class CurrencySell extends CurrencyOffer {

    private static final DbKey.LongKeyFactory<CurrencyOffer> sellOfferDbKeyFactory = new DbKey.LongKeyFactory<CurrencyOffer>("id") {

        @Override
        public DbKey newKey(CurrencyOffer sell) {
            return sell.dbKey;
        }

    };

    private static final VersionedEntityDbTable<CurrencyOffer> sellOfferTable = new VersionedEntityDbTable<CurrencyOffer>("sell_offer", sellOfferDbKeyFactory) {

        @Override
        protected CurrencySell load(Connection con, ResultSet rs) throws SQLException {
            return new CurrencySell(rs);
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

    CurrencySell(Transaction transaction, Attachment.MonetarySystemPublishExchangeOffer attachment) {
        super(transaction.getId(), attachment.getCurrencyId(), transaction.getSenderId(), attachment.getSellRateNQT(),
                attachment.getTotalSellLimit(), attachment.getInitialSellSupply(), attachment.getExpirationHeight(), transaction.getHeight());
        this.dbKey = sellOfferDbKeyFactory.newKey(id);
    }

    private CurrencySell(ResultSet rs) throws SQLException {
        super(rs);
        this.dbKey = sellOfferDbKeyFactory.newKey(super.id);
    }

    protected void save(Connection con, String table) throws SQLException {
        super.save(con, table);
    }

    @Override
    public CurrencyOffer getCounterOffer() {
        return CurrencyBuy.getBuyOffer(id);
    }

    static void addOffer(CurrencySell sellOffer) {
        sellOfferTable.insert(sellOffer);
    }

    static void remove(CurrencyOffer sellOffer) {
        sellOfferTable.delete(sellOffer);
    }

    public static DbIterator<CurrencyOffer> getCurrencyOffers(long currencyId) {
        Connection con = null;
        try {
            con = Db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM sell_offer WHERE currency_id = ? "
                    + "AND latest = TRUE ORDER BY rate DESC, height ASC, id ASC");
            pstmt.setLong(1, currencyId);
            return sellOfferTable.getManyBy(con, pstmt, true);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
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
