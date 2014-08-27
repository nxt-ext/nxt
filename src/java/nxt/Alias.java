package nxt;

import nxt.db.DbKey;
import nxt.db.DbUtils;
import nxt.db.VersionedEntityDbTable;

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
            this.buyerId  = DbUtils.getLong(rs, "buyer_id");
        }

        private void save(Connection con) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO alias_offer (id, price, buyer_id, "
                    + "height) VALUES (?, ?, ?, ?)")) {
                int i = 0;
                pstmt.setLong(++i, this.getId());
                pstmt.setLong(++i, this.getPriceNQT());
                DbUtils.setLong(pstmt, ++i, this.getBuyerId());
                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
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

    private static final DbKey.LongKeyFactory<Alias> aliasDbKeyFactory = new DbKey.LongKeyFactory<Alias>("id") {

        @Override
        public DbKey newKey(Alias alias) {
            return newKey(alias.getId());
        }

    };

    private static final VersionedEntityDbTable<Alias> aliasTable = new VersionedEntityDbTable<Alias>(aliasDbKeyFactory) {

        @Override
        protected String table() {
            return "alias";
        }

        @Override
        protected Alias load(Connection con, ResultSet rs) throws SQLException {
            return new Alias(rs);
        }

        @Override
        protected void save(Connection con, Alias alias) throws SQLException {
            alias.save(con);
        }

    };

    private static final DbKey.LongKeyFactory<Offer> offerDbKeyFactory = new DbKey.LongKeyFactory<Offer>("id") {

        @Override
        public DbKey newKey(Offer offer) {
            return newKey(offer.getId());
        }

    };

    private static final VersionedEntityDbTable<Offer> offerTable = new VersionedEntityDbTable<Offer>(offerDbKeyFactory) {

        @Override
        protected String table() {
            return "alias_offer";
        }

        @Override
        protected Offer load(Connection con, ResultSet rs) throws SQLException {
            return new Offer(rs);
        }

        @Override
        protected void save(Connection con, Offer offer) throws SQLException {
            offer.save(con);
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
        return aliasTable.get(aliasDbKeyFactory.newKey(id));
    }

    public static Offer getOffer(Alias alias) {
        return offerTable.get(offerDbKeyFactory.newKey(alias.getId()));
    }

    static void addOrUpdateAlias(Transaction transaction, Attachment.MessagingAliasAssignment attachment) {
        Alias alias = getAlias(attachment.getAliasName());
        if (alias == null) {
            alias = new Alias(transaction.getId(), transaction, attachment);
        } else {
            alias.accountId = transaction.getSenderId();
            alias.aliasURI = attachment.getAliasURI();
            alias.timestamp = transaction.getBlockTimestamp();
        }
        aliasTable.insert(alias);
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

    static void changeOwner(Long newOwnerId, String aliasName, int timestamp) {
        Alias alias = getAlias(aliasName);
        alias.accountId = newOwnerId;
        alias.timestamp = timestamp;
        aliasTable.insert(alias);
        Offer offer = getOffer(alias);
        offerTable.delete(offer);
    }

    static void init() {}


    private Long accountId;
    private final Long id;
    private final String aliasName;
    private String aliasURI;
    private int timestamp;

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

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO alias (id, account_id, alias_name, "
                + "alias_uri, timestamp, height) "
                + "VALUES (?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, this.getId());
            pstmt.setLong(++i, this.getAccountId());
            pstmt.setString(++i, this.getAliasName());
            pstmt.setString(++i, this.getAliasURI());
            pstmt.setInt(++i, this.getTimestamp());
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
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
