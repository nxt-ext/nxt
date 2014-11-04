package nxt;

import nxt.db.*;
import nxt.util.Convert;
import nxt.util.Logger;
import java.sql.*;
import java.util.Arrays;

public class PendingTransactionPoll extends AbstractPoll {
    private final Long id;
    private final DbKey dbKey;
    private final long[] whitelist;
    private final long[] blacklist;
    private final long quorum;

    private static final DbKey.LongKeyFactory<PendingTransactionPoll> pollDbKeyFactory = new DbKey.LongKeyFactory<PendingTransactionPoll>("id") {
        @Override
        public DbKey newKey(PendingTransactionPoll poll) {
            return poll.dbKey;
        }
    };


    final static class PendingTransactionsTable extends VersionedEntityDbTable<PendingTransactionPoll> {

        protected PendingTransactionsTable() {
            this(pollDbKeyFactory);
        }

        protected PendingTransactionsTable(DbKey.Factory<PendingTransactionPoll> dbKeyFactory) {
            super("pending_transactions", dbKeyFactory);
        }

        DbIterator<Long> finishing(int height) {
            try {
                Connection con = Db.getConnection();
                PreparedStatement pstmt = con.prepareStatement("SELECT id FROM " + table
                        + " WHERE finish = ?  AND finished = FALSE AND latest = TRUE");
                pstmt.setInt(1, height);
                return new DbIterator<>(con, pstmt, new DbIterator.ResultSetReader<Long>() {
                    @Override
                    public Long get(Connection con, ResultSet rs) throws Exception {
                        return rs.getLong("id");
                    }
                });
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }

        @Override
        protected PendingTransactionPoll load(Connection con, ResultSet rs) throws SQLException {
            try {
                return new PendingTransactionPoll(rs);
            } catch (Exception e) {
                throw new SQLException(e);
            }
        }

        @Override
        protected void save(Connection con, PendingTransactionPoll poll) throws SQLException {
            poll.save(con);
        }
    }

    final static PendingTransactionsTable pendingTransactionsTable = new PendingTransactionsTable();

    static void init() {
    }

    public PendingTransactionPoll(Long id, long accountId, int finishBlockHeight,
                                  byte votingModel, long quorum, long voteThreshold,
                                  long assetId, long[] whitelist, long[] blacklist) {
        super(accountId, finishBlockHeight, votingModel, assetId, voteThreshold);
        this.id = id;
        this.dbKey = pollDbKeyFactory.newKey(this.id);
        this.quorum = quorum;

        this.whitelist = whitelist;
        if (this.whitelist != null) {
            Arrays.sort(this.whitelist);
        }

        this.blacklist = blacklist;
        if (this.blacklist != null) {
            Arrays.sort(this.blacklist);
        }
    }

    private long[] restoreAccounts(String commaSeparatedAccounts){
        if(commaSeparatedAccounts==null) {
            return new long[0];
        }else{
            String[] accountStrings = commaSeparatedAccounts.split(",");
            long[] result = new long[accountStrings.length];
            for (int i = 0; i < accountStrings.length; i++) {
                result[i] = Long.parseLong(accountStrings[i]);
            }
            Arrays.sort(result);
            return result;
        }
    }

    public PendingTransactionPoll(ResultSet rs) throws SQLException {
        super(rs);
        this.id = rs.getLong("id");
        this.quorum = rs.getLong("quorum");
        this.dbKey = pollDbKeyFactory.newKey(this.id);

        String whitelistAsString = Convert.emptyToNull(rs.getString("whitelist"));
        this.whitelist = restoreAccounts(whitelistAsString);

        String blacklistAsString = Convert.emptyToNull(rs.getString("blacklist"));
        this.blacklist = restoreAccounts(blacklistAsString);
    }

    public Long getId() {
        return id;
    }

    public static PendingTransactionPoll byId(long id) {
        return pendingTransactionsTable.getBy(new DbClause.LongClause("id", id));
    }

    public static DbIterator<PendingTransactionPoll> getByAccountId(long accountId, int firstIndex, int lastIndex){
        DbClause clause = new DbClause.LongClause("account_id", accountId);
        return pendingTransactionsTable.getManyBy(clause, firstIndex, lastIndex);
    }

    public static DbIterator<PendingTransactionPoll> getActiveByAccountId(long accountId, int firstIndex, int lastIndex){
        DbClause clause = new DbClause.LongBooleanClause("account_id", accountId, "finished", false);
        return pendingTransactionsTable.getManyBy(clause, firstIndex, lastIndex);
    }

    public static DbIterator<PendingTransactionPoll> getFinishedByAccountId(long accountId, int firstIndex, int lastIndex){
        DbClause clause = new DbClause.LongBooleanClause("account_id", accountId, "finished", true);
        return pendingTransactionsTable.getManyBy(clause, firstIndex, lastIndex);
    }

    public long[] getWhitelist() { return whitelist; }

    public long[] getBlacklist() { return blacklist; }

    public Long getQuorum() {
        return quorum;
    }

    private String accountsToString(long[] accounts){
        if (accounts != null) {
            StringBuilder resultBuilder = new StringBuilder();
            for (long account : accounts) {
                resultBuilder.append(account);
                resultBuilder.append(",");
            }
            return resultBuilder.length() > 0 ? resultBuilder.substring(0, resultBuilder.length() - 1) : "";
        }else{
            return "";
        }
    }

    void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO pending_transactions (id, account_id, "
                + "finish,  whitelist, blacklist, voting_model, quorum, min_balance, asset_id, "
                + "finished, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, getId());
            pstmt.setLong(++i, getAccountId());
            pstmt.setInt(++i, getFinishBlockHeight());
            pstmt.setString(++i, accountsToString(whitelist));
            pstmt.setString(++i, accountsToString(blacklist));
            pstmt.setByte(++i, getVotingModel());
            pstmt.setLong(++i, getQuorum());
            pstmt.setLong(++i, getMinBalance());
            pstmt.setLong(++i, getAssetId());
            pstmt.setBoolean(++i, isFinished());
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }

    public static void finishPoll(PendingTransactionPoll poll) {
        poll.setFinished(true);
        pendingTransactionsTable.insert(poll);
    }
}
