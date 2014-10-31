package nxt;

import nxt.db.*;
import nxt.util.*;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
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


    private static class PollTable extends VersionedEntityDbTable<Poll> {

        protected PollTable(DbKey.Factory<Poll> dbKeyFactory) {
            super("poll", dbKeyFactory);
        }

        @Override
        protected Poll load(Connection con, ResultSet rs) throws SQLException {
            try {
                return new Poll(rs);
            } catch (Exception e) {
                throw new SQLException(e);
            }
        }

        @Override
        protected void save(Connection con, Poll poll) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO poll (id, account_id, "
                    + "name, description, options, finish, voting_model, min_balance, asset_id, "
                    + "min_num_options, max_num_options, min_range_value, max_range_value, finished, height) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                int i = 0;
                pstmt.setLong(++i, poll.getId());
                pstmt.setLong(++i, poll.getAccountId());
                pstmt.setString(++i, poll.getName());
                pstmt.setString(++i, poll.getDescription());
                String optionsJson = JSONArray.toJSONString(Arrays.asList(poll.getOptions()));
                pstmt.setString(++i, optionsJson);
                pstmt.setInt(++i, poll.getFinishBlockHeight());
                pstmt.setByte(++i, poll.getVotingModel());
                pstmt.setLong(++i, poll.getMinBalance());
                pstmt.setLong(++i, poll.getAssetId());
                pstmt.setByte(++i, poll.getMinNumberOfOptions());
                pstmt.setByte(++i, poll.getMaxNumberOfOptions());
                pstmt.setByte(++i, poll.getMinRangeValue());
                pstmt.setByte(++i, poll.getMaxRangeValue());
                pstmt.setBoolean(++i, poll.isFinished());
                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
        }
    }

    private static final ValuesDbTable<Poll, Pair<String, Long>> pollResultsTable = new ValuesDbTable<Poll,Pair<String,Long>>("poll_results", pollResultsDbKeyFactory) {

        @Override
        protected Pair<String,Long> load(Connection con, ResultSet rs) throws SQLException {
            return new Pair<>(rs.getString("option"), rs.getLong("result"));
        }

        @Override
        protected void save(Connection con, Poll poll, Pair<String, Long> optionResult) throws SQLException {
            String option = optionResult.getFirst();
            Long result = optionResult.getSecond();
            try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO "+table+"(poll_id, "
                    + "option, result, height) VALUES (?, ?, ?, ?)")) {
                int i = 0;
                pstmt.setLong(++i, poll.getId());
                pstmt.setString(++i, option);
                pstmt.setLong(++i, result);
                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
        }
    };

    private final static PollTable pollTable = new PollTable(pollDbKeyFactory);

    static void init() {}

    private final long id;
    private final DbKey dbKey;
    private final String name;
    private final String description;
    private final String[] options;
    private final byte minNumberOfOptions, maxNumberOfOptions;
    private final byte minRangeValue, maxRangeValue;

    static {
        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
                int height = block.getHeight();
                if(height >= Constants.VOTING_SYSTEM_BLOCK){
                    Poll.checkPolls(height);
                }
            }
        }, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
    }

    static void checkPolls(int currentHeight) {
        for (Poll poll : getActivePolls()) {
            if (poll.finishBlockHeight <= currentHeight) {
                poll.calculateAndSavePollResults();
                finishPoll(poll);
                System.out.println("Poll " + poll.getId() + " has been finished");
            }
        }
    }

    private Poll(long id, long accountId, String name, String description, String[] options, int finishBlockHeight,
                 byte votingModel,
                 byte minNumberOfOptions, byte maxNumberOfOptions,
                 byte minRangeValue, byte maxRangeValue,
                 long minBalance, long assetId) {
        super(accountId, finishBlockHeight, votingModel, assetId, minBalance);

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

    private Poll(ResultSet rs) throws Exception {
        super(rs);

        this.id = rs.getLong("id");
        this.dbKey = pollDbKeyFactory.newKey(this.id);
        this.name = rs.getString("name");
        this.description = rs.getString("description");

        String optionsJson = rs.getString("options");
        if(optionsJson.startsWith("(")){  //kushti: possible quoting bug in JDBC driver
            optionsJson = optionsJson.substring(1,optionsJson.length()-1);
        }
        List<String> optionsList = ((List<String>) (new JSONParser().parse(optionsJson)));
        this.options = optionsList.toArray(new String[optionsList.size()]);
        this.minNumberOfOptions = rs.getByte("min_num_options");
        this.maxNumberOfOptions = rs.getByte("max_num_options");
        this.minRangeValue = rs.getByte("min_range_value");
        this.maxRangeValue = rs.getByte("max_range_value");
    }

    static void addPoll(Long id, long accountId, String name, String description, String[] options,
                        int finishBlockHeight, byte votingModel,
                        long minBalance,
                        long assetId,
                        byte minNumberOfOptions,
                        byte maxNumberOfOptions,
                        byte minRangeValue,
                        byte maxRangeValue) {

        if (Poll.exists(id)) {
            throw new IllegalStateException("Poll with id " + Convert.toUnsignedLong(id) + " already exists");
        }

        Poll poll = new Poll(id, accountId, name, description, options, finishBlockHeight, votingModel,
                minNumberOfOptions, maxNumberOfOptions,minRangeValue, maxRangeValue, minBalance, assetId);

        pollTable.insert(poll);
    }

    static boolean exists(long id) {
        return getPoll(id) != null;
    }

    public static Poll getPoll(long id) {
        return pollTable.get(pollDbKeyFactory.newKey(id));
    }

    public static DbIterator<Poll> getActivePolls() {
        return pollTable.getManyBy(new DbClause.BooleanClause("finished", false), 0, Integer.MAX_VALUE);
    }

    public static DbIterator<Poll> getFinishedPolls() {
        return pollTable.getManyBy(new DbClause.BooleanClause("finished", true), 0, Integer.MAX_VALUE);
    }

    public static DbIterator<Poll> getAllPolls(int from, int to) {
        return pollTable.getAll(from, to);
    }

    public static DbIterator<Poll> getPollsByAccount(long accountId, int from, int to) {
        return pollTable.getManyBy(new DbClause.LongClause("account_id", accountId), from, to);
    }

    public static int getCount() {
        return pollTable.getCount();
    }

    public static boolean isFinished(long pollId) {
        return getPoll(pollId).isFinished();
    }

    public static void finishPoll(Poll poll) {
        poll.setFinished(true);
        pollTable.insert(poll);
    }

    public static List<Pair<String,Long>> getResults(Long pollId){
        return pollResultsTable.get(pollResultsDbKeyFactory.newKey(pollId));
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

    public List<Long> getVoters() {
        return Vote.getVoters(this);
    }

    void calculateAndSavePollResults() {
        try {
            List<Pair<String, Long>> results = countResults();
            pollResultsTable.insert(this, results);
        } catch (NxtException.IllegalStateException e) {
            Logger.logDebugMessage("Error while calculating poll results", e);
        }
    }

    private List<Pair<String,Long>> countResults() throws NxtException.IllegalStateException {
        final long[] counts = new long[options.length];

        for (Long voteId : Vote.getVoteIds(this)) {
            Vote vote = Vote.getVote(voteId);
            long[] partialResult = countVote(vote);

            if (partialResult != null) {
                for (int idx = 0; idx < partialResult.length; idx++) {
                    counts[idx] += partialResult[idx];
                }
            }
        }

        List<Pair<String, Long>> results = new ArrayList<>(options.length);
        for(int i=0; i < options.length; i++){
            results.add(new Pair<>(options[i], counts[i]));
        }
        return results;
    }



    private long[] countVote(Vote vote) throws NxtException.IllegalStateException {
        final long[] partialResult = new long[options.length];

        final Account voter = Account.getAccount(vote.getVoterId());
        final long weight = AbstractPoll.calcWeight(this, voter);

        final byte[] optVals = vote.getVote();

        if (weight > 0) {
            for (int idx = 0; idx < optVals.length; idx++) {
                if(optVals[idx] != Constants.VOTING_NO_VOTE_VALUE) {
                    partialResult[idx] = optVals[idx] * weight;
                }
            }
            return partialResult;
        } else {
            return null;
        }
    }
}