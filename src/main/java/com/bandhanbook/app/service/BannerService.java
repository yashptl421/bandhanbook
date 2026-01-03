package com.bandhanbook.app.service;

import com.bandhanbook.app.exception.RecordNotFoundException;
import com.bandhanbook.app.exception.ValidationExceptions;
import com.bandhanbook.app.model.Agents;
import com.bandhanbook.app.model.Banners;
import com.bandhanbook.app.model.Organization;
import com.bandhanbook.app.model.Users;
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

@Service
@RequiredArgsConstructor
public class BannerService {
    private final BannerRepository bannerRepository;
    private final OrganizationRepository organizationRepository;
    private final ImageUploadService imageUploadService;
    private final ModelMapper modelMapper;
    private final ReactiveMongoTemplate mongoTemplate;
    private final AgentRepository agentRepository;

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
            orgIdMono = agentRepository.findByUserId(authUser.getId())
                    .switchIfEmpty(Mono.error(new ValidationExceptions("Agent not found")))
                    .map(Agents::getOrganizationId);
        } else {
            orgIdMono = organizationRepository.findByUserId(authUser.getId())
                    .map(Organization::getId);
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
                // agent → banners of agent's organization
                return resolveAgentOrgId(authUser)
                        .flatMap(orgId ->
                                executeBannerQuery(criteria.and("organization_id").is(new ObjectId(orgId)),
                                        page, limit, skip)
                        );
            }

            if (authUser.isCandidate()) {
                // candidate → banners from organizations of participated events
                return resolveCandidateOrgIds(authUser)
                        .flatMap(orgIds ->
                                executeBannerQuery(criteria.and("organization_id").in(orgIds),
                                        page, limit, skip)
                        );
            }
        }

        if (authUser.isOrganization()) {
            criteria.and("organization_id").is(authUser.getId());
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

        Mono<List<BannerResponse>> dataMono = mongoTemplate.find(query, BannerResponse.class)
                .map(banner -> {
                    banner.getImage()
                            .setUrl(imageUploadService.getFullImageUrl(banner.getImage()));
                    return banner;
                })
                .collectList();

        Mono<Long> totalMono = mongoTemplate.count(new Query(criteria), BannerWrapper.class);

        Mono<Long> activeCountMono = mongoTemplate.count(
                new Query(criteria.and("is_active").is(true)),
                BannerWrapper.class
        );

/*        Mono<Long> inactiveCountMono = mongoTemplate.count(
                new Query(criteria.and("is_active").is(false)),
                BannerWrapper.class
        );*/

        return Mono.zip(dataMono, totalMono, activeCountMono)
                .map(tuple -> {
                    long total = tuple.getT2();
                    return BannerWrapper.builder()
                            .data(tuple.getT1())
                            .activeCount(tuple.getT3())
                            .inactiveCount(tuple.getT2()- tuple.getT3())
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
    private Mono<String> resolveAgentOrgId(Users authUser) {
        return Mono.justOrEmpty(authUser.getId())
                .flatMap(id -> Mono.just(id.toHexString())); // replace if agent entity lookup needed
    }

    private Mono<List<ObjectId>> resolveCandidateOrgIds(Users authUser) {
        // If you already have eventParticipants → orgIds in DB, fetch from there
        return Mono.just(List.of()); // implement using eventParticipantsRepository
    }
}
