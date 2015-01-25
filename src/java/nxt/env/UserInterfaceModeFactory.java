package nxt.env;

public class UserInterfaceModeFactory {

    public static UserInterfaceMode getMode() {
        if ("true".equalsIgnoreCase(System.getProperty("nxt.desktop.mode"))) {
            return new DesktopMode();
        } else {
            return new CommandLineMode();
        }
    }
}
