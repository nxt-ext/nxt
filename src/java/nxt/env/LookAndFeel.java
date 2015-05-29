package nxt.env;

import javax.swing.*;

public class LookAndFeel {

    public static void init() {
        if (System.getProperty("os.name").startsWith("Windows")) {
            try {
                UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
            } catch (UnsupportedLookAndFeelException | IllegalAccessException | InstantiationException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            UIManager.put("swing.boldMetal", Boolean.FALSE);
        }
    }

}
