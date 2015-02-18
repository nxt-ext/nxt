package nxt;

import nxt.db.DbClause;
import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.EntityDbTable;
import nxt.db.ValuesDbTable;
import nxt.util.Convert;
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

    public static final class PollResult {

        private final String option;
        private final long result;

        private PollResult(String option, long result) {
            this.option = option;
            this.result = result;
        }

        private PollResult(ResultSet rs) throws SQLException {
            this.option = rs.getString("option");
            this.result = rs.getLong("result");
        }

        private void save(Connection con, Poll poll) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO poll_result (poll_id, "
                    + "option, result, height) VALUES (?, ?, ?, ?)")) {
                int i = 0;
                pstmt.setLong(++i, poll.getId());
                pstmt.setString(++i, getOption());
                pstmt.setLong(++i, getResult());
                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
        }

        public String getOption() {
            return option;
        }

        public long getResult() {
            return result;
        }
    }

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

    private static final ValuesDbTable<Poll, PollResult> pollResultsTable = new ValuesDbTable<Poll,PollResult>("poll_result", pollResultsDbKeyFactory) {

        @Override
        protected PollResult load(Connection con, ResultSet rs) throws SQLException {
            return new PollResult(rs);
        }

        @Override
        protected void save(Connection con, Poll poll, PollResult pollResult) throws SQLException {
            pollResult.save(con, poll);
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
                    List<PollResult> results = poll.countResults(poll.getDefaultVoteWeighting(), currentHeight);
                    pollResultsTable.insert(poll, results);
                    Logger.logDebugMessage("Poll " + Convert.toUnsignedLong(poll.getId()) + " has been finished");
                } catch (RuntimeException e) { // could happen e.g. because of overflow in safeMultiply
                    Logger.logErrorMessage("Couldn't count votes for poll " + Convert.toUnsignedLong(poll.getId()));
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

    @Override
    public boolean isFinished() { return getFinishHeight() < Nxt.getBlockchain().getHeight(); }

    public List<PollResult> getResults(VoteWeighting voteWeighting) {
        if (this.getDefaultVoteWeighting().equals(voteWeighting)) {
            return getResults();
        } else {
            return countResults(voteWeighting);
        }

    }

    public List<PollResult> getResults() {
        if (Poll.isPollsProcessing && isFinished()) {
            return pollResultsTable.get(pollResultsDbKeyFactory.newKey(id));
        } else {
            return countResults(getDefaultVoteWeighting());
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

    private List<PollResult> countResults(VoteWeighting voteWeighting) {
        int countHeight = Math.min(getFinishHeight(), Nxt.getBlockchain().getHeight());
        if (countHeight < Nxt.getBlockchainProcessor().getMinRollbackHeight()) {
            return null;
        }
        return countResults(voteWeighting, countHeight);
    }

    private List<PollResult> countResults(VoteWeighting voteWeighting, int height) {
        final long[] counts = new long[options.length];

        try (DbIterator<Vote> votes = Vote.getVotes(this.getId(), 0, -1)) {
            for (Vote vote : votes) {
                long[] partialResult = countVote(voteWeighting, vote, height);
                if (partialResult != null) {
                    for (int idx = 0; idx < partialResult.length; idx++) {
                        counts[idx] = Convert.safeAdd(counts[idx], partialResult[idx]);
                    }
                }
            }
        }

        List<PollResult> results = new ArrayList<>(options.length);
        for (int i = 0; i < options.length; i++) {
            results.add(new PollResult(options[i], counts[i]));
        }
        return results;
    }

    private long[] countVote(VoteWeighting voteWeighting, Vote vote, int height) {
        final long[] partialResult = new long[options.length];

        final long weight = voteWeighting.calcWeight(vote.getVoterId(), height);

        final byte[] optVals = vote.getVote();

        if (weight > 0) {
            for (int idx = 0; idx < optVals.length; idx++) {
                if (optVals[idx] != Constants.VOTING_NO_VOTE_VALUE) {
                    partialResult[idx] = Convert.safeMultiply(optVals[idx], weight);
                }
            }
            return partialResult;
        } else {
            return null;
        }
    }

}