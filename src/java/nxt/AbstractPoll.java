package nxt;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Abstract poll structure, parent for concrete poll implementations e.g. Poll or PendingTransactionPoll
 */

abstract class AbstractPoll {

    private final PollCounting defaultPollCounting;
    private final long accountId;
    private final int finishHeight;

    AbstractPoll(long accountId, int finishHeight,
                 byte votingModel, long holdingId,
                 long minBalance, byte minBalanceModel) {
        this.accountId = accountId;
        this.finishHeight = finishHeight;
        this.defaultPollCounting = new PollCounting(votingModel, holdingId, minBalance, minBalanceModel);
    }

    AbstractPoll(ResultSet rs) throws SQLException {
        this.accountId = rs.getLong("account_id");
        this.finishHeight = rs.getInt("finish_height");
        this.defaultPollCounting = new PollCounting(rs.getByte("voting_model"), rs.getLong("holding_id"),
                rs.getLong("min_balance"), rs.getByte("min_balance_model"));
    }

    public final long getAccountId() {
        return accountId;
    }

    public final int getFinishHeight() {
        return finishHeight;
    }

    public final PollCounting getDefaultPollCounting() {
        return defaultPollCounting;
    }

}

