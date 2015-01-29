package nxt;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Abstract poll structure, parent for concrete poll implementations e.g. Poll or PendingTransactionPoll
 */

abstract class AbstractPoll {
    protected final long accountId;
    protected final int finishBlockHeight;
    protected final byte votingModel;

    protected final long holdingId; //either asset id or MS coin id
    protected final long minBalance;
    protected byte minBalanceModel = Constants.VOTING_MINBALANCE_UNDEFINED;

    AbstractPoll(long accountId, int finishBlockHeight,
                 byte votingModel, long holdingId,
                 long minBalance) {
        this(accountId, finishBlockHeight, votingModel, holdingId, minBalance,
                Constants.VOTING_MINBALANCE_UNDEFINED);
    }

    AbstractPoll(long accountId, int finishBlockHeight,
                 byte votingModel, long holdingId,
                 long minBalance, byte minBalanceModel) {
        this.accountId = accountId;
        this.finishBlockHeight = finishBlockHeight;
        this.votingModel = votingModel;
        this.holdingId = holdingId;
        this.minBalance = minBalance;
        this.minBalanceModel = minBalanceModel;
    }

    AbstractPoll(ResultSet rs) throws SQLException {
        this.accountId = rs.getLong("account_id");
        this.finishBlockHeight = rs.getInt("finish_height");
        this.votingModel = rs.getByte("voting_model");
        this.holdingId = rs.getLong("holding_id");
        this.minBalance = rs.getLong("min_balance");

        this.minBalanceModel = Constants.VOTING_MINBALANCE_UNDEFINED;
    }

    public final long getAccountId() {
        return accountId;
    }

    public final byte getVotingModel() {
        return votingModel;
    }

    public final int getFinishBlockHeight() {
        return finishBlockHeight;
    }

    public final long getMinBalance() {
        return minBalance;
    }

    public final long getHoldingId() { return holdingId; }

    public final byte getMinBalanceModel() { return minBalanceModel; }

    long calcWeight(long voterId) {
        long weight = 0;

        switch (votingModel) {
            case Constants.VOTING_MODEL_ASSET:
                long qntBalance = Account.getAssetBalanceQNT(voterId, holdingId);
                if (qntBalance >= getMinBalance()) {
                    weight = qntBalance;
                }
                break;
            case Constants.VOTING_MODEL_CURRENCY:
                long units = Account.getCurrencyUnits(voterId, holdingId);
                if (units >= getMinBalance()) {
                    weight = units;
                }
                break;
            case Constants.VOTING_MODEL_ACCOUNT:
                long balance;
                switch(minBalanceModel){
                    case Constants.VOTING_MINBALANCE_BYBALANCE:
                        balance = Account.getAccount(voterId).getBalanceNQT();
                        break;
                    case Constants.VOTING_MINBALANCE_ASSET:
                        balance = Account.getAssetBalanceQNT(voterId, holdingId);
                        break;
                    case Constants.VOTING_MINBALANCE_CURRENCY:
                        balance = Account.getCurrencyUnits(voterId, holdingId);
                        break;
                    default:
                        throw new RuntimeException("Invalid minBalanceModel " + minBalanceModel);
                }
                if (balance >= getMinBalance()) {
                    weight = 1;
                }
                break;
            case Constants.VOTING_MODEL_BALANCE:
                long nqtBalance = Account.getAccount(voterId).getBalanceNQT();
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

