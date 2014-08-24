package nxt.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public abstract class VersioningLinkDbTable<R> extends LinkDbTable<R> {

    protected VersioningLinkDbTable() {
        super(true);
    }

    @Override
    final void rollback(int height) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmtSelectToDelete = con.prepareStatement("SELECT DISTINCT " + idColumnA() + ", " + idColumnB()
                     + " FROM " + table() + " WHERE height >= ?");
             PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + table() + " WHERE height >= ?");
             PreparedStatement pstmtSetLatest = con.prepareStatement("UPDATE " + table() + " SET latest = TRUE "
                     + "WHERE " + idColumnA() + " = ? AND " + idColumnB() + " = ? AND height = "
                     + "(SELECT MAX(height) FROM " + table() + " WHERE " + idColumnA() + " = ? AND " + idColumnB() + " = ?)")) {
            pstmtSelectToDelete.setInt(1, height);
            try (ResultSet rs = pstmtSelectToDelete.executeQuery()) {
                while (rs.next()) {
                    Long idA = rs.getLong(idColumnA());
                    Long idB = rs.getLong(idColumnB());
                    pstmtDelete.setInt(1, height);
                    pstmtDelete.executeUpdate();
                    pstmtSetLatest.setLong(1, idA);
                    pstmtSetLatest.setLong(2, idB);
                    pstmtSetLatest.setLong(3, idA);
                    pstmtSetLatest.setLong(4, idB);
                    pstmtSetLatest.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public final void delete(Long idA, Long idB) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("UPDATE " + table()
                + " SET latest = FALSE WHERE " + idColumnA() + " = ? AND " + idColumnB() + " = ? AND latest = TRUE limit 1")) {
            pstmt.setLong(1, idA);
            pstmt.setLong(2, idB);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

}
