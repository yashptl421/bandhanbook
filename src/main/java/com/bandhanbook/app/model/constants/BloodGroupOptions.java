package com.bandhanbook.app.model.constants;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum BloodGroupOptions {
    A_POSITIVE("A+"),
    A_NEGATIVE("A-"),
    B_POSITIVE("B+"),
    B_NEGATIVE("B-"),
    AB_POSITIVE("AB+"),
    AB_NEGATIVE("AB-"),
    O_POSITIVE("O+"),
    O_NEGATIVE("O-");

    BloodGroupOptions(String name) {
    }
    @JsonCreator
    public static BloodGroupOptions fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return BloodGroupOptions.valueOf(value.trim().toUpperCase());
    }
}
