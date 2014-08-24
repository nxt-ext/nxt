package nxt.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class VersioningValuesDbTable<T, V> extends ValuesDbTable<T, V> {

    protected VersioningValuesDbTable(DbKey.Factory<T> dbKeyFactory) {
        super(dbKeyFactory, true);
    }

    @Override
    final void rollback(int height) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmtSelectToDelete = con.prepareStatement("SELECT " + dbKeyFactory.getDistinctClause()
                     + " FROM " + table() + " WHERE height >= ?");
             PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + table()
                     + " WHERE height >= ?");
             PreparedStatement pstmtSetLatest = con.prepareStatement("UPDATE " + table()
                     + " SET latest = TRUE " + dbKeyFactory.getPKClause() + " AND height ="
                     + " (SELECT MAX(height) FROM " + table() + dbKeyFactory.getPKClause() + ")")) {
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
                    Db.getCache(table()).remove(dbKey);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

}
