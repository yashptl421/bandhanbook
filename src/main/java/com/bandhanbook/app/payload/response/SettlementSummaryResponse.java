package com.bandhanbook.app.payload.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SettlementSummaryResponse {
    private double totalSettledAmount;
    private double totalRemainingAmount;
    private double totalAmount;
    private int totalSettlements;
}
