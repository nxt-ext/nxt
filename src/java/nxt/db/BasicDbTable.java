package nxt.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class BasicDbTable {

    protected abstract String table();

    public final void truncate() {
        try (Connection con = Db.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.executeUpdate("SET REFERENTIAL_INTEGRITY FALSE");
            stmt.executeUpdate("TRUNCATE TABLE " + table());
            stmt.executeUpdate("SET REFERENTIAL_INTEGRITY TRUE");
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
}
