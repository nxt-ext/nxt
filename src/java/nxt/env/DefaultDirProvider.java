package nxt.env;

import java.io.File;
import java.nio.file.Paths;
import java.util.Properties;

public class DefaultDirProvider implements DirProvider {

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
    public File getLogFileDir() {
        return null;
    }

    @Override
    public String getUserHomeDir() {
        return Paths.get(".").toAbsolutePath().getParent().toString();
    }

}
