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

    private static final DbKey.LongKeyFactory<Poll> pollDbKeyFactory = new DbKey.LongKeyFactory<Poll>("id") {
        @Override
        public DbKey newKey(Poll poll) {
            return poll.dbKey;
        }
    };

    private static final DbKey.LongKeyFactory<Poll> pollResultsDbKeyFactory = new DbKey.LongKeyFactory<Poll>("poll_id") {
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
            try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO poll (id, account_id, "
                    + "name, description, options, finish_height, voting_model, min_balance, min_balance_model, "
                    + "holding_id, min_num_options, max_num_options, min_range_value, max_range_value, height) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                int i = 0;
                pstmt.setLong(++i, poll.getId());
                pstmt.setLong(++i, poll.getAccountId());
                pstmt.setString(++i, poll.getName());
                pstmt.setString(++i, poll.getDescription());
                pstmt.setObject(++i, poll.getOptions());
                pstmt.setInt(++i, poll.getFinishHeight());
                pstmt.setByte(++i, poll.getVotingModel());
                pstmt.setLong(++i, poll.getMinBalance());
                pstmt.setByte(++i, poll.getMinBalanceModel());
                pstmt.setLong(++i, poll.getHoldingId());
                pstmt.setByte(++i, poll.getMinNumberOfOptions());
                pstmt.setByte(++i, poll.getMaxNumberOfOptions());
                pstmt.setByte(++i, poll.getMinRangeValue());
                pstmt.setByte(++i, poll.getMaxRangeValue());
                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
        }
    };

    private static final ValuesDbTable<Poll, PartialPollResult> pollResultsTable = new ValuesDbTable<Poll,PartialPollResult>("poll_result", pollResultsDbKeyFactory) {

        @Override
        protected PartialPollResult load(Connection con, ResultSet rs) throws SQLException {
            return new PartialPollResult(rs.getString("option"), rs.getLong("result"));
        }

        @Override
        protected void save(Connection con, Poll poll, PartialPollResult optionResult) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO poll_result (poll_id, "
                    + "option, result, height) VALUES (?, ?, ?, ?)")) {
                int i = 0;
                pstmt.setLong(++i, poll.getId());
                pstmt.setString(++i, optionResult.getOption());
                pstmt.setLong(++i, optionResult.getVotes());
                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
        }
    };

    static void init() {}

    static {
        if(Constants.isPollsProcessing) {
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

    private final long id;
    private final DbKey dbKey;
    private final String name;
    private final String description;
    private final String[] options;
    private final byte minNumberOfOptions, maxNumberOfOptions;
    private final byte minRangeValue, maxRangeValue;

    private static void checkPolls(int currentHeight) {
        for (Poll poll : getPollsFinishingAt(currentHeight)) {
            List<PartialPollResult> results = poll.countResults(currentHeight);
            pollResultsTable.insert(poll, results);
            Logger.logDebugMessage("Poll " + Convert.toUnsignedLong(poll.getId()) + " has been finished");
        }
    }

    private Poll(long id, long accountId, Attachment.MessagingPollCreation attachment) {
        super(accountId, attachment.getFinishHeight(), attachment.getVotingModel(), attachment.getHoldingId(),
                attachment.getMinBalance(), attachment.getMinBalanceModel());

        this.id = id;
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

        this.minBalanceModel = rs.getByte("min_balance_model");

        this.id = rs.getLong("id");
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

    public boolean isFinished() { return finishHeight < Nxt.getBlockchain().getHeight(); }

    static void addPoll(Transaction transaction, Attachment.MessagingPollCreation attachment) {
        Poll poll = new Poll(transaction.getId(), transaction.getSenderId(), attachment);
        pollTable.insert(poll);
    }

    public static Poll getPoll(long id) {
        return pollTable.get(pollDbKeyFactory.newKey(id));
    }

    //todo: fix
    public static DbIterator<Poll> getFinishedPolls() {
        int height = Nxt.getBlockchain().getHeight();
        //TODO: < or <= ?
        return pollTable.getManyBy(new DbClause.IntClause("finish_height", DbClause.Op.LT, height), 0, Integer.MAX_VALUE);
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

    public static int getCount() {
        return pollTable.getCount();
    }

    public List<PartialPollResult> getResults() {
        if (Constants.isPollsProcessing && isFinished()) {
            return pollResultsTable.get(pollResultsDbKeyFactory.newKey(id));
        } else {
            int countHeight = Math.min(finishHeight, Nxt.getBlockchain().getHeight());
            if (countHeight < Nxt.getBlockchainProcessor().getMinRollbackHeight()) {
                return null;
            }
            return countResults(countHeight);
        }
    }

    public DbIterator<Vote> getVotes(){
        return Vote.getVotes(this.getId(), 0, -1);
    }

    public long getId() {
        return id;
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

    private List<PartialPollResult> countResults(int height) {
        final long[] counts = new long[options.length];

        try (DbIterator<Vote> votes = Vote.getVotes(this.getId(), 0, -1)) {
            for (Vote vote : votes) {
                long[] partialResult = countVote(vote, height);
                if (partialResult != null) {
                    for (int idx = 0; idx < partialResult.length; idx++) {
                        counts[idx] += partialResult[idx];
                    }
                }
            }
        }

        List<PartialPollResult> results = new ArrayList<>(options.length);
        for (int i = 0; i < options.length; i++) {
            results.add(new PartialPollResult(options[i], counts[i]));
        }
        return results;
    }

    private long[] countVote(Vote vote, int height) {
        final long[] partialResult = new long[options.length];

        final long weight = calcWeight(vote.getVoterId(), height);

        final byte[] optVals = vote.getVote();

        if (weight > 0) {
            for (int idx = 0; idx < optVals.length; idx++) {
                if (optVals[idx] != Constants.VOTING_NO_VOTE_VALUE) {
                    partialResult[idx] = optVals[idx] * weight;
                }
            }
            return partialResult;
        } else {
            return null;
        }
    }

    public static class PartialPollResult {
        private final String option;
        private final long votes;

        public PartialPollResult(String option, long votes) {
            this.option = option;
            this.votes = votes;
        }

        public String getOption() {
            return option;
        }

        public long getVotes() {
            return votes;
        }
    }
}