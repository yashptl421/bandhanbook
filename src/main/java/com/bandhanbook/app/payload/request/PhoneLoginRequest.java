package com.bandhanbook.app.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PhoneLoginRequest {
    @NotBlank(message = "Phone Number must not be empty")
    private String phoneNumber;
    private String role;
    private String password;
    private String otp;
}
