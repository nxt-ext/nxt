package nxt;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CommonPollStructure {
    public static final byte VOTING_MODEL_BALANCE = 0;
    public static final byte VOTING_MODEL_ACCOUNT = 1;
    public static final byte VOTING_MODEL_ASSET = 2;

    public static final byte OPTION_MODEL_CHOICE = 0;
    public static final byte OPTION_MODEL_BINARY = 1;

    public static final byte DEFAULT_MIN_BALANCE = 0;
    public static final byte DEFAULT_MIN_NUMBER_OF_CHOICES = 1;

    public static final byte NO_ASSET_CODE = 0;


    protected final long accountId;
    protected final int finishBlockHeight;
    protected final byte votingModel;

    protected final long assetId;
    protected final long minBalance;
    protected boolean finished;

    public CommonPollStructure(long accountId, int finishBlockHeight, byte votingModel, long assetId, long minBalance) {
        this.accountId = accountId;
        this.finishBlockHeight = finishBlockHeight;
        this.votingModel = votingModel;
        this.assetId = assetId;
        this.minBalance = minBalance;
        this.finished = false;
    }

    public CommonPollStructure(ResultSet rs) throws SQLException {
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

    public boolean isFinished() { return finished; }

    public void setFinished(boolean finished) { this.finished = finished; }

    protected long calcWeight(Account voter) throws NxtException.IllegalStateException {
        long weight = 0;

        switch (votingModel) {
            case VOTING_MODEL_ASSET:
                long qntBalance = voter.getAssetBalanceQNT(assetId);
                if (qntBalance >= minBalance) {
                    weight = qntBalance;
                }
                break;
            case VOTING_MODEL_ACCOUNT:
            case VOTING_MODEL_BALANCE:
                long nqtBalance = voter.getGuaranteedBalanceNQT(Constants.CONFIRMATIONS_RELIABLE_TX);
                if (nqtBalance >= minBalance) {
                    long nxtBalance = nqtBalance / Constants.ONE_NXT;
                    weight = votingModel == VOTING_MODEL_ACCOUNT ? 1 : nxtBalance;
                }
                break;
            default:
                throw new NxtException.IllegalStateException("Wrong voting model");
        }
        return weight;
    }
}

