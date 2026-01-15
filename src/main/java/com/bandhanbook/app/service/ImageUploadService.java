package com.bandhanbook.app.service;

import com.bandhanbook.app.model.Image;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ImageUploadService {
    Mono<Image> upload(FilePart file, String fileName, String folder);

    Mono<Void> delete(String fileId);

    Mono<Void> bulkDelete(List<String> fileIds);

    String getFullImageUrl(Image image);
    Mono<Image> upload(byte[] image, String folder, String filename);
}
