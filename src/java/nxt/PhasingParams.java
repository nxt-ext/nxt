package nxt;

import java.nio.ByteBuffer;
import java.util.Arrays;

import nxt.NxtException.NotValidException;
import nxt.NxtException.ValidationException;
import nxt.util.Convert;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Class for handling phasing data shared between {@link Appendix.Phasing} and {@link AccountControlTxBlocking}
 */
public class PhasingParams {
    private final long quorum;
    private final long[] whitelist;
    private final VoteWeighting voteWeighting;
    
    PhasingParams(ByteBuffer buffer) {
        byte votingModel = buffer.get();
        quorum = buffer.getLong();
        long minBalance = buffer.getLong();
        byte whitelistSize = buffer.get();
        whitelist = new long[whitelistSize];
        for (int pvc = 0; pvc < whitelist.length; pvc++) {
            whitelist[pvc] = buffer.getLong();
        }
        long holdingId = buffer.getLong();
        byte minBalanceModel = buffer.get();
        voteWeighting = new VoteWeighting(votingModel, holdingId, minBalance, minBalanceModel);
    }
    
    PhasingParams(JSONObject attachmentData) {
        quorum = Convert.parseLong(attachmentData.get("phasingQuorum"));
        long minBalance = Convert.parseLong(attachmentData.get("phasingMinBalance"));
        byte votingModel = ((Long) attachmentData.get("phasingVotingModel")).byteValue();
        long holdingId = Convert.parseUnsignedLong((String) attachmentData.get("phasingHolding"));
        JSONArray whitelistJson = (JSONArray) (attachmentData.get("phasingWhitelist"));
        whitelist = new long[whitelistJson.size()];
        for (int i = 0; i < whitelist.length; i++) {
            whitelist[i] = Convert.parseUnsignedLong((String) whitelistJson.get(i));
        }
        byte minBalanceModel = ((Long) attachmentData.get("phasingMinBalanceModel")).byteValue();
        voteWeighting = new VoteWeighting(votingModel, holdingId, minBalance, minBalanceModel);
    }
    
    public PhasingParams(byte votingModel, long holdingId, long quorum,
            long minBalance, byte minBalanceModel, long[] whitelist) {
        this.quorum = quorum;
        this.whitelist = Convert.nullToEmpty(whitelist);
        if (this.whitelist.length > 0) {
            Arrays.sort(this.whitelist);
        }
        voteWeighting = new VoteWeighting(votingModel, holdingId, minBalance, minBalanceModel);
    }
    
    int getMySize() {
        return 1 + 8 + 8 + 1 + 8 * whitelist.length + 8 + 1;
    }
    
    void putMyBytes(ByteBuffer buffer) {
        buffer.put(voteWeighting.getVotingModel().getCode());
        buffer.putLong(quorum);
        buffer.putLong(voteWeighting.getMinBalance());
        buffer.put((byte) whitelist.length);
        for (long account : whitelist) {
            buffer.putLong(account);
        }
        buffer.putLong(voteWeighting.getHoldingId());
        buffer.put(voteWeighting.getMinBalanceModel().getCode());
    }
    
    void putMyJSON(JSONObject json) {
        json.put("phasingQuorum", quorum);
        json.put("phasingMinBalance", voteWeighting.getMinBalance());
        json.put("phasingVotingModel", voteWeighting.getVotingModel().getCode());
        json.put("phasingHolding", Long.toUnsignedString(voteWeighting.getHoldingId()));
        JSONArray whitelistJson = new JSONArray();
        for (long accountId : whitelist) {
            whitelistJson.add(Long.toUnsignedString(accountId));
        }
        json.put("phasingWhitelist", whitelistJson);
        json.put("phasingMinBalanceModel", voteWeighting.getMinBalanceModel().getCode());
    }

    public void validate() throws ValidationException {
        if (whitelist.length > Constants.MAX_PHASING_WHITELIST_SIZE) {
            throw new NxtException.NotValidException("Whitelist is too big");
        }

        long previousAccountId = 0;
        for (long accountId : whitelist) {
            if (accountId == 0) {
                throw new NxtException.NotValidException("Invalid accountId 0 in whitelist");
            }
            if (previousAccountId != 0 && accountId < previousAccountId) {
                throw new NxtException.NotValidException("Whitelist not sorted " + Arrays.toString(whitelist));
            }
            if (accountId == previousAccountId) {
                throw new NxtException.NotValidException("Duplicate accountId " + Long.toUnsignedString(accountId) + " in whitelist");
            }
            previousAccountId = accountId;
        }

        if (quorum <= 0 && voteWeighting.getVotingModel() != VoteWeighting.VotingModel.NONE) {
            throw new NxtException.NotValidException("quorum <= 0");
        }

        if (voteWeighting.getVotingModel() == VoteWeighting.VotingModel.NONE) {
            if (quorum != 0) {
                throw new NxtException.NotValidException("Quorum must be 0 for no-voting phased transaction");
            }
            if (whitelist.length != 0) {
                throw new NxtException.NotValidException("No whitelist needed for no-voting phased transaction");
            }
        }

        if (voteWeighting.getVotingModel() == VoteWeighting.VotingModel.ACCOUNT && whitelist.length > 0 && quorum > whitelist.length) {
            throw new NxtException.NotValidException("Quorum of " + quorum + " cannot be achieved in by-account voting with whitelist of length " + whitelist.length);
        }

        voteWeighting.validate();

    }
    
    public long getQuorum() {
        return quorum;
    }

    public long[] getWhitelist() {
        return whitelist;
    }

    public VoteWeighting getVoteWeighting() {
        return voteWeighting;
    }
    
}
