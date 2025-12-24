package com.bandhanbook.app.utilities;

import com.bandhanbook.app.model.MatrimonyCandidate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Component
public class UtilityHelper {
    public static String generateOtp() {
        int otp = (int) (Math.random() * 900000) + 100000;
        return String.valueOf(123456);
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

    public static boolean validPhoneNumber(String phoneNumber) {
        String regex = "^[0-9]{10}$";
        return phoneNumber != null && phoneNumber.matches(regex);
    }

    public int getProfileCompletion(MatrimonyCandidate profile) {
        if (profile == null) return 0;

        List<Object> values = new ArrayList<>();
        collectValues(profile, values);

        long filled = values.stream()
                .filter(this::isFilled)
                .count();

        int total = values.size();
        if (total == 0) return 0;

        return (int) Math.round((filled * 100.0) / total);
    }

    // ---------------------------------------------------
    // Recursive field traversal (replacement for getAllPaths)
    // ---------------------------------------------------
    private void collectValues(Object obj, List<Object> values) {
        if (obj == null) return;

        for (Field field : obj.getClass().getDeclaredFields()) {
            field.setAccessible(true);

            // Ignore technical fields
            if (field.getName().equals("id") ||
                    field.getName().equals("userId") ||
                    field.getName().equals("createdAt") ||
                    field.getName().equals("updatedAt")) {
                continue;
            }

            try {
                Object value = field.get(obj);

                if (value == null) {
                    values.add(null);
                }
                // Primitive / wrapper / String
                else if (isSimpleValue(value)) {
                    values.add(value);
                }
                // Collection
                else if (value instanceof Collection<?> col) {
                    if (!col.isEmpty()) values.add(col);
                }
                // Nested object → recurse
                else {
                    collectValues(value, values);
                }
            } catch (IllegalAccessException ignored) {
            }
        }
    }

    private boolean isSimpleValue(Object value) {
        return value instanceof String ||
                value instanceof Integer ||
                value instanceof Boolean ||
                value instanceof LocalDate ||
                value instanceof Date ||
                value instanceof LocalDateTime ||
                value instanceof Enum<?>;
    }

    private boolean isFilled(Object value) {
        if (value == null) return false;
        if (value instanceof String s) return !s.isBlank();
        if (value instanceof Collection<?> c) return !c.isEmpty();
        return true;
    }
}
