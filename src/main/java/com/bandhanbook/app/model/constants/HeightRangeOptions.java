package com.bandhanbook.app.model.constants;

public enum HeightRangeOptions {
    BELOW_4_5_FEET("Below 4.5 Feet"),
    BETWEEN_4_5_AND_5_FEET("Between 4.5 Feet and 5 Feet"),
    BETWEEN_5_AND_5_5_FEET("Between 5 Feet and 5.5 Feet"),
    BETWEEN_5_5_AND_6_FEET("Between 5.5 Feet and 6 Feet"),
    ABOVE_6_FEET("Above 6 Feet");

    private final String description;

    HeightRangeOptions(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
