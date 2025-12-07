package com.bandhanbook.app.utilities;

import org.springframework.stereotype.Component;

@Component
public class UtilityHelper {
    public static String generateOtp() {
        int otp = (int) (Math.random() * 900000) + 100000;
        return String.valueOf(otp);
    }
    public static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return phoneNumber;
        }
        String lastFourDigits = phoneNumber.substring(phoneNumber.length() - 4);
        return "XXXXXX" + lastFourDigits;
    }
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domainPart = parts[1];

        if (localPart.length() <= 2) {
            return "X@" + domainPart;
        }

        StringBuilder maskedLocalPart = new StringBuilder();
        maskedLocalPart.append(localPart.charAt(0));
        for (int i = 1; i < localPart.length() - 1; i++) {
            maskedLocalPart.append("X");
        }
        maskedLocalPart.append(localPart.charAt(localPart.length() - 1));

        return maskedLocalPart.toString() + "@" + domainPart;
    }
    public static String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%&*!";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * chars.length());
            password.append(chars.charAt(index));
        }
        return password.toString();
    }
}
