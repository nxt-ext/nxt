package nxt.env;

import java.net.URI;
import java.util.Properties;

public interface UserInterfaceMode {
    boolean isLoadPropertyFileFromUserDir();

    void updateLogFileHandler(Properties loggingProperties);

    String getDbDir(String dbDir);

    void init();

    void setServerStatus(String status, URI wallet);

    void shutdown();
}
