package com.bandhanbook.app.model.constants;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum HeightRangeOptions {
    BELOW_4_5_FEET("Below 4.5 Feet"),
    BETWEEN_4_5_AND_5_FEET("Between 4.5 Feet and 5 Feet"),
    BETWEEN_5_AND_5_5_FEET("Between 5 Feet and 5.5 Feet"),
    BETWEEN_5_5_AND_6_FEET("Between 5.5 Feet and 6 Feet"),
    ABOVE_6_FEET("Above 6 Feet");
    HeightRangeOptions(String name) {
    }
    @JsonCreator
    public static HeightRangeOptions fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return HeightRangeOptions.valueOf(value.trim().toUpperCase());
    }
}
