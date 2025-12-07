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
    private boolean isAgent=false;
    private Image profileImage;
    @JsonProperty("token")
    private String accessToken;
    @JsonProperty("refreshToken")
    private String refreshToken;
    private MatrimonyCandidate matrimony_data;
    private EventParticipants eventParticipants;
    private AgentResponse agent_details;
    private OrganizationResponse organization_details;


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
