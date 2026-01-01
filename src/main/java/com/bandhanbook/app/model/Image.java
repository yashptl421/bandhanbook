package com.bandhanbook.app.model;

import com.bandhanbook.app.utilities.UtilityHelper;
import com.fasterxml.jackson.annotation.JsonGetter;
import lombok.*;


@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode
@Builder
public class Image {
    private String url;
    private String id;
    @JsonGetter("fullUrl")
    public String getFullUrl() {
        return ImageContext.getBaseUrl()+url;
    }
}