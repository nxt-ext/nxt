package nxt;

import nxt.util.Logger;
import org.h2.jdbcx.JdbcConnectionPool;

import java.sql.Connection;
import java.sql.SQLException;

final class Db {

    private static JdbcConnectionPool cp;

    static void init() {
        long maxCacheSize = Runtime.getRuntime().maxMemory() / (1024 * 2);
        Logger.logDebugMessage("Database cache size set to " + maxCacheSize + " kB");
        cp = JdbcConnectionPool.create("jdbc:h2:nxt_db/nxt;DB_CLOSE_DELAY=10;DB_CLOSE_ON_EXIT=FALSE;CACHE_SIZE=" + maxCacheSize, "sa", "sa");
        cp.setMaxConnections(200);
        cp.setLoginTimeout(70);
        DbVersion.init();
    }

    static void shutdown() {
        if (cp != null) {
            cp.dispose();
        }
    }

    static Connection getConnection() throws SQLException {
        Connection con = cp.getConnection();
        con.setAutoCommit(false);
        return con;
    }

    private Db() {} // never

}
