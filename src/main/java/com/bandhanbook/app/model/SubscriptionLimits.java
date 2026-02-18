package com.bandhanbook.app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class SubscriptionLimits {
    private int maxUsers;
    private int maxAgents;
    private int maxBanners;
    private int maxAdvertisements;
    private double price;
}
