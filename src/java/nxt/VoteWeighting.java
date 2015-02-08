package nxt;

import nxt.util.Convert;

public final class VoteWeighting {

    private final byte votingModel;
    private final long holdingId; //either asset id or MS coin id
    private final long minBalance;
    private final byte minBalanceModel;


    public VoteWeighting(byte votingModel, long holdingId, long minBalance, byte minBalanceModel) {
        this.votingModel = votingModel;
        this.holdingId = holdingId;
        this.minBalance = minBalance;
        this.minBalanceModel = minBalanceModel;
    }

    public byte getVotingModel() {
        return votingModel;
    }

    public long getMinBalance() {
        return minBalance;
    }

    public long getHoldingId() {
        return holdingId;
    }

    public byte getMinBalanceModel() {
        return minBalanceModel;
    }

    public void validate() throws NxtException.ValidationException {
        if (votingModel != Constants.VOTING_MODEL_ACCOUNT
                && votingModel != Constants.VOTING_MODEL_NQT
                && votingModel != Constants.VOTING_MODEL_ASSET
                && votingModel != Constants.VOTING_MODEL_CURRENCY) {
            throw new NxtException.NotValidException("Invalid voting model value: " + votingModel);
        }
        if ((votingModel == Constants.VOTING_MODEL_ASSET || votingModel == Constants.VOTING_MODEL_CURRENCY) && holdingId == 0) {
            throw new NxtException.NotValidException("No holdingId provided");
        }
        if (votingModel == Constants.VOTING_MODEL_CURRENCY && Currency.getCurrency(holdingId) == null) {
            throw new NxtException.NotCurrentlyValidException("Currency " + Convert.toUnsignedLong(holdingId) + " not found");
        }
        if (votingModel == Constants.VOTING_MODEL_ASSET && Asset.getAsset(holdingId) == null) {
            throw new NxtException.NotCurrentlyValidException("Asset " + Convert.toUnsignedLong(holdingId) + " not found");
        }
        if (minBalance < 0) {
            throw new NxtException.NotValidException("Invalid minBalance " + minBalance);
        }
        if (minBalance > 0) {
            if (minBalanceModel != Constants.VOTING_MINBALANCE_ASSET
                    && minBalanceModel != Constants.VOTING_MINBALANCE_NQT
                    && minBalanceModel != Constants.VOTING_MINBALANCE_CURRENCY) {
                throw new NxtException.NotValidException("Invalid min balance model " + minBalanceModel);
            }
            if ((votingModel == Constants.VOTING_MODEL_ASSET && minBalanceModel != Constants.VOTING_MINBALANCE_ASSET)
                    || (votingModel == Constants.VOTING_MODEL_NQT && minBalanceModel != Constants.VOTING_MINBALANCE_NQT)
                    || (votingModel == Constants.VOTING_MODEL_CURRENCY && minBalanceModel != Constants.VOTING_MINBALANCE_CURRENCY)) {
                throw new NxtException.NotValidException("Invalid min balance model: " + minBalanceModel + " for voting model " + votingModel);
            }
            if ((minBalanceModel == Constants.VOTING_MINBALANCE_ASSET || minBalanceModel == Constants.VOTING_MINBALANCE_CURRENCY) && holdingId == 0) {
                throw new NxtException.NotValidException("No holdingId provided");
            }
            if (minBalanceModel == Constants.VOTING_MINBALANCE_ASSET && Asset.getAsset(holdingId) == null) {
                throw new NxtException.NotCurrentlyValidException("Invalid min balance asset: " + Convert.toUnsignedLong(holdingId));
            }
            if (minBalanceModel == Constants.VOTING_MINBALANCE_CURRENCY && Currency.getCurrency(holdingId) == null) {
                throw new NxtException.NotCurrentlyValidException("Invalid min balance currency: " + Convert.toUnsignedLong(holdingId));
            }
        }
        if (minBalance == 0 && votingModel == Constants.VOTING_MODEL_ACCOUNT && holdingId != 0) {
            throw new NxtException.NotValidException("HoldingId cannot be used in by account voting with no min balance");
        }
    }

    public long calcWeight(long voterId, int height) {
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
                if (minBalance == 0) {
                    weight = 1;
                } else {
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
                        weight = 1;
                    } else {
                        weight = 0;
                    }
                }
                break;
            default:
                throw new RuntimeException("Invalid votingModel " + votingModel);
        }
        return weight;
    }

}
