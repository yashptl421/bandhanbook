package com.bandhanbook.app.payload.response;

import com.bandhanbook.app.model.Address;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrganizationResponse {

    private String id;
    private String userId;
    private String organizationName;
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
    private String phoneVerifiedAt;
    private String phoneVerificationCode;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    private UserResponse user_details;
    private OrgSubscriptionsResponse subscription;
}
