package nxt.db;

import nxt.Nxt;
import nxt.util.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class DerivedDbTable {

    protected DerivedDbTable() {
        Nxt.getBlockchainProcessor().registerDerivedTable(this);
    }

    protected abstract String table();

    public void rollback(int height) {
        Logger.logDebugMessage("Rollback " + table() + " to " + height);
        if (!Db.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        try (Connection con = Db.getConnection();
             PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + table() + " WHERE height > ?")) {
            pstmtDelete.setInt(1, height);
            pstmtDelete.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public final void truncate() {
        Logger.logDebugMessage("Truncating table " + table());
        if (!Db.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        try (Connection con = Db.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.executeUpdate("SET REFERENTIAL_INTEGRITY FALSE");
            stmt.executeUpdate("TRUNCATE TABLE " + table());
            stmt.executeUpdate("SET REFERENTIAL_INTEGRITY TRUE");
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public void trim(int height) {
        //nothing to trim
    }

}
