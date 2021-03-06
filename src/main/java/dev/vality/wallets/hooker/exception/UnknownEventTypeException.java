package dev.vality.wallets.hooker.exception;

public class UnknownEventTypeException extends RuntimeException {

    public UnknownEventTypeException() {
    }

    public UnknownEventTypeException(String message) {
        super(message);
    }

    public UnknownEventTypeException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnknownEventTypeException(Throwable cause) {
        super(cause);
    }

    public UnknownEventTypeException(String message, Throwable cause, boolean enableSuppression,
                                     boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
