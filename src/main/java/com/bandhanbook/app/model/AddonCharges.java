package com.bandhanbook.app.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AddonCharges {
    private double agentCharges;
    private double candidateCharges;
    private double bannerCharges;
    private double advertisementsCharges;
}
