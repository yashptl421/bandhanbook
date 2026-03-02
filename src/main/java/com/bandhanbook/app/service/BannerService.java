package com.bandhanbook.app.service;

import com.bandhanbook.app.config.MessageUtil;
import com.bandhanbook.app.exception.RecordNotFoundException;
import com.bandhanbook.app.exception.ValidationExceptions;
import com.bandhanbook.app.model.Banners;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.payload.request.BannerRequest;
import com.bandhanbook.app.payload.response.BannerResponse;
import com.bandhanbook.app.payload.response.base.ApiResponse;
import com.bandhanbook.app.repository.BannerRepository;
import com.bandhanbook.app.wrappers.BannerWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BannerService {
    private final BannerRepository bannerRepository;
    private final ImageUploadService imageUploadService;
    private final ModelMapper modelMapper;
    private final ReactiveMongoTemplate mongoTemplate;
    private final AgentService agentService;
    private final UserService userService;
    private final ProfileService profileService;
    private final UsageMetricsService usageMetricsService;
    private final LimitEnforcementComponent limitEnforcementComponent;
    private final MessageUtil messageUtil;

    @Value("${images.base.path}")
    private String basePath;
    @Value("${images.banner.path}")
    private String bannerPath;
    @Value("${images.organization.path}")
    private String organizationImagePath;

    public Mono<BannerResponse> createBanner(BannerRequest request, FilePart file, Users authUser) {
        if (file == null || file.headers().getContentType() == null ||
                !Objects.requireNonNull(file.headers().getContentType()).toString().startsWith("image/")) {
            return Mono.error(new ValidationExceptions(messageUtil.get("banner.image.required")));
        }
        Map<String, String> params = new HashMap<>();
        if (request.getOrganizationId() != null && authUser.isSuperUser())
            params.put("organizationId", request.getOrganizationId());
        Mono<String> orgIdMono = agentService.getOrgIdMono(authUser, params);


        return orgIdMono.flatMap(orgId -> {

            String filename = orgId + "_" + System.currentTimeMillis();
            String folder = basePath + orgId + bannerPath;
            return limitEnforcementComponent.checkBannerLimit(new ObjectId(orgId))
                    .then(imageUploadService.upload(file, filename, folder)
                    .flatMap(image -> {

                        Banners banner = Banners.builder()
                                .title(request.getTitle())
                                .description(request.getDescription())
                                .active(Boolean.TRUE.equals(request.getIsActive()))
                                .image(image)
                                .organizationId(new ObjectId(orgId))
                                .createdBy(authUser.getId())
                                .build();
                        return bannerRepository.save(banner)
                                .doOnSuccess(banners ->
                                        triggerBannerMetrics(banners.getOrganizationId())
                                ).thenReturn(modelMapper.map(banner, BannerResponse.class));
                    }));
        });
    }
    private void triggerBannerMetrics(ObjectId orgId) {
        usageMetricsService.incrementBanners(orgId)
                .doOnError(e -> log.error("Failed to increment Banner metrics", e))
                .subscribe();
    }
    public Mono<ApiResponse<List<BannerResponse>>> listBanners(Users authUser, int page, int limit) {
        Mono<String> orgIdMono = agentService.getOrgIdMono(authUser, new HashMap<>());
        if (authUser.isCandidate()) {
            orgIdMono = userService.getCandidateOrgId(authUser);
        }
        List<AggregationOperation> pipeline = new ArrayList<>();
        Criteria baseCriteria = new Criteria();
        return orgIdMono.flatMap(orgId -> {
            int skip = (page - 1) * limit;
            if (!orgId.isEmpty()) {
                baseCriteria.and("organization_id")
                        .is(new ObjectId(orgId));
            }
            if (authUser.isAgent() || authUser.isCandidate()) {
                baseCriteria.and("is_active").is(true);
            }
            pipeline.add(Aggregation.match(baseCriteria));

            pipeline.add(Aggregation.facet(
                            Aggregation.sort(Sort.Direction.DESC, "created_at"),
                            Aggregation.skip(skip),
                            Aggregation.limit(limit)
                    ).as("data")

                    .and(Aggregation.count().as("count"))
                    .as("total")
                    .and(Aggregation.match(Criteria.where("is_active").is(true)),
                            Aggregation.count().as("count")
                    ).as("activeCount"));

            Aggregation aggregation = Aggregation.newAggregation(pipeline);

            return mongoTemplate
                    .aggregate(aggregation, "banners", BannerWrapper.class)
                    .collectList()
                    //.defaultIfEmpty(new BannerWrapper())
                    .map(wrapper1 -> {
                        BannerWrapper wrapper = wrapper1.get(0);
                        List<BannerResponse> data = wrapper.getData();

                        long total = extractCount(wrapper.getTotal());
                        long active = extractCount(wrapper.getActiveCount());
                        long inactive = total - active;

                        int totalPages = (int) Math.ceil((double) total / limit);

                        return ApiResponse.<List<BannerResponse>>builder()
                                .status(200)
                                .message(data.isEmpty() ? messageUtil.get("record.not.found") : messageUtil.get("records.found"))
                                .data(data)
                                .meta(ApiResponse.Meta.builder()
                                        .page(page)
                                        .limit(limit)
                                        .totalRecords(total)
                                        .totalPages(totalPages)
                                        .build())
                                .activeCount(active)
                                .inactiveCount(inactive)
                                .build();
                    });
        });
    }

    private long extractCount(List<BannerWrapper.RecordCount> list) {
        return (list == null || list.isEmpty()) ? 0 : list.get(0).getCount();
    }

    public Mono<BannerResponse> updateBanner(String bannerId, Boolean isActive) {
        ObjectId id;
        try {
            id = new ObjectId(bannerId);
        } catch (Exception e) {
            return Mono.error(new ValidationExceptions(messageUtil.get("banner.id.invalid")));
        }
        return bannerRepository.findById(id)
                .switchIfEmpty(Mono.error(new RecordNotFoundException(messageUtil.get("record.not.found"))))
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
            return Mono.error(new RecordNotFoundException(messageUtil.get("banner.id.invalid")));
        }
        return bannerRepository.findById(bannerId)
                .switchIfEmpty(Mono.error(new RecordNotFoundException(messageUtil.get("record.not.found"))))
                .map(banner -> modelMapper.map(banner, BannerResponse.class));
    }

    public Mono<String> deleteBanner(String id) {

        ObjectId bannerId;
        try {
            bannerId = new ObjectId(id);
        } catch (Exception e) {
            return Mono.error(new RecordNotFoundException(messageUtil.get("banner.id.invalid")));
        }

        return bannerRepository.findById(bannerId)
                .switchIfEmpty(Mono.error(new RecordNotFoundException(messageUtil.get("record.not.found"))))
                .flatMap(banner -> {

                    Mono<Void> deleteImageMono = Mono.empty();

                    if (banner.getImage() != null && banner.getImage().getId() != null) {
                        deleteImageMono = profileService.deleteExistingImage(banner.getImage());
                    }

                    return deleteImageMono
                            .then(bannerRepository.deleteById(bannerId))
                            .then(usageMetricsService.decrementBanners(banner.getOrganizationId()))
                            .thenReturn(messageUtil.get("banner.deleted"));
                });
    }
}
