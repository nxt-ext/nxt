package nxt.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public abstract class EntityDbTable<T> extends DerivedDbTable {

    private final boolean multiversion;
    protected final DbKey.Factory<T> dbKeyFactory;

    protected EntityDbTable(DbKey.Factory<T> dbKeyFactory) {
        this(dbKeyFactory, false);
    }

    EntityDbTable(DbKey.Factory<T> dbKeyFactory, boolean multiversion) {
        this.dbKeyFactory = dbKeyFactory;
        this.multiversion = multiversion;
    }

    protected abstract T load(Connection con, ResultSet rs) throws SQLException;

    protected abstract void save(Connection con, T t) throws SQLException;

    protected String defaultSort() {
        return "ORDER BY height DESC";
    }

    public final T get(DbKey dbKey) {
        if (Db.isInTransaction()) {
            T t = (T)Db.getCache(table()).get(dbKey);
            if (t != null) {
                return t;
            }
        }
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table() + dbKeyFactory.getPKClause()
             + (multiversion ? " AND latest = TRUE LIMIT 1" : ""))) {
            dbKey.setPK(pstmt);
            return get(con, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public final T getBy(String columnName, Object value) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table()
                     + " WHERE " + columnName + " = ?" + (multiversion ? " AND latest = TRUE LIMIT 1" : ""))) {
            if(value instanceof Long){
                pstmt.setLong(1, (Long)value);
            }else if(value instanceof Boolean){
                pstmt.setBoolean(1, (Boolean)value);
            }else if(value instanceof String){
                pstmt.setString(1, (String)value);
            }
            return get(con, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    private T get(Connection con, PreparedStatement pstmt) throws SQLException {
        final boolean cache = Db.isInTransaction();
        try (ResultSet rs = pstmt.executeQuery()) {
            if (!rs.next()) {
                return null;
            }
            T t = null;
            DbKey dbKey = null;
            if (cache) {
                dbKey = dbKeyFactory.newKey(rs);
                t = (T) Db.getCache(table()).get(dbKey);
            }
            if (t == null) {
                t = load(con, rs);
                if (cache) {
                    Db.getCache(table()).put(dbKey, t);
                }
            }
            if (rs.next()) {
                throw new RuntimeException("Multiple records found");
            }
            return t;
        }
    }

    public final DbIterator<T> getManyBy(String columnName, Object value) {
        return getManyBy(columnName, value, 0, -1);
    }

    public final DbIterator<T> getManyBy(String columnName, Object value, int from, int to) {
        Connection con = null;
        try {
            con = Db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table()
                    + " WHERE " + columnName + " = ?" + (multiversion ? " AND latest = TRUE " : " ") + defaultSort()
                    + DbUtils.limitsClause(from, to));
            if(value instanceof Long){
                pstmt.setLong(1, (Long)value);
            }else if(value instanceof Boolean){
                pstmt.setBoolean(1, (Boolean)value);
            }else if(value instanceof String){
                pstmt.setString(1, (String)value);
            }
            DbUtils.setLimits(2, pstmt, from, to);
            return getManyBy(con, pstmt, true);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public final DbIterator<T> getManyBy(Connection con, PreparedStatement pstmt, boolean cache) {
        final boolean doCache = cache && Db.isInTransaction();
        return new DbIterator<>(con, pstmt, new DbIterator.ResultSetReader<T>() {
            @Override
            public T get(Connection con, ResultSet rs) throws Exception {
                T t = null;
                DbKey dbKey = null;
                if (doCache) {
                    dbKey = dbKeyFactory.newKey(rs);
                    t = (T) Db.getCache(table()).get(dbKey);
                }
                if (t == null) {
                    t = load(con, rs);
                    if (doCache) {
                        Db.getCache(table()).put(dbKey, t);
                    }
                }
                return t;
            }
        });
    }

    //todo: change resulting type to DbIterator?
    public List<Long> getManyIdsBy(String targetColumnName, String filterColumnName, Long value) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT " + targetColumnName + " FROM " + table()
                     + " WHERE " + filterColumnName + " = ? ")) {
            pstmt.setLong(1, value);
            List<Long> result = new ArrayList<>();
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getLong(targetColumnName));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public final DbIterator<T> getAll(int from, int to) {
        Connection con = null;
        try {
            con = Db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table()
                     + (multiversion ? " WHERE latest = TRUE " : " ") + defaultSort()
                    + DbUtils.limitsClause(from, to));
            DbUtils.setLimits(1, pstmt, from, to);
            return getManyBy(con, pstmt, true);
        } catch (SQLException e) {
            DbUtils.close(con);
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

    public final int getRowCount() {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT COUNT(*) FROM " + table());
             ResultSet rs = pstmt.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public final void insert(T t) {
        if (!Db.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        DbKey dbKey = dbKeyFactory.newKey(t);
        T cachedT = (T)Db.getCache(table()).get(dbKey);
        if (cachedT == null) {
            Db.getCache(table()).put(dbKey, t);
        } else if (t != cachedT) { // not a bug
            throw new IllegalStateException("Different instance found in Db cache, perhaps trying to save an object "
                    + "that was read outside the current transaction");
        }
        try (Connection con = Db.getConnection()) {
            if (multiversion) {
                try (PreparedStatement pstmt = con.prepareStatement("UPDATE " + table()
                        + " SET latest = FALSE " + dbKeyFactory.getPKClause() + " AND latest = TRUE LIMIT 1")) {
                    dbKey.setPK(pstmt);
                    pstmt.executeUpdate();
                }
            }
            save(con, t);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

}
