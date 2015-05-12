package nxt.db;

import nxt.Constants;
import nxt.Nxt;
import nxt.util.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class PrunableDbTable<T> extends PersistentDbTable<T> {

    protected PrunableDbTable(String table, DbKey.Factory<T> dbKeyFactory) {
        super(table, dbKeyFactory);
    }

    protected PrunableDbTable(String table, DbKey.Factory<T> dbKeyFactory, String fullTextSearchColumns) {
        super(table, dbKeyFactory, fullTextSearchColumns);
    }

    PrunableDbTable(String table, DbKey.Factory<T> dbKeyFactory, boolean multiversion, String fullTextSearchColumns) {
        super(table, dbKeyFactory, multiversion, fullTextSearchColumns);
    }

    @Override
    public final void trim(int height) {
        prune();
        super.trim(height);
    }

    protected void prune() {
        if (Constants.ENABLE_PRUNING) {
            try (Connection con = db.getConnection();
                 PreparedStatement pstmt = con.prepareStatement("DELETE FROM " + table + " WHERE transaction_timestamp < ?")) {
                pstmt.setInt(1, Nxt.getEpochTime() - Constants.MAX_PRUNABLE_LIFETIME);
                int deleted = pstmt.executeUpdate();
                if (deleted > 0) {
                    Logger.logDebugMessage("Deleted " + deleted + " expired prunable data from " + table);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }
    }

}
