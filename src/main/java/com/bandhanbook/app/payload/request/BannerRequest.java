package com.bandhanbook.app.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BannerRequest {

    private String title;

    private String description;

    private Boolean isActive;

    private String organizationId; // optional (SUPERUSER only)
}
