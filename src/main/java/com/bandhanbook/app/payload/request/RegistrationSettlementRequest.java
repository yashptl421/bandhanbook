package com.bandhanbook.app.payload.request;

import com.bandhanbook.app.model.constants.SettlementStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.bson.types.ObjectId;

@Data
@Builder
public class RegistrationSettlementRequest {
    private String eventId;
    private double settlementAmount;
    private String remark;
    @JsonIgnore
    private SettlementStatus status;
}
