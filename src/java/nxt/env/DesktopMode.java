package nxt.env;

import nxt.util.Logger;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class DesktopMode implements UserInterfaceMode {

    public static final String NXT_USER_HOME = Paths.get(System.getProperty("user.home"), "AppData", "Roaming", "NXT").toString();
    public static final String LOG_FILE_PATTERN = "java.util.logging.FileHandler.pattern";
    private File logFileDir;

    private DesktopSystemTray desktopSystemTray;

    public void init() {
        /* Use an appropriate Look and Feel */
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (UnsupportedLookAndFeelException | IllegalAccessException | InstantiationException | ClassNotFoundException e) {
            Logger.logInfoMessage("Error initializing look & feel", e);
        }
        /* Turn off metal's use of bold fonts */
        UIManager.put("swing.boldMetal", Boolean.FALSE);
        desktopSystemTray = new DesktopSystemTray();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                desktopSystemTray.createAndShowGUI();
            }
        });
    }

    @Override
    public boolean isLoadPropertyFileFromUserDir() {
        return true;
    }

    @Override
    public void updateLogFileHandler(Properties loggingProperties) {
        if (loggingProperties.getProperty(LOG_FILE_PATTERN) == null) {
            return;
        }
        Path logFilePattern = Paths.get(NXT_USER_HOME, loggingProperties.getProperty(LOG_FILE_PATTERN));
        loggingProperties.setProperty(LOG_FILE_PATTERN, logFilePattern.toString());

        Path logDirPath = logFilePattern.getParent();
        this.logFileDir = new File(logDirPath.toString());
        if (!Files.isReadable(logDirPath)) {
            System.out.printf("Creating dir %s\n", logDirPath);
            try {
                Files.createDirectory(logDirPath);
            } catch (IOException e) {
                throw new IllegalArgumentException("Cannot create " + logDirPath, e);
            }
        }
    }

    @Override
    public String getDbDir(String dbDir) {
        return Paths.get(NXT_USER_HOME, dbDir).toString();
    }

    @Override
    public void setServerStatus(String status, URI wallet) {
        desktopSystemTray.setToolTip(new SystemTrayDataProvider(status, wallet, logFileDir));
    }

    @Override
    public void shutdown() {
        desktopSystemTray.shutdown();
    }
}
