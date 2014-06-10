package nxt;

import nxt.util.DbTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Vote {

    private static final DbTable<Vote> voteTable = new DbTable<Vote>() {

        @Override
        protected String table() {
            return "vote";
        }

        @Override
        protected Vote load(Connection con, ResultSet rs) throws SQLException {
            Long id = rs.getLong("id");
            Long pollId = rs.getLong("poll_id");
            Long voterId = rs.getLong("voter_id");
            byte[] voteBytes = rs.getBytes("vote_bytes");
            return new Vote(id, pollId, voterId, voteBytes);
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

    static Vote addVote(Long id, Long pollId, Long voterId, byte[] voteBytes) {
        Vote vote = new Vote(id, pollId, voterId, voteBytes);
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

    private Vote(Long id, Long pollId, Long voterId, byte[] voteBytes) {
        this.id = id;
        this.pollId = pollId;
        this.voterId = voterId;
        this.voteBytes = voteBytes;
    }

    public Long getId() {
        return id;
    }

    public Long getPollId() { return pollId; }

    public Long getVoterId() { return voterId; }

    public byte[] getVote() { return voteBytes; }

}
