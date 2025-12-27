package com.bandhanbook.app.payload.request;

import com.bandhanbook.app.model.constants.GenderOptions;
import com.bandhanbook.app.model.constants.ProfileStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AgentRequest {
    @NotBlank(message = "Full name must not be blank")
    @Size(min = 3, max = 100, message = "Full name must be between 3 and 100 characters")
    private String fullName;
    @Size(max = 50)
    @Email(message = "Input must be in Email format")
    private String email;
    @Pattern(regexp = "^(\\+91[\\-\\s]?)?[0]?(91)?[6789]\\d{9}$", message = "The phone number is not in the correct format")
    @Size(min = 10, max = 11, message = "Phone number must be between 10 and 11 characters")
    private String phoneNumber;

    private String organizationId;

    @NotNull(message = "Gender is required")
    private GenderOptions gender ;

    private String caste;

    private LocalDateTime phoneVerifiedAt;

    private String phoneVerificationCode;

    private String status ;

    private String address;
    private int country = 101; // India
    private int state = 4039; // Madhya Pradesh
    private int city;
    private String zip;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}
