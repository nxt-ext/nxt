package nxt.env;

import java.util.Properties;

public interface UserInterfaceMode {
    boolean isLoadPropertyFileFromUserDir();

    void updateLogFileHandler(Properties loggingProperties);

    String getDbDir(String dbDir);
}
