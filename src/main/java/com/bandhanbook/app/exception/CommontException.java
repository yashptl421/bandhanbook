package com.bandhanbook.app.exception;

public class CommontException extends RuntimeException {
    public CommontException() {
        super();
    }

    public CommontException(String message, Throwable cause) {
        super(message, cause);
    }

    public CommontException(String message) {
        super(message);
    }

    public CommontException(Throwable cause) {
        super(cause);
    }
}