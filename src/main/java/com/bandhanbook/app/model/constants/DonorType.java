package com.bandhanbook.app.model.constants;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DonorType {
    INDIVIDUAL("Individual"),
    ORGANIZATION("Organization"),
    ANONYMOUS("Anonymous");

    DonorType(String name) {
    }

    @JsonCreator
    public static DonorType fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return DonorType.valueOf(value.trim().toUpperCase());
    }
}
