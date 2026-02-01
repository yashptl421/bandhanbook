package com.bandhanbook.app.payload.response;

import com.bandhanbook.app.model.constants.DonationStatus;
import com.bandhanbook.app.model.constants.DonorType;
import com.bandhanbook.app.model.constants.EventType;
import com.bandhanbook.app.model.constants.PaymentMode;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DonationResponse {

    private String id;
    private String agentId;
    private String organizationId;
    private String submittedTo;
    private String eventId;
    private EventType eventType;
    private double amount;
    private String donorName;
    private String address;
    private String email;
    private String phoneNumber;
    private DonorType donorType;
    private DonationStatus status;
    private String remark;
    private PaymentMode paymentMode;
    private LocalDateTime createdAt;
}
