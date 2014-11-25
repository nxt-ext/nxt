package nxt;

import nxt.db.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

public class VotePhased {
    //TODO: why does pending_transaction_id need to be part of the key?
    // id is the id of the vote, and it is already unique as it is a transaction id
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

        protected long lastEstimate(long pendingTransactionId) {
            try {
                Connection con = db.getConnection();
                PreparedStatement pstmt = con.prepareStatement("SELECT estimated_total FROM " + table
                        + " WHERE pending_transaction_id = ?  ORDER BY db_id DESC LIMIT 1");
                pstmt.setLong(1, pendingTransactionId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getLong(1);
                } else {
                    return 0;
                }
                //TODO: con, pstmt and rs must be closed
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }
    }

    final static VotePhasedTable voteTable = new VotePhasedTable();


    static void init() {
    }

    private final long id;
    private final DbKey dbKey;
    private final long pendingTransactionId;
    private final long voterId;
    private final long estimatedTotal;

    private VotePhased(Transaction transaction, long pendingTransactionId, long estimatedTotal) {
        this.id = transaction.getId();
        this.pendingTransactionId = pendingTransactionId;
        this.dbKey = voteDbKeyFactory.newKey(this.id, this.pendingTransactionId);
        this.voterId = transaction.getSenderId();
        this.estimatedTotal = estimatedTotal;
    }

    private VotePhased(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.pendingTransactionId = rs.getLong("pending_transaction_id");
        this.dbKey = voteDbKeyFactory.newKey(this.id, this.pendingTransactionId);
        this.voterId = rs.getLong("voter_id");
        this.estimatedTotal = rs.getLong("estimated_total");
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

    //TODO: this method needs a better name, why is it not called simply countVotes?
    static long allVotesFromDb(PendingTransactionPoll poll) {
        long result = 0;
        DbClause clause = new DbClause.LongClause("pending_transaction_id", poll.getId());
        //TODO: DbIterators must be closed
        DbIterator<VotePhased> votesIterator = voteTable.getManyBy(clause, 0, -1);

        while (votesIterator.hasNext()) {
            //TODO: when you change calcWeight to take accountId instead of Account, you can skip the getAccount call here
            // and only get the Account if needed in calcWeight
            long w = AbstractPoll.calcWeight(poll, Account.getAccount(votesIterator.next().voterId));
            if (w >= poll.minBalance) {
                result += w;
            }
        }

        return result;
    }

    static boolean addVote(PendingTransactionPoll poll, Account voter,
                           Transaction transaction) {

        //TODO: why that duplicate isVoteGiven check here again? this should have been checked in validateAttachment
        if (!isVoteGiven(poll.getId(), voter.getId())) {

            long[] whitelist = poll.getWhitelist();
            if (whitelist != null && whitelist.length > 0 && Arrays.binarySearch(whitelist, voter.getId()) == -1) {
                return false; //todo: move to validate only? - yes
            }

            long[] blacklist = poll.getBlacklist();
            if (blacklist != null && blacklist.length > 0 && Arrays.binarySearch(blacklist, voter.getId()) != -1) {
                return false; //todo: move to validate only? - yes
            }

            long weight = AbstractPoll.calcWeight(poll, voter);

            long estimate = voteTable.lastEstimate(poll.getId());

            if (weight >= poll.minBalance) {
                estimate += weight;
            }

            if (estimate >= poll.getQuorum() && poll.getVotingModel() != Constants.VOTING_MODEL_ACCOUNT) {
                estimate = allVotesFromDb(poll);
                if (weight >= poll.minBalance) {
                    estimate += weight;
                }
            }

            VotePhased vote = new VotePhased(transaction, poll.getId(), estimate);
            voteTable.insert(vote);
            return estimate >= poll.getQuorum();
        } else {
            return false;
        }
    }

    static boolean isVoteGiven(long pendingTransactionId, long voterId){
        DbClause clause = new DbClause.LongLongClause("pending_transaction_id", pendingTransactionId,
                                                        "voter_id", voterId);
        return voteTable.getCount(clause) > 0;
    }
}
