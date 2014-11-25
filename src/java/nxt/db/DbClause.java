package nxt.db;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class DbClause {

    private final String clause;

    protected DbClause(String clause) {
        this.clause = clause;
    }

    final String getClause() {
        return clause;
    }

    protected abstract int set(PreparedStatement pstmt, int index) throws SQLException;

    public static final DbClause EMPTY_CLAUSE = new FixedClause(" TRUE ");

    public static final class FixedClause extends DbClause {

        public FixedClause(String clause) {
            super(clause);
        }

        @Override
        protected int set(PreparedStatement pstmt, int index) throws SQLException {
            return index;
        }

    }

    public static final class StringClause extends DbClause {

        private final String value;

        public StringClause(String columnName, String value) {
            super(" " + columnName + " = ? ");
            this.value = value;
        }

        protected int set(PreparedStatement pstmt, int index) throws SQLException {
            pstmt.setString(index, value);
            return index + 1;
        }

    }

    public static final class LongClause extends DbClause {

        private final long value;

        public LongClause(String columnName, long value) {
            super(" " + columnName + " = ? ");
            this.value = value;
        }

        protected int set(PreparedStatement pstmt, int index) throws SQLException {
            pstmt.setLong(index, value);
            return index + 1;
        }
    }

    public static final class BooleanClause extends DbClause {

        private final boolean value;

        public BooleanClause(String columnName, boolean value) {
            super(" " + columnName + " = ? ");
            this.value = value;
        }

        protected int set(PreparedStatement pstmt, int index) throws SQLException {
            pstmt.setBoolean(index, value);
            return index + 1;
        }
    }

    public static final class LongBooleanClause extends DbClause {

        private final long value1;
        private final boolean value2;

        public LongBooleanClause(String column1Name, long value1, String column2Name, boolean value2) {
            super(" " + column1Name + " = ? AND " + column2Name + " = ? ");
            this.value1 = value1;
            this.value2 = value2;
        }

        protected int set(PreparedStatement pstmt, int index) throws SQLException {
            pstmt.setLong(index, value1);
            pstmt.setBoolean(index + 1, value2);
            return index + 2;
        }
    }

    public static final class LongLongClause extends DbClause {

        private final long value1;
        private final long value2;

        public LongLongClause(String column1Name, long value1, String column2Name, long value2) {
            super(" " + column1Name + " = ? AND " + column2Name + " = ? ");
            this.value1 = value1;
            this.value2 = value2;
        }

        protected int set(PreparedStatement pstmt, int index) throws SQLException {
            pstmt.setLong(index, value1);
            pstmt.setLong(index + 1, value2);
            return index + 2;
        }
    }
}
