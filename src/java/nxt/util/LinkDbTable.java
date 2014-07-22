package nxt.util;

import nxt.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public abstract class LinkDbTable<R> {

    protected abstract String table();

    protected abstract R load(Connection con, ResultSet rs) throws SQLException;

    protected abstract void save(Connection con, Long idA, Long idB, R r) throws SQLException;

    protected abstract void deleteA(Connection con, Long idA) throws SQLException;

    protected abstract void deleteB(Connection con, Long idB) throws SQLException;

    protected abstract String idColumnA();

    protected abstract String idColumnB();

    public R get(Long idA, Long idB) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table() + " WHERE " + idColumnA() + " = ? "
                     + "AND " + idColumnB() + " = ?")) {
            pstmt.setLong(1, idA);
            pstmt.setLong(2, idB);
            return get(con, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    protected final R get(Connection con, PreparedStatement pstmt) throws SQLException {
        try (ResultSet rs = pstmt.executeQuery()) {
            R r = null;
            if (rs.next()) {
                r = load(con, rs);
            }
            if (rs.next()) {
                throw new RuntimeException("Multiple records found");
            }
            return r;
        }
    }

    public List<R> getManyByA(Long idA) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table()
                     + " WHERE " + idColumnA() + " = ? ")) {
            pstmt.setLong(1, idA);
            return getManyBy(con, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public List<R> getManyByB(Long idB) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table()
                     + " WHERE " + idColumnB() + " = ? ")) {
            pstmt.setLong(1, idB);
            return getManyBy(con, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public final List<R> getManyBy(Connection con, PreparedStatement pstmt) {
        try {
            List<R> result = new ArrayList<>();
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

    public void insert(Long idA, Long idB, R r) {
        try (Connection con = Db.getConnection()) {
            save(con, idA, idB, r);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public final void deleteA(Long idA) {
        try (Connection con = Db.getConnection()) {
            deleteA(con, idA);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public final void deleteB(Long idB) {
        try (Connection con = Db.getConnection()) {
            deleteB(con, idB);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public final void truncate() {
        try (Connection con = Db.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.executeUpdate("SET REFERENTIAL_INTEGRITY FALSE");
            stmt.executeUpdate("TRUNCATE TABLE " + table());
            stmt.executeUpdate("SET REFERENTIAL_INTEGRITY TRUE");
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }


}
