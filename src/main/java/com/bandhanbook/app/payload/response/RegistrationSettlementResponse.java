package com.bandhanbook.app.payload.response;

import com.bandhanbook.app.model.constants.SettlementStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationSettlementResponse {
    private String id;
    private String agentId;

    private String eventId;

    private String organizationId;

    private double registrationFee;

    private int registrations;

    private double totalAmount;

    private double totalRemainingAmount;

    private double totalSettledAmount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<History> settlementHistory;

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class History {
        private String id;
        private double remainingAmount;

        private double settledAmount;

        private String remark;

        private SettlementStatus status;

        private LocalDateTime settlementAt = null;

        private LocalDateTime createdAt;
    }
}
