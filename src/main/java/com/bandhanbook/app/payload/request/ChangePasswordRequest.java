package com.bandhanbook.app.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChangePasswordRequest {
    @NotBlank(message = "Current password must not be blank")
    private String currentPassword;
    @NotBlank(message = "New password must not be blank")
    private String newPassword;
}
