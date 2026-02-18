package com.bandhanbook.app.payload.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BuySubscriptionRequest {
    private String orgId;
    private String planId;
    private String eventId;
    private LocalDateTime planStartDate;
}
