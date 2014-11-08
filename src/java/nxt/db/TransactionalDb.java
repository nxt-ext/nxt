package nxt.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class TransactionalDb extends BasicDb {

    private final ThreadLocal<DbConnection> localConnection = new ThreadLocal<>();
    private final ThreadLocal<Map<String,Map<DbKey,Object>>> transactionCaches = new ThreadLocal<>();

    public TransactionalDb(DbProperties dbProperties) {
        super(dbProperties);
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection con = localConnection.get();
        if (con != null) {
            return con;
        }
        return new DbConnection(super.getConnection());
    }

    public boolean isInTransaction() {
        return localConnection.get() != null;
    }

    public Connection beginTransaction() {
        if (localConnection.get() != null) {
            throw new IllegalStateException("Transaction already in progress");
        }
        try {
            Connection con = getPooledConnection();
            con.setAutoCommit(false);
            con = new DbConnection(con);
            localConnection.set((DbConnection)con);
            transactionCaches.set(new HashMap<String, Map<DbKey, Object>>());
            return con;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public void commitTransaction() {
        DbConnection con = localConnection.get();
        if (con == null) {
            throw new IllegalStateException("Not in transaction");
        }
        try {
            con.doCommit();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public void rollbackTransaction() {
        DbConnection con = localConnection.get();
        if (con == null) {
            throw new IllegalStateException("Not in transaction");
        }
        try {
            con.doRollback();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        transactionCaches.get().clear();
    }

    public void endTransaction() {
        Connection con = localConnection.get();
        if (con == null) {
            throw new IllegalStateException("Not in transaction");
        }
        localConnection.set(null);
        transactionCaches.get().clear();
        transactionCaches.set(null);
        DbUtils.close(con);
    }

    Map<DbKey,Object> getCache(String tableName) {
        if (!isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        Map<DbKey,Object> cacheMap = transactionCaches.get().get(tableName);
        if (cacheMap == null) {
            cacheMap = new HashMap<>();
            transactionCaches.get().put(tableName, cacheMap);
        }
        return cacheMap;
    }

    private final class DbConnection extends FilteredConnection {

        private DbConnection(Connection con) {
            super(con);
        }

        @Override
        public void setAutoCommit(boolean autoCommit) throws SQLException {
            throw new UnsupportedOperationException("Use Db.beginTransaction() to start a new transaction");
        }

        @Override
        public void commit() throws SQLException {
            if (localConnection.get() == null) {
                super.commit();
            } else if (this != localConnection.get()) {
                throw new IllegalStateException("Previous connection not committed");
            } else {
                commitTransaction();
            }
        }

        private void doCommit() throws SQLException {
            super.commit();
        }

        @Override
        public void rollback() throws SQLException {
            if (localConnection.get() == null) {
                super.rollback();
            } else if (this != localConnection.get()) {
                throw new IllegalStateException("Previous connection not committed");
            } else {
                rollbackTransaction();
            }
        }

        private void doRollback() throws SQLException {
            super.rollback();
        }

        @Override
        public void close() throws SQLException {
            if (localConnection.get() == null) {
                super.close();
            } else if (this != localConnection.get()) {
                throw new IllegalStateException("Previous connection not committed");
            }
        }

    }
}
