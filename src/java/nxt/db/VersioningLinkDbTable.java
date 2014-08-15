package nxt.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public abstract class VersioningLinkDbTable<R> extends LinkDbTable<R> {

    public R get(Long idA, Long idB) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table() + " WHERE " + idColumnA() + " = ? "
                     + "AND " + idColumnB() + " = ? AND latest = TRUE LIMIT 1")) {
            pstmt.setLong(1, idA);
            pstmt.setLong(2, idB);
            return get(con, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public R get(Long idA, Long idB, int height) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table() + " WHERE " + idColumnA() + " = ? "
                     + "AND " + idColumnB() + " = ? AND height <= ? AND latest = TRUE LIMIT 1")) {
            pstmt.setLong(1, idA);
            pstmt.setLong(2, idB);
            pstmt.setInt(3, height);
            return get(con, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public List<R> getManyByA(Long idA) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table()
                     + " WHERE " + idColumnA() + " = ? AND latest = TRUE")) {
            pstmt.setLong(1, idA);
            return getManyBy(con, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public List<R> getManyByB(Long idB) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table()
                     + " WHERE " + idColumnB() + " = ? AND latest = TRUE")) {
            pstmt.setLong(1, idB);
            return getManyBy(con, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public void insert(Long idA, Long idB, R r) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("UPDATE " + table()
                     + " SET latest = FALSE WHERE " + idColumnA() + " = ? AND " + idColumnB() + " = ? AND latest = TRUE LIMIT 1")) {
            pstmt.setLong(1, idA);
            pstmt.setLong(2, idB);
            pstmt.executeUpdate();
            save(con, idA, idB, r);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
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
    protected final void delete(Connection con, Long idA, Long idB) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("UPDATE " + table()
                + " SET latest = FALSE WHERE " + idColumnA() + " = ? AND " + idColumnB() + " = ? AND latest = TRUE limit 1")) {
            pstmt.setLong(1, idA);
            pstmt.setLong(2, idB);
            pstmt.executeUpdate();
        }
    }

}
