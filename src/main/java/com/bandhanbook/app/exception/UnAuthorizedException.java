package com.bandhanbook.app.exception;

public class UnAuthrizedException extends RuntimeException {
    public UnAuthrizedException() {
        super();
    }

    public UnAuthrizedException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnAuthrizedException(String message) {
        super(message);
    }

    public UnAuthrizedException(Throwable cause) {
        super(cause);
    }
}