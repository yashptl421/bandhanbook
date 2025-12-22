package com.bandhanbook.app.payload.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bson.types.ObjectId;

import java.util.Date;

@Setter
@Getter
@RequiredArgsConstructor
public class UserRegisterRequest {

    @Pattern(regexp = "^(\\+91[\\-\\s]?)?[0]?(91)?[6789]\\d{9}$", message = "The phone number is not in the correct format")
    @Size(min = 10, max = 11, message = "Phone number must be between 10 and 11 characters")
    private String phoneNumber;

    @NotBlank(message = "Full name must not be blank")
    @Size(min = 3, max = 100, message = "Full name must be between 3 and 100 characters")
    private String fullName;

    @NotBlank(message = "Gender must not be blank")
    private String gender;

    private Date dob;

    @Size(max = 50)
    @Email(message = "Input must be in Email format")
    private String email;
    private String password;
    private String address;
    private int country;
    private int state;
    private int city;
    private String zip;

    private String eventId;
    private String otp;
}
