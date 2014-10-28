package nxt;

import nxt.db.*;

import nxt.util.*;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import java.sql.*;
import java.util.*;

public final class Poll extends CommonPollStructure {
    private static final DbKey.LongKeyFactory<Poll> pollDbKeyFactory = new DbKey.LongKeyFactory<Poll>("id") {
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
                    + "name, description, options, finish, option_model, voting_model, min_balance, asset_id, "
                    + "min_num_options, max_num_options, finished, height) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                int i = 0;
                pstmt.setLong(++i, poll.getId());
                pstmt.setLong(++i, poll.getAccountId());
                pstmt.setString(++i, poll.getName());
                pstmt.setString(++i, poll.getDescription());
                String optionsJson = JSONArray.toJSONString(Arrays.asList(poll.getOptions()));
                pstmt.setString(++i, optionsJson);
                pstmt.setInt(++i, poll.getFinishBlockHeight());
                pstmt.setByte(++i, poll.getOptionModel());
                pstmt.setByte(++i, poll.getVotingModel());
                pstmt.setLong(++i, poll.getMinBalance());
                pstmt.setLong(++i, poll.getAssetId());
                pstmt.setByte(++i, poll.getMinNumberOfOptions());
                pstmt.setByte(++i, poll.getMaxNumberOfOptions());
                pstmt.setBoolean(++i, poll.isFinished());
                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
        }
    }

    private final static PollTable pollTable = new PollTable(pollDbKeyFactory);

    static void init() {}

    private final long id;
    private final DbKey dbKey;
    private final String name;
    private final String description;
    private final String[] options;
    private final byte optionModel;
    private final byte minNumberOfOptions, maxNumberOfOptions;

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
                 byte optionModel, byte votingModel, long minBalance,
                 long assetId, byte minNumberOfOptions, byte maxNumberOfOptions) {
        super(accountId, finishBlockHeight, votingModel, assetId, minBalance);

        this.id = id;
        this.dbKey = pollDbKeyFactory.newKey(this.id);
        this.name = name;
        this.description = description;
        this.options = options;
        this.optionModel = optionModel;
        this.minNumberOfOptions = minNumberOfOptions;
        this.maxNumberOfOptions = maxNumberOfOptions;
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
        this.optionModel = rs.getByte("option_model");
        this.minNumberOfOptions = rs.getByte("min_num_options");
        this.maxNumberOfOptions = rs.getByte("max_num_options");
    }

    private Poll(Transaction transaction, Attachment.MessagingPollCreation attachment) {
        super(transaction.getSenderId(), attachment.getFinishBlockHeight(), attachment.getVotingModel(),
                attachment.getAssetId(), attachment.getMinBalance());

        this.id = transaction.getId();
        this.dbKey = pollDbKeyFactory.newKey(this.id);
        this.name = attachment.getPollName();
        this.description = attachment.getPollDescription();
        this.options = attachment.getPollOptions();
        this.optionModel = attachment.getOptionModel();
        this.minNumberOfOptions = attachment.getMinNumberOfOptions();
        this.maxNumberOfOptions = attachment.getMaxNumberOfOptions();
    }

    static void addPoll(Long id, long accountId, String name, String description, String[] options,
                        int finishBlockHeight, byte optionModel, byte votingModel,
                        long minBalance,
                        long assetId,
                        byte minNumberOfOptions,
                        byte maxNumberOfOptions) {

        if (Poll.exists(id)) {
            throw new IllegalStateException("Poll with id " + Convert.toUnsignedLong(id) + " already exists");
        }

        Poll poll = new Poll(id, accountId, name, description, options, finishBlockHeight, optionModel, votingModel,
                minBalance, assetId, minNumberOfOptions, maxNumberOfOptions);

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

    public byte getOptionModel() {
        return optionModel;
    }

    public byte getMinNumberOfOptions() {
        return minNumberOfOptions;
    }

    public byte getMaxNumberOfOptions() {
        return maxNumberOfOptions;
    }

    public List<Long> getVoters() {
        return Vote.getVoters(this);
    }

    void calculateAndSavePollResults() {
        try {
            PollResults.save(countResults());
        } catch (NxtException.IllegalStateException e) {
            Logger.logDebugMessage("Error while calculating poll results", e);
        }
    }

    private PollResults countResults() throws NxtException.IllegalStateException {
        final long[][] results = new long[options.length][2];

        for (Long voteId : Vote.getVoteIds(this)) {
            Vote vote = Vote.getVote(voteId);
            long[][] partialResult = countVote(vote);

            if (partialResult != null) {
                for (int idx = 0; idx < partialResult.length; idx++) {
                    results[idx][0] += partialResult[idx][0];
                    results[idx][1] += partialResult[idx][1];
                }
            }
        }

        final PollResults pollResults;

        switch (optionModel) {
            case Constants.VOTING_OPTION_MODEL_CHOICE:
                final Map<String, Long> pr = new HashMap<>();
                for (int idx = 0; idx < results.length; idx++) {
                    pr.put(options[idx], results[idx][1]);
                }
                pollResults = new PollResults.Choice(getId(), pr);
                break;

            case Constants.VOTING_OPTION_MODEL_BINARY:
                final Map<String, Pair.YesNoCounts> pr2 = new HashMap<>();
                for (int idx = 0; idx < results.length; idx++) {
                    pr2.put(options[idx], new Pair.YesNoCounts(results[idx][1], results[idx][0]));
                }
                pollResults = new PollResults.Binary(getId(), pr2);
                break;

            default:
                throw new NxtException.IllegalStateException("Illegal option model");
        }

        return pollResults;
    }



    private long[][] countVote(Vote vote) throws NxtException.IllegalStateException {
        final long[][] partialResult = new long[options.length][2];

        final Account voter = Account.getAccount(vote.getVoterId());
        final long weight = CommonPollStructure.calcWeight(this, voter);

        final byte[] optVals = vote.getVote();

        if (weight > 0) {
            for (int idx = 0; idx < optVals.length; idx++) {
                byte option = optVals[idx];

                switch (option) {
                    case 0:
                    case 1:
                        partialResult[idx][option] = weight;
                        break;
                    default:
                        throw new NxtException.IllegalStateException("Wrong option value");
                }
            }
            return partialResult;
        } else {
            return null;
        }
    }
}