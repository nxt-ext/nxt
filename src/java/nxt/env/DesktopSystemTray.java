package nxt.env;

import nxt.util.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class DesktopSystemTray {

    private SystemTray tray;
    private TrayIcon trayIcon;
    private MenuItem openWallet;
    private MenuItem viewLog;
    private SystemTrayDataProvider dataProvider;

    void createAndShowGUI() {
        if (!SystemTray.isSupported()) {
            Logger.logInfoMessage("SystemTray is not supported");
            return;
        }
        final PopupMenu popup = new PopupMenu();
        trayIcon = new TrayIcon(new ImageIcon("html/ui/img/nxt-icon-32x32.png", "tray icon").getImage());
        trayIcon.setImageAutoSize(true);
        tray = SystemTray.getSystemTray();

        MenuItem shutdown = new MenuItem("Shutdown");
        openWallet = new MenuItem("Open Wallet");
        if (!Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            openWallet.setEnabled(false);
        }
        viewLog = new MenuItem("View Log File");
        if (!Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            viewLog.setEnabled(false);
        }
        MenuItem status = new MenuItem("Status");

        popup.add(status);
        popup.add(viewLog);
        popup.addSeparator();
        popup.add(openWallet);
        popup.addSeparator();
        popup.add(shutdown);
        trayIcon.setPopupMenu(popup);
        trayIcon.setToolTip("Initializing");
        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            Logger.logInfoMessage("TrayIcon could not be added", e);
            return;
        }

        trayIcon.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                trayIcon.displayMessage("NXT", trayIcon.getToolTip(), TrayIcon.MessageType.INFO);
            }
        });

        openWallet.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    Desktop.getDesktop().browse(dataProvider.getWallet());
                } catch (IOException ex) {
                    Logger.logInfoMessage("Cannot open wallet", ex);
                }
            }
        });

        viewLog.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    Desktop.getDesktop().open(dataProvider.getLogFile());
                } catch (IOException ex) {
                    Logger.logInfoMessage("Cannot view log", ex);
                }
            }
        });

        shutdown.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tray.remove(trayIcon);
                Logger.logInfoMessage("Shutdown requested by System Tray");
                System.exit(0); // Implicitly invokes shutdown using the shutdown hook
            }
        });
    }

    void setToolTip(final SystemTrayDataProvider dataProvider) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                trayIcon.setToolTip(dataProvider.getToolTip());
                openWallet.setEnabled(dataProvider.getWallet() != null && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE));
                viewLog.setEnabled(dataProvider.getWallet() != null);
                DesktopSystemTray.this.dataProvider = dataProvider;
            }
        });
    }

}
