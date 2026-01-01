package com.bandhanbook.app.model;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ImageContext {

    private static String BASE_URL;

    public ImageContext(@Value("${imagekit.url-endpoint}") String baseUrl) {
        BASE_URL = baseUrl;
    }

    public static String getBaseUrl() {
        return BASE_URL;
    }
}
