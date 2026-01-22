package com.bandhanbook.app.payload.request;

import com.bandhanbook.app.model.constants.SettlementStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
public class SettlementUpdateRequest {

    private String settlementId;

    private History settlementHistory;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class History {

        private String id;

        private double remainingAmount;

        private double settledAmount;

        private String remark;

        private SettlementStatus status;

        private LocalDateTime settlementAt = LocalDateTime.now();

    }
}
