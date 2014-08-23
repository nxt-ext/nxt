package nxt.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public abstract class EntityDbTable<T> extends BasicDbTable {

    private final boolean multiversion;

    public EntityDbTable() {
        this.multiversion = false;
    }

    EntityDbTable(boolean multiversion) {
        this.multiversion = multiversion;
    }

    protected abstract Long getId(T t);

    protected abstract T load(Connection con, ResultSet rs) throws SQLException;

    protected abstract void save(Connection con, T t) throws SQLException;

    protected String defaultSort() {
        return "ORDER BY height DESC";
    }

    public final T get(Long id) {
        T t;
        if (Db.isInTransaction()) {
            t = (T)Db.getCache(table()).get(id);
            if (t != null) {
                return t;
            }
        }
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table() + " WHERE id = ?"
             + (multiversion ? " AND latest = TRUE LIMIT 1" : ""))) {
            pstmt.setLong(1, id);
            t = get(con, pstmt, true);
            return t;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public final T get(Long id, int height) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table()
                     + " WHERE id = ? AND height <= ? ORDER BY height DESC LIMIT 1")) {
            pstmt.setLong(1, id);
            pstmt.setInt(2, height);
            return get(con, pstmt, false);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public final T getBy(String columnName, String value) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table()
                     + " WHERE " + columnName + " = ?" + (multiversion ? " AND latest = TRUE" : ""))) {
            pstmt.setString(1, value);
            return get(con, pstmt, true);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    private T get(Connection con, PreparedStatement pstmt, boolean cache) throws SQLException {
        cache = cache && Db.isInTransaction();
        try (ResultSet rs = pstmt.executeQuery()) {
            if (!rs.next()) {
                return null;
            }
            T t = null;
            if (cache) {
                Long id = rs.getLong("id");
                t = (T) Db.getCache(table()).get(id);
            }
            if (t == null) {
                t = load(con, rs);
                if (cache) {
                    Db.getCache(table()).put(getId(t), t);
                }
            }
            if (rs.next()) {
                throw new RuntimeException("Multiple records found");
            }
            return t;
        }
    }

    public final List<T> getManyBy(String columnName, Long value) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table()
                     + " WHERE " + columnName + " = ?" + (multiversion ? " AND latest = TRUE " : " ") + defaultSort())) {
            pstmt.setLong(1, value);
            return getManyBy(con, pstmt, true);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public final List<T> getManyBy(Connection con, PreparedStatement pstmt, boolean cache) {
        cache = cache && Db.isInTransaction();
        try {
            List<T> result = new ArrayList<>();
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    T t = null;
                    if (cache) {
                        Long id = rs.getLong("id");
                        t = (T) Db.getCache(table()).get(id);
                    }
                    if (t == null) {
                        t = load(con, rs);
                        if (cache) {
                            Db.getCache(table()).put(getId(t), t);
                        }
                    }
                    result.add(t);
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public final List<T> getAll() {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table()
                     + (multiversion ? " WHERE latest = TRUE " : " ") + defaultSort())) {
            return getManyBy(con, pstmt, true);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public final int getCount() {
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

    public final void insert(T t) {
        T cachedT = (T)Db.getCache(table()).get(getId(t));
        if (cachedT == null) {
            Db.getCache(table()).put(getId(t), t);
        } else if (t != cachedT) { // not a bug
            throw new IllegalStateException("Different instance found in Db cache, perhaps trying to save an object "
                    + "that was read outside the current transaction");
        }
        try (Connection con = Db.getConnection()) {
            if (multiversion) {
                try (PreparedStatement pstmt = con.prepareStatement("UPDATE " + table()
                        + " SET latest = FALSE WHERE id = ? AND latest = TRUE LIMIT 1")) {
                    pstmt.setLong(1, getId(t));
                    pstmt.executeUpdate();
                }
            }
            save(con, t);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public void delete(T t) {
        if (t == null) {
            return;
        }
        Db.getCache(table()).remove(getId(t));
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("DELETE FROM " + table() + " WHERE id = ?")) {
            pstmt.setLong(1, getId(t));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

}
