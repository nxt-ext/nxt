package nxt.env.service;

import nxt.Nxt;
import nxt.env.LookAndFeel;

import javax.swing.*;

@SuppressWarnings("UnusedDeclaration")
public class NxtService_ServiceManagement {

    public static boolean serviceInit() {
        LookAndFeel.init();
        new Thread(new Runnable() {
            @Override
            public void run() {
                String[] args = {};
                Nxt.main(args);
            }
        }).start();
        return true;
    }

    // Invoked when registering the service
    public static String[] serviceGetInfo() {
        return new String[]{
                "NXT Server", // Long name
                "Manages the NXT cryptographic currency protocol", // Description
                "true", // IsAutomatic
                "true", // IsAcceptStop
                "", // failure exe
                "", // args failure
                "", // dependencies
                "NONE/NONE/NONE", // ACTION = NONE | REBOOT | RESTART | RUN
                "0/0/0", // ActionDelay in seconds
                "-1", // Reset time in seconds
                "", // Boot Message
                "false" // IsAutomatic Delayed
        };
    }

    public static boolean serviceIsCreate() {
        return JOptionPane.showConfirmDialog(null, "Do you want to install the NXT service ?", "Create Service", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    public static boolean serviceIsLaunch() {
        return true;
    }

    public static boolean serviceIsDelete() {
        return JOptionPane.showConfirmDialog(null, "This NXT service is already installed. Do you want to delete it ?", "Delete Service", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    public static boolean serviceControl_Pause() {
        return false;
    }

    public static boolean serviceControl_Continue() {
        return false;
    }

    public static boolean serviceControl_Stop() {
        return true;
    }

    public static boolean serviceControl_Shutdown() {
        return true;
    }

    public static void serviceFinish() {
        System.exit(0);
    }

}
