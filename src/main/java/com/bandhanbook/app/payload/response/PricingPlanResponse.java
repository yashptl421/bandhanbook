package com.bandhanbook.app.payload.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PricingPlanResponse {
    private String id;
    private String name;
    private double price;
    private String billingCycle;
    private int maxUsers;
    private int maxAgents;
    private int maxBanners;
    private int maxAdvertisements;
    private List<String> features;
    private boolean active;
}
