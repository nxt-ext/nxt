package nxt;

import org.h2.jdbcx.JdbcConnectionPool;

import java.sql.Connection;
import java.sql.SQLException;

final class Db {

    private static JdbcConnectionPool cp;

    static void init() {
        cp = JdbcConnectionPool.create("jdbc:h2:nxt_db/nxt", "sa", "sa");
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
