package nxt.db;

import nxt.Db;
import nxt.Nxt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class DerivedDbTable {

    protected static final TransactionalDb db = Db.db;

    protected final String table;

    protected DerivedDbTable(String table) {
        this.table = table;
        Nxt.getBlockchainProcessor().registerDerivedTable(this);
    }

    public void rollback(int height) {
        if (!db.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        try (Connection con = db.getConnection();
             PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + table + " WHERE height > ?")) {
            pstmtDelete.setInt(1, height);
            pstmtDelete.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public void truncate() {
        if (!db.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        try (Connection con = db.getConnection();
             Statement stmt = con.createStatement()) {
            //TODO: don't disable referential integrity and don't rely on foreign keys for all derived tables
            // The problem with foreign keys is that H2 does not support deferred integrity checking, and
            // rollbacks may cause constraint violation errors depending on what order tables are rolled back.
            stmt.addBatch("SET REFERENTIAL_INTEGRITY FALSE");
            stmt.addBatch("TRUNCATE TABLE " + table);
            stmt.addBatch("SET REFERENTIAL_INTEGRITY TRUE");
            stmt.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public void trim(int height) {
        //nothing to trim
    }

}
