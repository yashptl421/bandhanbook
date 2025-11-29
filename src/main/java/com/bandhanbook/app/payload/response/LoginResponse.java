package com.bandhanbook.app.payload.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    @JsonProperty("token")
    private String accessToken;
    @JsonProperty("refreshToken")
    private String refreshToken;
    private String id;
    private String phoneNumber;
    @JsonProperty("name")
    private String fullName;
    private String email;
    private String role;
}
