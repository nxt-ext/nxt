package nxt;

import nxt.db.DbClause;
import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.DbUtils;
import nxt.db.EntityDbTable;
import nxt.db.ValuesDbTable;
import nxt.util.Listener;
import nxt.util.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Poll extends AbstractPoll {

    private static final boolean isPollsProcessing = Nxt.getBooleanProperty("nxt.processPolls");

    private static final DbKey.LongKeyFactory<Poll> pollDbKeyFactory = new DbKey.LongKeyFactory<Poll>("id") {
        @Override
        public DbKey newKey(Poll poll) {
            return poll.dbKey;
        }
    };

    private final static EntityDbTable<Poll> pollTable = new EntityDbTable<Poll>("poll", pollDbKeyFactory) {

        @Override
        protected Poll load(Connection con, ResultSet rs) throws SQLException {
            return new Poll(rs);
        }

        @Override
        protected void save(Connection con, Poll poll) throws SQLException {
            poll.save(con);
        }
    };

    private static final DbKey.LongKeyFactory<Poll> pollResultsDbKeyFactory = new DbKey.LongKeyFactory<Poll>("poll_id") {
        @Override
        public DbKey newKey(Poll poll) {
            return poll.dbKey;
        }
    };

    private static final ValuesDbTable<Poll, Long> pollResultsTable = new ValuesDbTable<Poll, Long>("poll_result", pollResultsDbKeyFactory) {

        @Override
        protected Long load(Connection con, ResultSet rs) throws SQLException {
            long result = rs.getLong("result");
            return rs.wasNull() ? null : result;
        }

        @Override
        protected void save(Connection con, Poll poll, Long result) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO poll_result (poll_id, "
                    + "result, height) VALUES (?, ?, ?)")) {
                int i = 0;
                pstmt.setLong(++i, poll.getId());
                DbUtils.setLong(pstmt, ++i, result);
                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
        }
    };

    public static Poll getPoll(long id) {
        return pollTable.get(pollDbKeyFactory.newKey(id));
    }

    public static DbIterator<Poll> getPollsFinishingAtOrBefore(int height) {
        return pollTable.getManyBy(new DbClause.IntClause("finish_height", DbClause.Op.LTE, height), 0, Integer.MAX_VALUE);
    }

    public static DbIterator<Poll> getAllPolls(int from, int to) {
        return pollTable.getAll(from, to);
    }

    public static DbIterator<Poll> getPollsByAccount(long accountId, int from, int to) {
        return pollTable.getManyBy(new DbClause.LongClause("account_id", accountId), from, to);
    }

    public static DbIterator<Poll> getPollsFinishingAt(int height) {
        return pollTable.getManyBy(new DbClause.IntClause("finish_height", height), 0, Integer.MAX_VALUE);
    }

    public static DbIterator<Poll> searchPolls(String query, boolean includeFinished, int from, int to) {
        DbClause dbClause = includeFinished ? DbClause.EMPTY_CLAUSE : new DbClause.IntClause("finish_height", DbClause.Op.GT, Nxt.getBlockchain().getHeight());
        return pollTable.search(query, dbClause, from, to, " ORDER BY ft.score DESC, poll.height DESC ");
    }

    public static int getCount() {
        return pollTable.getCount();
    }

    static void addPoll(Transaction transaction, Attachment.MessagingPollCreation attachment) {
        Poll poll = new Poll(transaction, attachment);
        pollTable.insert(poll);
    }

    static void init() {}

    static {
        if (Poll.isPollsProcessing) {
            Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
                @Override
                public void notify(Block block) {
                    int height = block.getHeight();
                    if (height >= Constants.VOTING_SYSTEM_BLOCK) {
                        Poll.checkPolls(height);
                    }
                }
            }, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
        }
    }

    private static void checkPolls(int currentHeight) {
        try (DbIterator<Poll> polls = getPollsFinishingAt(currentHeight)) {
            for (Poll poll : polls) {
                try {
                    List<Long> results = poll.countResults(poll.getDefaultVoteWeighting(), currentHeight);
                    pollResultsTable.insert(poll, results);
                    Logger.logDebugMessage("Poll " + Long.toUnsignedString(poll.getId()) + " has been finished");
                } catch (RuntimeException e) { // could happen e.g. because of overflow in safeMultiply
                    Logger.logErrorMessage("Couldn't count votes for poll " + Long.toUnsignedString(poll.getId()));
                }
            }
        }
    }

    private final DbKey dbKey;
    private final String name;
    private final String description;
    private final String[] options;
    private final byte minNumberOfOptions;
    private final byte maxNumberOfOptions;
    private final byte minRangeValue;
    private final byte maxRangeValue;

    private Poll(Transaction transaction, Attachment.MessagingPollCreation attachment) {
        super(transaction.getId(), transaction.getSenderId(), attachment.getFinishHeight(), attachment.getVoteWeighting());
        this.dbKey = pollDbKeyFactory.newKey(this.id);
        this.name = attachment.getPollName();
        this.description = attachment.getPollDescription();
        this.options = attachment.getPollOptions();
        this.minNumberOfOptions = attachment.getMinNumberOfOptions();
        this.maxNumberOfOptions = attachment.getMaxNumberOfOptions();
        this.minRangeValue = attachment.getMinRangeValue();
        this.maxRangeValue = attachment.getMaxRangeValue();
    }

    private Poll(ResultSet rs) throws SQLException {
        super(rs);
        this.dbKey = pollDbKeyFactory.newKey(this.id);
        this.name = rs.getString("name");
        this.description = rs.getString("description");

        Object[] array = (Object[])rs.getArray("options").getArray();
        this.options = Arrays.copyOf(array, array.length, String[].class);
        this.minNumberOfOptions = rs.getByte("min_num_options");
        this.maxNumberOfOptions = rs.getByte("max_num_options");
        this.minRangeValue = rs.getByte("min_range_value");
        this.maxRangeValue = rs.getByte("max_range_value");
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO poll (id, account_id, "
                + "name, description, options, finish_height, voting_model, min_balance, min_balance_model, "
                + "holding_id, min_num_options, max_num_options, min_range_value, max_range_value, height) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, id);
            pstmt.setLong(++i, accountId);
            pstmt.setString(++i, name);
            pstmt.setString(++i, description);
            pstmt.setObject(++i, options);
            pstmt.setInt(++i, finishHeight);
            pstmt.setByte(++i, defaultVoteWeighting.getVotingModel().getCode());
            pstmt.setLong(++i, defaultVoteWeighting.getMinBalance());
            pstmt.setByte(++i, defaultVoteWeighting.getMinBalanceModel().getCode());
            pstmt.setLong(++i, defaultVoteWeighting.getHoldingId());
            pstmt.setByte(++i, minNumberOfOptions);
            pstmt.setByte(++i, maxNumberOfOptions);
            pstmt.setByte(++i, minRangeValue);
            pstmt.setByte(++i, maxRangeValue);
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }

    public List<Long> getResults(VoteWeighting voteWeighting) {
        if (defaultVoteWeighting.equals(voteWeighting)) {
            return getResults();
        } else {
            return countResults(voteWeighting);
        }

    }

    public List<Long> getResults() {
        if (Poll.isPollsProcessing && isFinished()) {
            return pollResultsTable.get(pollResultsDbKeyFactory.newKey(id));
        } else {
            return countResults(defaultVoteWeighting);
        }
    }

    public DbIterator<Vote> getVotes(){
        return Vote.getVotes(this.getId(), 0, -1);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String[] getOptions() {
        return options;
    }


    public byte getMinNumberOfOptions() {
        return minNumberOfOptions;
    }

    public byte getMaxNumberOfOptions() {
        return maxNumberOfOptions;
    }

    public byte getMinRangeValue() {
        return minRangeValue;
    }

    public byte getMaxRangeValue() {
        return maxRangeValue;
    }

    private List<Long> countResults(VoteWeighting voteWeighting) {
        int countHeight = Math.min(finishHeight, Nxt.getBlockchain().getHeight());
        if (countHeight < Nxt.getBlockchainProcessor().getMinRollbackHeight()) {
            return null;
        }
        return countResults(voteWeighting, countHeight);
    }

    private List<Long> countResults(VoteWeighting voteWeighting, int height) {
        final long[] result = new long[options.length];
        Arrays.fill(result, Long.MIN_VALUE);
        try (DbIterator<Vote> votes = Vote.getVotes(this.getId(), 0, -1)) {
            for (Vote vote : votes) {
                long[] partialResult = countVote(voteWeighting, vote, height);
                if (partialResult != null) {
                    for (int idx = 0; idx < partialResult.length; idx++) {
                        if (partialResult[idx] != Long.MIN_VALUE) {
                            result[idx] = result[idx] == Long.MIN_VALUE ? partialResult[idx] : result[idx] + partialResult[idx];
                        }
                    }
                }
            }
        }
        List<Long> list = new ArrayList<>();
        for (long r : result) {
            list.add(r == Long.MIN_VALUE ? null : r);
        }
        return list;
    }

    private long[] countVote(VoteWeighting voteWeighting, Vote vote, int height) {
        final long weight = voteWeighting.calcWeight(vote.getVoterId(), height);
        if (weight <= 0) {
            return null;
        }
        final long[] partialResult = new long[options.length];
        final byte[] optVals = vote.getVote();
        for (int idx = 0; idx < optVals.length; idx++) {
            if (optVals[idx] != Constants.VOTING_NO_VOTE_VALUE) {
                partialResult[idx] = (long) optVals[idx] * weight;
            } else {
                partialResult[idx] = Long.MIN_VALUE;
            }
        }
        return partialResult;
    }

}