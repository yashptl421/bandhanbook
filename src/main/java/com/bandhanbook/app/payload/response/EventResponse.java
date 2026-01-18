package com.bandhanbook.app.payload.response;

import com.bandhanbook.app.model.Organization;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.model.constants.Status;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EventResponse {

    private String id;

    private String name;
    private String createdBy;
    private String organizationId;
    private String location;
    private Status status;
    private double registrationFee;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private OrganizationResponse organization_details;
    private UserResponse created_by_details;
}
