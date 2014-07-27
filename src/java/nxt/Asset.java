package nxt;

import nxt.db.DbTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

public final class Asset {

    private static final DbTable<Asset> assetTable = new DbTable<Asset>() {

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
            try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO asset (id, account_id, name, "
                    + "description, quantity, decimals) VALUES (?, ?, ?, ?, ?, ?)")) {
                int i = 0;
                pstmt.setLong(++i, asset.getId());
                pstmt.setLong(++i, asset.getAccountId());
                pstmt.setString(++i, asset.getName());
                pstmt.setString(++i, asset.getDescription());
                pstmt.setLong(++i, asset.getQuantityQNT());
                pstmt.setByte(++i, asset.getDecimals());
                pstmt.executeUpdate();
            }
        }

        @Override
        protected void delete(Connection con, Asset asset) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("DELETE FROM asset WHERE id = ?")) {
                pstmt.setLong(1, asset.getId());
                pstmt.executeUpdate();
            }
        }

    };

    public static Collection<Asset> getAllAssets() {
        return assetTable.getAll();
    }

    public static int getCount() {
        return assetTable.getCount();
    }

    public static Asset getAsset(Long id) {
        return assetTable.get(id);
    }

    public static List<Asset> getAssetsIssuedBy(Long accountId) {
        return assetTable.getManyBy("account_id", accountId);
    }

    static void addAsset(Transaction transaction, Attachment.ColoredCoinsAssetIssuance attachment) {
        assetTable.insert(new Asset(transaction, attachment));
    }

    static void removeAsset(Long assetId) {
        assetTable.delete(getAsset(assetId));
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
