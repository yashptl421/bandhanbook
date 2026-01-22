package com.bandhanbook.app.payload.request;

import com.bandhanbook.app.model.constants.SettlementStatus;
import lombok.*;

@Data
@Builder
public class RegistrationSettlementRequest {
    private String settlementId;
    private double settlementAmount;
    private String remark;
    private SettlementStatus status;
}
