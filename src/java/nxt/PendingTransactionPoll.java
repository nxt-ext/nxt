package nxt;

import nxt.db.*;
import nxt.util.Convert;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    private static final DbKey.LongKeyFactory<PendingTransactionPoll> signersDbKeyFactory = new DbKey.LongKeyFactory<PendingTransactionPoll>("poll_id") {
        @Override
        public DbKey newKey(PendingTransactionPoll poll) {
            return poll.dbKey;
        }
    };

    final static ValuesDbTable<PendingTransactionPoll, Long> signersTable = new ValuesDbTable<PendingTransactionPoll, Long>("pending_transactions_signers", signersDbKeyFactory) {

        @Override
        protected Long load(Connection con, ResultSet rs) throws SQLException {
            return rs.getLong("account_id");
        }

        @Override
        protected void save(Connection con, PendingTransactionPoll poll, Long accountId) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO " + table + "(poll_id, "
                    + "account_id, height) VALUES (?, ?, ?)")) {
                int i = 0;
                pstmt.setLong(++i, poll.getId());
                pstmt.setLong(++i, accountId);
                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
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
                Connection con = db.getConnection();
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

        if (whitelist != null) {
            this.whitelist = whitelist;
            Arrays.sort(this.whitelist);
        } else {
            this.whitelist = new long[0];
        }


        if (blacklist != null) {
            this.blacklist = blacklist;
            Arrays.sort(this.blacklist);
        } else {
            this.blacklist = new long[0];
        }
    }

    public PendingTransactionPoll(ResultSet rs) throws SQLException {
        super(rs);
        this.id = rs.getLong("id");
        this.quorum = rs.getLong("quorum");
        this.dbKey = pollDbKeyFactory.newKey(this.id);

        byte signersCount = rs.getByte("signersCount");
        if (signersCount == 0) {
            this.whitelist = new long[0];
            this.blacklist = new long[0];
        } else {
            List<Long> signers = signersTable.get(signersDbKeyFactory.newKey(this));
            boolean isBlacklist = rs.getBoolean("blacklist");
            if (isBlacklist) {
                this.whitelist = new long[0];
                this.blacklist = Convert.reversedListOfLongsToArray(signers);
            } else {
                this.whitelist = Convert.reversedListOfLongsToArray(signers);
                this.blacklist = new long[0];
            }
        }
    }

    public Long getId() {
        return id;
    }

    public static PendingTransactionPoll byId(long id) {
        return pendingTransactionsTable.getBy(new DbClause.LongClause("id", id));
    }

    public static DbIterator<PendingTransactionPoll> getByAccountId(long accountId, int firstIndex, int lastIndex) {
        DbClause clause = new DbClause.LongClause("account_id", accountId);
        return pendingTransactionsTable.getManyBy(clause, firstIndex, lastIndex);
    }

    public static DbIterator<PendingTransactionPoll> getActiveByAccountId(long accountId, int firstIndex, int lastIndex) {
        DbClause clause = new DbClause.LongBooleanClause("account_id", accountId, "finished", false);
        return pendingTransactionsTable.getManyBy(clause, firstIndex, lastIndex);
    }

    public static DbIterator<PendingTransactionPoll> getFinishedByAssetId(long assetId, int firstIndex, int lastIndex) {
        DbClause clause = new DbClause.LongBooleanClause("asset_id", assetId, "finished", true);
        return pendingTransactionsTable.getManyBy(clause, firstIndex, lastIndex);
    }

    public static DbIterator<PendingTransactionPoll> getByAssetId(long assetId, int firstIndex, int lastIndex) {
        DbClause clause = new DbClause.LongClause("asset_id", assetId);
        return pendingTransactionsTable.getManyBy(clause, firstIndex, lastIndex);
    }

    public static DbIterator<PendingTransactionPoll> getActiveByAssetId(long assetId, int firstIndex, int lastIndex) {
        DbClause clause = new DbClause.LongBooleanClause("asset_id", assetId, "finished", false);
        return pendingTransactionsTable.getManyBy(clause, firstIndex, lastIndex);
    }

    public static DbIterator<PendingTransactionPoll> getFinishedByAccountId(long accountId, int firstIndex, int lastIndex) {
        DbClause clause = new DbClause.LongBooleanClause("account_id", accountId, "finished", true);
        return pendingTransactionsTable.getManyBy(clause, firstIndex, lastIndex);
    }

    public static List<Long> getIdsByWhitelistedSigner(Account signer, Boolean finished, int from, int to) {
        String finishedClause = "";
        if (finished != null) {
            if (finished) {
                finishedClause = " pending_transactions.finished = true AND ";
            } else {
                finishedClause = " pending_transactions.finished = false AND ";
            }
        }

        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT DISTINCT pending_transactions.id "
                     + "from pending_transactions, pending_transactions_signers "
                     + "WHERE pending_transactions.latest = TRUE AND "
                     + "pending_transactions.blacklist = false AND " + finishedClause
                     + "pending_transactions.id = pending_transactions_signers.poll_id "
                     + "AND pending_transactions_signers.account_id = ? "
                     + DbUtils.limitsClause(from, to))) {
            pstmt.setLong(1, signer.getId());
            DbUtils.setLimits(2, pstmt, from, to);

            DbIterator<Long> iterator = new DbIterator<>(con, pstmt, new DbIterator.ResultSetReader<Long>() {
                @Override
                public Long get(Connection con, ResultSet rs) throws Exception {
                    return rs.getLong(1);
                }
            });
            List<Long> result = new ArrayList<>();
            while (iterator.hasNext()) {
                result.add(iterator.next());
            }
            return result;
        } catch (SQLException e) {
            throw new NxtException.StopException(e.toString(), e);
        }
    }


    public long[] getWhitelist() {
        return whitelist;
    }

    public long[] getBlacklist() {
        return blacklist;
    }

    public Long getQuorum() {
        return quorum;
    }

    void save(Connection con) throws SQLException {
        boolean isBlacklist;
        long[] signers;

        if (blacklist.length > 0) {
            isBlacklist = true;
            signers = blacklist;
        } else {
            isBlacklist = false;
            signers = whitelist;
        }

        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO pending_transactions (id, account_id, "
                + "finish, signersCount, blacklist, voting_model, quorum, min_balance, asset_id, "
                + "finished, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, getId());
            pstmt.setLong(++i, getAccountId());
            pstmt.setInt(++i, getFinishBlockHeight());
            pstmt.setByte(++i, (byte) signers.length);
            pstmt.setBoolean(++i, isBlacklist);
            pstmt.setByte(++i, getVotingModel());
            pstmt.setLong(++i, getQuorum());
            pstmt.setLong(++i, getMinBalance());
            pstmt.setLong(++i, getAssetId());
            pstmt.setBoolean(++i, isFinished());
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();

            if (signers.length > 0) {
                signersTable.insert(this, Convert.arrayOfLongsToList(signers));
            }
        }
    }

    public static void finishPoll(PendingTransactionPoll poll) {
        poll.setFinished(true);
        pendingTransactionsTable.insert(poll);
    }
}