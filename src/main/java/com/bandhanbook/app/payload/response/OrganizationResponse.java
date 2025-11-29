package com.bandhanbook.app.payload.response;

import com.bandhanbook.app.model.OrgSubscriptions;
import com.bandhanbook.app.model.Users;
import com.fasterxml.jackson.annotation.JsonInclude;
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
    private String address;
    private int country;
    private int state;
    private int city;
    private String zip;
    private String phoneVerifiedAt;
    private String phoneVerificationCode;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    private Users user_details;
    private OrgSubscriptions subscription;
}
