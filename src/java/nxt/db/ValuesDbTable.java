package nxt.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public abstract class ValuesDbTable<T,V> extends DerivedDbTable {

    private final boolean multiversion;
    protected final DbKey.Factory<T> dbKeyFactory;

    protected ValuesDbTable(DbKey.Factory<T> dbKeyFactory) {
        this(dbKeyFactory, false);
    }

    ValuesDbTable(DbKey.Factory<T> dbKeyFactory, boolean multiversion) {
        this.dbKeyFactory = dbKeyFactory;
        this.multiversion = multiversion;
    }

    protected abstract V load(Connection con, ResultSet rs) throws SQLException;

    protected abstract void save(Connection con, T t, V v) throws SQLException;

    public final List<V> get(DbKey dbKey) {
        List<V> values;
        if (Db.isInTransaction()) {
            values = (List<V>)Db.getCache(table()).get(dbKey);
            if (values != null) {
                return values;
            }
        }
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table() + dbKeyFactory.getPKClause()
             + (multiversion ? " AND latest = TRUE" : ""))) {
            dbKey.setPK(pstmt);
            values = get(con, pstmt);
            if (Db.isInTransaction()) {
                Db.getCache(table()).put(dbKey, values);
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
        if (!Db.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        DbKey dbKey = dbKeyFactory.newKey(t);
        Db.getCache(table()).remove(dbKey);
        try (Connection con = Db.getConnection()) {
            if (multiversion) {
                try (PreparedStatement pstmt = con.prepareStatement("UPDATE " + table()
                        + " SET latest = FALSE " + dbKeyFactory.getPKClause() + " AND latest = TRUE LIMIT 1")) {
                    dbKey.setPK(pstmt);
                    pstmt.executeUpdate();
                }
            }
            save(con, t, v);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public final void insert(T t, List<V> values) {
        if (!Db.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        DbKey dbKey = dbKeyFactory.newKey(t);
        Db.getCache(table()).put(dbKey, values);
        try (Connection con = Db.getConnection()) {
            if (multiversion) {
                try (PreparedStatement pstmt = con.prepareStatement("UPDATE " + table()
                        + " SET latest = FALSE " + dbKeyFactory.getPKClause() + " AND latest = TRUE")) {
                    dbKey.setPK(pstmt);
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
