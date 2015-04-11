package nxt.db;

public abstract class PersistentDbTable<T> extends EntityDbTable<T> {

    protected PersistentDbTable(String table, DbKey.Factory<T> dbKeyFactory) {
        super(table, dbKeyFactory, false, null);
    }

    protected PersistentDbTable(String table, DbKey.Factory<T> dbKeyFactory, String fullTextSearchColumns) {
        super(table, dbKeyFactory, false, fullTextSearchColumns);
    }

    @Override
    public final void rollback(int height) {
        clearCache();
    }

    @Override
    public final void truncate() {
        clearCache();
    }

}
