package com.bandhanbook.app.payload.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@RequiredArgsConstructor
public class VerifyOtpRequest {
    private String phoneNumber;
    private String otp;
    private String role;
}
