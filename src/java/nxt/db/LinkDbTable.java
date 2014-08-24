package nxt.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public abstract class LinkDbTable<R> extends BasicDbTable {

    protected static final class cacheKey {

        private final Long idA;
        private final Long idB;

        public cacheKey(Long idA, Long idB) {
            this.idA = idA;
            this.idB = idB;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof cacheKey && ((cacheKey) o).idA.equals(idA) && ((cacheKey) o).idB.equals(idB);
        }

        @Override
        public int hashCode() {
            return idA.hashCode() ^ idB.hashCode();
        }
    }

    private final boolean multiversion;

    protected LinkDbTable() {
        this.multiversion = false;
    }

    LinkDbTable(boolean multiversion) {
        this.multiversion = multiversion;
    }

    protected abstract R load(Connection con, ResultSet rs) throws SQLException;

    protected abstract void save(Connection con, R r) throws SQLException;

    protected abstract String idColumnA();

    protected abstract String idColumnB();

    protected abstract Long getIdA(R r);

    protected abstract Long getIdB(R r);

    protected String defaultSort() {
        return "ORDER BY height DESC";
    }

    public final R get(Long idA, Long idB) {
        R r;
        cacheKey cacheKey = new cacheKey(idA, idB);
        if (Db.isInTransaction()) {
            r = (R)Db.getCache(table()).get(cacheKey);
            if (r != null) {
                return r;
            }
        }
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table() + " WHERE " + idColumnA() + " = ? "
                     + "AND " + idColumnB() + " = ?" + (multiversion ? " AND latest = TRUE LIMIT 1" : ""))) {
            pstmt.setLong(1, idA);
            pstmt.setLong(2, idB);
            r = get(con, pstmt);
            if (Db.isInTransaction()) {
                Db.getCache(table()).put(cacheKey, r);
            }
            return r;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    private R get(Connection con, PreparedStatement pstmt) throws SQLException {
        try (ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return load(con, rs);
            }
            if (rs.next()) {
                throw new RuntimeException("Multiple records found");
            }
            return null;
        }
    }

    public final List<R> getManyBy(String columnName, Long value) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table()
                     + " WHERE " + columnName + " = ?" + (multiversion ? " AND latest = TRUE" : ""))) {
            pstmt.setLong(1, value);
            return getManyBy(con, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }

    }

    private List<R> getManyBy(Connection con, PreparedStatement pstmt) {
        final boolean cache = Db.isInTransaction();
        try {
            List<R> result = new ArrayList<>();
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    R r = null;
                    cacheKey cacheKey = null;
                    if (cache) {
                        cacheKey = new cacheKey(rs.getLong(idColumnA()), rs.getLong(idColumnB()));
                        r = (R) Db.getCache(table()).get(cacheKey);
                    }
                    if (r == null) {
                        r = load(con, rs);
                        if (cache) {
                            Db.getCache(table()).put(cacheKey, r);
                        }
                    }
                    result.add(r);
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public List<R> getAll() {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table()
                     + (multiversion ? " WHERE latest = TRUE " : " ") + defaultSort())) {
            return getManyBy(con, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public int getCount() {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT COUNT(*) FROM " + table()
                     + (multiversion ? " WHERE latest = TRUE" : ""));
             ResultSet rs = pstmt.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public final void insert(R r) {
        Long idA = getIdA(r);
        Long idB = getIdB(r);
        Db.getCache(table()).put(new cacheKey(idA, idB), r);
        try (Connection con = Db.getConnection()) {
            if (multiversion) {
                try (PreparedStatement pstmt = con.prepareStatement("UPDATE " + table()
                        + " SET latest = FALSE WHERE " + idColumnA() + " = ? AND " + idColumnB() + " = ? AND latest = TRUE LIMIT 1")) {
                    pstmt.setLong(1, idA);
                    pstmt.setLong(2, idB);
                    pstmt.executeUpdate();
                }
            }
            save(con, r);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public void delete(R r) {
        Long idA = getIdA(r);
        Long idB = getIdB(r);
        Db.getCache(table()).remove(new cacheKey(idA, idB));
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
