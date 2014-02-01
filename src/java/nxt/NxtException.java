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

    public static final class ValidationFailure extends NxtException {

        public ValidationFailure(String message) {
            super(message);
        }

        public ValidationFailure(String message, Throwable cause) {
            super(message, cause);
        }

    }

}
