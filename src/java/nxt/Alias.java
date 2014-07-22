package nxt;

import nxt.util.VersioningDbTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public final class Alias {

    public static class Offer {

        private final long priceNQT;
        private final Long buyerId;
        private final Long aliasId;

        private Offer(Long aliasId, long priceNQT, Long buyerId) {
            this.priceNQT = priceNQT;
            this.buyerId = buyerId;
            this.aliasId = aliasId;
        }

        private Offer(ResultSet rs) throws SQLException {
            this.aliasId = rs.getLong("id");
            this.priceNQT = rs.getLong("price");
            this.buyerId  = rs.getLong("buyer_id");
        }

        public Long getId() {
            return aliasId;
        }

        public long getPriceNQT() {
            return priceNQT;
        }

        public Long getBuyerId() {
            return buyerId;
        }

    }

    private static final VersioningDbTable<Alias> aliasTable = new VersioningDbTable<Alias>() {

        @Override
        protected String table() {
            return "alias";
        }

        @Override
        protected Long getId(Alias alias) {
            return alias.getId();
        }

        @Override
        protected Alias load(Connection con, ResultSet rs) throws SQLException {
            return new Alias(rs);
        }

        @Override
        protected void save(Connection con, Alias alias) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO alias (id, account_id, alias_name, "
                    + "alias_uri, timestamp, height) "
                    + "VALUES (?, ?, ?, ?, ?, ?)")) {
                int i = 0;
                pstmt.setLong(++i, alias.getId());
                pstmt.setLong(++i, alias.getAccountId());
                pstmt.setString(++i, alias.getAliasName());
                pstmt.setString(++i, alias.getAliasURI());
                pstmt.setInt(++i, alias.getTimestamp());
                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
        }

    };

    private static final VersioningDbTable<Offer> offerTable = new VersioningDbTable<Offer>() {

        @Override
        protected String table() {
            return "alias_offer";
        }

        @Override
        protected Long getId(Offer offer) {
            return offer.getId();
        }

        @Override
        protected Offer load(Connection con, ResultSet rs) throws SQLException {
            return new Offer(rs);
        }

        @Override
        protected void save(Connection con, Offer offer) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO alias_offer (id, price, buyer_id, "
                    + "height) VALUES (?, ?, ?, ?)")) {
                int i = 0;
                pstmt.setLong(++i, offer.getId());
                pstmt.setLong(++i, offer.getPriceNQT());
                pstmt.setLong(++i, offer.getBuyerId());
                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
        }

    };

    public static int getCount() {
        return aliasTable.getCount();
    }

    public static List<Alias> getAliasesByOwner(Long accountId) {
        return aliasTable.getManyBy("account_id", accountId);
    }

    public static Alias getAlias(String aliasName) {
        return aliasTable.getBy("alias_name_lower", aliasName.toLowerCase());
    }

    public static Alias getAlias(Long id) {
        return aliasTable.get(id);
    }

    public static Offer getOffer(Alias alias) {
        return offerTable.get(alias.getId());
    }

    static void addOrUpdateAlias(Transaction transaction, Attachment.MessagingAliasAssignment attachment) {
        Alias alias = getAlias(attachment.getAliasName());
        Long aliasId = alias == null ? transaction.getId() : alias.getId();
        aliasTable.insert(new Alias(aliasId, transaction, attachment));
    }

    static void rollbackAlias(Long aliasId) {
        aliasTable.deleteAfter(aliasId, Nxt.getBlockchain().getHeight());
    }

    static void sellAlias(Transaction transaction, Attachment.MessagingAliasSell attachment) {
        final String aliasName = attachment.getAliasName();
        final long priceNQT = attachment.getPriceNQT();
        final Long buyerId = transaction.getRecipientId();
        if (priceNQT > 0) {
            Alias alias = getAlias(aliasName);
            offerTable.insert(new Offer(alias.id, priceNQT, buyerId));
        } else {
            changeOwner(buyerId, aliasName, transaction.getBlockTimestamp());
        }

    }

    static void rollbackOffer(Long aliasId) {
        offerTable.deleteAfter(aliasId, Nxt.getBlockchain().getHeight());
    }

    static void changeOwner(Long newOwnerId, String aliasName, int timestamp) {
        Alias oldAlias = getAlias(aliasName);
        aliasTable.insert(new Alias(oldAlias.id, newOwnerId, aliasName, oldAlias.aliasURI, timestamp));
        Offer offer = getOffer(oldAlias);
        offerTable.delete(offer);
    }

    static void clear() {
        aliasTable.truncate();
        offerTable.truncate();
    }

    private final Long accountId;
    private final Long id;
    private final String aliasName;
    private final String aliasURI;
    private final int timestamp;

    private Alias(Long id, Long accountId, String aliasName, String aliasURI, int timestamp) {
        this.id = id;
        this.accountId = accountId;
        this.aliasName = aliasName;
        this.aliasURI = aliasURI;
        this.timestamp = timestamp;
    }

    private Alias(Long aliasId, Transaction transaction, Attachment.MessagingAliasAssignment attachment) {
        this(aliasId, transaction.getSenderId(), attachment.getAliasName(), attachment.getAliasURI(),
                transaction.getBlockTimestamp());
    }

    private Alias(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.accountId = rs.getLong("account_id");
        this.aliasName = rs.getString("alias_name");
        this.aliasURI = rs.getString("alias_uri");
        this.timestamp = rs.getInt("timestamp");
    }

    public Long getId() {
        return id;
    }

    public String getAliasName() {
        return aliasName;
    }

    public String getAliasURI() {
        return aliasURI;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public Long getAccountId() {
        return accountId;
    }

}
