package nxt;

import nxt.db.DbClause;
import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.DbUtils;
import nxt.db.ValuesDbTable;
import nxt.db.VersionedEntityDbTable;
import nxt.util.Convert;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

public class PhasingPoll extends AbstractPoll {

    private static final DbKey.LongKeyFactory<PhasingPoll> pollDbKeyFactory = new DbKey.LongKeyFactory<PhasingPoll>("id") {
        @Override
        public DbKey newKey(PhasingPoll poll) {
            return poll.dbKey;
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

    private static final VersionedEntityDbTable<PhasingPoll> phasingPollTable =
            new VersionedEntityDbTable<PhasingPoll>("phasing_poll", pollDbKeyFactory) {

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

    public static int getPendingCount() {
        return phasingPollTable.getCount(new DbClause.FixedClause("finished = FALSE"));
    }

    public static DbIterator<PhasingPoll> getFinishingBefore(int height) {
        return phasingPollTable.getManyBy(new DbClause.IntClause("finish_height", DbClause.Op.LT, height), 0, Integer.MAX_VALUE);
    }

    public static PhasingPoll getPoll(long id) {
        return phasingPollTable.getBy(new DbClause.LongClause("id", id));
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
                    + "ORDER BY transaction.height DESC"
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
                    "ORDER BY transaction.height DESC " +
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

    public static DbIterator<? extends Transaction> getAccountPendingTransactions(Account sender, int from, int to) {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT transaction.* FROM transaction, phasing_poll " +
                    " WHERE phasing_poll.account_id = ? AND phasing_poll.id = transaction.id " +
                    " AND phasing_poll.finished = FALSE AND phasing_poll.latest = TRUE " +
                    " ORDER BY transaction.height DESC " +
                    DbUtils.limitsClause(from, to));
            pstmt.setLong(1, sender.getId());
            DbUtils.setLimits(2, pstmt, from, to);

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

    private final long id;
    private final DbKey dbKey;
    private final long[] whitelist;
    private final long quorum;
    private final byte[] fullHash;
    private boolean finished;

    private PhasingPoll(Transaction transaction, Appendix.Phasing appendix) {
        super(transaction.getSenderId(), appendix.getFinishHeight(), appendix.getVotingModel(), appendix.getHoldingId(),
                appendix.getMinBalance(), appendix.getMinBalanceModel());
        this.id = transaction.getId();
        this.dbKey = pollDbKeyFactory.newKey(this.id);
        this.quorum = appendix.getQuorum();
        this.whitelist = appendix.getWhitelist();
        if (this.whitelist.length > 0) {
            Arrays.sort(this.whitelist);
        }
        this.fullHash = Convert.parseHexString(transaction.getFullHash());
    }

    private PhasingPoll(ResultSet rs) throws SQLException {
        super(rs);
        this.id = rs.getLong("id");
        this.quorum = rs.getLong("quorum");
        this.dbKey = pollDbKeyFactory.newKey(this.id);
        byte voterCount = rs.getByte("voter_count");
        this.whitelist = voterCount == 0 ? Convert.EMPTY_LONG : Convert.toArray(votersTable.get(votersDbKeyFactory.newKey(this)));
        this.fullHash = rs.getBytes("full_hash");
        this.finished = rs.getBoolean("finished");
    }

    void finish() {
        this.finished = true;
        phasingPollTable.insert(this);
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
            pstmt.setLong(++i, getId());
            pstmt.setLong(++i, getAccountId());
            pstmt.setInt(++i, getFinishHeight());
            pstmt.setByte(++i, (byte) getWhitelist().length);
            pstmt.setByte(++i, getDefaultVoteWeighting().getVotingModel().getCode());
            pstmt.setLong(++i, getQuorum());
            pstmt.setLong(++i, getDefaultVoteWeighting().getMinBalance());
            pstmt.setLong(++i, getDefaultVoteWeighting().getHoldingId());
            pstmt.setByte(++i, getDefaultVoteWeighting().getMinBalanceModel().getCode());
            pstmt.setBytes(++i, getFullHash());
            pstmt.setBoolean(++i, isFinished());
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }
}
