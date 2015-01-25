package nxt.env;

import java.util.Properties;

public class CommandLineMode implements UserInterfaceMode {

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
}
