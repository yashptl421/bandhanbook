package com.bandhanbook.app.model.constants;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum FamilyValues {
    TRADITIONAL("Traditional"), MODERATE("Moderate"), LIBERAL("Liberal");

    FamilyValues(String name) {
    }
    @JsonCreator
    public static FamilyValues fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return FamilyValues.valueOf(value.trim().toUpperCase());
    }
}
