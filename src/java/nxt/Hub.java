package nxt;

import nxt.util.VersioningDbTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Hub {

    public static class Hit implements Comparable<Hit> {

        public final Hub hub;
        public final long hitTime;

        private Hit(Hub hub, long hitTime) {
            this.hub = hub;
            this.hitTime = hitTime;
        }

        @Override
        public int compareTo(Hit hit) {
            if (this.hitTime < hit.hitTime) {
                return -1;
            } else if (this.hitTime > hit.hitTime) {
                return 1;
            } else {
                return this.hub.accountId.compareTo(hit.hub.accountId);
            }
        }

    }

    private static final VersioningDbTable<Hub> hubTable = new VersioningDbTable<Hub>() {

        @Override
        protected Long getId(Hub hub) {
            return hub.getAccountId();
        }

        @Override
        protected String table() {
            return "hub";
        }

        @Override
        protected Hub load(Connection con, ResultSet rs) throws SQLException {
            return new Hub(rs);
        }

        @Override
        protected void save(Connection con, Hub hub) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO hub (account_id, min_fee_per_byte, "
                    + "uris, height) KEY (account_id, height) VALUES (?, ?, ?, ?)")) {
                int i = 0;
                pstmt.setLong(++i, hub.getAccountId());
                pstmt.setLong(++i, hub.getMinFeePerByteNQT());
                pstmt.setObject(++i, hub.getUris().toArray(new String[hub.getUris().size()]));
                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
        }

    };

    static void addOrUpdateHub(Transaction transaction, Attachment.MessagingHubAnnouncement attachment) {
        hubTable.insert(new Hub(transaction, attachment));
    }

    static void rollbackHub(Long accountId) {
        hubTable.deleteAfter(accountId, Nxt.getBlockchain().getHeight());
    }

    static void clear() {
        hubTable.truncate();
    }

    private static Long lastBlockId;
    private static List<Hit> lastHits;

    public static List<Hit> getHubHits(Block block) {

        synchronized (Hub.class) {
            if (block.getId().equals(lastBlockId) && lastHits != null) {
                return lastHits;
            }
            List<Hit> currentHits = new ArrayList<>();
            Long currentLastBlockId;

            synchronized (BlockchainImpl.getInstance()) {
                currentLastBlockId = BlockchainImpl.getInstance().getLastBlock().getId();
                if (! currentLastBlockId.equals(block.getId())) {
                    return Collections.emptyList();
                }
                List<Hub> hubs = hubTable.getAll();
                for (Hub hub : hubs) {
                    Account account = Account.getAccount(hub.getAccountId());
                    if (account != null && account.getEffectiveBalanceNXT() >= Constants.MIN_HUB_EFFECTIVE_BALANCE
                            && account.getPublicKey() != null) {
                        currentHits.add(new Hit(hub, Generator.getHitTime(account, block)));
                    }
                }
            }

            Collections.sort(currentHits);
            lastHits = currentHits;
            lastBlockId = currentLastBlockId;
        }
        return lastHits;

    }

    private final Long accountId;
    private final long minFeePerByteNQT;
    private final List<String> uris;

    private Hub(Transaction transaction, Attachment.MessagingHubAnnouncement attachment) {
        this.accountId = transaction.getSenderId();
        this.minFeePerByteNQT = attachment.getMinFeePerByteNQT();
        this.uris = Collections.unmodifiableList(Arrays.asList(attachment.getUris()));
    }

    private Hub(ResultSet rs) throws SQLException {
        this.accountId = rs.getLong("account_id");
        this.minFeePerByteNQT = rs.getLong("min_fee_per_byte");
        this.uris = Collections.unmodifiableList(Arrays.asList((String[])rs.getObject("uris")));
    }

    public Long getAccountId() {
        return accountId;
    }

    public long getMinFeePerByteNQT() {
        return minFeePerByteNQT;
    }

    public List<String> getUris() {
        return uris;
    }

}
