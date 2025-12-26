package com.bandhanbook.app.model.constants;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum HeightOptions {
    FEET_41("4'1\""),
    FEET_42("4'2\""),
    FEET_43("4'3\""),
    FEET_44("4'4\""),
    FEET_45("4'5\""),
    FEET_46("4'6\""),
    FEET_47("4'7\""),
    FEET_48("4'8\""),
    FEET_49("4'9\""),
    FEET_410("4'10\""),
    FEET_411("4'11\""),
    FEET_5("5 ft"),
    FEEt_51("5'1\""),
    FEET_52("5'2\""),
    FEET_53("5'3\""),
    FEET_54("5'4\""),
    FEET_55("5'5\""),
    FEET_56("5'6\""),
    FEET_57("5'7\""),
    FEET_58("5'8\""),
    FEET_59("5'9\""),
    FEET_510("5'10\""),
    FEET_511("5'11\""),
    FEET_6("6 ft"),
    FEET_61("6'1\""),
    FEET_62("6'2\""),
    FEET_63("6'3\""),
    FEET_64("6'4\""),
    FEET_65("6'5\""),
    FEET_66("6'6\""),
    FEET_67("6'7\""),
    FEET_68("6'8\""),
    FEET_69("6'9\""),
    FEET_610("6'10\""),
    FEET_611("6'11\""),
    FEET_7("7 ft"),
    FEET_71("7'1\""),
    FEET_72("7'2\""),
    FEET_73("7'3\""),
    FEET_74("7'4\""),
    FEET_75("7'5\""),
    FEET_76("7'6\"");


    HeightOptions(String name) {
    }

    @JsonCreator
    public static HeightOptions fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return HeightOptions.valueOf(value.trim().toUpperCase());
    }
}
