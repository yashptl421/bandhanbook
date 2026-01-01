package com.bandhanbook.app.payload.response;

import com.bandhanbook.app.model.Address;
import com.bandhanbook.app.model.Image;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EventDbResponse {
    private String id;

    private String name;
    @JsonProperty("createdBy")
    private String created_by;
    @JsonProperty("organizationId")
    private String organization_id;
    private String location;

    @JsonProperty("startDate")
    private LocalDateTime start_date;
    @JsonProperty("endDate")
    private LocalDateTime end_date;
    @JsonProperty("createdAt")
    private LocalDateTime created_at;
    private OrganizationResponse organization_details;
    private UserResponse created_by_details;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    private static class OrganizationResponse {
        private String id;
        @JsonProperty("userId")
        private String user_id;
        private String organizationName;
        @JsonProperty("profileImage")
        private Image profile_image;
        @JsonProperty("address")
        private Address localAddress;
        @JsonIgnore
        private String address;
        @JsonIgnore
        private int country;
        @JsonIgnore
        private int state;
        @JsonIgnore
        private int city;
        @JsonIgnore
        private String zip;
        private String status;
        @JsonProperty("createdAt")
        private LocalDateTime created_at;
    }

    private static class UserResponse {
        @JsonIgnore
        private String id;
        @JsonProperty("fullName")
        private String full_name;
        private String email;
        @JsonProperty("phoneNumber")
        private String phone_number;
        private List<String> role;
    }
}
