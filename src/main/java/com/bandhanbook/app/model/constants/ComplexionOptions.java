package com.bandhanbook.app.model.constants;

public enum ComplexionOptions {
    VERY_FAIR("Very Fair"),
    FAIR("Fair"),
    WHEATISH("Wheatish"),
    WHEATISH_FAIR("Wheatish Fair"),
    WHEATISH_DUSKY("Wheatish Dusky"),
    DUSKY("Dusky"),
    DARK("Dark");

    private final String name;

    ComplexionOptions(String name) {
        this.name = name;
    }
}
