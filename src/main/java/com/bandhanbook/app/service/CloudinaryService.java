package com.bandhanbook.app.service;

import com.bandhanbook.app.model.Image;
import com.cloudinary.Cloudinary;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService{/* implements ImageUploadService {

    private final Cloudinary cloudinary;

    @Override
    public Mono<Image> upload(byte[] image, String folder, String filename) {
        return Mono.fromCallable(() -> {
            Map<?, ?> result = cloudinary.uploader().upload(
                    image,
                    Map.of(
                            "folder", folder,
                            "public_id", filename,
                            "resource_type", "image"
                    )
            );

            return new Image(
                    result.get("public_id").toString(),
                    result.get("secure_url").toString()
            );
        });
    }

    @Override
    public Mono<Image> upload(FilePart file, String fileName, String folder) {
        return null;
    }

    @Override
    public Mono<Void> delete(String fileId) {
        return Mono.fromRunnable(() ->
                {
                    try {
                        cloudinary.uploader().destroy(fileId, Map.of());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    @Override
    public String getFullImageUrl(Image image) {
        return "https://cdn.example.com" + image.getUrl();
    }*/
}
