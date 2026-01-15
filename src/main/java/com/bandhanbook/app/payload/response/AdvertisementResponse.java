package com.bandhanbook.app.payload.response;

import com.bandhanbook.app.model.Image;
import com.bandhanbook.app.model.constants.Frequency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdvertisementResponse {

    private String id;

    private String eventId;

    private Image images;

    private Frequency frequency;

    private int durationInDays;

    private boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
