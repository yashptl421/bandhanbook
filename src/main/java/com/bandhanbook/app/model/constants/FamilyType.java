package com.bandhanbook.app.model.constants;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum FamilyType {
    NUCLEAR("Nuclear"), JOINT("Joint"), EXTENDED("Extended");

    FamilyType(String name) {
    }

    @JsonCreator
    public static FamilyType fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return FamilyType.valueOf(value.trim().toUpperCase());
    }
}
