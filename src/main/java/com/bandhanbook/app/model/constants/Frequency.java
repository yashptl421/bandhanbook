package com.bandhanbook.app.model.constants;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Frequency {
    HIGH("High"), MEDIUM("Medium"), LOW("Low");

    Frequency(String name) {
    }

    @JsonCreator
    public static Frequency fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return Frequency.valueOf(value.trim().toUpperCase());
    }
}
