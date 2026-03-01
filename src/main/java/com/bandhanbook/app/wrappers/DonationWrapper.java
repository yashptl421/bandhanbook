package com.bandhanbook.app.wrappers;

import com.bandhanbook.app.payload.response.DonationResponse;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DonationWrapper {
    private List<DonationResponse> data = new ArrayList<>();
    private List<Metadata> metadata = new ArrayList<>();

    public long getTotal() {
        return metadata.isEmpty() ? 0 : metadata.get(0).getTotal();
    }

    @Data
    public static class Metadata {
        private long total;
    }
}