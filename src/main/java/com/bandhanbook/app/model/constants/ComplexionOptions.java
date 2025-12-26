package com.bandhanbook.app.model.constants;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ComplexionOptions {
    VERY_FAIR("Very Fair"),
    FAIR("Fair"),
    WHEATISH("Wheatish"),
    WHEATISH_FAIR("Wheatish Fair"),
    WHEATISH_DUSKY("Wheatish Dusky"),
    DUSKY("Dusky"),
    DARK("Dark");

    private final String name;

    ComplexionOptions(String name) {
        this.name = name;
    }
    @JsonCreator
    public static ComplexionOptions fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return ComplexionOptions.valueOf(value.trim().toUpperCase());
    }
}
