package nxt.env;

import java.net.URI;
import java.util.Properties;

public interface RuntimeMode {
    public final String RUNTIME_MODE_ARG = "nxt.runtime.mode";

    boolean isLoadPropertyFileFromUserDir();

    void updateLogFileHandler(Properties loggingProperties);

    String getDbDir(String dbDir);

    void init();

    void setServerStatus(String status, URI wallet);

    void shutdown();
}
