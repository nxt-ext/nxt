package nxt;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class Vote {

    private static final ConcurrentMap<Long, Vote> votes = new ConcurrentHashMap<>();

    private final Long id;
    private final Long pollId;
    private final Long voterId;
    private final byte[] vote;

    private Vote(Long id, Long pollId, Long voterId, byte[] vote) {

        this.id = id;
        this.pollId = pollId;
        this.voterId = voterId;
        this.vote = vote;

    }

    public static Vote addVote(Long id, Long pollId, Long voterId, byte[] vote) {
        Vote voteData = new Vote(id, pollId, voterId, vote);
        votes.put(id, voteData);
        return voteData;
    }

    public static ConcurrentMap<Long, Vote> getVotes() { return votes; }

    public static void clear() {
        votes.clear();
    }

    public static Vote getVote(Long id) {
        return votes.get(id);
    }

    public Long getId() {
        return id;
    }

    public Long getPollId() { return pollId; }

    public Long getVoterId() { return voterId; }

    public byte[] getVote() { return vote; }

    @Override
    public boolean equals(Object o) {
        return o instanceof Poll && this.getId().equals(((Poll)o).getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

}
