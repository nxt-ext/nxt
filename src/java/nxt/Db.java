package nxt;

import nxt.util.Logger;
import org.h2.jdbcx.JdbcConnectionPool;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

final class Db {

    private static volatile JdbcConnectionPool cp;
    private static volatile int maxActiveConnections;

    static void init() {
        long maxCacheSize = Nxt.getIntProperty("nxt.dbCacheKB", (int)(Runtime.getRuntime().maxMemory() / (1024 * 2)));
        String dbUrl = Nxt.getStringProperty("nxt.dbUrl", "jdbc:h2:nxt_db/nxt;DB_CLOSE_ON_EXIT=FALSE;CACHE_SIZE=" + maxCacheSize);
        if (! dbUrl.contains("CACHE_SIZE=")) {
            dbUrl += ";CACHE_SIZE=" + maxCacheSize;
        }
        Logger.logDebugMessage("Database jdbc url set to: " + dbUrl);
        cp = JdbcConnectionPool.create(dbUrl, "sa", "sa");
        cp.setMaxConnections(Nxt.getIntProperty("nxt.maxDbConnections", 10));
        cp.setLoginTimeout(Nxt.getIntProperty("nxt.dbLoginTimeout", 70));
        DbVersion.init();
    }

    static void shutdown() {
        if (cp != null) {
            try (Connection con = cp.getConnection();
                 Statement stmt = con.createStatement()) {
                stmt.execute("SHUTDOWN COMPACT");
                Logger.logDebugMessage("Database shutdown completed");
            } catch (SQLException e) {
                Logger.logDebugMessage(e.toString(), e);
            }
            //cp.dispose();
            cp = null;
        }
    }

    static Connection getConnection() throws SQLException {
        Connection con = cp.getConnection();
        con.setAutoCommit(false);
        int activeConnections = cp.getActiveConnections();
        if (activeConnections > maxActiveConnections) {
            maxActiveConnections = activeConnections;
            Logger.logDebugMessage("Database connection pool max size: " + activeConnections);
        }
        return con;
    }

    private Db() {} // never

}
