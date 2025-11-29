package com.bandhanbook.app.payload.response;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class UserRegisterResponse {
    @Size(min = 10, max = 500)
    private String message;
}
