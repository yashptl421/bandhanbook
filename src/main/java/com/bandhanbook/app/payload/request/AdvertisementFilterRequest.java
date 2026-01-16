package com.bandhanbook.app.payload.request;

import com.bandhanbook.app.model.constants.Frequency;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdvertisementFilterRequest {
    private Boolean isActive;
    private List<String> frequencies;
    private int page = 1;
    private int limit = 10;

}
