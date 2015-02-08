package nxt;

import nxt.db.*;
import nxt.util.Convert;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class PendingTransactionPoll extends AbstractPoll {

    private static final DbKey.LongKeyFactory<PendingTransactionPoll> pollDbKeyFactory = new DbKey.LongKeyFactory<PendingTransactionPoll>("id") {
        @Override
        public DbKey newKey(PendingTransactionPoll poll) {
            return poll.dbKey;
        }
    };

    private static final DbKey.LongKeyFactory<PendingTransactionPoll> signersDbKeyFactory = new DbKey.LongKeyFactory<PendingTransactionPoll>("pending_transaction_id") {
        @Override
        public DbKey newKey(PendingTransactionPoll poll) {
            return poll.dbKey;
        }
    };

    private static final ValuesDbTable<PendingTransactionPoll, Long> signersTable = new ValuesDbTable<PendingTransactionPoll, Long>("pending_transaction_signer", signersDbKeyFactory) {

        @Override
        protected Long load(Connection con, ResultSet rs) throws SQLException {
            return rs.getLong("account_id");
        }

        @Override
        protected void save(Connection con, PendingTransactionPoll poll, Long accountId) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO " + table + "(pending_transaction_id, "
                    + "account_id, height) VALUES (?, ?, ?)")) {
                int i = 0;
                pstmt.setLong(++i, poll.getId());
                pstmt.setLong(++i, accountId);
                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
        }
    };

    private static final VersionedEntityDbTable<PendingTransactionPoll> pendingTransactionsTable =
            new VersionedEntityDbTable<PendingTransactionPoll>("pending_transaction", pollDbKeyFactory) {

                @Override
                protected PendingTransactionPoll load(Connection con, ResultSet rs) throws SQLException {
                    return new PendingTransactionPoll(rs);
                }

                @Override
                protected void save(Connection con, PendingTransactionPoll poll) throws SQLException {
                    poll.save(con);
                }

                @Override
                public void trim(int height) {
                    super.trim(height);
                    try (Connection con = Db.db.getConnection();
                         DbIterator<PendingTransactionPoll> pollsToTrim = getFinishingBefore(height);
                         PreparedStatement pstmt1 = con.prepareStatement("DELETE FROM pending_transaction WHERE id = ?");
                         PreparedStatement pstmt2 = con.prepareStatement("DELETE FROM pending_transaction_signer WHERE pending_transaction_id = ?");
                         PreparedStatement pstmt3 = con.prepareStatement("DELETE FROM vote_phased WHERE pending_transaction_id = ?")) {
                        while (pollsToTrim.hasNext()) {
                            PendingTransactionPoll polltoTrim = pollsToTrim.next();
                            long id = polltoTrim.getId();
                            pstmt1.setLong(1, id);
                            pstmt1.executeUpdate();
                            pstmt2.setLong(1, id);
                            pstmt2.executeUpdate();
                            pstmt3.setLong(1, id);
                            pstmt3.executeUpdate();
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(e.toString(), e);
                    } finally {
                        clearCache();
                    }
                }
            };

    public static int getPendingCount() {
        return pendingTransactionsTable.getCount(new DbClause.FixedClause("finished = FALSE"));
    }

    public static DbIterator<PendingTransactionPoll> getFinishingBefore(int height) {
        return pendingTransactionsTable.getManyBy(new DbClause.IntClause("finish_height", DbClause.Op.LT, height), 0, Integer.MAX_VALUE);
    }

    public static PendingTransactionPoll getPoll(long id) {
        return pendingTransactionsTable.getBy(new DbClause.LongClause("id", id));
    }

    public static DbIterator<PendingTransactionPoll> getByAccountId(long accountId, int firstIndex, int lastIndex) {
        DbClause clause = new DbClause.LongClause("account_id", accountId);
        return pendingTransactionsTable.getManyBy(clause, firstIndex, lastIndex);
    }

    public static DbIterator<PendingTransactionPoll> getByHoldingId(long holdingId, int firstIndex, int lastIndex) {
        DbClause clause = new DbClause.LongClause("holding_id", holdingId);
        return pendingTransactionsTable.getManyBy(clause, firstIndex, lastIndex);
    }

    public static DbIterator<? extends Transaction> getPendingTransactionsForApprover(Account signer, int from, int to) {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT transaction.* "
                    + "FROM transaction, pending_transaction, pending_transaction_signer "
                    + "WHERE transaction.id = pending_transaction.id AND pending_transaction.latest = TRUE AND "
                    + "pending_transaction.blacklist = false AND "
                    + "pending_transaction.finished = false AND "
                    + "pending_transaction.id = pending_transaction_signer.pending_transaction_id "
                    + "AND pending_transaction_signer.account_id = ? "
                    + DbUtils.limitsClause(from, to));
            pstmt.setLong(1, signer.getId());
            DbUtils.setLimits(2, pstmt, from, to);

            return Nxt.getBlockchain().getTransactions(con, pstmt);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static DbIterator<? extends Transaction> getPendingTransactionsForHolding(long holdingId, byte votingModel, int from, int to) {

        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT transaction.* " +
                    "FROM transaction, pending_transaction " +
                    "WHERE pending_transaction.holding_id = ? " +
                    "AND pending_transaction.voting_model = ? " +
                    "AND pending_transaction.id = transaction.id " +
                    "AND pending_transaction.finished = FALSE " +
                    "AND pending_transaction.latest = TRUE " +
                    DbUtils.limitsClause(from, to));
            pstmt.setLong(1, holdingId);
            pstmt.setByte(2, votingModel);
            DbUtils.setLimits(3, pstmt, from, to);

            return Nxt.getBlockchain().getTransactions(con, pstmt);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static DbIterator<? extends Transaction> getPendingTransactionsForAccount(Account account, int from, int to) {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT transaction.* FROM transaction, pending_transaction " +
                    " WHERE transaction.sender_id = ? AND pending_transaction.id = transaction.id " +
                    " AND pending_transaction.finished = FALSE AND pending_transaction.latest = TRUE " +
                    DbUtils.limitsClause(from, to));
            pstmt.setLong(1, account.getId());
            DbUtils.setLimits(2, pstmt, from, to);

            return Nxt.getBlockchain().getTransactions(con, pstmt);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    static void addPoll(Transaction transaction, Appendix.Phasing appendix) {
        PendingTransactionPoll poll = new PendingTransactionPoll(transaction.getId(), transaction.getSenderId(), appendix);

        pendingTransactionsTable.insert(poll);

        long[] signers;

        if (poll.getBlacklist().length > 0) {
            signers = poll.getBlacklist();
        } else {
            signers = poll.getWhitelist();
        }

        if (signers.length > 0) {
            signersTable.insert(poll, Convert.arrayOfLongsToList(signers));
        }
    }

    static void init() {
    }

    private final long id;
    private final DbKey dbKey;
    private final long[] whitelist;
    private final long[] blacklist;
    private final long quorum;
    private boolean finished;

    private PendingTransactionPoll(long id, long accountId, Appendix.Phasing appendix) {
        super(accountId, appendix.getFinishHeight(), appendix.getVotingModel(), appendix.getHoldingId(),
                appendix.getMinBalance(), appendix.getMinBalanceModel());
        this.id = id;
        this.dbKey = pollDbKeyFactory.newKey(this.id);
        this.quorum = appendix.getQuorum();
        this.whitelist = appendix.getWhitelist();
        if (this.whitelist.length > 0) {
            Arrays.sort(this.whitelist);
        }
        this.blacklist = appendix.getBlacklist();
        if (this.blacklist.length > 0) {
            Arrays.sort(this.blacklist);
        }
    }

    private PendingTransactionPoll(ResultSet rs) throws SQLException {
        super(rs);
        this.id = rs.getLong("id");
        this.quorum = rs.getLong("quorum");
        this.dbKey = pollDbKeyFactory.newKey(this.id);

        byte signersCount = rs.getByte("signers_count");
        if (signersCount == 0) {
            this.whitelist = new long[0];
            this.blacklist = this.whitelist;
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

        this.finished = rs.getBoolean("finished");
    }

    void finish() {
        this.finished = true;
        pendingTransactionsTable.insert(this);
    }

    public boolean isFinished() {
        return finished;
    }

    public long getId() {
        return id;
    }

    public long[] getWhitelist() {
        return whitelist;
    }

    public long[] getBlacklist() {
        return blacklist;
    }

    public long getQuorum() {
        return quorum;
    }

    private void save(Connection con) throws SQLException {
        boolean isBlacklist = getBlacklist().length > 0;
        byte signersCount = isBlacklist ? (byte) getBlacklist().length : (byte) getWhitelist().length;

        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO pending_transaction (id, account_id, "
                + "finish_height, signers_count, blacklist, voting_model, quorum, min_balance, holding_id, "
                + "min_balance_model, finished, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, getId());
            pstmt.setLong(++i, getAccountId());
            pstmt.setInt(++i, getFinishHeight());
            pstmt.setByte(++i, signersCount);
            pstmt.setBoolean(++i, isBlacklist);
            pstmt.setByte(++i, getDefaultPollCounting().getVotingModel());
            pstmt.setLong(++i, getQuorum());
            pstmt.setLong(++i, getDefaultPollCounting().getMinBalance());
            pstmt.setLong(++i, getDefaultPollCounting().getHoldingId());
            pstmt.setByte(++i, getDefaultPollCounting().getMinBalanceModel());
            pstmt.setBoolean(++i, isFinished());
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }
}
