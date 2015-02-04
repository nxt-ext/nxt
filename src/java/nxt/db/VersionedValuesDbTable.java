package nxt.db;

import nxt.Nxt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public abstract class VersionedValuesDbTable<T, V> extends ValuesDbTable<T, V> {

    protected VersionedValuesDbTable(String table, DbKey.Factory<T> dbKeyFactory) {
        super(table, dbKeyFactory, true);
    }

    public final boolean delete(T t) {
        if (t == null) {
            return false;
        }
        if (!db.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        DbKey dbKey = dbKeyFactory.newKey(t);
        int height = Nxt.getBlockchain().getHeight();
        try (Connection con = db.getConnection();
             PreparedStatement pstmtCount = con.prepareStatement("SELECT COUNT(*) AS count FROM " + table + dbKeyFactory.getPKClause()
                     + " AND height < ?")) {
            int i = dbKey.setPK(pstmtCount);
            pstmtCount.setInt(i, height);
            try (ResultSet rs = pstmtCount.executeQuery()) {
                rs.next();
                if (rs.getInt("count") > 0) {
                    try (PreparedStatement pstmt = con.prepareStatement("UPDATE " + table
                            + " SET latest = FALSE " + dbKeyFactory.getPKClause() + " AND height = ? AND latest = TRUE")) {
                        int j = dbKey.setPK(pstmt);
                        pstmt.setInt(j, height);
                        if (pstmt.executeUpdate() > 0) {
                            return true;
                        }
                    }
                    List<V> values = get(dbKey);
                    if (values.isEmpty()) {
                        return false;
                    }
                    for (V v : values) {
                        save(con, t, v);
                    }
                    try (PreparedStatement pstmt = con.prepareStatement("UPDATE " + table
                            + " SET latest = FALSE " + dbKeyFactory.getPKClause() + " AND latest = TRUE")) {
                        dbKey.setPK(pstmt);
                        if (pstmt.executeUpdate() == 0) {
                            throw new RuntimeException(); // should not happen
                        }
                    }
                    return true;
                } else {
                    try (PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + table + dbKeyFactory.getPKClause())) {
                        dbKey.setPK(pstmtDelete);
                        return pstmtDelete.executeUpdate() > 0;
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            db.getCache(table).remove(dbKey);
        }
    }

    @Override
    public final void rollback(int height) {
        VersionedEntityDbTable.rollback(db, table, height, dbKeyFactory);
    }

    @Override
    public final void trim(int height) {
        VersionedEntityDbTable.trim(db, table, height, dbKeyFactory);
    }

}
