package nxt;

import nxt.util.Convert;

public final class VoteWeighting {

    public enum VotingModel {
        ACCOUNT(0) {
            @Override
            public final long calcWeight(VoteWeighting voteWeighting, long voterId, int height) {
                return (voteWeighting.minBalance == 0 || voteWeighting.minBalanceModel.getBalance(voteWeighting, voterId, height) >= voteWeighting.minBalance) ? 1 : 0;
            }
            @Override
            public final MinBalanceModel defaultMinBalanceModel() {
                return MinBalanceModel.NONE;
            }
        },
        NQT(1) {
            @Override
            public final long calcWeight(VoteWeighting voteWeighting, long voterId, int height) {
                long nqtBalance = Account.getAccount(voterId, height).getBalanceNQT();
                return nqtBalance >= voteWeighting.minBalance ? nqtBalance : 0;
            }
            @Override
            public final MinBalanceModel defaultMinBalanceModel() {
                return MinBalanceModel.NQT;
            }
        },
        ASSET(2) {
            @Override
            public final long calcWeight(VoteWeighting voteWeighting, long voterId, int height) {
                long qntBalance = Account.getAssetBalanceQNT(voterId, voteWeighting.holdingId, height);
                return qntBalance >= voteWeighting.minBalance ? qntBalance : 0;
            }
            @Override
            public final MinBalanceModel defaultMinBalanceModel() {
                return MinBalanceModel.ASSET;
            }
        },
        CURRENCY(3) {
            @Override
            public final long calcWeight(VoteWeighting voteWeighting, long voterId, int height) {
                long units = Account.getCurrencyUnits(voterId, voteWeighting.holdingId, height);
                return units >= voteWeighting.minBalance ? units : 0;
            }
            @Override
            public final MinBalanceModel defaultMinBalanceModel() {
                return MinBalanceModel.CURRENCY;
            }
        };

        private final byte code;

        private VotingModel(int code) {
            this.code = (byte)code;
        }

        public byte getCode() {
            return code;
        }

        public abstract long calcWeight(VoteWeighting voteWeighting, long voterId, int height);

        public abstract MinBalanceModel defaultMinBalanceModel();

        public static VotingModel get(byte code) {
            for (VotingModel votingModel : values()) {
                if (votingModel.getCode() == code) {
                    return votingModel;
                }
            }
            return null;
        }
    }

    public enum MinBalanceModel {
        NONE(0) {
            @Override
            public final long getBalance(VoteWeighting voteWeighting, long voterId, int height) {
                throw new UnsupportedOperationException();
            }
        },
        NQT(1) {
            @Override
            public final long getBalance(VoteWeighting voteWeighting, long voterId, int height) {
                return Account.getAccount(voterId, height).getBalanceNQT();
            }
        },
        ASSET(2) {
            @Override
            public final long getBalance(VoteWeighting voteWeighting, long voterId, int height) {
                return Account.getAssetBalanceQNT(voterId, voteWeighting.holdingId, height);
            }
        },
        CURRENCY(3) {
            @Override
            public final long getBalance(VoteWeighting voteWeighting, long voterId, int height) {
                return Account.getCurrencyUnits(voterId, voteWeighting.holdingId, height);
            }
        };

        private final byte code;

        private MinBalanceModel(int code) {
            this.code = (byte)code;
        }

        public byte getCode() {
            return code;
        }

        public abstract long getBalance(VoteWeighting voteWeighting, long voterId, int height);

        public static MinBalanceModel get(byte code) {
            for (MinBalanceModel minBalanceModel : values()) {
                if (minBalanceModel.getCode() == code) {
                    return minBalanceModel;
                }
            }
            return null;
        }
    }

    private final VotingModel votingModel;
    private final long holdingId; //either asset id or MS coin id
    private final long minBalance;
    private final MinBalanceModel minBalanceModel;


    public VoteWeighting(byte votingModel, long holdingId, long minBalance, byte minBalanceModel) {
        this.votingModel = VotingModel.get(votingModel);
        this.holdingId = holdingId;
        this.minBalance = minBalance;
        this.minBalanceModel = MinBalanceModel.get(minBalanceModel);
    }

    public VotingModel getVotingModel() {
        return votingModel;
    }

    public long getMinBalance() {
        return minBalance;
    }

    public long getHoldingId() {
        return holdingId;
    }

    public MinBalanceModel getMinBalanceModel() {
        return minBalanceModel;
    }

    public void validate() throws NxtException.ValidationException {
        if (votingModel == null) {
            throw new NxtException.NotValidException("Invalid voting model");
        }
        if (minBalanceModel == null) {
            throw new NxtException.NotValidException("Invalid min balance model");
        }
        if ((votingModel == VotingModel.ASSET || votingModel == VotingModel.CURRENCY) && holdingId == 0) {
            throw new NxtException.NotValidException("No holdingId provided");
        }
        if (votingModel == VotingModel.CURRENCY && Currency.getCurrency(holdingId) == null) {
            throw new NxtException.NotCurrentlyValidException("Currency " + Convert.toUnsignedLong(holdingId) + " not found");
        }
        if (votingModel == VotingModel.ASSET && Asset.getAsset(holdingId) == null) {
            throw new NxtException.NotCurrentlyValidException("Asset " + Convert.toUnsignedLong(holdingId) + " not found");
        }
        if (minBalance < 0) {
            throw new NxtException.NotValidException("Invalid minBalance " + minBalance);
        }
        if (minBalance > 0) {
            if (minBalanceModel == MinBalanceModel.NONE) {
                throw new NxtException.NotValidException("Invalid min balance model " + minBalanceModel);
            }
            if (votingModel.defaultMinBalanceModel() != MinBalanceModel.NONE && votingModel.defaultMinBalanceModel() != minBalanceModel) {
                throw new NxtException.NotValidException("Invalid min balance model: " + minBalanceModel + " for voting model " + votingModel);
            }
            if ((minBalanceModel == MinBalanceModel.ASSET || minBalanceModel == MinBalanceModel.CURRENCY) && holdingId == 0) {
                throw new NxtException.NotValidException("No holdingId provided");
            }
            if (minBalanceModel == MinBalanceModel.ASSET && Asset.getAsset(holdingId) == null) {
                throw new NxtException.NotCurrentlyValidException("Invalid min balance asset: " + Convert.toUnsignedLong(holdingId));
            }
            if (minBalanceModel == MinBalanceModel.CURRENCY && Currency.getCurrency(holdingId) == null) {
                throw new NxtException.NotCurrentlyValidException("Invalid min balance currency: " + Convert.toUnsignedLong(holdingId));
            }
        }
        if (minBalance == 0 && votingModel == VotingModel.ACCOUNT && holdingId != 0) {
            throw new NxtException.NotValidException("HoldingId cannot be used in by account voting with no min balance");
        }
    }

    public long calcWeight(long voterId, int height) {
        return votingModel.calcWeight(this, voterId, height);
    }

    public boolean isBalanceIndependent() {
        return votingModel == VotingModel.ACCOUNT && minBalance == 0;
    }

}
