package com.bandhanbook.app.model;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PricingPlans {
    private String id;
    private String name;
    private int price;
    private int maxAgents;
    private int maxUsers;
    private int registrationPeriod;
    private String period;
    private List<String> features;
}
