package nxt;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Abstract poll structure, parent for concrete poll implementations e.g. Poll or PendingTransactionPoll
 */

public abstract class AbstractPoll {
    protected final long accountId;
    protected final int finishBlockHeight;
    protected final byte votingModel;

    protected final long holdingId; //whether asset id or MS coin id
    protected final long minBalance;
    protected byte minBalanceModel = Constants.VOTING_MINBALANCE_UNDEFINED;

    AbstractPoll(long accountId, int finishBlockHeight,
                 byte votingModel, long holdingId,
                 long minBalance){
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

    public long getHoldingId() { return holdingId; }

    public byte getMinBalanceModel() { return minBalanceModel; }

    long calcWeight(Account voter) {
        long weight = 0;

        switch (votingModel) {
            case Constants.VOTING_MODEL_ASSET:
                long assetId = getHoldingId();
                long qntBalance = voter.getAssetBalanceQNT(assetId);
                if (qntBalance >= getMinBalance()) {
                    weight = qntBalance;
                }
                break;
            case Constants.VOTING_MODEL_MS_COIN:
                long currencyId = getHoldingId();
                long units = voter.getCurrency(currencyId).getUnits();
                if (units >= getMinBalance()) {
                    weight = units;
                }
                break;
            case Constants.VOTING_MODEL_ACCOUNT:
                long holdingId = getHoldingId();
                long balance = 0;
                switch(minBalanceModel){
                    case Constants.VOTING_MINBALANCE_BYBALANCE:
                        balance = voter.getBalanceNQT();
                        break;
                    case Constants.VOTING_MINBALANCE_ASSET:
                        balance = voter.getAssetBalanceQNT(holdingId);
                        break;
                    case Constants.VOTING_MINBALANCE_COIN:
                        balance = voter.getCurrency(holdingId).getUnits();
                }
                if (balance >= getMinBalance()) {
                    weight = 1;
                }
                break;
            case Constants.VOTING_MODEL_BALANCE:
                long nqtBalance = voter.getBalanceNQT();
                if (nqtBalance >= getMinBalance()) {
                    weight = nqtBalance;
                }
        }
        return weight;
    }
}

