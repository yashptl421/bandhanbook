package com.bandhanbook.app.payload.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

public class PhoneLoginResponse {
    private String id;
    private String phoneNumber;
    private String email;
    private String fullName;
    private String role;
    private boolean isAgent;
    private Image profileImage;
    @JsonProperty("token")
    private String accessToken;
    @JsonProperty("refreshToken")
    private String refreshToken;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Image {
        private String url;
        private String id;
    }
}
