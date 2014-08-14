package nxt.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public final class DbUtils {

    public static void close(AutoCloseable... closeables) {
        for (AutoCloseable closeable : closeables) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (Exception ignore) {}
            }
        }
    }

    public static void setBytes(PreparedStatement pstmt, int index, byte[] bytes) throws SQLException {
        if (bytes != null) {
            pstmt.setBytes(index, bytes);
        } else {
            pstmt.setNull(index, Types.BINARY);
        }
    }

    public static void setString(PreparedStatement pstmt, int index, String s) throws SQLException {
        if (s != null) {
            pstmt.setString(index, s);
        } else {
            pstmt.setNull(index, Types.VARCHAR);
        }
    }

    public static void setInt(PreparedStatement pstmt, int index, Integer n) throws SQLException {
        if (n != null) {
            pstmt.setInt(index, n);
        } else {
            pstmt.setNull(index, Types.INTEGER);
        }
    }

    public static void setLong(PreparedStatement pstmt, int index, Long l) throws SQLException {
        if (l != null) {
            pstmt.setLong(index, l);
        } else {
            pstmt.setNull(index, Types.BIGINT);
        }
    }

    public static Long getLong(ResultSet rs, String columnName) throws SQLException {
        long l = rs.getLong(columnName);
        return rs.wasNull() ? null : l;
    }

    public static Integer getInt(ResultSet rs, String columnName) throws SQLException {
        int n = rs.getInt(columnName);
        return rs.wasNull() ? null : n;
    }

    private DbUtils() {} // never

}
