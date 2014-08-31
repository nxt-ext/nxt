package nxt.db;

import nxt.util.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class VersionedEntityDbTable<T> extends EntityDbTable<T> {

    protected VersionedEntityDbTable(DbKey.Factory<T> dbKeyFactory) {
        super(dbKeyFactory, true);
    }

    @Override
    public final void rollback(int height) {
        rollback(table(), height, dbKeyFactory);
    }

    @Override
    public final void delete(T t) {
        if (t == null) {
            return;
        }
        insert(t); // make sure current height is saved
        DbKey dbKey = dbKeyFactory.newKey(t);
        Db.getCache(table()).remove(dbKey);
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("UPDATE " + table()
                     + " SET latest = FALSE " + dbKeyFactory.getPKClause() + " AND latest = TRUE LIMIT 1")) {
            dbKey.setPK(pstmt);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public final void trim(int height) {
        //Logger.logDebugMessage("Trimming table " + table());
        //Logger.logDebugMessage("Initial entity count is " + getCount() + " row count is " + getRowCount());
        trim(table(), height, dbKeyFactory);
        //Logger.logDebugMessage("Final entity count is " + getCount() + " row count is " + getRowCount());
    }

    static void rollback(String table, int height, DbKey.Factory dbKeyFactory) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmtSelectToDelete = con.prepareStatement("SELECT " + dbKeyFactory.getDistinctClause()
                     + " FROM " + table + " WHERE height >= ?");
             PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + table
                     + " WHERE height >= ?");
             PreparedStatement pstmtSetLatest = con.prepareStatement("UPDATE " + table
                     + " SET latest = TRUE " + dbKeyFactory.getPKClause() + " AND height ="
                     + " (SELECT MAX(height) FROM " + table + dbKeyFactory.getPKClause() + ")")) {
            pstmtSelectToDelete.setInt(1, height);
            try (ResultSet rs = pstmtSelectToDelete.executeQuery()) {
                while (rs.next()) {
                    DbKey dbKey = dbKeyFactory.newKey(rs);
                    pstmtDelete.setInt(1, height);
                    pstmtDelete.executeUpdate();
                    int i = 1;
                    i = dbKey.setPK(pstmtSetLatest, i);
                    i = dbKey.setPK(pstmtSetLatest, i);
                    pstmtSetLatest.executeUpdate();
                    Db.getCache(table).remove(dbKey);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    static void trim(String table, int height, DbKey.Factory dbKeyFactory) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmtSelect = con.prepareStatement("SELECT " + dbKeyFactory.getDistinctClause()
                     + " FROM " + table + " WHERE height < ?");
             PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + table + dbKeyFactory.getPKClause()
                     + " AND height < (SELECT MAX(height) FROM " + table + dbKeyFactory.getPKClause() + " AND height < ?)")) {
            pstmtSelect.setInt(1, height);
            try (ResultSet rs = pstmtSelect.executeQuery()) {
                while (rs.next()) {
                    DbKey dbKey = dbKeyFactory.newKey(rs);
                    int i = 1;
                    i = dbKey.setPK(pstmtDelete, i);
                    i = dbKey.setPK(pstmtDelete, i);
                    pstmtDelete.setInt(i, height);
                    pstmtDelete.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

}
