package nxt.env;

import javax.swing.*;
import java.io.File;
import java.net.URI;

public class DesktopMode implements RuntimeMode {

    private DesktopSystemTray desktopSystemTray;

    @Override
    public void init() {
        LookAndFeel.init();
        desktopSystemTray = new DesktopSystemTray();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                desktopSystemTray.createAndShowGUI();
            }
        });
    }

    @Override
    public void setServerStatus(String status, URI wallet, File logFileDir) {
        desktopSystemTray.setToolTip(new SystemTrayDataProvider(status, wallet, logFileDir));
    }

    @Override
    public void shutdown() {
        desktopSystemTray.shutdown();
    }
}
