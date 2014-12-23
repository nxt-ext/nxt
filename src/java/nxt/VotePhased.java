package nxt;

import nxt.db.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class VotePhased {
    //TODO: why does pending_transaction_id need to be part of the key?
    // id is the id of the vote, and it is already unique as it is a transaction id
    //
    //edit: I didn't realize you allow voting for multiple pending transactions with a single transaction.
    // But I don't like this either, first it leads to code complication,
    // second it couples unrelated pending transactions and will lead to special cases that need to be considered
    // I would remove this vanity feature and have one vote per transaction only.
    private static final DbKey.LinkKeyFactory<VotePhased> voteDbKeyFactory =
            new DbKey.LinkKeyFactory<VotePhased>("id", "pending_transaction_id") {
                @Override
                public DbKey newKey(VotePhased vote) {
                    return vote.dbKey;
                }
            };

    private static final class VotePhasedTable extends EntityDbTable<VotePhased> {
        protected VotePhasedTable() {
            super("vote_phased", voteDbKeyFactory);
        }

        @Override
        protected VotePhased load(Connection con, ResultSet rs) throws SQLException {
            return new VotePhased(rs);
        }

        @Override
        protected void save(Connection con, VotePhased vote) throws SQLException {
            vote.save(con);
        }
    }

    final static VotePhasedTable voteTable = new VotePhasedTable();


    static void init() {
    }

    private final long id;
    private final DbKey dbKey;
    private final long pendingTransactionId;
    private final long voterId;

    private VotePhased(Transaction transaction, long pendingTransactionId) {
        this.id = transaction.getId();
        this.pendingTransactionId = pendingTransactionId;
        this.dbKey = voteDbKeyFactory.newKey(this.id, this.pendingTransactionId);
        this.voterId = transaction.getSenderId();
    }

    private VotePhased(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.pendingTransactionId = rs.getLong("pending_transaction_id");
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

    protected void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO vote_phased (id, pending_transaction_id, "
                + "voter_id, height) VALUES (?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, this.getId());
            pstmt.setLong(++i, this.getPendingTransactionId());
            pstmt.setLong(++i, this.getVoterId());
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }

    public static DbIterator<VotePhased> getByTransaction(long pendingTransactionId, int from, int to){
        return voteTable.getManyBy(new DbClause.LongClause("pending_transaction_id",pendingTransactionId), from, to);
    }

    public static int getCount(long pendingTransactionId){
        return voteTable.getCount(new DbClause.LongClause("pending_transaction_id",pendingTransactionId));
    }

    static boolean addVote(PendingTransactionPoll poll, Transaction transaction) {
        voteTable.insert(new VotePhased(transaction, poll.getId()));
        return poll.getVotingModel() == Constants.VOTING_MODEL_ACCOUNT
                && VotePhased.getCount(poll.getId()) >= poll.getQuorum();
    }

    static boolean isVoteGiven(long pendingTransactionId, long voterId) {
        DbClause clause = new DbClause.LongLongClause("pending_transaction_id", pendingTransactionId, "voter_id", voterId);
        return voteTable.getCount(clause) > 0;
    }
}
