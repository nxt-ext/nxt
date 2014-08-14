package nxt;

import nxt.db.CachingDbTable;
import nxt.db.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Vote {

    private static final CachingDbTable<Vote> voteTable = new CachingDbTable<Vote>() {

        @Override
        protected Long getId(Vote vote) {
            return vote.getId();
        }

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
                    + "vote_bytes, height) VALUES (?, ?, ?, ?, ?)")) {
                int i = 0;
                pstmt.setLong(++i, vote.getId());
                pstmt.setLong(++i, vote.getPollId());
                pstmt.setLong(++i, vote.getVoterId());
                pstmt.setBytes(++i, vote.getVote());
                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
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

    public static Vote getVote(Long id) {
        return voteTable.get(id);
    }

    public static Map<Long,Long> getVoters(Poll poll) {
        Map<Long,Long> map = new HashMap<>();
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM vote WHERE poll_id = ?")) {
            pstmt.setLong(1, poll.getId());
            List<Vote> votes = voteTable.getManyBy(con, pstmt);
            for (Vote vote : votes) {
                map.put(vote.getVoterId(), vote.getId());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return map;
    }

    private final Long id;
    private final Long pollId;
    private final Long voterId;
    private final byte[] voteBytes;

    private Vote(Transaction transaction, Attachment.MessagingVoteCasting attachment) {
        this.id = transaction.getId();
        this.pollId = attachment.getPollId();
        this.voterId = transaction.getSenderId();
        this.voteBytes = attachment.getPollVote();
    }

    private Vote(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.pollId = rs.getLong("poll_id");
        this.voterId = rs.getLong("voter_id");
        this.voteBytes = rs.getBytes("vote_bytes");
    }

    public Long getId() {
        return id;
    }

    public Long getPollId() { return pollId; }

    public Long getVoterId() { return voterId; }

    public byte[] getVote() { return voteBytes; }

}
