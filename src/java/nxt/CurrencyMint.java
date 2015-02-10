package nxt;

import nxt.db.DbClause;
import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.VersionedEntityDbTable;
import nxt.util.Convert;
import nxt.util.Listener;
import nxt.util.Listeners;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages currency proof of work minting
 */
public final class CurrencyMint {

    public static enum Event {
        CURRENCY_MINT
    }

    public static class Mint {

        public final long accountId;
        public final long currencyId;
        public final long units;

        private Mint(long accountId, long currencyId, long units) {
            this.accountId = accountId;
            this.currencyId = currencyId;
            this.units = units;
        }

    }

    private static final DbKey.LinkKeyFactory<CurrencyMint> currencyMintDbKeyFactory = new DbKey.LinkKeyFactory<CurrencyMint>("currency_id", "account_id") {

        @Override
        public DbKey newKey(CurrencyMint currencyMint) {
            return currencyMint.dbKey;
        }

    };

    private static final VersionedEntityDbTable<CurrencyMint> currencyMintTable = new VersionedEntityDbTable<CurrencyMint>("currency_mint", currencyMintDbKeyFactory) {

        @Override
        protected CurrencyMint load(Connection con, ResultSet rs) throws SQLException {
            return new CurrencyMint(rs);
        }

        @Override
        protected void save(Connection con, CurrencyMint currencyMint) throws SQLException {
            currencyMint.save(con);
        }

    };

    private static final Listeners<Mint,Event> listeners = new Listeners<>();

    public static boolean addListener(Listener<Mint> listener, Event eventType) {
        return listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<Mint> listener, Event eventType) {
        return listeners.removeListener(listener, eventType);
    }


    static void init() {}

    private final DbKey dbKey;
    private final long currencyId;
    private final long accountId;
    private long counter;

    private CurrencyMint(long currencyId, long accountId, long counter) {
        this.currencyId = currencyId;
        this.accountId = accountId;
        this.dbKey = currencyMintDbKeyFactory.newKey(this.currencyId, this.accountId);
        this.counter = counter;
    }

    private CurrencyMint(ResultSet rs) throws SQLException {
        this.currencyId = rs.getLong("currency_id");
        this.accountId = rs.getLong("account_id");
        this.dbKey = currencyMintDbKeyFactory.newKey(this.currencyId, this.accountId);
        this.counter = rs.getLong("counter");
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO currency_mint (currency_id, account_id, counter, height, latest) "
                + "KEY (currency_id, account_id, height) VALUES (?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, this.getCurrencyId());
            pstmt.setLong(++i, this.getAccountId());
            pstmt.setLong(++i, this.getCounter());
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }

    public long getCurrencyId() {
        return currencyId;
    }

    public long getAccountId() {
        return accountId;
    }

    public long getCounter() {
        return counter;
    }

    static void mintCurrency(final Account account, final Attachment.MonetarySystemCurrencyMinting attachment) {
        CurrencyMint currencyMint = currencyMintTable.get(currencyMintDbKeyFactory.newKey(attachment.getCurrencyId(), account.getId()));
        if (currencyMint != null && attachment.getCounter() <= currencyMint.getCounter()) {
            return;
        }
        Currency currency = Currency.getCurrency(attachment.getCurrencyId());
        if (CurrencyMinting.meetsTarget(account.getId(), currency, attachment)) {
            if (currencyMint == null) {
                currencyMint = new CurrencyMint(attachment.getCurrencyId(), account.getId(), attachment.getCounter());
            } else {
                currencyMint.counter = attachment.getCounter();
            }
            currencyMintTable.insert(currencyMint);
            long units = Math.min(attachment.getUnits(), currency.getMaxSupply() - currency.getCurrentSupply());
            account.addToCurrencyAndUnconfirmedCurrencyUnits(currency.getId(), units);
            currency.increaseSupply(units);
            listeners.notify(new Mint(account.getId(), currency.getId(), units), Event.CURRENCY_MINT);
        }
    }

    public static long getCounter(long currencyId, long accountId) {
        CurrencyMint currencyMint = currencyMintTable.get(currencyMintDbKeyFactory.newKey(currencyId, accountId));
        if (currencyMint != null) {
            return currencyMint.getCounter();
        } else {
            return 0;
        }
    }

    static String debug(long currencyId, long accountId) {
        StringBuilder buf = new StringBuilder();
        CurrencyMint currencyMint = currencyMintTable.get(currencyMintDbKeyFactory.newKey(currencyId, accountId));
        buf.append("CurrencyMint counter: ").append(currencyMint.counter).append('\n');
        buf.append("blockchain height ").append(Nxt.getBlockchain().getHeight()).append('\n');
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM currency_mint WHERE currency_id = ? AND account_id = ?")) {
            pstmt.setLong(1, currencyId);
            pstmt.setLong(2, accountId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    buf.append("currency_id ").append(Convert.toUnsignedLong(rs.getLong("currency_id"))).append(" account_id ")
                            .append(Convert.toUnsignedLong(rs.getLong("account_id"))).append(" counter ").append(rs.getLong("counter")).append(" height ")
                            .append(rs.getInt("height")).append(" latest ").append(rs.getBoolean("latest")).append('\n');
                }
            }
        } catch (SQLException e) {
            buf.append(e.toString());
        }
        return buf.toString();
    }

    static void deleteCurrency(Currency currency) {
        List<CurrencyMint> currencyMints = new ArrayList<>();
        try (DbIterator<CurrencyMint> mints = currencyMintTable.getManyBy(new DbClause.LongClause("currency_id", currency.getId()), 0, -1)) {
            while (mints.hasNext()) {
                currencyMints.add(mints.next());
            }
        }
        for (CurrencyMint mint : currencyMints) {
            currencyMintTable.delete(mint);
        }
    }

}
