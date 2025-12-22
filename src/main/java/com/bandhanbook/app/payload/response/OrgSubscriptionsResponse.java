package com.bandhanbook.app.payload.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrgSubscriptionsResponse {
    private String id;

    private String orgId;
    private String planId;
    private String registrationPeriod;
    private String startDate;
    private String endDate;
    private boolean active;
    private int maxAgents;
    private int maxUsers;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt = null;
}
