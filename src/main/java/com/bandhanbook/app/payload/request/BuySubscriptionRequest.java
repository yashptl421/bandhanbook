package com.bandhanbook.app.payload.request;

import lombok.Data;

@Data
public class BuySubscriptionRequest {
    private String orgId;
    private String planId;
    private String planStartDate;
}
