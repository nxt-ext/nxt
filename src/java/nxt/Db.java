package nxt;

import nxt.util.Logger;
import org.h2.jdbcx.JdbcConnectionPool;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

final class Db {

    private static volatile JdbcConnectionPool cp;

    static void init() {
        long maxCacheSize = Nxt.getIntProperty("nxt.dbCacheKB", (int)Runtime.getRuntime().maxMemory() / (1024 * 2));
        String dbUrl = Nxt.getStringProperty("nxt.dbUrl", "jdbc:h2:nxt_db/nxt;DB_CLOSE_ON_EXIT=FALSE;CACHE_SIZE=" + maxCacheSize);
        if (! dbUrl.contains("CACHE_SIZE=")) {
            dbUrl += ";CACHE_SIZE=" + maxCacheSize;
        }
        Logger.logDebugMessage("Database jdbc url set to: " + dbUrl);
        cp = JdbcConnectionPool.create(dbUrl, "sa", "sa");
        cp.setMaxConnections(200);
        cp.setLoginTimeout(70);
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
        return con;
    }

    private Db() {} // never

}
