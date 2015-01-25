package nxt.env;

import java.nio.file.Paths;
import java.util.Properties;

public class DesktopMode implements UserInterfaceMode {

    public static final String NXT_USER_HOME = Paths.get(System.getProperty("user.home"), "AppData", "Roaming", "NXT").toString();
    public static final String LOG_FILE_PATTERN = "java.util.logging.FileHandler.pattern";

    @Override
    public boolean isLoadPropertyFileFromUserDir() {
        return true;
    }

    @Override
    public void updateLogFileHandler(Properties loggingProperties) {
        if (loggingProperties.getProperty(LOG_FILE_PATTERN) == null) {
            return;
        }
        loggingProperties.setProperty(LOG_FILE_PATTERN, Paths.get(NXT_USER_HOME,  loggingProperties.getProperty(LOG_FILE_PATTERN)).toString());
    }

    @Override
    public String getDbDir(String dbDir) {
        return Paths.get(NXT_USER_HOME, dbDir).toString();
    }
}
