package com.bandhanbook.app.model.constants;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum FamilyStatus {
    LOWER_CLASS("Lower Class"), MIDDLE_CLASS("Middle Class"), UPPER_MIDDLE_CLASS("Upper Middle Class"), AFFLUENT("Affluent"), RICH("Rich");

    FamilyStatus(String name) {
    }


    @JsonCreator
    public static FamilyStatus fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return FamilyStatus.valueOf(value.trim().toUpperCase());
    }
}
