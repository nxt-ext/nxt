package nxt.db;

import nxt.Nxt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class PrunableDbTable<T> extends EntityDbTable<T> {

    protected PrunableDbTable(String table, DbKey.Factory<T> dbKeyFactory) {
        super(table, dbKeyFactory, false, null);
    }

    protected PrunableDbTable(String table, DbKey.Factory<T> dbKeyFactory, String fullTextSearchColumns) {
        super(table, dbKeyFactory, false, fullTextSearchColumns);
    }

    @Override
    public void trim(int height) {
        super.trim(height);
        try (Connection con = db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("DELETE FROM " + table + " WHERE expiration < ?")) {
            pstmt.setInt(1, Nxt.getEpochTime());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }

    }
}
