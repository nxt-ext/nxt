package nxt.env;

import java.awt.*;

public class UserInterfaceModeFactory {

    public static UserInterfaceMode getMode() {
        if ("true".equalsIgnoreCase(System.getProperty("nxt.desktop.mode")) && !GraphicsEnvironment.isHeadless()) {
            return new DesktopMode();
        } else {
            return new CommandLineMode();
        }
    }
}
