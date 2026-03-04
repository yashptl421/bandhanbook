package com.bandhanbook.app.wrappers;

import com.bandhanbook.app.payload.response.EventDbResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventWrapper {
    private List<EventDbResponse> data = new ArrayList<>();

    private List<RecordCount> totalRecords = new ArrayList<>();
    private List<RecordCount> activeCount = new ArrayList<>();

    @Data
    public static class RecordCount {
        private long total;
    }
}
