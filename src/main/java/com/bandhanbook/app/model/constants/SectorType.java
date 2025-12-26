package com.bandhanbook.app.model.constants;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum SectorType {
    GOVERNMENT("Government"), PRIVATE("Private"), PUBLIC("Public"), BUSINESS("Business"), SELF_EMPLOYED("Self Employed"), OTHER("Other");

    SectorType(String name) {
    }

    @JsonCreator
    public static SectorType fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return SectorType.valueOf(value.trim().toUpperCase());
    }
}
