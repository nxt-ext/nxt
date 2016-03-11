package nxt.env;

import nxt.Nxt;

public enum ServerStatus {
    BEFORE_DATABASE("Loading Database"), AFTER_DATABASE("Loading Resources"), STARTED("Online");

    private String message;

    ServerStatus(String message) {
        this.message = message;
    }

    public boolean isLaunchApplication() {
        return this == STARTED && Nxt.getBooleanProperty("nxt.launchDesktopApplication");
    }

    public String getMessage() {
        return message;
    }
}
