package nxt;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Abstract poll structure, parent for concrete poll implementations e.g. Poll or PendingTransactionPoll
 */

abstract class AbstractPoll {
    protected final long accountId;
    protected final int finishHeight;
    protected final byte votingModel;

    protected final long holdingId; //either asset id or MS coin id
    protected final long minBalance;

    AbstractPoll(long accountId, int finishHeight,
                 byte votingModel, long holdingId,
                 long minBalance) {
        this.accountId = accountId;
        this.finishHeight = finishHeight;
        this.votingModel = votingModel;
        this.holdingId = holdingId;
        this.minBalance = minBalance;
    }

    AbstractPoll(ResultSet rs) throws SQLException {
        this.accountId = rs.getLong("account_id");
        this.finishHeight = rs.getInt("finish_height");
        this.votingModel = rs.getByte("voting_model");
        this.holdingId = rs.getLong("holding_id");
        this.minBalance = rs.getLong("min_balance");
    }

    public final long getAccountId() {
        return accountId;
    }

    public final byte getVotingModel() {
        return votingModel;
    }

    public final int getFinishHeight() {
        return finishHeight;
    }

    public final long getMinBalance() {
        return minBalance;
    }

    public final long getHoldingId() {
        return holdingId;
    }

    abstract long calcWeightForByAccountModel(long voterId, int height);

    long calcWeight(long voterId, int height) {
        long weight = 0;

        switch (votingModel) {
            case Constants.VOTING_MODEL_ASSET:
                long qntBalance = Account.getAssetBalanceQNT(voterId, holdingId, height);
                if (qntBalance >= getMinBalance()) {
                    weight = qntBalance;
                }
                break;
            case Constants.VOTING_MODEL_CURRENCY:
                long units = Account.getCurrencyUnits(voterId, holdingId, height);
                if (units >= getMinBalance()) {
                    weight = units;
                }
                break;
            case Constants.VOTING_MODEL_ACCOUNT:
                weight = calcWeightForByAccountModel(voterId, height);
                break;
            case Constants.VOTING_MODEL_BALANCE:
                long nqtBalance = Account.getAccount(voterId, height).getBalanceNQT();
                if (nqtBalance >= getMinBalance()) {
                    weight = nqtBalance;
                }
                break;
            default:
                throw new RuntimeException("Invalid votingModel " + votingModel);
        }
        return weight;
    }
}

