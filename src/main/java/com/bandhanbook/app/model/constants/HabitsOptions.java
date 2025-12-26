package com.bandhanbook.app.model.constants;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum HabitsOptions {
    YES("Yes"), NO("No"), OCCASIONALLY("Occasionally");

    HabitsOptions(String name) {
    }
    @JsonCreator
    public static HeightOptions fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return HeightOptions.valueOf(value.trim().toUpperCase());
    }
}
