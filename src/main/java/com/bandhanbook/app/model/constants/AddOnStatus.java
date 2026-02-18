package com.bandhanbook.app.model.constants;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum AddOnStatus {

    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected");

    AddOnStatus(String name) {
    }

    @JsonCreator
    public static AddOnStatus fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return AddOnStatus.valueOf(value.trim().toUpperCase());
    }
}
