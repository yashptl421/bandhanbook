package com.bandhanbook.app.payload.response;

import com.bandhanbook.app.model.OrgSubscriptions;
import com.bandhanbook.app.model.PricingPlans;
import com.bandhanbook.app.model.SubscriptionLimits;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Field;

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
    private SubscriptionLimits limits;
    private String planId;

    private double planPrice;

    private String registrationPeriod;

    private String startDate;

    private String endDate;

    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private OrganizationResponse organizationDetails;
}
