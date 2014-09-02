package nxt.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public interface DbKey {

    public static interface Factory<T> {

        DbKey newKey(T t);

        DbKey newKey(ResultSet rs) throws SQLException;

        String getPKClause();

        String getPKColumns();

    }

    int setPK(PreparedStatement pstmt) throws SQLException;

    int setPK(PreparedStatement pstmt, int index) throws SQLException;


    public static abstract class LongKeyFactory<T> implements Factory<T> {

        private final String idColumn;

        public LongKeyFactory(String idColumn) {
            this.idColumn = idColumn;
        }

        @Override
        public DbKey newKey(ResultSet rs) throws SQLException {
            return new LongKey(rs.getLong(idColumn));
        }

        public DbKey newKey(Long id) {
            return new LongKey(id);
        }

        @Override
        public String getPKClause() {
            return " WHERE " + idColumn + " = ? ";
        }

        @Override
        public String getPKColumns() {
            return idColumn;
        }

    }

    public static abstract class LinkKeyFactory<T> implements Factory<T> {

        private final String idColumnA;
        private final String idColumnB;

        public LinkKeyFactory(String idColumnA, String idColumnB) {
            this.idColumnA = idColumnA;
            this.idColumnB = idColumnB;
        }

        @Override
        public DbKey newKey(ResultSet rs) throws SQLException {
            return new LinkKey(rs.getLong(idColumnA), rs.getLong(idColumnB));
        }

        public DbKey newKey(Long idA, Long idB) {
            return new LinkKey(idA, idB);
        }

        @Override
        public String getPKClause() {
            return " WHERE " + idColumnA + " = ? AND " + idColumnB + " = ? ";
        }

        @Override
        public String getPKColumns() {
            return idColumnA + ", " + idColumnB;
        }
    }

    static final class LongKey implements DbKey {

        private final Long id;

        private LongKey(Long id) {
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
            return o instanceof LongKey && ((LongKey)o).id.equals(id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

    }

    static final class LinkKey implements DbKey {

        private final Long idA;
        private final Long idB;

        private LinkKey(Long idA, Long idB) {
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
            return o instanceof LinkKey && ((LinkKey) o).idA.equals(idA) && ((LinkKey) o).idB.equals(idB);
        }

        @Override
        public int hashCode() {
            return idA.hashCode() ^ idB.hashCode();
        }

    }

}
