package nxt.db;

import nxt.util.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public abstract class VersionedEntityDbTable<T> extends EntityDbTable<T> {

    protected VersionedEntityDbTable(DbKey.Factory<T> dbKeyFactory) {
        super(dbKeyFactory, true);
    }

    @Override
    public void rollback(int height) {
        rollback(table(), height, dbKeyFactory);
    }

    public final void delete(T t) {
        if (t == null) {
            return;
        }
        DbKey dbKey = dbKeyFactory.newKey(t);
        Db.getCache(table()).remove(dbKey);
        insert(t); // make sure current height is saved
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("UPDATE " + table()
                     + " SET latest = FALSE " + dbKeyFactory.getPKClause() + " AND latest = TRUE LIMIT 1")) {
            dbKey.setPK(pstmt);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        Db.getCache(table()).remove(dbKey);
    }

    @Override
    public final void trim(int height) {
        //Logger.logDebugMessage("Trimming table " + table());
        //int beforeCount = getCount();
        //Logger.logDebugMessage("Initial entity count is " + beforeCount + " row count is " + getRowCount());
        trim(table(), height, dbKeyFactory);
        //int afterCount = getCount();
        //Logger.logDebugMessage("Final entity count is " + afterCount + " row count is " + getRowCount());
        //if (beforeCount != afterCount) {
        //    throw new RuntimeException("ERROR: Entity count changed during trimming!");
        //}
    }

    static void rollback(String table, int height, DbKey.Factory dbKeyFactory) {
        Logger.logDebugMessage("Rollback " + table + " to " + height);
        try (Connection con = Db.getConnection();
             PreparedStatement pstmtSelectToDelete = con.prepareStatement("SELECT DISTINCT " + dbKeyFactory.getPKColumns()
                     + " FROM " + table + " WHERE height > ?");
             PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + table
                     + " WHERE height > ?");
             PreparedStatement pstmtSetLatest = con.prepareStatement("UPDATE " + table
                     + " SET latest = TRUE " + dbKeyFactory.getPKClause() + " AND height ="
                     + " (SELECT MAX(height) FROM " + table + dbKeyFactory.getPKClause() + ")")) {
            pstmtSelectToDelete.setInt(1, height);
            List<DbKey> dbKeys = new ArrayList<>();
            try (ResultSet rs = pstmtSelectToDelete.executeQuery()) {
                while (rs.next()) {
                    dbKeys.add(dbKeyFactory.newKey(rs));
                }
            }
            pstmtDelete.setInt(1, height);
            pstmtDelete.executeUpdate();
            for (DbKey dbKey : dbKeys) {
                int i = 1;
                i = dbKey.setPK(pstmtSetLatest, i);
                i = dbKey.setPK(pstmtSetLatest, i);
                pstmtSetLatest.executeUpdate();
                Db.getCache(table).remove(dbKey);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    static void trim(String table, int height, DbKey.Factory dbKeyFactory) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmtSelect = con.prepareStatement("SELECT " + dbKeyFactory.getPKColumns() + ", MAX(height) AS max_height"
                     + " FROM " + table + " WHERE height < ? GROUP BY " + dbKeyFactory.getPKColumns() + " HAVING COUNT(DISTINCT height) > 1");
             PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + table + dbKeyFactory.getPKClause()
                     + " AND height < ?")) {
            pstmtSelect.setInt(1, height);
            try (ResultSet rs = pstmtSelect.executeQuery()) {
                while (rs.next()) {
                    DbKey dbKey = dbKeyFactory.newKey(rs);
                    int maxHeight = rs.getInt("max_height");
                    int i = 1;
                    i = dbKey.setPK(pstmtDelete, i);
                    pstmtDelete.setInt(i, maxHeight);
                    pstmtDelete.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

}
