package nxt;

import nxt.db.DbClause;
import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.EntityDbTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PhasingVote {

    private static final DbKey.LinkKeyFactory<PhasingVote> voteDbKeyFactory =
            new DbKey.LinkKeyFactory<PhasingVote>("id", "transaction_id") {
                @Override
                public DbKey newKey(PhasingVote vote) {
                    return vote.dbKey;
                }
            };

    private static final EntityDbTable<PhasingVote> votePhasedTable = new EntityDbTable<PhasingVote>("phasing_vote", voteDbKeyFactory) {

        @Override
        protected PhasingVote load(Connection con, ResultSet rs) throws SQLException {
            return new PhasingVote(rs);
        }

        @Override
        protected void save(Connection con, PhasingVote vote) throws SQLException {
            vote.save(con);
        }

    };

    public static DbIterator<PhasingVote> getByTransaction(long pendingTransactionId, int from, int to) {
        return votePhasedTable.getManyBy(new DbClause.LongClause("transaction_id", pendingTransactionId), from, to);
    }

    public static long countVotes(PhasingPoll poll) {
        if (poll.getDefaultVoteWeighting().getVotingModel() == Constants.VOTING_MODEL_ACCOUNT && poll.getDefaultVoteWeighting().getMinBalance() == 0) {
            return votePhasedTable.getCount(new DbClause.LongClause("transaction_id", poll.getId()));
        }
        long cumulativeWeight = 0;
        try (DbIterator<PhasingVote> votes = PhasingVote.getByTransaction(poll.getId(), 0, Integer.MAX_VALUE)) {
            for (PhasingVote vote : votes) {
                cumulativeWeight += poll.getDefaultVoteWeighting().calcWeight(vote.getVoterId(), Math.min(poll.getFinishHeight(), Nxt.getBlockchain().getHeight()));
            }
        }
        return cumulativeWeight;
    }

    static boolean addVote(PhasingPoll poll, Transaction transaction) {
        votePhasedTable.insert(new PhasingVote(transaction, poll.getId()));
        return poll.getDefaultVoteWeighting().getVotingModel() == Constants.VOTING_MODEL_ACCOUNT && poll.getDefaultVoteWeighting().getMinBalance() == 0
                && votePhasedTable.getCount(new DbClause.LongClause("transaction_id", poll.getId())) >= poll.getQuorum();
    }

    static boolean isVoteGiven(long pendingTransactionId, long voterId) {
        DbClause clause = new DbClause.LongClause("transaction_id", pendingTransactionId).and(new DbClause.LongClause("voter_id", voterId));
        return votePhasedTable.getCount(clause) > 0;
    }

    static void init() {
    }

    private final long id;
    private final DbKey dbKey;
    private final long pendingTransactionId;
    private final long voterId;

    private PhasingVote(Transaction transaction, long pendingTransactionId) {
        this.id = transaction.getId();
        this.pendingTransactionId = pendingTransactionId;
        this.dbKey = voteDbKeyFactory.newKey(this.id, this.pendingTransactionId);
        this.voterId = transaction.getSenderId();
    }

    private PhasingVote(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.pendingTransactionId = rs.getLong("transaction_id");
        this.dbKey = voteDbKeyFactory.newKey(this.id, this.pendingTransactionId);
        this.voterId = rs.getLong("voter_id");
    }

    public long getId() {
        return id;
    }

    public long getPendingTransactionId() {
        return pendingTransactionId;
    }

    public long getVoterId() {
        return voterId;
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO phasing_vote (id, transaction_id, "
                + "voter_id, height) VALUES (?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, this.getId());
            pstmt.setLong(++i, this.getPendingTransactionId());
            pstmt.setLong(++i, this.getVoterId());
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }

}
