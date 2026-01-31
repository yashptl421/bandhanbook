package com.bandhanbook.app.payload.response;

import com.bandhanbook.app.model.constants.SettlementStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SettlementHistoryResponse {
    private String batchId;
    private String settlementId;
    private String settlementHistoryId;
    private String agentId;
    private String eventId;

    private double totalAmount;
    private double remainingAmount;
    private double settledAmount;

    private String remark;
    private SettlementStatus status;
    private LocalDateTime createdAt;
}
