package com.bandhanbook.app.utilities;

public interface ErrorResponseMessages {
    String INTERNAL_SERVER_ERROR = "Internal server error;";
    String DATA_NOT_FOUND = "Data not found";
    String VALIDATION_ERROR = "Validation error";
    String PHONE_EXISTS = "User with this phone number already exists.";
    String EMAIL_EXISTS = "User with this email already exists.";
    String BLOCKED = "Your account is blocked; kindly contact your administrator.";
    String INVALID_OTP = "Invalid or expired OTP";
    String INVALID_CREDENTIALS = "Invalid credentials";
    String INCORRECT_PASSWORD = "Current password is incorrect";
    String PLAN_NOT_FOUND = "Selected plan is not available currently";

}
