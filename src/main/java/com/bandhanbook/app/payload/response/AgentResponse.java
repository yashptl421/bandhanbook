package com.bandhanbook.app.payload.response;

import com.bandhanbook.app.model.Address;
import com.bandhanbook.app.model.constants.ProfileStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;

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
    @JsonIgnore
    private String address;
    @JsonIgnore
    private int country;
    @JsonIgnore
    private int state ;
    @JsonIgnore
    private int city;
    @JsonIgnore
    private String zip;
    private UserDetails user_details;
    private OrganizationDetails organization_details;


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

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class OrganizationDetails {
        private String id;

        @JsonProperty("userId")
        private String user_id;
        private String organizationName;
        private String address;
        private int country;
        private int state;
        private int city;
        private String zip;
        private String status = ProfileStatus.pending.name();
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
