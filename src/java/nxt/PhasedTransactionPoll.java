package nxt;

import nxt.db.*;
import nxt.util.Logger;
import java.sql.*;
import java.util.Arrays;


public class PhasedTransactionPoll extends CommonPollStructure {
    private final Long id;   // tx id
    private final DbKey dbKey;
    long[] possibleVoters; //todo: serialization , sorting ?
    long votes;
    long quorum;


    private static final DbKey.LongKeyFactory<PhasedTransactionPoll> pollDbKeyFactory = new DbKey.LongKeyFactory<PhasedTransactionPoll>("id") {
        @Override
        public DbKey newKey(PhasedTransactionPoll poll) {
            return poll.dbKey;
        }
    };


    final static class PendingTransactionsTable extends VersionedEntityDbTable<PhasedTransactionPoll> {

        protected PendingTransactionsTable() {
            this(pollDbKeyFactory);
        }

        protected PendingTransactionsTable(DbKey.Factory<PhasedTransactionPoll> dbKeyFactory) {
            super("pending_transactions", dbKeyFactory);
        }

        DbIterator<Long> finishing(int height){
            try {
                Connection con = Db.getConnection();
                PreparedStatement pstmt = con.prepareStatement("SELECT id FROM " + table
                        + " WHERE finish = ?  AND finished = FALSE AND latest = TRUE" );
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
        protected PhasedTransactionPoll load(Connection con, ResultSet rs) throws SQLException {
            try {
                return new PhasedTransactionPoll(rs);
            } catch (Exception e) {
                throw new SQLException(e);
            }
        }

        @Override
        protected void save(Connection con, PhasedTransactionPoll poll) throws SQLException {
            poll.save(con);
        }
    }

    final static PendingTransactionsTable pendingTransactionsTable = new PendingTransactionsTable();


    //todo: reduce boilerplate below

    public PhasedTransactionPoll(Long id, int finishBlockHeight, byte votingModel,
                                 long quorum, long voteThreshold, long assetId) {
        super(finishBlockHeight, votingModel, assetId, voteThreshold);
        this.id = id;
        this.dbKey = pollDbKeyFactory.newKey(this.id);
        this.quorum = quorum;
        possibleVoters = null;
    }

    public PhasedTransactionPoll(ResultSet rs) throws SQLException {
        super(rs);
        this.id = rs.getLong("id");
        this.quorum = rs.getLong("quorum");
        this.dbKey = pollDbKeyFactory.newKey(this.id);
    }

    public Long getId() {
        return id;
    }

    public static PhasedTransactionPoll byId(long id) {
        return pendingTransactionsTable.getBy(new DbClause.LongClause("id", id));
    }

    public Long getQuorum() {
        return quorum;
    }


    void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO pending_transactions (id, "
                + "finish,  voting_model, quorum, min_balance, asset_id, "
                + "finished, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, getId());
            pstmt.setInt(++i, getFinishBlockHeight());
            pstmt.setByte(++i, getVotingModel());
            pstmt.setLong(++i, getQuorum());
            pstmt.setLong(++i, getMinBalance());
            pstmt.setLong(++i, getAssetId());
            pstmt.setBoolean(++i, isFinished());
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }

    static boolean exists(Connection con, long id) {
        try (PreparedStatement pstmt = con.prepareStatement("SELECT COUNT (*) FROM pending_transactions where id=?")) {
            pstmt.setLong(1, id);
            ResultSet rs = pstmt.executeQuery();
            int cnt = rs.getInt(1);
            return cnt > 0;
        } catch (SQLException e) {
            Logger.logErrorMessage("PhasedPollTransaction.exists failed: ", e);
            return false;
        }
    }

    boolean addVote(Account voter) throws NxtException.NotValidException, NxtException.IllegalStateException {
        long[] voters = this.possibleVoters;
        if (voters != null && voters.length > 0 && Arrays.binarySearch(voters, voter.getId()) == -1) {
            throw new NxtException.NotValidException("Voting for tx is prohibited for " + voter);
        }

        long vote = calcWeight(voter);
        if (vote >= minBalance) {
            votes += vote;
        }

        finished = votes >= quorum;
        pendingTransactionsTable.insert(this);
        return finished;
    }
}
