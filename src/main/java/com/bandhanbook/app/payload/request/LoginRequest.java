package com.bandhanbook.app.payload.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class LoginRequest {
    @NotBlank(message = "email must not be blank")
    private String email;
    @NotBlank(message = "Password must not be blank")
    private String password;
    private String role;
}