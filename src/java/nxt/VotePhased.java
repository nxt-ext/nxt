package nxt;

import nxt.db.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

public class VotePhased {
    private static final DbKey.LongKeyFactory<VotePhased> voteDbKeyFactory = new DbKey.LongKeyFactory<VotePhased>("id") {
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

        protected long lastEstimate(long pendingTransactionId){
            try {
                Connection con = Db.getConnection();
                PreparedStatement pstmt = con.prepareStatement("SELECT estimated_total FROM " + table
                        + " WHERE pending_transaction_id = ?  ORDER BY db_id DESC LIMIT 1");
                pstmt.setLong(1, pendingTransactionId);
                ResultSet rs = pstmt.executeQuery();
                if(rs.next()){
                    return rs.getLong(1);
                }else{
                    return 0;
                }
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }
    }

    final static VotePhasedTable voteTable = new VotePhasedTable();


    static void init() {}

    private final long id;
    private final DbKey dbKey;
    private final long pendingTransactionId;
    private final long voterId;
    private final long estimatedTotal;

    private VotePhased(Transaction transaction, Attachment.PendingPaymentVoteCasting attachment, long estimatedTotal) {
        this.id = transaction.getId();
        this.dbKey = voteDbKeyFactory.newKey(this.id);
        this.pendingTransactionId = attachment.getPendingTransactionId();
        this.voterId = transaction.getSenderId();
        this.estimatedTotal = estimatedTotal;
    }

    private VotePhased(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.dbKey = voteDbKeyFactory.newKey(this.id);
        this.pendingTransactionId = rs.getLong("pending_transaction_id");
        this.voterId = rs.getLong("voter_id");
        this.estimatedTotal =  rs.getLong("estimated_total");
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
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO vote_phased (id, pending_transaction_id, voter_id, "
                + "estimated_total, height) VALUES (?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, this.getId());
            pstmt.setLong(++i, this.getPendingTransactionId());
            pstmt.setLong(++i, this.getVoterId());
            pstmt.setLong(++i, this.estimatedTotal);
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }

    static boolean addVote(PhasedTransactionPoll poll, Account voter,
                        Transaction transaction, Attachment.PendingPaymentVoteCasting attachment)
            throws NxtException.IllegalStateException {

        long[] voters = poll.getPossibleVoters();
        if (voters != null && voters.length > 0 && Arrays.binarySearch(voters, voter.getId()) == -1) {
            return false; //todo: move to validate only?
        }

        long weight = CommonPollStructure.calcWeight(poll, voter);

        long estimate = voteTable.lastEstimate(poll.getId()) + weight;

        if(estimate >= poll.getQuorum() && poll.getVotingModel() != CommonPollStructure.VOTING_MODEL_ACCOUNT){
            DbClause clause = new DbClause.LongClause("pending_transaction_id", poll.getId());
            DbIterator<VotePhased> votesIterator = voteTable.getManyBy(clause, 0, -1);
            estimate = 0;
            while(votesIterator.hasNext()){
                long w = CommonPollStructure.calcWeight(poll, Account.getAccount(votesIterator.next().voterId));
                if(w >= poll.minBalance) {
                    estimate += w;
                }
            }
            if(weight >= poll.minBalance){
                estimate += weight;
            }
        }

        VotePhased vote = new VotePhased(transaction, attachment, estimate);
        voteTable.insert(vote);
        return estimate >= poll.getQuorum();
    }
}
