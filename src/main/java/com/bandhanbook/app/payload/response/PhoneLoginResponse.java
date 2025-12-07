package com.bandhanbook.app.payload.response;

import com.bandhanbook.app.model.EventParticipants;
import com.bandhanbook.app.model.MatrimonyCandidate;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
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
    private MatrimonyCandidate matrimony_data;
    private EventParticipants eventParticipants;
    private AgentResponse agent_details;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Address {
        private String address;
        private int country = 101; // India
        private int state = 4039; // Madhya Pradesh
        private int city;
        private String zip;

    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Image {
        private String url;
        private String id;
    }
}
