package com.bandhanbook.app.model.constants;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum EventType {
    CANDIDATE_REGISTRATION("Candidate_Registration"),
    DONATION("Donation");

    EventType(String name) {
    }

    @JsonCreator
    public static EventType fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return EventType.valueOf(value.trim().toUpperCase());
    }
}
