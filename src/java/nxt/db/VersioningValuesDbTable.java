package nxt.db;

public abstract class VersioningValuesDbTable<T, V> extends ValuesDbTable<T, V> {

    protected VersioningValuesDbTable(DbKey.Factory<T> dbKeyFactory) {
        super(dbKeyFactory, true);
    }

    @Override
    final void rollback(int height) {
        VersioningEntityDbTable.rollback(table(), height, dbKeyFactory);
    }

}
