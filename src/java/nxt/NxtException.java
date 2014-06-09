package nxt;

public abstract class NxtException extends Exception {

    protected NxtException() {
        super();
    }

    protected NxtException(String message) {
        super(message);
    }

    protected NxtException(String message, Throwable cause) {
        super(message, cause);
    }

    protected NxtException(Throwable cause) {
        super(cause);
    }

    public static class ValidationException extends NxtException {

        ValidationException(String message) {
            super(message);
        }

        ValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class IllegalStateException extends NxtException {

        IllegalStateException(String message) {
            super(message);
        }

        IllegalStateException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
