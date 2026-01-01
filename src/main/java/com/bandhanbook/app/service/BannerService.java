package com.bandhanbook.app.service;

import com.bandhanbook.app.exception.ValidationExceptions;
import com.bandhanbook.app.model.Banners;
import com.bandhanbook.app.model.Organization;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.model.constants.RoleNames;
import com.bandhanbook.app.payload.request.BannerRequest;
import com.bandhanbook.app.repository.BannerRepository;
import com.bandhanbook.app.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class BannerService {
    private final BannerRepository bannerRepository;
    private final OrganizationRepository organizationRepository;
    private final ImageUploadService imageUploadService;

    public Mono<Banners> createBanner(BannerRequest request, FilePart file,Users authUser) {

        // ---------- 1. Validate image ----------
        if (file == null || file.headers().getContentType() == null ||
                !file.headers().getContentType().toString().startsWith("image/")) {
            return Mono.error(new ValidationExceptions("Banner image is required"));
        }

        // ---------- 2. Resolve Organization ID ----------
        Mono<ObjectId> orgIdMono;

        if (request.getOrganizationId() != null &&
                authUser.getRoles().contains(RoleNames.SuperUser.name())) {

            orgIdMono = Mono.just(new ObjectId(request.getOrganizationId()));
        } else {
            // fallback → authUser organization
            orgIdMono = organizationRepository.findByUserId(authUser.getId())
                    .map(Organization::getId);
        }

        // ---------- 3. Upload image + save banner ----------
        return orgIdMono.flatMap(orgId -> {

            String filename = orgId + "_" + System.currentTimeMillis();
            String folder = "/uploads/banners";

            return imageUploadService.upload(file, filename, folder)
                    .flatMap(image -> {

                        Banners banner = Banners.builder()
                                .title(request.getTitle())
                                .description(request.getDescription())
                                .active(Boolean.TRUE.equals(request.getIsActive()))
                                .image(image)
                                .organizationId(orgId)
                                .createdBy(authUser.getId())
                                .build();

                        return bannerRepository.save(banner);
                    });
        });
    }
}
