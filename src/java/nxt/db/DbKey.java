package nxt.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public interface DbKey<T> {

    public static interface Factory<T> {

        DbKey<T> newKey(T t);

        DbKey<T> newKey(ResultSet rs) throws SQLException;

        String getPKClause();

        String getDistinctClause();

    }

    int setPK(PreparedStatement pstmt) throws SQLException;

    int setPK(PreparedStatement pstmt, int index) throws SQLException;


    public static abstract class LongIdFactory<T> implements Factory<T> {

        private final String idColumn;

        public LongIdFactory(String idColumn) {
            this.idColumn = idColumn;
        }

        @Override
        public DbKey<T> newKey(ResultSet rs) throws SQLException {
            return new LongId<>(rs.getLong(idColumn));
        }

        public DbKey<T> newKey(Long id) {
            return new LongId<>(id);
        }

        @Override
        public String getPKClause() {
            return " WHERE " + idColumn + " = ? ";
        }

        @Override
        public String getDistinctClause() {
            return " DISTINCT " + idColumn + " ";
        }

    }

    public static abstract class LinkIdFactory<T> implements Factory<T> {

        private final String idColumnA;
        private final String idColumnB;

        public LinkIdFactory(String idColumnA, String idColumnB) {
            this.idColumnA = idColumnA;
            this.idColumnB = idColumnB;
        }

        @Override
        public DbKey<T> newKey(ResultSet rs) throws SQLException {
            return new LinkId<>(rs.getLong(idColumnA), rs.getLong(idColumnB));
        }

        public DbKey<T> newKey(Long idA, Long idB) {
            return new LinkId<>(idA, idB);
        }

        @Override
        public String getPKClause() {
            return " WHERE " + idColumnA + " = ? AND " + idColumnB + " = ? ";
        }

        @Override
        public String getDistinctClause() {
            return " DISTINCT " + idColumnA + ", " + idColumnB + " ";
        }
    }

    static final class LongId<T> implements DbKey<T> {

        private final Long id;

        private LongId(Long id) {
            this.id = id;
        }

        @Override
        public int setPK(PreparedStatement pstmt) throws SQLException {
            return setPK(pstmt, 1);
        }

        @Override
        public int setPK(PreparedStatement pstmt, int index) throws SQLException {
            pstmt.setLong(index, id);
            return index + 1;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof LongId && ((LongId)o).id.equals(id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

    }

    static final class LinkId<T> implements DbKey<T> {

        private final Long idA;
        private final Long idB;

        private LinkId(Long idA, Long idB) {
            this.idA = idA;
            this.idB = idB;
        }

        @Override
        public int setPK(PreparedStatement pstmt) throws SQLException {
            return setPK(pstmt, 1);
        }

        @Override
        public int setPK(PreparedStatement pstmt, int index) throws SQLException {
            pstmt.setLong(index, idA);
            pstmt.setLong(index + 1, idB);
            return index + 2;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof LinkId && ((LinkId) o).idA.equals(idA) && ((LinkId) o).idB.equals(idB);
        }

        @Override
        public int hashCode() {
            return idA.hashCode() ^ idB.hashCode();
        }

    }

}
