package nxt.env;

import nxt.Block;
import nxt.Constants;
import nxt.Generator;
import nxt.Nxt;
import nxt.http.API;
import nxt.peer.Peers;
import nxt.util.Convert;
import nxt.util.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Date;

public class DesktopSystemTray {

    private SystemTray tray;
    private ImageIcon imageIcon;
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
        imageIcon = new ImageIcon("html/ui/img/nxt-icon-32x32.png", "tray icon");
        trayIcon = new TrayIcon(imageIcon.getImage());
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
                displayStatus();
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

        status.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                displayStatus();
            }
        });

        shutdown.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Logger.logInfoMessage("Shutdown requested by System Tray");
                System.exit(0); // Implicitly invokes shutdown using the shutdown hook
            }
        });
    }

    private void displayStatus() {
        Block lastBlock = Nxt.getBlockchain().getLastBlock();
        Collection<Generator> allGenerators = Generator.getAllGenerators();

        StringBuilder generators = new StringBuilder();
        for (Generator generator : allGenerators) {
            generators.append('\n').append(Convert.rsAccount(generator.getAccountId()));
        }
        StringBuilder sb = new StringBuilder();
        String format = "%s: %s\n";
        sb.append(String.format(format, "Application", Nxt.APPLICATION));
        sb.append(String.format(format, "Version", Nxt.VERSION));
        sb.append(String.format(format, "Network", (Constants.isTestnet) ? "test" : "main"));
        sb.append(Constants.isTestnet ? String.format(format, "Working offline", Constants.isOffline) : "");
        sb.append(String.format(format, "Wallet", API.getBrowserUri()));
        sb.append(String.format(format, "Peer port", Peers.getDefaultPeerPort()));
        sb.append(String.format(format, "Program folder", Paths.get(".").toAbsolutePath().getParent()));
        sb.append(String.format(format, "User folder", Paths.get(DesktopMode.NXT_USER_HOME).toAbsolutePath()));

        if (lastBlock != null) {
            sb.append("\nLast Block\n");
            sb.append(String.format(format, "Height", lastBlock.getHeight()));
            sb.append(String.format(format, "Timestamp", lastBlock.getTimestamp()));
            sb.append(String.format(format, "Time", new Date(Convert.fromEpochTime(lastBlock.getTimestamp()))));
            sb.append(String.format(format, "Seconds passed", Nxt.getEpochTime() - lastBlock.getTimestamp()));
        }

        sb.append("\n");
        sb.append(String.format(format, "Forging", allGenerators.size() > 0));
        if (allGenerators.size() > 0) {
            sb.append(String.format(format, "Forging accounts", generators.toString()));
        }
        sb.append("\nEnvironment\n");
        sb.append(String.format(format, "Number of peers", Peers.getAllPeers().size()));
        sb.append(String.format(format, "Available processors", Runtime.getRuntime().availableProcessors()));
        sb.append(String.format(format, "Max memory", humanReadableByteCount(Runtime.getRuntime().maxMemory())));
        sb.append(String.format(format, "Total memory", humanReadableByteCount(Runtime.getRuntime().totalMemory())));
        sb.append(String.format(format, "Free memory", humanReadableByteCount(Runtime.getRuntime().freeMemory())));
        sb.append(String.format(format, "Process id", Nxt.getProcessId()));
        JOptionPane.showMessageDialog(null, sb.toString(), "NXT Server Status", JOptionPane.INFORMATION_MESSAGE, imageIcon);
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

    void shutdown() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                tray.remove(trayIcon);
            }
        });
    }

    public static String humanReadableByteCount(long bytes) {
        int unit = 1000;
        if (bytes < unit) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = "" + ("KMGTPE").charAt(exp-1);
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}
