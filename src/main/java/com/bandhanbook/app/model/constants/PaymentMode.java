package com.bandhanbook.app.model.constants;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PaymentMode {
        CASH("cash"),
        ONLINE("Online"),
        CHEQUE("CHEQUE");

    PaymentMode(String name) {
        }

        @JsonCreator
        public static PaymentMode fromValue(String value) {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            return PaymentMode.valueOf(value.trim().toUpperCase());
        }
}
