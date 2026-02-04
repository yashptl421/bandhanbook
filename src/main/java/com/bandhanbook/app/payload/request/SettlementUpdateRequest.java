package com.bandhanbook.app.payload.request;

import com.bandhanbook.app.model.constants.SettlementStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SettlementUpdateRequest {

    private String settlementId;

    private String settlementHistoryId;

    private SettlementStatus status;
}
