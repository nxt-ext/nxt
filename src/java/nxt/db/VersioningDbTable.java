package nxt.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public abstract class VersioningDbTable<T> extends CachingDbTable<T> {

    @Override
    public final T get(Long id) {
        T t;
        if (Db.isInTransaction()) {
            t = (T)Db.getCache(table()).get(id);
            if (t != null) {
                return t;
            }
        }
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table()
                     + " WHERE id = ? AND latest = TRUE LIMIT 1")) {
            pstmt.setLong(1, id);
            t = get(con, pstmt);
            if (Db.isInTransaction() && t != null) {
                Db.getCache(table()).put(id, t);
            }
            return t;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public final T get(Long id, int height) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table()
                     + " WHERE id = ? AND height <= ? LIMIT 1")) {
            pstmt.setLong(1, id);
            pstmt.setInt(2, height);
            return get(con, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public final T getBy(String columnName, String value) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table()
                     + " WHERE " + columnName + " = ? AND latest = TRUE")) {
            pstmt.setString(1, value);
            return get(con, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public final List<T> getManyBy(String columnName, Long value) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table()
                     + " WHERE " + columnName + " = ? AND latest = TRUE " + defaultSort())) {
            pstmt.setLong(1, value);
            return getManyBy(con, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public final List<T> getAll() {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table()
                     + " WHERE latest = TRUE " + defaultSort())) {
            return getManyBy(con, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public final int getCount() {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT COUNT(*) FROM " + table()
                     + " WHERE latest = TRUE");
             ResultSet rs = pstmt.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public final void insert(T t) {
        T cachedT = (T)Db.getCache(table()).get(getId(t));
        if (cachedT == null) {
            Db.getCache(table()).put(getId(t), t);
        } else if (t != cachedT) { // not a bug
            throw new IllegalStateException("Different instance found in Db cache, perhaps trying to save an object "
                    + "that was read outside the current transaction");
        }
        try (Connection con = Db.getConnection();
        PreparedStatement pstmt = con.prepareStatement("UPDATE " + table()
                + " SET latest = FALSE WHERE id = ? AND latest = TRUE LIMIT 1")) {
            pstmt.setLong(1, getId(t));
            pstmt.executeUpdate();
            save(con, t);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
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
    protected final void delete(Connection con, T t) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("UPDATE " + table()
                + " SET latest = FALSE WHERE id = ? AND latest = TRUE LIMIT 1")) {
            pstmt.setLong(1, getId(t));
            pstmt.executeUpdate();
        }
    }
}
