package nxt;

import nxt.db.DbTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;


public final class Vote {

    private static final DbTable<Vote> voteTable = new DbTable<Vote>() {
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
            try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO vote (id, poll_id, voter_id, "
                    + "vote_bytes) VALUES (?, ?, ?, ?)")) {
                int i = 0;
                pstmt.setLong(++i, vote.getId());
                pstmt.setLong(++i, vote.getPollId());
                pstmt.setLong(++i, vote.getVoterId());
                pstmt.setBytes(++i, vote.getVote());
                pstmt.executeUpdate();
            }
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

    public static int getCount() {
        return voteTable.getCount();
    }

    static void clear() {
        voteTable.truncate();
    }

    public static Vote getVote(Long id) {
        return voteTable.get(id);
    }

    public static List<Long> getVoters(Poll poll) {
       return voteTable.getManyIdsBy("voter_id", "poll_id", poll.getId());
    }

    public static List<Long> getVoteIds(Poll poll) {
       return voteTable.getManyIdsBy("id", "poll_id", poll.getId());
    }

    public Long getId() {
        return id;
    }

    public Long getPollId() { return pollId; }

    public Long getVoterId() { return voterId; }

    public byte[] getVote() { return vote; }

}
