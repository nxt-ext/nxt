package nxt.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class VersionedPrunableDbTable<T> extends PrunableDbTable<T> {

    protected VersionedPrunableDbTable(String table, DbKey.Factory<T> dbKeyFactory) {
        super(table, dbKeyFactory, true, null);
    }

    protected VersionedPrunableDbTable(String table, DbKey.Factory<T> dbKeyFactory, String fullTextSearchColumns) {
        super(table, dbKeyFactory, true, fullTextSearchColumns);
    }

    public final boolean delete(T t) {
        throw new UnsupportedOperationException("Versioned prunable tables cannot support delete");
    }

    @Override
    public final void rollback(int height) {
        if (!db.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        try (Connection con = db.getConnection();
             PreparedStatement pstmtSetLatest = con.prepareStatement("UPDATE " + table
                     + " AS a SET a.latest = TRUE WHERE a.latest = FALSE AND a.height = "
                     + " (SELECT MAX(height) FROM " + table + " AS b WHERE " + dbKeyFactory.getSelfJoinClause() + ")")) {
            pstmtSetLatest.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        db.getCache(table).clear();
    }

}
