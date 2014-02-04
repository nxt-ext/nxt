package nxt.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

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

    public static interface ResultSetReader<T> {
        public T get(ResultSet rs) throws Exception;
    }

    public static <T> Iterator<T> getDbIterator(final Connection con, final PreparedStatement pstmt, final ResultSetReader<T> rsReader) {
        try {
            final ResultSet rs = pstmt.executeQuery();

            return new Iterator<T>() {
                boolean hasNext = rs.next();

                @Override
                public boolean hasNext() {
                    if (! hasNext) {
                        DbUtils.close(rs, pstmt, con);
                    }
                    return hasNext;
                }

                @Override
                public T next() {
                    if (! hasNext) {
                        DbUtils.close(rs, pstmt, con);
                        return null;
                    }
                    try {
                        T result = rsReader.get(rs);
                        hasNext = rs.next();
                        return result;
                    } catch (Exception e) {
                        DbUtils.close(rs, pstmt, con);
                        throw new RuntimeException(e.toString(), e);
                    }
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("Removal not suported");
                }
            };

        } catch (SQLException e) {
            DbUtils.close(pstmt, con);
            throw new RuntimeException(e.toString(), e);
        }

    }


    private DbUtils() {} // never
}
