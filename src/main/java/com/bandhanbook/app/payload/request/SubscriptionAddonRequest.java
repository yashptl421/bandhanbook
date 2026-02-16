package com.bandhanbook.app.payload.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionAddonRequest {
    private String subscriptionId;
    private int maxAgents;
    private int maxUsers;
    private int maxBanners;
    private int maxAdvertisements;
    private double price;
}
