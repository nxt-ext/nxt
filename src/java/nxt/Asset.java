package nxt;

import nxt.util.DbTable;

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
            Long assetId = rs.getLong("id");
            Long accountId = rs.getLong("account_id");
            String name = rs.getString("name");
            String description = rs.getString("description");
            long quantityQNT = rs.getLong("quantity");
            byte decimals = rs.getByte("decimals");
            return new Asset(assetId, accountId, name, description, quantityQNT, decimals);
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

    static void addAsset(Long assetId, Long accountId, String name, String description, long quantityQNT, byte decimals) {
        Asset asset = new Asset(assetId, accountId, name, description, quantityQNT, decimals);
        assetTable.insert(asset);
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

    private Asset(Long assetId, Long accountId, String name, String description, long quantityQNT, byte decimals) {
        this.assetId = assetId;
        this.accountId = accountId;
        this.name = name;
        this.description = description;
        this.quantityQNT = quantityQNT;
        this.decimals = decimals;
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
