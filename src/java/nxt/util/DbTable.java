package nxt.util;

import nxt.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public abstract class DbTable<T> {

    protected abstract String table();

    protected abstract Long getId(T t);

    protected abstract T load(Connection con, ResultSet rs);

    protected abstract void save(Connection con, T t);

    public final T get(Long id) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table()
                     + " WHERE id = ? AND latest = TRUE LIMIT 1")) {
            pstmt.setLong(1, id);
            return get(con, pstmt);
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

    public final List<T> getManyBy(String columnName, Long value) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table()
                     + " WHERE " + columnName + " = ? AND latest = TRUE")) {
            pstmt.setLong(1, value);
            List<T> result = new ArrayList<>();
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    result.add(load(con, rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    private T get(Connection con, PreparedStatement pstmt) throws SQLException {
        try (ResultSet rs = pstmt.executeQuery()) {
            T t = null;
            if (rs.next()) {
                t = load(con, rs);
            }
            if (rs.next()) {
                throw new RuntimeException("Multiple records found");
            }
            return t;
        }
    }

    public int getCount() {
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

    public void insert(T t) {
        try (Connection con = Db.getConnection();
        PreparedStatement pstmt = con.prepareStatement("UPDATE " + table()
                + " SET latest = FALSE WHERE id = ? AND latest = TRUE limit 1")) {
            try {
                pstmt.setLong(1, getId(t));
                pstmt.executeUpdate();
                save(con, t);
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public void deleteAfter(Long id, int height) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + table()
                     + " WHERE id = ? AND height > ?");
             PreparedStatement pstmtSetLast = con.prepareStatement("UPDATE " + table()
                     + " SET latest = TRUE WHERE id = ? AND height ="
                     + " (SELECT MAX(height) FROM " + table() + " WHERE id = ?)")) {
            try {
                pstmtDelete.setLong(1, id);
                pstmtDelete.setInt(2, height);
                pstmtDelete.executeUpdate();
                pstmtSetLast.setLong(1, id);
                pstmtSetLast.setLong(2, id);
                pstmtSetLast.executeUpdate();
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public void invalidate(Long id) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("UPDATE " + table()
                     + " SET latest = FALSE WHERE id = ? AND latest = TRUE limit 1")) {
            try {
                pstmt.setLong(1, id);
                pstmt.executeUpdate();
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public void truncate() {
        try (Connection con = Db.getConnection();
             Statement stmt = con.createStatement()) {
            try {
                stmt.executeUpdate("SET REFERENTIAL_INTEGRITY FALSE");
                stmt.executeUpdate("TRUNCATE TABLE " + table());
                stmt.executeUpdate("SET REFERENTIAL_INTEGRITY TRUE");
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

}
