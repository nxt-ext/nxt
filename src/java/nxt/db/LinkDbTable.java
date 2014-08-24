package nxt.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

//TODO: caching
public abstract class LinkDbTable<R> extends BasicDbTable {

    private final boolean multiversion;

    protected LinkDbTable() {
        this.multiversion = false;
    }

    LinkDbTable(boolean multiversion) {
        this.multiversion = multiversion;
    }

    protected abstract R load(Connection con, ResultSet rs) throws SQLException;

    protected abstract void save(Connection con, Long idA, Long idB, R r) throws SQLException;

    protected abstract String idColumnA();

    protected abstract String idColumnB();

    public final R get(Long idA, Long idB) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table() + " WHERE " + idColumnA() + " = ? "
                     + "AND " + idColumnB() + " = ?" + (multiversion ? " AND latest = TRUE LIMIT 1" : ""))) {
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

    public final List<R> getManyByA(Long idA) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table()
                     + " WHERE " + idColumnA() + " = ?" + (multiversion ? " AND latest = TRUE" : ""))) {
            pstmt.setLong(1, idA);
            return getManyBy(con, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public final List<R> getManyByB(Long idB) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table()
                     + " WHERE " + idColumnB() + " = ?" + (multiversion ? " AND latest = TRUE" : ""))) {
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

    public final void insert(Long idA, Long idB, R r) {
        try (Connection con = Db.getConnection()) {
            if (multiversion) {
                try (PreparedStatement pstmt = con.prepareStatement("UPDATE " + table()
                        + " SET latest = FALSE WHERE " + idColumnA() + " = ? AND " + idColumnB() + " = ? AND latest = TRUE LIMIT 1")) {
                    pstmt.setLong(1, idA);
                    pstmt.setLong(2, idB);
                    pstmt.executeUpdate();
                }
            }
            save(con, idA, idB, r);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public void delete(Long idA, Long idB) {
        try (Connection con = Db.getConnection();
        PreparedStatement pstmt = con.prepareStatement("DELETE FROM " + table() + " WHERE " + idColumnA() + " = ? "
                + "AND " + idColumnB() + " = ?")) {
            pstmt.setLong(1, idA);
            pstmt.setLong(2, idB);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

}
