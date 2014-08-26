package nxt.db;

public abstract class VersionedValuesDbTable<T, V> extends ValuesDbTable<T, V> {

    protected VersionedValuesDbTable(DbKey.Factory<T> dbKeyFactory) {
        super(dbKeyFactory, true);
    }

    @Override
    public final void rollback(int height) {
        VersionedEntityDbTable.rollback(table(), height, dbKeyFactory);
    }

}
