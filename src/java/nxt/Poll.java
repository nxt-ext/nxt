package nxt;

import nxt.util.Convert;
import nxt.util.Listener;
import nxt.util.Logger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

//todo: Blind signatures??? https://en.wikipedia.org/wiki/Blind_signature
public final class Poll {

    public static final byte VOTING_MODEL_BALANCE = 0;
    public static final byte VOTING_MODEL_ACCOUNT = 1;
    public static final byte VOTING_MODEL_ASSET = 2;

    public static final byte COUNTING_AT_THE_END = 0;
    public static final byte COUNTING_EVERY_BLOCK = 1;

    public static final byte OPTION_MODEL_CHOICE = 0;
    public static final byte OPTION_MODEL_BINARY = 1;

    private static final ConcurrentMap<Long, Poll> polls = new ConcurrentHashMap<>();
    private static final Collection<Poll> allPolls = Collections.unmodifiableCollection(polls.values());

    private static final ConcurrentMap<Long, Poll> activePolls = new ConcurrentHashMap<>();
    private static final Collection<Poll> allActivePolls = Collections.unmodifiableCollection(activePolls.values());

    private static final ConcurrentMap<Long, PollResults> pollResults = new ConcurrentHashMap<>();
    private static final Collection<PollResults> allPollResults = Collections.unmodifiableCollection(pollResults.values());


    static{
        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
                Poll.checkPolls(block.getHeight());
            }
        }, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
    }


    private final Long id;
    private final String name;
    private final String description;
    private final String[] options;
    private final byte minNumberOfOptions, maxNumberOfOptions;
    private final byte votingModel;
    private final byte countingModel;
    private final byte optionModel;

    private final int finishBlockHeight;

    //kushti: assetId here if votingModel==VOTING_MODEL_ASSET, could be used for other data for other voting models
    private final long parameter1;


    private final ConcurrentMap<Long, Long> voters;


    static void checkPolls(int height){
        for(Poll poll: getActivePolls()){
            if(poll.finishBlockHeight <= height){
                poll.calculateAndSavePollResults();
                removeActivePoll(poll.getId());
                System.out.println("Poll "+poll.getId()+" has been finished");
            }
        }
    }


    public PollResults calculateAndSavePollResults(){
        try {
            PollResults results = countTotal();
            pollResults.put(getId(), results);
            return results;
        }catch(NxtException.IllegalStateException e){
            Logger.logDebugMessage("Error while calculating poll results", e);
            return null;
        }
    }

    //todo: prevent doublevoting
    private PollResults countTotal() throws NxtException.IllegalStateException {
        final long[][] results = new long[options.length][2];

        switch (countingModel) {
            case COUNTING_EVERY_BLOCK:
                throw new NxtException.IllegalStateException("COUNTING_EVERY_BLOCK in countTotal");
            case COUNTING_AT_THE_END:
                for (Long voteId : voters.values()) {
                    Vote vote = Vote.getVote(voteId);
                    long[][] partialResult = countVote(vote);
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
    public long[][] countVote(Vote vote) throws NxtException.IllegalStateException {
        final long[][] partialResult = new long[options.length][2];

        final Long voter = vote.getVoterId();
            final byte[] optVals = vote.getVote();

            for (int idx = 0; idx < optVals.length; idx++) {
                byte option = optVals[idx];

                long weight;

                switch (votingModel) {
                    case VOTING_MODEL_ACCOUNT:
                        weight = 1;
                        break;
                    case VOTING_MODEL_ASSET:
                        weight = Account.getAccount(voter).getAssetBalanceQNT(parameter1);
                        break;
                    case VOTING_MODEL_BALANCE:
                        //todo: 10???
                        weight = Account.getAccount(voter).getGuaranteedBalanceNQT(10) / Constants.ONE_NXT;
                        break;
                    default:
                        throw new NxtException.IllegalStateException("Wrong voting model");
                }

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
    }

    private Poll(Long id, String name, String description, int finishBlockHeight, String[] options,
                 byte minNumberOfOptions, byte maxNumberOfOptions, byte optionModel,
                 byte votingModel, byte countingModel, long parameter1) {
        this.id = id;
        this.name = name;
        this.description = description;

        this.finishBlockHeight = finishBlockHeight;

        this.options = options;
        this.minNumberOfOptions = minNumberOfOptions;
        this.maxNumberOfOptions = maxNumberOfOptions;

        this.voters = new ConcurrentHashMap<>();

        this.optionModel = optionModel;
        this.votingModel = votingModel;
        this.countingModel = countingModel;
        this.parameter1 = parameter1;
    }

    static void addPoll(Long id, String name, String description, int finishBlockHeight, String[] options, byte minNumberOfOptions,
                        byte maxNumberOfOptions, byte optionModel, byte votingModel,
                        byte countingModel, long parameter1) {


        Poll poll = new Poll(id, name, description, finishBlockHeight, options, minNumberOfOptions, maxNumberOfOptions,
                optionModel, votingModel, countingModel, parameter1);
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

    static void clear() {
        polls.clear();
    }

    public static Poll getPoll(Long id) {
        return polls.get(id);
    }

    public static Collection<PollResults> getAllPollResults() {
        return allPollResults;
    }

    public static PollResults getPollResults(long pollId){
        return pollResults.get(pollId);
    }

    public static void removeActivePoll(long pollId){
        activePolls.remove(pollId);
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
        ChoicePollResults(long pollId, Map<String, Long> results) { super(pollId, results); }
    }

    public class BinaryPollResults extends PollResults<String, long[]> {
        BinaryPollResults(long pollId, Map<String, long[]> results) { super(pollId, results); }
    }
}
