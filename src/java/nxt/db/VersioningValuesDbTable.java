package nxt.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public abstract class VersioningValuesDbTable<T, V> extends ValuesDbTable<T, V> {

    protected abstract Long getId(T t);

    @Override
    public final List<V> get(Long id) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table()
                     + " WHERE id = ? AND latest = TRUE")) {
            pstmt.setLong(1, id);
            return getManyBy(con, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public final List<V> get(Long id, int height) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table()
                     + " WHERE id = ? AND height <= ? AND latest = TRUE")) {
            pstmt.setLong(1, id);
            pstmt.setInt(2, height);
            return getManyBy(con, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public final void insert(T t, List<V> values) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("UPDATE " + table()
                     + " SET latest = FALSE WHERE id = ? AND latest = TRUE LIMIT 1")) {
            pstmt.setLong(1, getId(t));
            pstmt.executeUpdate();
            for (V v : values) {
                save(con, t, v);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public final void insert(T t, V v) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("UPDATE " + table()
                     + " SET latest = FALSE WHERE id = ? AND latest = TRUE LIMIT 1")) {
            pstmt.setLong(1, getId(t));
            pstmt.executeUpdate();
            save(con, t, v);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public final void rollbackTo(Long id, int height) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + table()
                     + " WHERE id = ? AND height > ?");
             PreparedStatement pstmtSetLatest = con.prepareStatement("UPDATE " + table()
                     + " SET latest = TRUE WHERE id = ? AND height ="
                     + " (SELECT MAX(height) FROM " + table() + " WHERE id = ?)")) {
            pstmtDelete.setLong(1, id);
            pstmtDelete.setInt(2, height);
            pstmtDelete.executeUpdate();
            pstmtSetLatest.setLong(1, id);
            pstmtSetLatest.setLong(2, id);
            pstmtSetLatest.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    protected final void delete(Connection con, T t) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("UPDATE " + table()
                + " SET latest = FALSE WHERE id = ? AND latest = TRUE LIMIT 1")) {
            pstmt.setLong(1, getId(t));
            pstmt.executeUpdate();
        }
    }
}
