package nxt;

import nxt.db.DbClause;
import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.DbUtils;
import nxt.db.EntityDbTable;
import nxt.db.ValuesDbTable;
import nxt.db.VersionedEntityDbTable;
import nxt.util.Convert;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

public final class PhasingPoll extends AbstractPoll {

    public static final class PhasingPollResult {

        private final long id;
        private final DbKey dbKey;
        private final int applyHeight;
        private final long result;
        private final boolean approved;

        private PhasingPollResult(PhasingPoll poll, int height, long result) {
            this.id = poll.getId();
            this.dbKey = resultDbKeyFactory.newKey(this.id);
            this.applyHeight = height;
            this.result = result;
            this.approved = result >= poll.getQuorum();
        }

        private PhasingPollResult(ResultSet rs) throws SQLException {
            this.id = rs.getLong("id");
            this.dbKey = resultDbKeyFactory.newKey(this.id);
            this.applyHeight = rs.getInt("apply_height");
            this.result = rs.getLong("result");
            this.approved = rs.getBoolean("approved");
        }

        private void save(Connection con) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO phasing_poll_result (id, "
                    + "apply_height, result, approved, height) VALUES (?, ?, ?, ?, ?)")) {
                int i = 0;
                pstmt.setLong(++i, id);
                pstmt.setInt(++i, applyHeight);
                pstmt.setLong(++i, result);
                pstmt.setBoolean(++i, approved);
                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
        }

        public long getId() {
            return id;
        }

        public int getApplyHeight() {
            return applyHeight;
        }

        public long getResult() {
            return result;
        }

        public boolean isApproved() {
            return approved;
        }
    }

    private static final DbKey.LongKeyFactory<PhasingPoll> phasingPollDbKeyFactory = new DbKey.LongKeyFactory<PhasingPoll>("id") {
        @Override
        public DbKey newKey(PhasingPoll poll) {
            return poll.dbKey;
        }
    };

    private static final VersionedEntityDbTable<PhasingPoll> phasingPollTable = new VersionedEntityDbTable<PhasingPoll>("phasing_poll", phasingPollDbKeyFactory) {

        @Override
        protected PhasingPoll load(Connection con, ResultSet rs) throws SQLException {
            return new PhasingPoll(rs);
        }

        @Override
        protected void save(Connection con, PhasingPoll poll) throws SQLException {
            poll.save(con);
        }

        @Override
        public void trim(int height) {
            super.trim(height);
            try (Connection con = Db.db.getConnection();
                 DbIterator<PhasingPoll> pollsToTrim = getFinishingBefore(height);
                 PreparedStatement pstmt1 = con.prepareStatement("DELETE FROM phasing_poll WHERE id = ?");
                 PreparedStatement pstmt2 = con.prepareStatement("DELETE FROM phasing_poll_voter WHERE transaction_id = ?");
                 PreparedStatement pstmt3 = con.prepareStatement("DELETE FROM phasing_vote WHERE transaction_id = ?")) {
                while (pollsToTrim.hasNext()) {
                    PhasingPoll polltoTrim = pollsToTrim.next();
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

    private static final DbKey.LongKeyFactory<PhasingPoll> votersDbKeyFactory = new DbKey.LongKeyFactory<PhasingPoll>("transaction_id") {
        @Override
        public DbKey newKey(PhasingPoll poll) {
            return poll.dbKey;
        }
    };

    private static final ValuesDbTable<PhasingPoll, Long> votersTable = new ValuesDbTable<PhasingPoll, Long>("phasing_poll_voter", votersDbKeyFactory) {

        @Override
        protected Long load(Connection con, ResultSet rs) throws SQLException {
            return rs.getLong("voter_id");
        }

        @Override
        protected void save(Connection con, PhasingPoll poll, Long accountId) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO phasing_poll_voter (transaction_id, "
                    + "voter_id, height) VALUES (?, ?, ?)")) {
                int i = 0;
                pstmt.setLong(++i, poll.getId());
                pstmt.setLong(++i, accountId);
                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
        }
    };

    private static final DbKey.LongKeyFactory<PhasingPollResult> resultDbKeyFactory = new DbKey.LongKeyFactory<PhasingPollResult>("id") {
        @Override
        public DbKey newKey(PhasingPollResult phasingPollResult) {
            return phasingPollResult.dbKey;
        }
    };

    private static final EntityDbTable<PhasingPollResult> resultTable = new EntityDbTable<PhasingPollResult>("phasing_poll_result", resultDbKeyFactory) {

        @Override
        protected PhasingPollResult load(Connection con, ResultSet rs) throws SQLException {
            return new PhasingPollResult(rs);
        }

        @Override
        protected void save(Connection con, PhasingPollResult phasingPollResult) throws SQLException {
            phasingPollResult.save(con);
        }
    };

    public static PhasingPollResult getResult(long id) {
        return resultTable.get(resultDbKeyFactory.newKey(id));
    }

    public static int getPendingCount() {
        return phasingPollTable.getCount(new DbClause.FixedClause("finished = FALSE"));
    }

    public static DbIterator<PhasingPoll> getFinishingBefore(int height) {
        return phasingPollTable.getManyBy(new DbClause.IntClause("finish_height", DbClause.Op.LT, height), 0, Integer.MAX_VALUE);
    }

    public static PhasingPoll getPoll(long id) {
        return phasingPollTable.get(phasingPollDbKeyFactory.newKey(id));
    }

    public static DbIterator<PhasingPoll> getByAccountId(long accountId, int firstIndex, int lastIndex) {
        DbClause clause = new DbClause.LongClause("account_id", accountId);
        return phasingPollTable.getManyBy(clause, firstIndex, lastIndex);
    }

    public static DbIterator<PhasingPoll> getByHoldingId(long holdingId, int firstIndex, int lastIndex) {
        DbClause clause = new DbClause.LongClause("holding_id", holdingId);
        return phasingPollTable.getManyBy(clause, firstIndex, lastIndex);
    }

    public static DbIterator<? extends Transaction> getVoterPendingTransactions(Account voter, int from, int to) {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT transaction.* "
                    + "FROM transaction, phasing_poll, phasing_poll_voter "
                    + "WHERE transaction.id = phasing_poll.id AND phasing_poll.latest = TRUE AND "
                    + "phasing_poll.finished = false AND "
                    + "phasing_poll.id = phasing_poll_voter.transaction_id "
                    + "AND phasing_poll_voter.voter_id = ? "
                    + "ORDER BY transaction.height DESC, transaction.transaction_index DESC "
                    + DbUtils.limitsClause(from, to));
            pstmt.setLong(1, voter.getId());
            DbUtils.setLimits(2, pstmt, from, to);

            return Nxt.getBlockchain().getTransactions(con, pstmt);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static DbIterator<? extends Transaction> getHoldingPendingTransactions(long holdingId, VoteWeighting.VotingModel votingModel, int from, int to) {

        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT transaction.* " +
                    "FROM transaction, phasing_poll " +
                    "WHERE phasing_poll.holding_id = ? " +
                    "AND phasing_poll.voting_model = ? " +
                    "AND phasing_poll.id = transaction.id " +
                    "AND phasing_poll.finished = FALSE " +
                    "AND phasing_poll.latest = TRUE " +
                    "ORDER BY transaction.height DESC, transaction.transaction_index DESC " +
                    DbUtils.limitsClause(from, to));
            pstmt.setLong(1, holdingId);
            pstmt.setByte(2, votingModel.getCode());
            DbUtils.setLimits(3, pstmt, from, to);

            return Nxt.getBlockchain().getTransactions(con, pstmt);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static DbIterator<? extends Transaction> getAccountPendingTransactions(Account account, int from, int to) {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT transaction.* FROM transaction, phasing_poll  " +
                    " WHERE transaction.phased = true AND (transaction.sender_id = ? OR transaction.recipient_id = ?) " +
                    " AND phasing_poll.id = transaction.id  AND phasing_poll.latest = TRUE " +
                    " AND phasing_poll.finished = FALSE ORDER BY transaction.height DESC, transaction.transaction_index DESC " +
                    DbUtils.limitsClause(from, to));
            pstmt.setLong(1, account.getId());
            pstmt.setLong(2, account.getId());
            DbUtils.setLimits(3, pstmt, from, to);

            return Nxt.getBlockchain().getTransactions(con, pstmt);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    static void addPoll(Transaction transaction, Appendix.Phasing appendix) {
        PhasingPoll poll = new PhasingPoll(transaction, appendix);
        phasingPollTable.insert(poll);
        long[] voters = poll.getWhitelist();
        if (voters.length > 0) {
            votersTable.insert(poll, Convert.toList(voters));
        }
    }

    static void init() {
    }

    private final DbKey dbKey;
    private final long[] whitelist;
    private final long quorum;
    private final byte[] fullHash;
    private boolean finished;

    private PhasingPoll(Transaction transaction, Appendix.Phasing appendix) {
        super(transaction.getId(), transaction.getSenderId(), appendix.getFinishHeight(), appendix.getVoteWeighting());
        this.dbKey = phasingPollDbKeyFactory.newKey(this.id);
        this.quorum = appendix.getQuorum();
        this.whitelist = appendix.getWhitelist();
        if (this.whitelist.length > 0) {
            Arrays.sort(this.whitelist);
        }
        this.fullHash = Convert.parseHexString(transaction.getFullHash());
    }

    private PhasingPoll(ResultSet rs) throws SQLException {
        super(rs);
        this.dbKey = phasingPollDbKeyFactory.newKey(this.id);
        this.quorum = rs.getLong("quorum");
        byte voterCount = rs.getByte("voter_count");
        this.whitelist = voterCount == 0 ? Convert.EMPTY_LONG : Convert.toArray(votersTable.get(votersDbKeyFactory.newKey(this)));
        this.fullHash = rs.getBytes("full_hash");
        this.finished = rs.getBoolean("finished");
    }

    void finish(long result) {
        if (finished) {
            throw new IllegalStateException("Poll " + Convert.toUnsignedLong(id) + " already finished");
        }
        PhasingPollResult phasingPollResult = new PhasingPollResult(this, Nxt.getBlockchain().getHeight(), result);
        resultTable.insert(phasingPollResult);
        this.finished = true;
        phasingPollTable.insert(this);
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    public long[] getWhitelist() {
        return whitelist;
    }

    public long getQuorum() {
        return quorum;
    }

    public byte[] getFullHash() {
        return fullHash;
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO phasing_poll (id, account_id, "
                + "finish_height, voter_count, voting_model, quorum, min_balance, holding_id, "
                + "min_balance_model, full_hash, finished, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, id);
            pstmt.setLong(++i, accountId);
            pstmt.setInt(++i, finishHeight);
            pstmt.setByte(++i, (byte) whitelist.length);
            pstmt.setByte(++i, defaultVoteWeighting.getVotingModel().getCode());
            pstmt.setLong(++i, quorum);
            pstmt.setLong(++i, defaultVoteWeighting.getMinBalance());
            pstmt.setLong(++i, defaultVoteWeighting.getHoldingId());
            pstmt.setByte(++i, defaultVoteWeighting.getMinBalanceModel().getCode());
            pstmt.setBytes(++i, fullHash);
            pstmt.setBoolean(++i, finished);
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }
}