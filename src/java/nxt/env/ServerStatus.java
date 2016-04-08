package nxt.env;

public enum ServerStatus {
    BEFORE_DATABASE("Loading Database"), AFTER_DATABASE("Loading Resources"), STARTED("Online");

    private final String message;

    ServerStatus(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
