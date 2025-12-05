package com.bandhanbook.app.exception;

public class PhoneOrEmailNotFoundException extends RuntimeException {
    public PhoneOrEmailNotFoundException() {
        super();
    }

    public PhoneOrEmailNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public PhoneOrEmailNotFoundException(String message) {
        super(message);
    }

    public PhoneOrEmailNotFoundException(Throwable cause) {
        super(cause);
    }
}