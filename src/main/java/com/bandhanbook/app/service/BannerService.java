package com.bandhanbook.app.service;

import com.bandhanbook.app.exception.RecordNotFoundException;
import com.bandhanbook.app.exception.ValidationExceptions;
import com.bandhanbook.app.model.Banners;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.payload.request.BannerRequest;
import com.bandhanbook.app.payload.response.BannerResponse;
import com.bandhanbook.app.payload.response.base.ApiResponse;
import com.bandhanbook.app.repository.BannerRepository;
import com.bandhanbook.app.repository.OrganizationRepository;
import com.bandhanbook.app.wrappers.BannerWrapper;
import lombok.RequiredArgsConstructor;
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

import static com.bandhanbook.app.utilities.ErrorResponseMessages.DATA_NOT_FOUND;
import static com.bandhanbook.app.utilities.SuccessResponseMessages.BANNER_DELETED;
import static com.bandhanbook.app.utilities.SuccessResponseMessages.DATA_FOUND;

@Service
@RequiredArgsConstructor
public class BannerService {
    private final BannerRepository bannerRepository;
    private final OrganizationRepository organizationRepository;
    private final ImageUploadService imageUploadService;
    private final ModelMapper modelMapper;
    private final ReactiveMongoTemplate mongoTemplate;
    private final AgentService agentService;
    private final UserService userService;
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
        Map<String, String> params = new HashMap<>();
        if (request.getOrganizationId() != null && authUser.isSuperUser())
            params.put("organizationId", request.getOrganizationId());
        Mono<String> orgIdMono = agentService.getOrgIdMono(authUser, params);


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
                                .organizationId(new ObjectId(orgId))
                                .createdBy(authUser.getId())
                                .build();
                        return bannerRepository.save(banner).thenReturn(modelMapper.map(banner, BannerResponse.class));
                    });
        });
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
            if (authUser.isOrganization()) {
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
                                .message(data.isEmpty() ? DATA_NOT_FOUND : DATA_FOUND)
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
}
