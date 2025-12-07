package com.bandhanbook.app.payload.response;

import com.bandhanbook.app.model.Address;
import com.bandhanbook.app.model.Organization;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentResponse {

    private String id;
    @JsonProperty("userId")
    private String user_id;
    @JsonProperty("organizationId")
    private String organization_id;
    private String gender;
    private String status;
    private Image profileImage;
    @JsonProperty("address")
    private Address localAddress;
    private UserDetails user_details;
    private Organization organization_details;


    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class UserDetails {
        private String id;
        @JsonProperty("phoneNumber")
        private String phone_number;
        @JsonProperty("fullName")
        private String full_name;
        private String email;
        private String role;
    }

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
