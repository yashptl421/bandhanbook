package com.bandhanbook.app.payload.request;

import com.bandhanbook.app.model.constants.DonorType;
import com.bandhanbook.app.model.constants.PaymentMode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DonationCreateRequest {
    @NotNull
    private String eventId;
    @NotNull
    private Double amount;
    private String donorName;
    private String address;
    private String email;
    private String phoneNumber;
    private DonorType donorType;
    private String remark;
    @NotNull
    private PaymentMode paymentMode;
}