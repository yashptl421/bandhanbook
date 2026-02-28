package com.bandhanbook.app.utilities;

public interface ErrorResponseMessages {
    String INTERNAL_SERVER_ERROR = "Internal server error;";
    String DATA_NOT_FOUND = "Data not found";
    String VALIDATION_ERROR = "Validation error";
    String PHONE_EXISTS = "User with this phone number already exists.";
    String PHONE_EMAIL_EXISTS = "User with this phone number Or Email already exists.";
    String EMAIL_EXISTS = "User with this email already exists.";
    String BLOCKED = "Your account is blocked; kindly contact your administrator.";
    String INVALID_OTP = "Invalid or expired OTP";
    String INVALID_CREDENTIALS = "Invalid credentials";
    String INCORRECT_PASSWORD = "Current password is incorrect";
    String USER_NOT_FOUND = "User not found";
    String UNAUTHORIZED_ACCESS = "Unauthorized access";
    String FILE_UPLOAD_ERROR = "Error uploading file";
    String RECORD_NOT_FOUND = "Record not found";
    String INVALID_FILE_TYPE = "Invalid file type";
    String PLAN_NOT_FOUND = "Selected plan is not available currently";
    String IMAGE_SIZE_EXCEEDED = "Image size exceeds the maximum allowed limit of 5 MB";
    String INVALID_RESOURCE = "Please login to access this resource.";
    String SUBSCRIPTION_INACTIVE = "Organization subscription is inactive. Please contact administrator.";
    String AGENT_LIMIT_EXCEED = "Agent Limit Exceeded. Please Upgrade your Plan.";
    String CANDIDATE_LIMIT_EXCEED = "Candidate Limit Exceeded. Please Upgrade your Plan.";
    String PENDING_CLOSER = "You have pending closure, Please contact organization to accept pending one";
    String SETTLEMENT_ACCESS_ERROR = "Don't have access to settlement";
    String SETTLEMENT_REVERT_ERROR = "Can not revert to pending status";
    String SETTLEMENT_INVALID = "Invalid settlement request settlement request";
    String SETTLEMENT_INSUFFICIENT = "Insufficient remaining amount";
    String SETTLEMENT_NOT_FOUND = "Settlement record not found";
    String DONATION_NOT_FOUND = "Donation record not found";
    String DONATION_INSUFFICIENT = "Insufficient remaining amount for donation";
    String DONATION_INVALID = "Invalid donation request";
    String SUBSCRIPTION_NOT_FOUND = "Subscription not found";
    String ADDON_NOT_FOUND = "No addons found for the subscription";
    String SUBSCRIPTION_EXIST = "An active subscription already exists for this organization. Please contact support for assistance.";
    String BANNER_LIMIT_EXCEED = "Banner Limit Exceeded. Please Upgrade your Plan.";
    String ADVERTISEMENT_LIMIT_EXCEED = "Advertisement Limit Exceeded. Please Upgrade your Plan.";
}
