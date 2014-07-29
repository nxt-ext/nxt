package nxt;

import nxt.util.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

    private static class PollTable extends DbTable<Poll> {
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
                    + "options, finish, option_model, voting_model, min_balance, asset_id, "
                    + "min_num_options, max_num_options, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                int i = 0;
                pstmt.setLong(++i, poll.getId());
                pstmt.setString(++i, poll.getName());
                pstmt.setString(++i, poll.getDescription());
                pstmt.setObject(++i, poll.getOptions());
                pstmt.setInt(++i, poll.getFinishBlockHeight());
                pstmt.setByte(++i, poll.getOptionModel());
                pstmt.setByte(++i, poll.getVotingModel());
                pstmt.setLong(++i, poll.getMinBalance());
                if(poll.getAssetId()==Poll.NO_ASSET_CODE){
                    pstmt.setObject(++i, null);
                }else{
                    pstmt.setLong(++i, poll.getAssetId());
                }
                pstmt.setByte(++i, poll.getMinNumberOfOptions());
                pstmt.setByte(++i, poll.getMaxNumberOfOptions());
                pstmt.setBoolean(++i, poll.isActive());
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

        public void updateActive(Boolean active, long id){
            String query = "UPDATE poll SET active="+active.toString()+" WHERE ID="+id;
            try (Connection con = Db.getConnection();
                 PreparedStatement pstmt = con.prepareStatement(query)) {
                pstmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }
    }


    private final static PollTable pollTable = new PollTable();



    private final Long id;
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

    private Poll(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.name = rs.getString("name");
        this.description = rs.getString("description");
        this.options = (String[])rs.getArray("options").getArray();
        this.finishBlockHeight = rs.getInt("finish");
        this.optionModel = rs.getByte("option_model");
        this.votingModel = rs.getByte("voting_model");
        this.minBalance = rs.getLong("min_balance");
        this.assetId = rs.getLong("asset_id");
        this.minNumberOfOptions = rs.getByte("min_num_options");
        this.maxNumberOfOptions = rs.getByte("max_num_options");
        this.active = rs.getBoolean("active");
    }

    private Poll(Transaction transaction, Attachment.MessagingPollCreation attachment){
        this.id = transaction.getId();
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

    static void clear() {
        pollTable.truncate();
    }

    static boolean exists(long pollId){
        return pollTable.get(pollId) != null;
    }

    public static Poll getPoll(Long id) {
        return pollTable.get(id);
    }

    public static Collection<Poll> getActivePolls() {
        return pollTable.getManyBy("active", true);
    }

    public static Collection<Poll> getFinishedPolls() {
        return pollTable.getManyBy("active", false);
    }

    public static boolean isActive(long pollId) {
        Poll poll = pollTable.get(pollId);
        return poll.isActive();
    }

    public static void markPollAsFinished(Poll poll){
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

    public boolean isActive(){
        return active;
    }

    public void setActive(boolean active){
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