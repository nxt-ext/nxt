package nxt;

import nxt.util.DbUtils;
import nxt.util.FilteredConnection;
import nxt.util.Logger;
import org.h2.jdbcx.JdbcConnectionPool;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class Db {

    private static volatile JdbcConnectionPool cp;
    private static volatile int maxActiveConnections;

    private static final ThreadLocal<Connection> localConnection = new ThreadLocal<>();

    private static final class DbConnection extends FilteredConnection {

        private DbConnection(Connection con) {
            super(con);
        }

        @Override
        public void setAutoCommit(boolean autoCommit) throws SQLException {
            throw new UnsupportedOperationException("Use Db.beginTransaction() to start a new transaction");
        }

        @Override
        public void close() throws SQLException {
            if (localConnection.get() == null) {
                super.close();
            } else if (! this.equals(localConnection.get())) {
                throw new IllegalStateException("Previous connection not committed");
            }
        }

    }

    static void init() {
        long maxCacheSize = Nxt.getIntProperty("nxt.dbCacheKB");
        if (maxCacheSize == 0) {
            maxCacheSize = Runtime.getRuntime().maxMemory() / (1024 * 2);
        }
        String dbUrl = Constants.isTestnet ? Nxt.getStringProperty("nxt.testDbUrl") : Nxt.getStringProperty("nxt.dbUrl");
        if (! dbUrl.contains("CACHE_SIZE=")) {
            dbUrl += ";CACHE_SIZE=" + maxCacheSize;
        }
        Logger.logDebugMessage("Database jdbc url set to: " + dbUrl);
        cp = JdbcConnectionPool.create(dbUrl, "sa", "sa");
        cp.setMaxConnections(Nxt.getIntProperty("nxt.maxDbConnections"));
        cp.setLoginTimeout(Nxt.getIntProperty("nxt.dbLoginTimeout"));
        int defaultLockTimeout = Nxt.getIntProperty("nxt.dbDefaultLockTimeout") * 1000;
        try (Connection con = cp.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.executeUpdate("SET DEFAULT_LOCK_TIMEOUT " + defaultLockTimeout);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        DbVersion.init();
    }

    static void shutdown() {
        if (cp != null) {
            try (Connection con = cp.getConnection();
                 Statement stmt = con.createStatement()) {
                stmt.execute("SHUTDOWN COMPACT");
                Logger.logMessage("Database shutdown completed");
            } catch (SQLException e) {
                Logger.logDebugMessage(e.toString(), e);
            }
            cp = null;
        }
    }

    private static Connection getPooledConnection() throws SQLException {
        Connection con = cp.getConnection();
        int activeConnections = cp.getActiveConnections();
        if (activeConnections > maxActiveConnections) {
            maxActiveConnections = activeConnections;
            Logger.logDebugMessage("Database connection pool current size: " + activeConnections);
        }
        return con;
    }

    public static Connection getConnection() throws SQLException {
        Connection con = localConnection.get();
        if (con != null) {
            return con;
        }
        con = getPooledConnection();
        con.setAutoCommit(true);
        return new DbConnection(con);
    }

    public static boolean isInTransaction() {
        return localConnection.get() != null;
    }

    public static Connection beginTransaction() {
        if (localConnection.get() != null) {
            throw new IllegalStateException("Transaction already in progress");
        }
        try {
            Connection con = getPooledConnection();
            con.setAutoCommit(false);
            con = new DbConnection(con);
            localConnection.set(con);
            return con;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static void commitTransaction() {
        Connection con = localConnection.get();
        if (con == null) {
            throw new IllegalStateException("Not in transaction");
        }
        try {
            con.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static void rollbackTransaction() {
        Connection con = localConnection.get();
        if (con == null) {
            throw new IllegalStateException("Not in transaction");
        }
        try {
            con.rollback();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static void endTransaction() {
        Connection con = localConnection.get();
        if (con == null) {
            throw new IllegalStateException("Not in transaction");
        }
        localConnection.set(null);
        DbUtils.close(con);
    }

    private Db() {} // never

}
