package com.bandhanbook.app.wrappers;

import com.bandhanbook.app.payload.response.AgentResponse;
import com.bandhanbook.app.payload.response.CandidateResponse;
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
public class CandidateWrapper {

    private List<CandidateResponse> data = new ArrayList<>();

    private List<RecordCount> metadata = new ArrayList<>();

    @Data
    public static class RecordCount {
        private long total;
    }
}
