package com.bandhanbook.app.wrappers;

import com.bandhanbook.app.payload.response.DonationResponse;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DonationWrapper {
    private List<DonationResponse> data = new ArrayList<>();
    private long total;
}
