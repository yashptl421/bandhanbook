package com.bandhanbook.app.wrappers;

import com.bandhanbook.app.payload.response.BannerResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BannerWrapper {

    private List<BannerResponse> data;


    private List<RecordCount> total = new ArrayList<>();
    private List<RecordCount> activeCount = new ArrayList<>();
    private List<RecordCount> inactiveCount = new ArrayList<>();

    @Data
    public static class RecordCount {
        private long count;
    }
}
