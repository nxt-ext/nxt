package nxt;

import nxt.db.*;
import nxt.util.Logger;
import java.sql.*;

public class PhasedTransactionPoll extends CommonPollStructure {
    private final Long id;   // tx id
    private final DbKey dbKey;
    private long[] possibleVoters;
    private long quorum;

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

    static void init() {}

    public PhasedTransactionPoll(Long id, long accountId, int finishBlockHeight,
                                 byte votingModel, long quorum, long voteThreshold,
                                 long assetId, long[] possibleVoters) {
        super(accountId, finishBlockHeight, votingModel, assetId, voteThreshold);
        this.id = id;
        this.dbKey = pollDbKeyFactory.newKey(this.id);
        this.quorum = quorum;
        this.possibleVoters = possibleVoters;
    }

    public PhasedTransactionPoll(ResultSet rs) throws SQLException {
        super(rs);
        this.id = rs.getLong("id");
        this.quorum = rs.getLong("quorum");
        this.dbKey = pollDbKeyFactory.newKey(this.id);

        String votersCombined = rs.getString("possible_voters");
        if(votersCombined.isEmpty()){
            this.possibleVoters = null;
        }else {
            String[] voterStrings = votersCombined.split(",");
            this.possibleVoters = new long[voterStrings.length];
            for (int i = 0; i < voterStrings.length; i++) {
                this.possibleVoters[i] = Long.parseLong(voterStrings[i]);
            }
        }
    }

    public Long getId() {
        return id;
    }

    public static PhasedTransactionPoll byId(long id) {
        return pendingTransactionsTable.getBy(new DbClause.LongClause("id", id));
    }

    public long[] getPossibleVoters() {
        return possibleVoters;
    }

    public Long getQuorum() {
        return quorum;
    }


    void save(Connection con) throws SQLException {
        String voters = "";
        if(getPossibleVoters()!=null) {
            StringBuilder votersBuilder = new StringBuilder();
            for (long voter : getPossibleVoters()) {
                votersBuilder.append(voter);
                votersBuilder.append(",");
            }
            voters = votersBuilder.length() > 0 ? votersBuilder.substring(0, votersBuilder.length() - 1) : "";
        }

        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO pending_transactions (id, account_id, "
                + "finish,  possible_voters, voting_model, quorum, min_balance, asset_id, "
                + "finished, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, getId());
            pstmt.setLong(++i, getAccountId());
            pstmt.setInt(++i, getFinishBlockHeight());
            pstmt.setString(++i, voters);
            pstmt.setByte(++i, getVotingModel());
            pstmt.setLong(++i, getQuorum());
            pstmt.setLong(++i, getMinBalance());
            pstmt.setLong(++i, getAssetId());
            pstmt.setBoolean(++i, isFinished());
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }

    public static void finishPoll(PhasedTransactionPoll poll) {
        poll.setFinished(true);
        pendingTransactionsTable.insert(poll);
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
}
