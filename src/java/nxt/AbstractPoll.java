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

    protected final long holdingId; //whether asset id or MS coin id
    protected final long minBalance;
    //protected final byte minBalanceModel;

    AbstractPoll(long accountId, int finishBlockHeight, byte votingModel, long holdingId, long minBalance) {
        this.accountId = accountId;
        this.finishBlockHeight = finishBlockHeight;
        this.votingModel = votingModel;
        this.holdingId = holdingId;
        this.minBalance = minBalance;
    }

    AbstractPoll(ResultSet rs) throws SQLException {
        this.accountId = rs.getLong("account_id");
        this.finishBlockHeight = rs.getInt("finish_height");
        this.votingModel = rs.getByte("voting_model");
        this.holdingId = rs.getLong("holding_id");
        this.minBalance = rs.getLong("min_balance");
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

    public boolean isFinished() { return Nxt.getBlockchain().getHeight() >= finishBlockHeight; }

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
                long assetId0 = getHoldingId(); //todo: fix
                long balance;
                if (assetId0 == 0) {
                    balance = voter.getBalanceNQT();
                } else {
                    balance = voter.getAssetBalanceQNT(assetId0);
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

