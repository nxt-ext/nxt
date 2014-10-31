package nxt;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Abstract poll structure, parent for concrete poll implementations e.g. Poll or PendingTransactionPoll
 */

public class AbstractPoll {
    protected final long accountId;
    protected final int finishBlockHeight;
    protected final byte votingModel;

    protected final long assetId;
    protected final long minBalance;
    protected boolean finished;

    public AbstractPoll(long accountId, int finishBlockHeight, byte votingModel, long assetId, long minBalance) {
        this.accountId = accountId;
        this.finishBlockHeight = finishBlockHeight;
        this.votingModel = votingModel;
        this.assetId = assetId;
        this.minBalance = minBalance;
        this.finished = false;
    }

    public AbstractPoll(ResultSet rs) throws SQLException {
        this.accountId = rs.getLong("account_id");
        this.finishBlockHeight = rs.getInt("finish");
        this.votingModel = rs.getByte("voting_model");
        this.assetId = rs.getLong("asset_id");
        this.minBalance = rs.getLong("min_balance");
        this.finished = rs.getBoolean("finished");
    }

    public long getAccountId() {
        return accountId;
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

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    static long calcWeight(AbstractPoll pollStructure, Account voter) throws NxtException.IllegalStateException {
        long weight = 0;

        switch (pollStructure.votingModel) {
            case Constants.VOTING_MODEL_ASSET:
                long qntBalance = voter.getAssetBalanceQNT(pollStructure.assetId);
                if (qntBalance >= pollStructure.getMinBalance()) {
                    weight = qntBalance;
                }
                break;
            case Constants.VOTING_MODEL_ACCOUNT:
                long assetId = pollStructure.getAssetId();
                long balance;
                if (assetId == 0) {
                    balance = voter.getGuaranteedBalanceNQT(Constants.CONFIRMATIONS_RELIABLE_TX) / Constants.ONE_NXT;
                } else {
                    balance = voter.getAssetBalanceQNT(pollStructure.assetId);
                }
                if (balance >= pollStructure.getMinBalance()) {
                    weight = 1;
                }
                break;
            case Constants.VOTING_MODEL_BALANCE:
                long nxtBalance = voter.getGuaranteedBalanceNQT(Constants.CONFIRMATIONS_RELIABLE_TX) / Constants.ONE_NXT;
                if (nxtBalance >= pollStructure.getMinBalance()) {
                    weight = nxtBalance;
                }
                break;
            default:
                throw new NxtException.IllegalStateException("Wrong voting model"); //todo: chack in tx validation only?
        }
        return weight;
    }
}

