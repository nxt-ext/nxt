package nxt.env;

import java.io.File;
import java.net.URI;

public class CommandLineMode implements RuntimeMode {

    @Override
    public void init() {}

    @Override
    public void setServerStatus(String status, URI wallet, File logFileDir) {}

    @Override
    public void shutdown() {}
}
