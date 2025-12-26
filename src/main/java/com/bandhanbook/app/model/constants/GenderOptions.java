package com.bandhanbook.app.model.constants;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum GenderOptions {
    MALE("Male"), FEMALE("Female");

    private final String name;

    GenderOptions(String name) {
        this.name = name;
    }

    @JsonCreator
    public static GenderOptions fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return GenderOptions.valueOf(value.trim().toUpperCase());
    }
}
