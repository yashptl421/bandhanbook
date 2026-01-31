package com.bandhanbook.app.payload.request;

import com.bandhanbook.app.model.constants.DonationStatus;
import lombok.Data;

@Data
public class DonationUpdateRequest {
    private Double amount;
    private String donorName;
    private String remark;
    private DonationStatus status;
}
