package nxt.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public abstract class CachingDbTable<T> extends DbTable<T> {

    protected abstract Long getId(T t);

    protected abstract T load(Connection con, ResultSet rs) throws SQLException;

    protected abstract void save(Connection con, T t) throws SQLException;

    protected abstract void delete(Connection con, T t) throws SQLException;

    protected String defaultSort() {
        return "ORDER BY height DESC";
    }

    public T get(Long id) {
        T t;
        if (Db.isInTransaction()) {
            t = (T)Db.getCache(table()).get(id);
            if (t != null) {
                return t;
            }
        }
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table() + " WHERE id = ?")) {
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

    protected final T get(Connection con, PreparedStatement pstmt) throws SQLException {
        if (!Db.isInTransaction()) {
            return super.get(con, pstmt);
        }
        try (ResultSet rs = pstmt.executeQuery()) {
            if (!rs.next()) {
                return null;
            }
            Long id = rs.getLong("id");
            T t;
            t = (T) Db.getCache(table()).get(id);
            if (t == null) {
                t = load(con, rs);
            }
            if (rs.next()) {
                throw new RuntimeException("Multiple records found");
            } else {
                Db.getCache(table()).put(id, t);
            }
            return t;
        }
    }

    public final List<T> getManyBy(Connection con, PreparedStatement pstmt) {
        if (!Db.isInTransaction()) {
            return super.getManyBy(con, pstmt);
        }
        try {
            List<T> result = new ArrayList<>();
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Long id = rs.getLong("id");
                    T t = (T) Db.getCache(table()).get(id);
                    if (t == null) {
                        t = load(con, rs);
                        Db.getCache(table()).put(id, t);
                    }
                    result.add(t);
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public void insert(T t) {
        T cachedT = (T)Db.getCache(table()).get(getId(t));
        if (cachedT == null) {
            Db.getCache(table()).put(getId(t), t);
        } else if (t != cachedT) { // not a bug
            throw new IllegalStateException("Different instance found in Db cache, perhaps trying to save an object "
                    + "that was read outside the current transaction");
        }
        super.insert(t);
    }

    public final void delete(T t) {
        if (t == null) {
            return;
        }
        Db.getCache(table()).remove(getId(t));
        super.delete(t);
    }

}
