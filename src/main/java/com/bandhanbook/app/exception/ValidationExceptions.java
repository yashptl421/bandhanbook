package com.bandhanbook.app.exception;

public class ValidationExceptions extends RuntimeException {
    public ValidationExceptions() {
        super();
    }

    public ValidationExceptions(String message, Throwable cause) {
        super(message, cause);
    }

    public ValidationExceptions(String message) {
        super(message);
    }

    public ValidationExceptions(Throwable cause) {
        super(cause);
    }
}