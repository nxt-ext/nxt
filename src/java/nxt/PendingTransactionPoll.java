package nxt;

import nxt.db.*;
import nxt.util.Logger;
import java.sql.*;
import java.util.Arrays;

public class PendingTransactionPoll extends AbstractPoll {
    private final Long id;
    private final DbKey dbKey;
    private long[] possibleVoters;
    private long quorum;

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
                                  long assetId, long[] possibleVoters) {
        super(accountId, finishBlockHeight, votingModel, assetId, voteThreshold);
        this.id = id;
        this.dbKey = pollDbKeyFactory.newKey(this.id);
        this.quorum = quorum;
        this.possibleVoters = possibleVoters;
        if (this.possibleVoters != null) {
            Arrays.sort(this.possibleVoters);
        }
    }

    public PendingTransactionPoll(ResultSet rs) throws SQLException {
        super(rs);
        this.id = rs.getLong("id");
        this.quorum = rs.getLong("quorum");
        this.dbKey = pollDbKeyFactory.newKey(this.id);

        String votersCombined = rs.getString("possible_voters");
        if (votersCombined.isEmpty()) {
            this.possibleVoters = null;
        } else {
            String[] voterStrings = votersCombined.split(",");
            this.possibleVoters = new long[voterStrings.length];
            for (int i = 0; i < voterStrings.length; i++) {
                this.possibleVoters[i] = Long.parseLong(voterStrings[i]);
            }
            Arrays.sort(this.possibleVoters);
        }
    }

    public Long getId() {
        return id;
    }

    public static PendingTransactionPoll byId(long id) {
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
        if (getPossibleVoters() != null) {
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

    public static void finishPoll(PendingTransactionPoll poll) {
        poll.setFinished(true);
        pendingTransactionsTable.insert(poll);
    }

    static boolean exists(long id) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT COUNT (*) FROM pending_transactions where id=?")) {
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
