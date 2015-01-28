package nxt.env;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class UserInterfaceModeFactory {

    public static RuntimeMode getMode() {
        boolean isHeadless;
        try {
            // Load by reflection to prevent exception in case java.awt does not exist
            Class graphicsEnvironmentClass = Class.forName("java.awt.GraphicsEnvironment");
            Method isHeadlessMethod = graphicsEnvironmentClass.getMethod("isHeadless");
            isHeadless = (Boolean)isHeadlessMethod.invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            isHeadless = true;
        }
        System.out.println("isHeadless=" + isHeadless);
        if ("desktop".equalsIgnoreCase(System.getProperty(RuntimeMode.RUNTIME_MODE_ARG)) && !isHeadless) {
            return new DesktopMode();
        } else if ("service".equalsIgnoreCase(System.getProperty(RuntimeMode.RUNTIME_MODE_ARG))) {
            return new WindowsServiceMode();
        } else {
            return new CommandLineMode();
        }
    }
}
