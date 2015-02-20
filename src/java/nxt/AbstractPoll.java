package nxt;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Abstract poll structure, parent for concrete poll implementations e.g. Poll or PendingTransactionPoll
 */

abstract class AbstractPoll {

    protected final long id;
    protected final VoteWeighting defaultVoteWeighting;
    protected final long accountId;
    protected final int finishHeight;

    AbstractPoll(long id, long accountId, int finishHeight, VoteWeighting voteWeighting) {
        this.id = id;
        this.accountId = accountId;
        this.finishHeight = finishHeight;
        this.defaultVoteWeighting = voteWeighting;
    }

    AbstractPoll(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.accountId = rs.getLong("account_id");
        this.finishHeight = rs.getInt("finish_height");
        this.defaultVoteWeighting = new VoteWeighting(rs.getByte("voting_model"), rs.getLong("holding_id"),
                rs.getLong("min_balance"), rs.getByte("min_balance_model"));
    }

    public final long getId() {
        return id;
    }

    public final long getAccountId() {
        return accountId;
    }

    public final int getFinishHeight() {
        return finishHeight;
    }

    public final VoteWeighting getDefaultVoteWeighting() {
        return defaultVoteWeighting;
    }

    public abstract boolean isFinished();

}

