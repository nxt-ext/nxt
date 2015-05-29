package nxt.env;

import java.io.File;
import java.net.URI;
import java.util.Properties;

public interface RuntimeMode {

    public final String RUNTIME_MODE_ARG = "nxt.runtime.mode";

    void init();

    void setServerStatus(String status, URI wallet, File logFileDir);

    void shutdown();
}
