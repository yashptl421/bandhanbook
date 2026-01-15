package com.bandhanbook.app.payload.request;

import com.bandhanbook.app.model.constants.Frequency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdvertisementUpdateRequest {
    private String id;
    private Frequency frequency;
    private boolean active;
}
