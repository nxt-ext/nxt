package nxt.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class VersioningEntityDbTable<T> extends EntityDbTable<T> {

    protected VersioningEntityDbTable() {
        super(true);
    }

    @Override
    final void rollback(int height) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmtSelectToDelete = con.prepareStatement("SELECT DISTINCT id FROM " + table()
                    + " WHERE height >= ?");
             PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + table()
                     + " WHERE height >= ?");
             PreparedStatement pstmtSetLatest = con.prepareStatement("UPDATE " + table()
                     + " SET latest = TRUE WHERE id = ? AND height ="
                     + " (SELECT MAX(height) FROM " + table() + " WHERE id = ?)")) {
            pstmtSelectToDelete.setInt(1, height);
            try (ResultSet rs = pstmtSelectToDelete.executeQuery()) {
                while (rs.next()) {
                    Long id = rs.getLong("id");
                    pstmtDelete.setInt(1, height);
                    pstmtDelete.executeUpdate();
                    pstmtSetLatest.setLong(1, id);
                    pstmtSetLatest.setLong(2, id);
                    pstmtSetLatest.executeUpdate();
                    Db.getCache(table()).remove(id);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public final void delete(T t) {
        if (t == null) {
            return;
        }
        insert(t); // make sure current height is saved
        Db.getCache(table()).remove(getId(t));
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("UPDATE " + table()
                     + " SET latest = FALSE WHERE id = ? AND latest = TRUE LIMIT 1")) {
            pstmt.setLong(1, getId(t));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

}
