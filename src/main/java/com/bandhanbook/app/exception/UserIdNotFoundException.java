package com.bandhanbook.app.exception;

public class UserIdNotFoundException extends RuntimeException {
    public UserIdNotFoundException() {
        super();
    }

    public UserIdNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserIdNotFoundException(String message) {
        super(message);
    }

    public UserIdNotFoundException(Throwable cause) {
        super(cause);
    }
}