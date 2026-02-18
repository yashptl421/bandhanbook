package com.bandhanbook.app.payload.response;

import com.bandhanbook.app.model.constants.AddOnStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SubscriptionAddonResponse {
    private String id;
    private String orgId;
    private String subscriptionId;
    private int maxAgents;
    private int maxUsers;
    private int maxBanners;
    private int maxAdvertisements;
    private AddOnStatus status;
    private double price;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
