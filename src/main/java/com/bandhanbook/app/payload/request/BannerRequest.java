package com.bandhanbook.app.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BannerRequest {

    @NotBlank
    private String title;

    private String description;

    private Boolean isActive;

    private String organizationId; // optional (SUPERUSER only)
}
