package com.bandhanbook.app.payload.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@RequiredArgsConstructor
public class OrganizationRequest {

    @NotBlank(message = "Full name must not be blank")
    @Size(min = 3, max = 100, message = "Full name must be between 3 and 100 characters")
    private String organizationName;
    @Pattern(regexp = "^(\\+91[\\-\\s]?)?[0]?(91)?[6789]\\d{9}$", message = "The phone number is not in the correct format")
    @Size(min = 10, max = 11, message = "Phone number must be between 10 and 11 characters")
    private String phoneNumber;

    @NotBlank(message = "Full name must not be blank")
    @Size(min = 3, max = 100, message = "Full name must be between 3 and 100 characters")
    private String fullName;

    @Size(max = 50)
    @Email(message = "Input must be in Email format")
    private String email;

    private String address;
    private int country;
    private int state;
    private int city;
    private String zip;

    private String planId;

    private String status;

    @Future(message = "The date must be in the future.")
    private LocalDateTime planStartDate;

}
