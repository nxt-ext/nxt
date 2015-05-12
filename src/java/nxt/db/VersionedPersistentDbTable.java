package nxt.db;

public abstract class VersionedPersistentDbTable<T> extends VersionedPrunableDbTable<T> {

    protected VersionedPersistentDbTable(String table, DbKey.Factory<T> dbKeyFactory) {
        super(table, dbKeyFactory);
    }

    protected VersionedPersistentDbTable(String table, DbKey.Factory<T> dbKeyFactory, String fullTextSearchColumns) {
        super(table, dbKeyFactory, fullTextSearchColumns);
    }

    @Override
    protected final void prune() {}

}
