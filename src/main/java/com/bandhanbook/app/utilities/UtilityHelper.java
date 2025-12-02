package com.bandhanbook.app.utilities;

import org.springframework.stereotype.Component;

@Component
public class UtilityHelper {
    public static String generateOtp() {
        int otp = (int) (Math.random() * 900000) + 100000;
        return String.valueOf(otp);
    }
}
