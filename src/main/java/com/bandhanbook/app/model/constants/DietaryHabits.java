package com.bandhanbook.app.model.constants;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DietaryHabits {
    VEGETARIAN("Vegetarian"), NON_VEGETARIAN("Non Vegetarian"), EGGETARIAN("Eggetarian");


    private final String name;

    DietaryHabits(String name) {
        this.name = name;
    }
    @JsonCreator
    public static DietaryHabits fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return DietaryHabits.valueOf(value.trim().toUpperCase());
    }
}
