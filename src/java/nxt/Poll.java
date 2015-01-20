package nxt;

import nxt.db.*;
import nxt.util.*;
import java.sql.*;
import java.util.*;

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
                pstmt.setInt(++i, poll.getFinishBlockHeight());
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

    static void init() {
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
                List<PartialPollResult> results = poll.countResults();
                pollResultsTable.insert(poll, results);
                System.out.println("Poll " + poll.getId() + " has been finished"); //TODO: Logger
        }
    }

    private Poll(long id, long accountId, String name, String description, String[] options, int finishBlockHeight,
                 byte votingModel,
                 byte minNumberOfOptions, byte maxNumberOfOptions,
                 byte minRangeValue, byte maxRangeValue,
                 long minBalance, byte minBalanceModel, long holdingId) {
        super(accountId, finishBlockHeight, votingModel, holdingId, minBalance, minBalanceModel);

        this.id = id;
        this.dbKey = pollDbKeyFactory.newKey(this.id);
        this.name = name;
        this.description = description;
        this.options = options;

        this.minNumberOfOptions = minNumberOfOptions;
        this.maxNumberOfOptions = maxNumberOfOptions;
        this.minRangeValue = minRangeValue;
        this.maxRangeValue = maxRangeValue;
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

    public boolean isFinished(){ return finishBlockHeight < Nxt.getBlockchain().getHeight(); }

    static void addPoll(long pollId, long accountId, Attachment.MessagingPollCreation attachment) {

        Poll poll = new Poll(pollId,
                accountId,
                attachment.getPollName(),
                attachment.getPollDescription(),
                attachment.getPollOptions(),
                attachment.getFinishBlockHeight(),
                attachment.getVotingModel(),
                attachment.getMinNumberOfOptions(),
                attachment.getMaxNumberOfOptions(),
                attachment.getMinRangeValue(),
                attachment.getMaxRangeValue(),
                attachment.getMinBalance(),
                attachment.getMinBalanceModel(),
                attachment.getHoldingId());
        pollTable.insert(poll);
    }

    public static Poll getPoll(long id) {
        return pollTable.get(pollDbKeyFactory.newKey(id));
    }

    //todo: fix
    public static DbIterator<Poll> getFinishedPolls() {
        int height = Nxt.getBlockchain().getHeight();
        return pollTable.getManyBy(new DbClause.IntNotGreaterThan("finish_height", height), 0, Integer.MAX_VALUE);
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

    public static List<PartialPollResult> getResults(long pollId){
        return pollResultsTable.get(pollResultsDbKeyFactory.newKey(pollId));
    }

    public List<Vote> getVotes(){
        return Vote.getVotes(this.getId(), 0, -1).toList();
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

    public List<PartialPollResult> countResults() {
        final long[] counts = new long[options.length];

        for (Vote vote : Vote.getVotes(this.getId(), 0, -1).toList()) {
            long[] partialResult = countVote(vote);

            if (partialResult != null) {
                for (int idx = 0; idx < partialResult.length; idx++) {
                    counts[idx] += partialResult[idx];
                }
            }
        }

        List<PartialPollResult> results = new ArrayList<>(options.length);
        for (int i = 0; i < options.length; i++) {
            results.add(new PartialPollResult(options[i], counts[i]));
        }
        return results;
    }


    //TODO: pass accountId instead of Account to calcWeight, then when voting by asset balance there is no need to ever get
    // the actual Account object, this will help performance
    private long[] countVote(Vote vote) {
        final long[] partialResult = new long[options.length];

        final Account voter = Account.getAccount(vote.getVoterId());
        final long weight = calcWeight(voter);

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

    public static class PartialPollResult{
        final String option;
        final long votes;

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