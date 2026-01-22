package com.bandhanbook.app.model.constants;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum SettlementStatus {
    PENDING("Pending"), ACCEPTED("Accepted"), REJECTED("Rejected");

    SettlementStatus(String name) {
    }

    @JsonCreator
    public static SettlementStatus fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return SettlementStatus.valueOf(value.trim().toUpperCase());
    }
}
