package nxt.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public abstract class ValuesDbTable<T,V> extends BasicDbTable {

    private final boolean multiversion;

    public ValuesDbTable() {
        multiversion = false;
    }

    ValuesDbTable(boolean multiversion) {
        this.multiversion = multiversion;
    }

    protected abstract Long getId(T t);

    protected abstract V load(Connection con, ResultSet rs) throws SQLException;

    protected abstract void save(Connection con, T t, V v) throws SQLException;

    public final List<V> get(Long id) {
        List<V> values;
        if (Db.isInTransaction()) {
            values = (List<V>)Db.getCache(table()).get(id);
            if (values != null) {
                return values;
            }
        }
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table() + " WHERE id = ?"
             + (multiversion ? " AND latest = TRUE" : ""))) {
            pstmt.setLong(1, id);
            values = get(con, pstmt);
            if (Db.isInTransaction()) {
                Db.getCache(table()).put(id, values);
            }
            return values;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    private List<V> get(Connection con, PreparedStatement pstmt) {
        try {
            List<V> result = new ArrayList<>();
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

    public final void insert(T t, V v) {
        Db.getCache(table()).remove(getId(t));
        try (Connection con = Db.getConnection()) {
            if (multiversion) {
                try (PreparedStatement pstmt = con.prepareStatement("UPDATE " + table()
                        + " SET latest = FALSE WHERE id = ? AND latest = TRUE LIMIT 1")) {
                    pstmt.setLong(1, getId(t));
                    pstmt.executeUpdate();
                }
            }
            save(con, t, v);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public final void insert(T t, List<V> values) {
        Db.getCache(table()).put(getId(t), values);
        try (Connection con = Db.getConnection()) {
            if (multiversion) {
                try (PreparedStatement pstmt = con.prepareStatement("UPDATE " + table()
                        + " SET latest = FALSE WHERE id = ? AND latest = TRUE")) {
                    pstmt.setLong(1, getId(t));
                    pstmt.executeUpdate();
                }
            }
            for (V v : values) {
                save(con, t, v);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

}
