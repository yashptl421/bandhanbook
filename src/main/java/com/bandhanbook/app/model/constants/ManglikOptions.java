package com.bandhanbook.app.model.constants;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ManglikOptions {
     YES("Yes"), NO("No");
     ManglikOptions(String name) {
     }
     @JsonCreator
     public static ManglikOptions fromValue(String value) {
          if (value == null || value.trim().isEmpty()) {
               return null;
          }
          return ManglikOptions.valueOf(value.trim().toUpperCase());
     }
}
