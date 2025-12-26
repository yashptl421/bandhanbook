package com.bandhanbook.app.model.constants;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum MaritalStatus {
    SINGLE("Single"), MARRIED("Married"), UNMARRIED("Unmarried"), ENGAGED("Engaged"), DIVORCED("Divorced"), WIDOWED("Widowed");

    MaritalStatus(String name) {
    }
    @JsonCreator
    public static MaritalStatus fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return MaritalStatus.valueOf(value.trim().toUpperCase());
    }
}
