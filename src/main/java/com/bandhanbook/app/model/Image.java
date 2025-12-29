package com.bandhanbook.app.model;

import lombok.*;


@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode
@Builder
public class Image {
    private String url;
    private String id; // cloudinary unique ID for the image
}