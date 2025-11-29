package com.bandhanbook.app.exception;

public class InvalidOtpException extends RuntimeException {
    public InvalidOtpException() {
        super();
    }

    public InvalidOtpException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidOtpException(String message) {
        super(message);
    }

    public InvalidOtpException(Throwable cause) {
        super(cause);
    }
}
