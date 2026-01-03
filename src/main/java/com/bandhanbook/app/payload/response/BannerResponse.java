package com.bandhanbook.app.payload.response;

import com.bandhanbook.app.model.Image;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BannerResponse {
    private String id;

    private String title;

    private Image image;

    private String description;

    private boolean active;

    private String createdBy;   // ref User

    private String organizationId; // ref Organization

    private LocalDateTime createdAt;
}
