package nxt;

import nxt.db.DbKey;
import nxt.db.EntityDbTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

public final class Asset {

    private static final DbKey.LongIdFactory<Asset> assetDbKeyFactory = new DbKey.LongIdFactory<Asset>("id") {

        @Override
        public DbKey<Asset> newKey(Asset asset) {
            return newKey(asset.getId());
        }

    };

    private static final EntityDbTable<Asset> assetTable = new EntityDbTable<Asset>(assetDbKeyFactory) {

        @Override
        protected String table() {
            return "asset";
        }

        @Override
        protected Asset load(Connection con, ResultSet rs) throws SQLException {
            return new Asset(rs);
        }

        @Override
        protected void save(Connection con, Asset asset) throws SQLException {
            asset.save(con);
        }

    };

    public static Collection<Asset> getAllAssets() {
        return assetTable.getAll();
    }

    public static int getCount() {
        return assetTable.getCount();
    }

    public static Asset getAsset(Long id) {
        return assetTable.get(assetDbKeyFactory.newKey(id));
    }

    public static List<Asset> getAssetsIssuedBy(Long accountId) {
        return assetTable.getManyBy("account_id", accountId);
    }

    static void addAsset(Transaction transaction, Attachment.ColoredCoinsAssetIssuance attachment) {
        assetTable.insert(new Asset(transaction, attachment));
    }

    static void clear() {
        assetTable.truncate();
    }

    private final Long assetId;
    private final Long accountId;
    private final String name;
    private final String description;
    private final long quantityQNT;
    private final byte decimals;

    private Asset(Transaction transaction, Attachment.ColoredCoinsAssetIssuance attachment) {
        this.assetId = transaction.getId();
        this.accountId = transaction.getSenderId();
        this.name = attachment.getName();
        this.description = attachment.getDescription();
        this.quantityQNT = attachment.getQuantityQNT();
        this.decimals = attachment.getDecimals();
    }

    private Asset(ResultSet rs) throws SQLException {
        this.assetId = rs.getLong("id");
        this.accountId = rs.getLong("account_id");
        this.name = rs.getString("name");
        this.description = rs.getString("description");
        this.quantityQNT = rs.getLong("quantity");
        this.decimals = rs.getByte("decimals");
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO asset (id, account_id, name, "
                + "description, quantity, decimals, height) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, this.getId());
            pstmt.setLong(++i, this.getAccountId());
            pstmt.setString(++i, this.getName());
            pstmt.setString(++i, this.getDescription());
            pstmt.setLong(++i, this.getQuantityQNT());
            pstmt.setByte(++i, this.getDecimals());
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }

    public Long getId() {
        return assetId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public long getQuantityQNT() {
        return quantityQNT;
    }

    public byte getDecimals() {
        return decimals;
    }

}
