package com.bandhanbook.app.service;

import com.bandhanbook.app.exception.RecordNotFoundException;
import com.bandhanbook.app.exception.UnAuthorizedException;
import com.bandhanbook.app.exception.ValidationExceptions;
import com.bandhanbook.app.model.Image;
import io.imagekit.sdk.ImageKit;
import io.imagekit.sdk.exceptions.*;
import io.imagekit.sdk.models.FileCreateRequest;
import io.imagekit.sdk.models.results.Result;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static com.bandhanbook.app.utilities.ErrorResponseMessages.*;

@Primary
@Service
@RequiredArgsConstructor
public class ImageKitService implements ImageUploadService {
    private static final Logger logger = LoggerFactory.getLogger(ImageKitService.class);
    private final ImageKit imageKit;
    private final Tika tika = new Tika();

    @Value("${images.max_size_mb}")
    private int maxImageSizeMb;

    @Value("${imagekit.url-endpoint}")
    private  String urlEndpoint;


    @Override
    public Mono<Image> upload(byte[] image, String folder, String filename) {
        return Mono.fromCallable(() -> {
            FileCreateRequest req = new FileCreateRequest(image, filename);
            req.setFolder(folder);
            Result result = imageKit.upload(req);
            return Image.builder().id(result.getFileId()).url(result.getFilePath()).build();
        });
    }

    @Override
    public Mono<Void> delete(String fileId) {
        return Mono.fromRunnable(() -> {
            try {
                imageKit.deleteFile(fileId);
            } catch (ForbiddenException | UnauthorizedException e) {
                Mono.error(new UnAuthorizedException(UNAUTHORIZED_ACCESS, e));
            } catch (TooManyRequestsException | InternalServerException | BadRequestException | UnknownException e) {
                Mono.error(new ValidationExceptions(FILE_UPLOAD_ERROR, e));
            }
        });
    }

    @Override
    public Mono<Void> bulkDelete(List<String> fileIds) {
        return Mono.fromRunnable(() -> {
            try {
                imageKit.bulkDeleteFiles(fileIds);
            } catch (ForbiddenException | UnauthorizedException e) {
                Mono.error(new UnAuthorizedException(UNAUTHORIZED_ACCESS, e));
            } catch (TooManyRequestsException | InternalServerException | BadRequestException | UnknownException |
                     PartialSuccessException | NotFoundException e) {
                Mono.error(new ValidationExceptions(FILE_UPLOAD_ERROR, e));
            }
        });
    }

/*    public Mono<Image> upload(FilePart file, String fileName, String folder) {
        return DataBufferUtils.join(file.content())
                .map(buffer -> {
                    // upload buffer to ImageKit / S3
                    return Image.builder()
                            .id(UUID.randomUUID().toString())
                            .url(folder + "/" + fileName)
                            .build();
                });
    }*/

    public Mono<Image> upload(FilePart file, String filename, String folder) {

        return DataBufferUtils.join(file.content())
                .flatMap(buffer -> {
                    byte[] bytes = new byte[buffer.readableByteCount()];
                    buffer.read(bytes);
                    DataBufferUtils.release(buffer);

                    // validations
                    validateSize(bytes);
                    validateMime(bytes);

                    // resize + compress
                    byte[] processed = resizeAndCompress(bytes);

                    return uploadToImageKit(processed, folder, filename);
                });
    }

    @Override
    public String getFullImageUrl(Image image) {
        return urlEndpoint + image.getUrl();
    }
    public  String getFullImageUrl(String url) {
        return urlEndpoint + url;
    }

    private Mono<Image> uploadToImageKit(byte[] image, String folder, String filename) {
        return Mono.fromCallable(() -> {
                    FileCreateRequest req = new FileCreateRequest(image, filename);
                    req.setUseUniqueFileName(true);
                    req.setFolder(folder);

                    Result result = imageKit.upload(req);
                    if (result == null || result.getFileId() == null) {
                        throw new RuntimeException("Image upload failed");
                    }
                    return Image.builder()
                            .id(result.getFileId())
                            .url(result.getFilePath())
                            .build();
                }).subscribeOn(Schedulers.boundedElastic())
                .doOnError(ex -> logger.error("ImageKit upload failed for file {}", filename, ex));
    }

    private void validateSize(byte[] bytes) {
        if (bytes.length > maxImageSizeMb) {
            throw new ValidationExceptions(IMAGE_SIZE_EXCEEDED);
        }
    }

    private void validateMime(byte[] bytes) {
        String mime = tika.detect(bytes);
        if (!mime.startsWith("image/")) {
            throw new ValidationExceptions(INVALID_FILE_TYPE);
        }
    }

    private Mono<DataBuffer> validateSize(FilePart file) {
        return DataBufferUtils.join(file.content())
                .flatMap(buffer -> {
                    if (buffer.readableByteCount() > maxImageSizeMb) {
                        return Mono.error(new RecordNotFoundException("File too large"));
                    }
                    return Mono.just(buffer);
                });
    }

    private byte[] resizeAndCompress(byte[] original) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Thumbnails.of(new ByteArrayInputStream(original))
                    .size(600, 600)
                    .outputFormat("jpg")
                    .outputQuality(0.8)
                    .toOutputStream(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ValidationExceptions("Image processing failed");
        }
    }
}