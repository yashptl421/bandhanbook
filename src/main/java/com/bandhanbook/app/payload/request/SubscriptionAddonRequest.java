package com.bandhanbook.app.payload.request;

import com.bandhanbook.app.model.SubscriptionLimits;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionAddonRequest {
    private String subscriptionId;
    SubscriptionLimits limits;
    private double price;
}
