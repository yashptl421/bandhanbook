package com.bandhanbook.app.payload.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PricingPlanRequest {
    private String name;
    private double price;
    private String billingCycle;
    private int maxUsers;
    private int maxAgents;
    private int maxBanners;
    private int maxAdvertisements;
    private List<String> features;
}