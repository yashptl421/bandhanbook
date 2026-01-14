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
public class AdvertisementRequest {

    private String eventId;
    private int durationInDays;
    private List<Frequency> Frequency;
    private List<Boolean> active;
}
