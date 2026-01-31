package com.bandhanbook.app.wrappers;

import com.bandhanbook.app.payload.response.SettlementHistoryResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SettlementHistoryWrapper {

    private List<SettlementHistoryResponse> data;

    private List<RecordCount> metadata;

    @Data
    public static class RecordCount {
        private long total;
    }
}