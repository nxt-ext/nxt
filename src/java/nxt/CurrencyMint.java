package nxt;

import nxt.crypto.HashFunction;
import nxt.db.DbClause;
import nxt.db.DbKey;
import nxt.db.EntityDbTable;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * Manages currency proof of work minting
 */
public final class CurrencyMint {


    private static final DbKey.LongKeyFactory<CurrencyMint> currencyMintDbKeyFactory = new DbKey.LongKeyFactory<CurrencyMint>("id") {

        @Override
        public DbKey newKey(CurrencyMint currencyMint) {
            return currencyMint.dbKey;
        }

    };

    private static final EntityDbTable<CurrencyMint> currencyMintTable = new EntityDbTable<CurrencyMint>("currency_mint", currencyMintDbKeyFactory) {

        @Override
        protected CurrencyMint load(Connection con, ResultSet rs) throws SQLException {
            return new CurrencyMint(rs);
        }

        @Override
        protected void save(Connection con, CurrencyMint currencyMint) throws SQLException {
            currencyMint.save(con);
        }

    };

    static void init() {}

    private final long id;
    private final DbKey dbKey;
    private final long currencyId;
    private final long accountId;
    private final long counter;
    private final int height;

    private CurrencyMint(long transactionId, long currencyId, long accountId, long counter) {
        this.id = transactionId;
        this.dbKey = currencyMintDbKeyFactory.newKey(this.id);
        this.currencyId = currencyId;
        this.accountId = accountId;
        this.counter = counter;
        this.height = Nxt.getBlockchain().getHeight();
    }

    private CurrencyMint(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.dbKey = currencyMintDbKeyFactory.newKey(this.id);
        this.currencyId = rs.getLong("currency_id");
        this.accountId = rs.getLong("account_id");
        this.counter = rs.getLong("counter");
        this.height = rs.getInt("height");
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO currency_mint (id, currency_id, account_id, counter, height)"
                + "VALUES (?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, this.getId());
            pstmt.setLong(++i, this.getCurrencyId());
            pstmt.setLong(++i, this.getAccountId());
            pstmt.setLong(++i, this.getCounter());
            pstmt.setInt(++i, this.getHeight());
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

    public long getCounter() {
        return counter;
    }

    public int getHeight() {
        return height;
    }

    
    static void mintCurrency(Transaction transaction, final Account account, final Attachment.MonetarySystemCurrencyMinting attachment) {
        DbClause mintClause = new DbClause(" currency_id = ? AND account_id = ? AND counter = ? ") {
            @Override
            protected int set(PreparedStatement pstmt, int index) throws SQLException {
                pstmt.setLong(index++, attachment.getCurrencyId());
                pstmt.setLong(index++, account.getId());
                pstmt.setLong(index++, attachment.getCounter());
                return index;
            }
        };
        CurrencyMint currencyMint = currencyMintTable.getBy(mintClause);
        if (currencyMint != null && attachment.getCounter() <= currencyMint.getCounter()) {
            return;
        }

        Currency currency = Currency.getCurrency(attachment.getCurrencyId());
        byte[] hash = getHash(currency.getAlgorithm(), attachment.getNonce(), attachment.getCurrencyId(), attachment.getUnits(),
                attachment.getCounter(), account.getId());
        byte[] target = getTarget(currency.getMinDifficulty(), currency.getMaxDifficulty(),
                attachment.getUnits(), currency.getCurrentSupply(), currency.getTotalSupply());
        if (meetsTarget(hash, target)) {
            currencyMintTable.insert(new CurrencyMint(transaction.getId(), attachment.getCurrencyId(), account.getId(), attachment.getCounter()));
            long units = Math.min(attachment.getUnits(), currency.getTotalSupply() - currency.getCurrentSupply());
            account.addToCurrencyAndUnconfirmedCurrencyUnits(attachment.getUnits(), units);
            currency.increaseSupply(units);
        }
    }

    public static boolean meetsTarget(byte[] hash, byte[] target) {
        for (int i = hash.length - 1; i >= 0; i--) {
            if ((hash[i] & 0xff) > (target[i] & 0xff)) {
                return false;
            }
            if ((hash[i] & 0xff) < (target[i] & 0xff)) {
                return true;
            }
        }
        return true;
    }

    public static byte[] getHash(byte algorithm, long nonce, long currencyId, long units, long counter, long accountId) {
        ByteBuffer buffer = ByteBuffer.allocate(8 + 8 + 8 + 8 + 8);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(nonce);
        buffer.putLong(currencyId);
        buffer.putLong(units);
        buffer.putLong(counter);
        buffer.putLong(accountId);
        return HashFunction.getHashFunction(algorithm).hash(buffer.array());
    }

    public static byte[] getTarget(byte min, byte max, long units, long currentSupply, long totalSupply) {
        BigInteger targetNum = getNumericTarget(min, max, units, currentSupply, totalSupply);
        byte[] targetRowBytes = targetNum.toByteArray();
        if (targetRowBytes.length == 32) {
            return reverse(targetRowBytes);
        }
        byte[] targetBytes = new byte[32];
        Arrays.fill(targetBytes, 0, 32 - targetRowBytes.length, (byte) 0);
        System.arraycopy(targetRowBytes, 0, targetBytes, 32 - targetRowBytes.length, targetRowBytes.length);
        return reverse(targetBytes);
    }

    public static BigInteger getNumericTarget(byte min, byte max, long units, float currentSupply, float totalSupply) {
        int exp = 256 - (min + Math.round((max - min) * (currentSupply / totalSupply)));
        return (BigInteger.valueOf(2).pow(exp)).divide(BigInteger.valueOf(units));
    }

    static byte[] reverse(byte[] b) {
        for(int i=0; i < b.length/2; i++) {
            byte temp = b[i];
            b[i] = b[b.length - i - 1];
            b[b.length - i - 1] = temp;
        }
        return b;
    }
}
