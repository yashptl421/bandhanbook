package com.bandhanbook.app.payload.response;

import com.bandhanbook.app.model.PricingPlans;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SubscriptionResponse {
    private String id;

    private String orgId;

    private String eventId;

    private String eventName;

    private String planName;

    private String planId;

    private int planPrice;

    private String registrationPeriod;

    private String startDate;

    private String endDate;

    private boolean active;

    private int maxAgents;

    private int maxUsers;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private OrganizationResponse organizationDetails;
}
