package nxt;

import nxt.db.DbTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public final class Poll {

    private static final DbTable<Poll> pollTable = new DbTable<Poll>() {

        @Override
        protected String table() {
            return "poll";
        }

        @Override
        protected Poll load(Connection con, ResultSet rs) throws SQLException {
            return new Poll(rs);
        }

        @Override
        protected void save(Connection con, Poll poll) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO poll (id, name, description, "
                    + "options, min_num_options, max_num_options, binary_options) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                int i = 0;
                pstmt.setLong(++i, poll.getId());
                pstmt.setString(++i, poll.getName());
                pstmt.setString(++i, poll.getDescription());
                pstmt.setObject(++i, poll.getOptions());
                pstmt.setByte(++i, poll.getMinNumberOfOptions());
                pstmt.setByte(++i, poll.getMaxNumberOfOptions());
                pstmt.setBoolean(++i, poll.isOptionsAreBinary());
                pstmt.executeUpdate();
            }
        }

        @Override
        protected void delete(Connection con, Poll poll) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("DELETE FROM poll WHERE id = ?")) {
                pstmt.setLong(1, poll.getId());
                pstmt.executeUpdate();
            }
        }
    };

    private final Long id;
    private final String name;
    private final String description;
    private final String[] options;
    private final byte minNumberOfOptions, maxNumberOfOptions;
    private final boolean optionsAreBinary;

    private Poll(Long id, Attachment.MessagingPollCreation attachment) {
        this.id = id;
        this.name = attachment.getPollName();
        this.description = attachment.getPollDescription();
        this.options = attachment.getPollOptions();
        this.minNumberOfOptions = attachment.getMinNumberOfOptions();
        this.maxNumberOfOptions = attachment.getMaxNumberOfOptions();
        this.optionsAreBinary = attachment.isOptionsAreBinary();
    }

    private Poll(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.name = rs.getString("name");
        this.description = rs.getString("description");
        this.options = (String[])rs.getArray("options").getArray();
        this.minNumberOfOptions = rs.getByte("min_num_options");
        this.maxNumberOfOptions = rs.getByte("max_num_options");
        this.optionsAreBinary = rs.getBoolean("binary_options");
    }

    static void addPoll(Transaction transaction, Attachment.MessagingPollCreation attachment) {
        pollTable.insert(new Poll(transaction.getId(), attachment));
    }

    static void clear() {
        pollTable.truncate();
    }

    public static Poll getPoll(Long id) {
        return pollTable.get(id);
    }

    public static List<Poll> getAllPolls() {
        return pollTable.getAll();
    }

    public Long getId() {
        return id;
    }

    public String getName() { return name; }

    public String getDescription() { return description; }

    public String[] getOptions() { return options; }

    public byte getMinNumberOfOptions() { return minNumberOfOptions; }

    public byte getMaxNumberOfOptions() { return maxNumberOfOptions; }

    public boolean isOptionsAreBinary() { return optionsAreBinary; }

    public Map<Long, Long> getVoters() {
        return Vote.getVoters(this);
    }

}
