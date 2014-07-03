package nxt;

import nxt.util.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

//todo: Blind signatures??? https://en.wikipedia.org/wiki/Blind_signature
public final class Poll {
    public static final byte VOTING_MODEL_BALANCE = 0;
    public static final byte VOTING_MODEL_ACCOUNT = 1;
    public static final byte VOTING_MODEL_ASSET = 2;

    public static final byte OPTION_MODEL_CHOICE = 0;
    public static final byte OPTION_MODEL_BINARY = 1;

    public static final byte DEFAULT_MIN_BALANCE = 0;
    public static final byte DEFAULT_MIN_NUMBER_OF_CHOICES = 1;

    private static final ConcurrentMap<Long, Poll> polls = new ConcurrentHashMap<>();
    private static final Collection<Poll> allPolls = Collections.unmodifiableCollection(polls.values());

    private static final ConcurrentMap<Long, Poll> activePolls = new ConcurrentHashMap<>();
    private static final Collection<Poll> allActivePolls = Collections.unmodifiableCollection(activePolls.values());

    private static final ConcurrentMap<Long, Poll> finishedPolls = new ConcurrentHashMap<>();
    private static final Collection<Poll> allFinishedPolls = Collections.unmodifiableCollection(finishedPolls.values());

    private static final ConcurrentMap<Long, PollResults> pollResults = new ConcurrentHashMap<>();
    private static final Collection<PollResults> allPollResults = Collections.unmodifiableCollection(pollResults.values());

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

    private final ConcurrentMap<Long, Long> voters;

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

    void calculateAndSavePollResults() {
        try {
            pollResults.put(getId(), countResults());
        } catch (NxtException.IllegalStateException e) {
            Logger.logDebugMessage("Error while calculating poll results", e);
        }
    }

    //todo: prevent doublevoting
    private PollResults countResults() throws NxtException.IllegalStateException {
        final long[][] results = new long[options.length][2];

        for (Long voteId : voters.values()) {
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
                pollResults = new ChoicePollResults(getId(), pr);
                break;

            case OPTION_MODEL_BINARY:
                final Map<String, long[]> pr2 = new HashMap<>();
                for (int idx = 0; idx < results.length; idx++) {
                    pr2.put(options[idx], results[idx]);
                }
                pollResults = new BinaryPollResults(getId(), pr2);
                break;

            default:
                throw new NxtException.IllegalStateException("Illegal option model");
        }

        return pollResults;
    }

    //todo: exception?
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

        this.voters = new ConcurrentHashMap<>();
    }

    static void addPoll(Long id, String name, String description, String[] options,
                        int finishBlockHeight, byte optionModel, byte votingModel,
                        long minBalance,
                        long assetId,
                        byte minNumberOfOptions,
                        byte maxNumberOfOptions) {

        Poll poll = new Poll(id, name, description, options, finishBlockHeight, optionModel, votingModel,
                minBalance, assetId, minNumberOfOptions, maxNumberOfOptions);
        if (polls.putIfAbsent(id, poll) != null) {
            throw new IllegalStateException("Poll with id " + Convert.toUnsignedLong(id) + " already exists");
        }
        activePolls.put(id, poll);
    }

    public static Collection<Poll> getAllPolls() {
        return allPolls;
    }

    public static Collection<Poll> getActivePolls() {
        return allActivePolls;
    }

    public static Collection<Poll> getFinishedPolls() {
        return allFinishedPolls;
    }

    public static PollResults getPollResults(long pollId) {
        return pollResults.get(pollId);
    }

    public static boolean isPollActive(long pollId) {
        return activePolls.containsKey(pollId);
    }

    static void clear() {
        polls.clear();
    }

    public static Poll getPoll(Long id) {
        return polls.get(id);
    }

    public static Collection<PollResults> getAllPollResults() {
        return allPollResults;
    }

    public static void markPollAsFinished(Poll poll){
        activePolls.remove(poll.getId());
        finishedPolls.putIfAbsent(poll.getId(), poll);
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

    public Map<Long, Long> getVoters() {
        return Collections.unmodifiableMap(voters);
    }

    void addVoter(Long voterId, Long voteId) {
        voters.put(voterId, voteId);
    }

    public abstract class PollResults<K, V> {
        private long pollId;
        private Map<K, V> results;

        PollResults(long pollId, Map<K, V> results) {
            this.pollId = pollId;
            this.results = results;
        }

        public long getPollId() {
            return pollId;
        }

        public Map<K, V> getResults() {
            return Collections.unmodifiableMap(results);
        }
    }

    public class ChoicePollResults extends PollResults<String, Long> {
        ChoicePollResults(long pollId, Map<String, Long> results) {
            super(pollId, results);
        }
    }

    public class BinaryPollResults extends PollResults<String, long[]> {
        BinaryPollResults(long pollId, Map<String, long[]> results) {
            super(pollId, results);
        }
    }
}
