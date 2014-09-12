package nxt;

import nxt.db.Db;
import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.EntityDbTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public final class Vote {

    private static final DbKey.LongKeyFactory<Vote> voteDbKeyFactory = new DbKey.LongKeyFactory<Vote>("id") {

        @Override
        public DbKey newKey(Vote vote) {
            return vote.dbKey;
        }

    };

    private static final EntityDbTable<Vote> voteTable = new EntityDbTable<Vote>(voteDbKeyFactory) {

        @Override
        protected String table() {
            return "vote";
        }

        @Override
        protected Vote load(Connection con, ResultSet rs) throws SQLException {
            return new Vote(rs);
        }

        @Override
        protected void save(Connection con, Vote vote) throws SQLException {
            vote.save(con);
        }

        @Override
        protected void delete(Connection con, Vote vote) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("DELETE FROM vote WHERE id = ?")) {
                pstmt.setLong(1, vote.getId());
                pstmt.executeUpdate();
            }
        }
    };


    private final Long id;
    private final Long pollId;
    private final Long voterId;
    private final byte[] vote; //vote[i]==0 means no(or not being chosen), 1 means yes(or an option being chosen)
    private final DbKey dbKey;

    private Vote(Transaction transaction, Attachment.MessagingVoteCasting attachment) {
        this.id = transaction.getId();
        this.pollId = attachment.getPollId();
        this.voterId = transaction.getSenderId();
        this.vote = attachment.getPollVote();
    }

    private Vote(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.pollId = rs.getLong("poll_id");
        this.voterId = rs.getLong("voter_id");
        this.vote = rs.getBytes("vote_bytes");
    }

    static Vote addVote(Transaction transaction, Attachment.MessagingVoteCasting attachment) {
        Vote vote = new Vote(transaction, attachment);
        voteTable.insert(vote);
        return vote;
    }

    static void clear() {
        voteTable.truncate();
    }

    public static int getCount() {
        return voteTable.getCount();
    }

    static void clear() {
        voteTable.truncate();
    }

    public static Vote getVote(Long id) {
        return voteTable.get(voteDbKeyFactory.newKey(id));
    }

    public static List<Long> getVoters(Poll poll) {
       return voteTable.getManyIdsBy("voter_id", "poll_id", poll.getId());
    }

    static void init() {}


    private final Long id;
    private final DbKey dbKey;
    private final Long pollId;
    private final Long voterId;
    private final byte[] voteBytes;

    private Vote(Transaction transaction, Attachment.MessagingVoteCasting attachment) {
        this.id = transaction.getId();
        this.dbKey = voteDbKeyFactory.newKey(this.id);
        this.pollId = attachment.getPollId();
        this.voterId = transaction.getSenderId();
        this.voteBytes = attachment.getPollVote();
    public static List<Long> getVoteIds(Poll poll) {
       return voteTable.getManyIdsBy("id", "poll_id", poll.getId());
    }

    private Vote(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.dbKey = voteDbKeyFactory.newKey(this.id);
        this.pollId = rs.getLong("poll_id");
        this.voterId = rs.getLong("voter_id");
        this.voteBytes = rs.getBytes("vote_bytes");
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO vote (id, poll_id, voter_id, "
                + "vote_bytes, height) VALUES (?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, this.getId());
            pstmt.setLong(++i, this.getPollId());
            pstmt.setLong(++i, this.getVoterId());
            pstmt.setBytes(++i, this.getVote());
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getPollId() { return pollId; }

    public Long getVoterId() { return voterId; }

    public byte[] getVote() { return vote; }

}
