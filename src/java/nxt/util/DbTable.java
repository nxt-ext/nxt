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

    protected abstract T load(Connection con, ResultSet rs) throws SQLException;

    protected abstract void save(Connection con, T t) throws SQLException;

    protected abstract void delete(Connection con, T t) throws SQLException;

    public T get(Long id) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table() + " WHERE id = ?")) {
            pstmt.setLong(1, id);
            return get(con, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public T getBy(String columnName, String value) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table()
                     + " WHERE " + columnName + " = ?")) {
            pstmt.setString(1, value);
            return get(con, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    protected final T get(Connection con, PreparedStatement pstmt) throws SQLException {
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

    public List<T> getManyBy(String columnName, Long value) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table()
                     + " WHERE " + columnName + " = ?")) {
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

    public List<T> getAll() {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table());
             ResultSet rs = pstmt.executeQuery()) {
            List<T> result = new ArrayList<>();
            while (rs.next()) {
                result.add(load(con, rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public int getCount() {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT COUNT(*) FROM " + table());
             ResultSet rs = pstmt.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public void insert(T t) {
        try (Connection con = Db.getConnection()) {
            try {
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

    public final void delete(T t) {
        if (t == null) {
            return;
        }
        try (Connection con = Db.getConnection()) {
            try {
                delete(con, t);
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public final void truncate() {
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
