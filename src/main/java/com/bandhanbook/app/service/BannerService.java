package com.bandhanbook.app.service;

import com.bandhanbook.app.exception.RecordNotFoundException;
import com.bandhanbook.app.exception.ValidationExceptions;
import com.bandhanbook.app.model.Agents;
import com.bandhanbook.app.model.Banners;
import com.bandhanbook.app.model.Organization;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.model.constants.RoleNames;
import com.bandhanbook.app.payload.request.BannerRequest;
import com.bandhanbook.app.payload.response.BannerResponse;
import com.bandhanbook.app.payload.response.base.ApiResponse;
import com.bandhanbook.app.repository.AgentRepository;
import com.bandhanbook.app.repository.BannerRepository;
import com.bandhanbook.app.repository.OrganizationRepository;
import com.bandhanbook.app.wrappers.BannerWrapper;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

import static com.bandhanbook.app.utilities.ErrorResponseMessages.DATA_NOT_FOUND;
import static com.bandhanbook.app.utilities.ErrorResponseMessages.RECORD_NOT_FOUND;
import static com.bandhanbook.app.utilities.SuccessResponseMessages.BANNER_DELETED;

@Service
@RequiredArgsConstructor
public class BannerService {
    private final BannerRepository bannerRepository;
    private final OrganizationRepository organizationRepository;
    private final ImageUploadService imageUploadService;
    private final ModelMapper modelMapper;
    private final ReactiveMongoTemplate mongoTemplate;
    private final AgentRepository agentRepository;
    private final AuthService authService;
    private final ProfileService profileService;

    @Value("${images.base.path}")
    private String basePath;
    @Value("${images.banner.path}")
    private String bannerPath;
    @Value("${images.organization.path}")
    private String organizationImagePath;

    public Mono<BannerResponse> createBanner(BannerRequest request, FilePart file, Users authUser) {

        if (file == null || file.headers().getContentType() == null ||
                !Objects.requireNonNull(file.headers().getContentType()).toString().startsWith("image/")) {
            return Mono.error(new ValidationExceptions("Banner image is required"));
        }

        Mono<ObjectId> orgIdMono;

        if (request.getOrganizationId() != null && authUser.isSuperUser()) {
            orgIdMono = Mono.just(new ObjectId(request.getOrganizationId()));
        } else if (authUser.isAgent()) {
            orgIdMono = resolveAgentOrgId(authUser);
        } else {
            orgIdMono = resolveOrgId(authUser);
        }
        return orgIdMono.flatMap(orgId -> {
            String filename = orgId + "_" + System.currentTimeMillis();
            String folder = basePath + orgId + bannerPath;
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
                        return bannerRepository.save(banner).thenReturn(modelMapper.map(banner, BannerResponse.class));
                    });
        });
    }

    public Mono<BannerWrapper> listBanners(
            Users authUser,
            int page,
            int limit
    ) {
        int skip = (page - 1) * limit;

        Criteria criteria = new Criteria();
        if (authUser.isAgent() || authUser.isCandidate()) {
            criteria.and("is_active").is(true);

            if (authUser.isAgent()) {
                return resolveAgentOrgId(authUser)
                        .flatMap(orgId ->
                                executeBannerQuery(criteria.and("organization_id").is(orgId),
                                        page, limit, skip)
                        );
            }

            if (authUser.isCandidate()) {
                return authService.getMatrimonyDetails(RoleNames.Candidate.name(), authUser)
                        .flatMap(res -> res.getEventParticipants().stream().findFirst()
                                .map(response -> {
                                            return executeBannerQuery(criteria.and("organization_id").in(new ObjectId(response.getOrganizationId())),
                                                    page, limit, skip);
                                        }
                                ).orElseThrow(() -> new RecordNotFoundException(RECORD_NOT_FOUND)));
            }
        }

        if (authUser.isOrganization()) {
            return resolveOrgId(authUser).flatMap(orgIds ->
                    executeBannerQuery(criteria.and("organization_id").in(orgIds),
                            page, limit, skip));
        }

        return executeBannerQuery(criteria, page, limit, skip);
    }

    private Mono<BannerWrapper> executeBannerQuery(
            Criteria criteria,
            int page,
            int limit,
            int skip
    ) {

        Query query = new Query(criteria)
                .with(Sort.by(Sort.Direction.DESC, "created_at"))
                .skip(skip)
                .limit(limit);

        Mono<List<BannerResponse>> dataMono = mongoTemplate.find(query, Banners.class)
                .map(banner -> modelMapper.map(banner, BannerResponse.class))
                .collectList();

        Mono<Long> totalMono = mongoTemplate.count(new Query(criteria), Banners.class);

        Mono<Long> activeCountMono = mongoTemplate.count(
                new Query(criteria.and("is_active").is(true)),
                Banners.class
        );

        return Mono.zip(dataMono, totalMono, activeCountMono)
                .map(tuple -> {
                    long total = tuple.getT2();
                    return BannerWrapper.builder()
                            .data(tuple.getT1())
                            .activeCount(tuple.getT3())
                            .inactiveCount(tuple.getT2() - tuple.getT3())
                            .meta(ApiResponse.Meta.builder()
                                    .page(page)
                                    .limit(limit)
                                    .totalRecords(total)
                                    .totalPages((int) Math.ceil((double) total / limit))
                                    .build())
                            .build();
                });
    }

    public Mono<BannerResponse> updateBanner(String bannerId, Boolean isActive) {
        ObjectId id;
        try {
            id = new ObjectId(bannerId);
        } catch (Exception e) {
            return Mono.error(new IllegalArgumentException("Invalid banner id"));
        }
        return bannerRepository.findById(id)
                .switchIfEmpty(Mono.error(new RecordNotFoundException("Banner not found")))
                .flatMap(banner -> {
                    banner.setActive(isActive);
                    return bannerRepository.save(banner).thenReturn(modelMapper.map(banner, BannerResponse.class));
                });
    }

    public Mono<BannerResponse> showBanner(String id) {

        ObjectId bannerId;
        try {
            bannerId = new ObjectId(id);
        } catch (Exception e) {
            return Mono.error(new RecordNotFoundException("Invalid banner id"));
        }
        return bannerRepository.findById(bannerId)
                .switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND)))
                .map(banner -> modelMapper.map(banner, BannerResponse.class));
    }

    public Mono<String> deleteBanner(String id) {

        ObjectId bannerId;
        try {
            bannerId = new ObjectId(id);
        } catch (Exception e) {
            return Mono.error(new RecordNotFoundException("Invalid banner id"));
        }

        return bannerRepository.findById(bannerId)
                .switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND)))
                .flatMap(banner -> {

                    Mono<Void> deleteImageMono = Mono.empty();

                    if (banner.getImage() != null && banner.getImage().getId() != null) {
                        deleteImageMono = profileService.deleteExistingImage(banner.getImage());
                    }

                    return deleteImageMono
                            .then(bannerRepository.deleteById(bannerId))
                            .thenReturn(BANNER_DELETED);
                });
    }

    private Mono<ObjectId> resolveAgentOrgId(Users authUser) {
        return agentRepository.findByUserId(authUser.getId())
                .switchIfEmpty(Mono.error(new ValidationExceptions("Agent not found")))
                .map(Agents::getOrganizationId);
    }

    private Mono<ObjectId> resolveOrgId(Users authUser) {
        return organizationRepository.findByUserId(authUser.getId())
                .map(Organization::getId);
    }
}
