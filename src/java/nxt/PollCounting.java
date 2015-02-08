package nxt;

public final class PollCounting {

    private final byte votingModel;
    private final long holdingId; //either asset id or MS coin id
    private final long minBalance;
    private final byte minBalanceModel;


    public PollCounting(byte votingModel, long holdingId, long minBalance, byte minBalanceModel) {
        this.votingModel = votingModel;
        this.holdingId = holdingId;
        this.minBalance = minBalance;
        this.minBalanceModel = minBalanceModel;
    }

    public final byte getVotingModel() {
        return votingModel;
    }

    public final long getMinBalance() {
        return minBalance;
    }

    public final long getHoldingId() {
        return holdingId;
    }

    public final byte getMinBalanceModel() {
        return minBalanceModel;
    }

    long calcWeight(long voterId, int height) {
        long weight = 0;

        switch (votingModel) {
            case Constants.VOTING_MODEL_ASSET:
                long qntBalance = Account.getAssetBalanceQNT(voterId, holdingId, height);
                if (qntBalance >= minBalance) {
                    weight = qntBalance;
                }
                break;
            case Constants.VOTING_MODEL_CURRENCY:
                long units = Account.getCurrencyUnits(voterId, holdingId, height);
                if (units >= minBalance) {
                    weight = units;
                }
                break;
            case Constants.VOTING_MODEL_NQT:
                long nqtBalance = Account.getAccount(voterId, height).getBalanceNQT();
                if (nqtBalance >= minBalance) {
                    weight = nqtBalance;
                }
                break;
            case Constants.VOTING_MODEL_ACCOUNT:
                long result;
                long balance;
                switch (minBalanceModel) {
                    case Constants.VOTING_MINBALANCE_ASSET:
                        balance = Account.getAssetBalanceQNT(voterId, holdingId, height);
                        break;
                    case Constants.VOTING_MINBALANCE_CURRENCY:
                        balance = Account.getCurrencyUnits(voterId, holdingId, height);
                        break;
                    case Constants.VOTING_MINBALANCE_NQT:
                        balance = Account.getAccount(voterId, height).getBalanceNQT();
                        break;
                    default:
                        throw new RuntimeException("Invalid minBalanceModel " + minBalanceModel);
                }
                if (balance >= minBalance) {
                    result = 1;
                } else {
                    result = 0;
                }
                weight = result;
                break;
            default:
                throw new RuntimeException("Invalid votingModel " + votingModel);
        }
        return weight;
    }

}
