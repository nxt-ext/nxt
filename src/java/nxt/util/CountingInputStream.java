package nxt.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class CountingInputStream extends FilterInputStream {

    private long count;
    private final long limit;

    public CountingInputStream(InputStream in, long limit) {
        super(in);
        this.limit = limit;
    }

    @Override
    public int read() throws IOException {
        int read = super.read();
        if (read >= 0) {
            incCount(1);
        }
        return read;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int read = super.read(b, off, len);
        if (read >= 0) {
            incCount(read);
        }
        return read;
    }

    @Override
    public long skip(long n) throws IOException {
        long skipped = super.skip(n);
        if (skipped >= 0) {
            incCount(skipped);
        }
        return skipped;
    }

    public long getCount() {
        return count;
    }

    private void incCount(long n) {
        count += n;
        if (count > limit) {
            throw new RuntimeException("Maximum size exceeded: " + count);
        }
    }
}
