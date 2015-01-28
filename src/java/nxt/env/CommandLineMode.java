package nxt.env;

import java.net.URI;
import java.util.Properties;

public class CommandLineMode implements RuntimeMode {

    @Override
    public boolean isLoadPropertyFileFromUserDir() {
        return false;
    }

    @Override
    public void updateLogFileHandler(Properties loggingProperties) {}

    @Override
    public String getDbDir(String dbDir) {
        return dbDir;
    }

    @Override
    public void init() {}

    @Override
    public void setServerStatus(String status, URI wallet) {}

    @Override
    public void shutdown() {}
}
