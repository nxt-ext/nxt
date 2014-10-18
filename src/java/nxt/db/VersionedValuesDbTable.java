package nxt.db;

public abstract class VersionedValuesDbTable<T, V> extends ValuesDbTable<T, V> {

    protected VersionedValuesDbTable(String table, DbKey.Factory<T> dbKeyFactory) {
        super(table, dbKeyFactory, true);
    }

    @Override
    public final void rollback(int height) {
        VersionedEntityDbTable.rollback(db, table, height, dbKeyFactory);
    }

    @Override
    public final void trim(int height) {
        VersionedEntityDbTable.trim(db, table, height, dbKeyFactory);
    }

}
