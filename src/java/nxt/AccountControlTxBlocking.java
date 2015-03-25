package nxt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import nxt.NxtException.AccountControlException;
import nxt.db.DbKey;
import nxt.db.DbUtils;
import nxt.db.VersionedEntityDbTable;
import nxt.db.VersionedValuesDbTable;
import nxt.util.Convert;

public class AccountControlTxBlocking {
    public static void checkTransaction(Transaction transaction) throws AccountControlException {
        
    }
    
    public static class PhasingOnly {
        private final DbKey dbKey;
        private final long accountId;
        private final PhasingParams phasingParams;
        
        private PhasingOnly(long accountId, PhasingParams params){
            this.accountId = accountId;
            
            dbKey = ruleDbKeyFactory.newKey(this.accountId);
            
            phasingParams = params;
        }

        private PhasingOnly(ResultSet rs) throws SQLException {
            accountId = rs.getLong("account_id");
            
            dbKey = ruleDbKeyFactory.newKey(this.accountId);
            
            List<Long> whitelistedVoters;
            if (rs.getByte("whitelist_size") == 0) {
                whitelistedVoters = Collections.emptyList();
            } else {
                whitelistedVoters = phasingControlVoterTable.get(dbKey);
            }
            
            phasingParams = new PhasingParams(rs.getByte("voting_model"), 
                    rs.getLong("holding_id"), 
                    rs.getLong("quorum"), 
                    rs.getLong("min_balance"), 
                    rs.getByte("min_balance_model"),
                    Convert.toArray(whitelistedVoters));
        }
        
        private void save(Connection con) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO account_control_phasing "
                    + "(account_id, whitelist_size, voting_model, quorum, min_balance, holding_id, min_balance_model, height) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                int i = 0;
                pstmt.setLong(++i, this.accountId);
                pstmt.setByte(++i, (byte) phasingParams.getWhitelist().length);
                pstmt.setByte(++i, phasingParams.getVoteWeighting().getVotingModel().getCode());
                DbUtils.setLongZeroToNull(pstmt, ++i, phasingParams.getQuorum());
                DbUtils.setLongZeroToNull(pstmt, ++i, phasingParams.getVoteWeighting().getMinBalance());
                DbUtils.setLongZeroToNull(pstmt, ++i, phasingParams.getVoteWeighting().getHoldingId());
                pstmt.setByte(++i, phasingParams.getVoteWeighting().getMinBalanceModel().getCode());
                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
            phasingControlVoterTable.insert(this, Arrays.stream(phasingParams.getWhitelist()).boxed().collect(Collectors.toList()));
        }
        
        public static void set(Transaction transaction, Attachment.SetPhasingOnly attachment) {
            //transaction.getSenderId()
            //phasingControlTable.insert(new PhasingOnly(transaction.getSenderId(), attachment.getPhasingParams()));
        }
    }
    
    private static final DbKey.LongKeyFactory<PhasingOnly> ruleDbKeyFactory = new DbKey.LongKeyFactory<PhasingOnly>("account_id") {
        @Override
        public DbKey newKey(PhasingOnly rule) {
            return rule.dbKey;
        }
    };
    
    private static final VersionedEntityDbTable<PhasingOnly> phasingControlTable = new VersionedEntityDbTable<PhasingOnly>("account_control_phasing", ruleDbKeyFactory) {

        @Override
        protected PhasingOnly load(Connection con, ResultSet rs)
                throws SQLException {
            return new PhasingOnly(rs);
        }

        @Override
        protected void save(Connection con, PhasingOnly t)
                throws SQLException {
            t.save(con);
        }
    };
    
    private static final VersionedValuesDbTable<PhasingOnly, Long> phasingControlVoterTable = 
            new VersionedValuesDbTable<PhasingOnly, Long>("account_control_phasing_voter", ruleDbKeyFactory) {

        @Override
        protected Long load(Connection con, ResultSet rs)
                throws SQLException {
            return rs.getLong("voter_id");
        }

        @Override
        protected void save(Connection con, PhasingOnly t, Long v)
                throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO account_control_phasing_voter (account_id, voter_id, height) VALUES (?, ?, ?)")) {
                int i = 0;
                pstmt.setLong(++i, t.accountId);
                pstmt.setLong(++i, v);
                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
        }
    };
}
