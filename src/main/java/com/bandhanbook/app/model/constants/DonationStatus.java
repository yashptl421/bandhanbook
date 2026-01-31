package com.bandhanbook.app.model.constants;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DonationStatus {

    PENDING("Pending"), RECEIVED("Received"), CANCELLED("Cancelled");

    DonationStatus(String name) {
    }

    @JsonCreator
    public static DonationStatus fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return DonationStatus.valueOf(value.trim().toUpperCase());
    }
}
