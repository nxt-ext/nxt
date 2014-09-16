package nxt;

import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.EntityDbTable;

import nxt.db.Db;
import nxt.util.*;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.sql.*;
import java.util.*;

public final class Poll {
    public static final byte VOTING_MODEL_BALANCE = 0;
    public static final byte VOTING_MODEL_ACCOUNT = 1;
    public static final byte VOTING_MODEL_ASSET = 2;

    public static final byte OPTION_MODEL_CHOICE = 0;
    public static final byte OPTION_MODEL_BINARY = 1;

    public static final byte DEFAULT_MIN_BALANCE = 0;
    public static final byte DEFAULT_MIN_NUMBER_OF_CHOICES = 1;

    public static final byte NO_ASSET_CODE = 0;

    private static final DbKey.LongKeyFactory<Poll> pollDbKeyFactory = new DbKey.LongKeyFactory<Poll>("id") {

        @Override
        public DbKey newKey(Poll poll) {
            return poll.dbKey;
        }

    };

    private static class PollTable extends EntityDbTable<Poll> {

        protected PollTable(DbKey.Factory<Poll> dbKeyFactory) {
            super(dbKeyFactory);
        }

        @Override
        protected String table() {
            return "poll";
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
            try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO poll (id, name, description, "
                    + "options, finish, option_model, voting_model, min_balance, asset_id, "
                    + "min_num_options, max_num_options, active, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                int i = 0;
                pstmt.setLong(++i, poll.getId());
                pstmt.setString(++i, poll.getName());
                pstmt.setString(++i, poll.getDescription());
                String optionsJson = JSONArray.toJSONString(Arrays.asList(poll.getOptions()));
                System.out.println("Options json: "+optionsJson);
                pstmt.setString(++i, optionsJson);
                pstmt.setInt(++i, poll.getFinishBlockHeight());
                pstmt.setByte(++i, poll.getOptionModel());
                pstmt.setByte(++i, poll.getVotingModel());
                pstmt.setLong(++i, poll.getMinBalance());
                if (poll.getAssetId() == Poll.NO_ASSET_CODE) {
                    pstmt.setNull(++i, Types.INTEGER);
                } else {
                    pstmt.setLong(++i, poll.getAssetId());
                }
                pstmt.setByte(++i, poll.getMinNumberOfOptions());
                pstmt.setByte(++i, poll.getMaxNumberOfOptions());
                pstmt.setBoolean(++i, poll.isActive());
                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
        }

        public void updateActive(Boolean active, long id) {
            String query = "UPDATE poll SET active=" + active.toString() + " WHERE ID=" + id;
            try (Connection con = Db.getConnection();
                 PreparedStatement pstmt = con.prepareStatement(query)) {
                pstmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }

        protected Long getId(Poll poll) {
            return poll.getId();
        }
    }

    private final static PollTable pollTable = new PollTable(pollDbKeyFactory);

    static void init() {
    }


    private final Long id;
    private final DbKey dbKey;
    private final String name;
    private final String description;
    private final String[] options;

    private final int finishBlockHeight;
    private final byte votingModel;
    private final byte optionModel;

    private long minBalance = DEFAULT_MIN_BALANCE;

    private final byte minNumberOfOptions, maxNumberOfOptions;
    private final long assetId;

    private boolean active;

    static {
        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
                Poll.checkPolls(block.getHeight());
            }
        }, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
    }

    static void checkPolls(int height) {
        for (Poll poll : getActivePolls()) {
            if (poll.finishBlockHeight <= height) {
                poll.calculateAndSavePollResults();
                markPollAsFinished(poll);
                System.out.println("Poll " + poll.getId() + " has been finished");
            }
        }
    }

    private Poll(Long id, String name, String description, String[] options, int finishBlockHeight,
                 byte optionModel, byte votingModel, long minBalance,
                 long assetId, byte minNumberOfOptions, byte maxNumberOfOptions) {
        this.id = id;
        this.dbKey = pollDbKeyFactory.newKey(this.id);
        this.name = name;
        this.description = description;
        this.options = options;
        this.finishBlockHeight = finishBlockHeight;
        this.optionModel = optionModel;
        this.votingModel = votingModel;
        this.minBalance = minBalance;
        this.assetId = assetId;
        this.minNumberOfOptions = minNumberOfOptions;
        this.maxNumberOfOptions = maxNumberOfOptions;
        this.active = true;
    }

    private Poll(ResultSet rs) throws Exception {
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

        this.finishBlockHeight = rs.getInt("finish");
        this.optionModel = rs.getByte("option_model");
        this.votingModel = rs.getByte("voting_model");
        this.minBalance = rs.getLong("min_balance");
        this.assetId = rs.getLong("asset_id");
        this.minNumberOfOptions = rs.getByte("min_num_options");
        this.maxNumberOfOptions = rs.getByte("max_num_options");
        this.active = rs.getBoolean("active");
    }

    private Poll(Transaction transaction, Attachment.MessagingPollCreation attachment) {
        this.id = transaction.getId();
        this.dbKey = pollDbKeyFactory.newKey(this.id);
        this.name = attachment.getPollName();
        this.description = attachment.getPollDescription();
        this.options = attachment.getPollOptions();
        this.finishBlockHeight = attachment.getFinishBlockHeight();
        this.optionModel = attachment.getOptionModel();
        this.votingModel = attachment.getVotingModel();
        this.minBalance = attachment.getMinBalance();
        this.assetId = attachment.getAssetId();
        this.minNumberOfOptions = attachment.getMinNumberOfOptions();
        this.maxNumberOfOptions = attachment.getMaxNumberOfOptions();
        this.active = true;
    }

    static void addPoll(Long id, String name, String description, String[] options,
                        int finishBlockHeight, byte optionModel, byte votingModel,
                        long minBalance,
                        long assetId,
                        byte minNumberOfOptions,
                        byte maxNumberOfOptions) {

        if (Poll.exists(id)) {
            throw new IllegalStateException("Poll with id " + Convert.toUnsignedLong(id) + " already exists");
        }

        Poll poll = new Poll(id, name, description, options, finishBlockHeight, optionModel, votingModel,
                minBalance, assetId, minNumberOfOptions, maxNumberOfOptions);

        pollTable.insert(poll);
    }

    static boolean exists(long id) {
        return getPoll(id) != null;
    }

    public static Poll getPoll(Long id) {
        return pollTable.get(pollDbKeyFactory.newKey(id));
    }

    public static DbIterator<Poll> getActivePolls() {
        return pollTable.getManyBy("active", true, 0, Integer.MAX_VALUE);
    }

    public static DbIterator<Poll> getFinishedPolls() {
        return pollTable.getManyBy("active", false, 0, Integer.MAX_VALUE);
    }

    public static DbIterator<Poll> getAllPolls(int from, int to) {
        return pollTable.getAll(from, to);
    }

    public static int getCount() {
        return pollTable.getCount();
    }

    public static boolean isActive(long pollId) {
        return getPoll(pollId).isActive();
    }

    public static void markPollAsFinished(Poll poll) {
        pollTable.updateActive(false, poll.getId());
    }

    public Long getId() {
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

    public byte getOptionModel() {
        return optionModel;
    }

    public byte getVotingModel() {
        return votingModel;
    }

    public int getFinishBlockHeight() {
        return finishBlockHeight;
    }

    public long getMinBalance() {
        return minBalance;
    }

    public long getAssetId() {
        return assetId;
    }

    public List<Long> getVoters() {
        return Vote.getVoters(this);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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
            case OPTION_MODEL_CHOICE:
                final Map<String, Long> pr = new HashMap<>();
                for (int idx = 0; idx < results.length; idx++) {
                    pr.put(options[idx], results[idx][1]);
                }
                pollResults = new PollResults.Choice(getId(), pr);
                break;

            case OPTION_MODEL_BINARY:
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

        final Long voter = vote.getVoterId();

        long weight = 0;

        switch (votingModel) {
            case VOTING_MODEL_ASSET:
                long qntBalance = Account.getAccount(voter).getAssetBalanceQNT(assetId);
                if (qntBalance >= minBalance) {
                    weight = qntBalance;
                }
                break;
            case VOTING_MODEL_ACCOUNT:
            case VOTING_MODEL_BALANCE:
                long nqtBalance = Account.getAccount(voter).getGuaranteedBalanceNQT(Constants.CONFIRMATIONS_RELIABLE_TX);
                if (nqtBalance >= minBalance) {
                    long nxtBalance = nqtBalance / Constants.ONE_NXT;
                    weight = votingModel == VOTING_MODEL_ACCOUNT ? 1 : nxtBalance;
                }
                break;
            default:
                throw new NxtException.IllegalStateException("Wrong voting model");
        }

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