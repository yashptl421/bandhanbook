package com.bandhanbook.app.service;

import com.bandhanbook.app.config.MessageUtil;
import com.bandhanbook.app.exception.RecordNotFoundException;
import com.bandhanbook.app.exception.ValidationExceptions;
import com.bandhanbook.app.model.Advertisement;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.model.constants.Frequency;
import com.bandhanbook.app.payload.request.AdvertisementFilterRequest;
import com.bandhanbook.app.payload.request.AdvertisementRequest;
import com.bandhanbook.app.payload.request.AdvertisementUpdateRequest;
import com.bandhanbook.app.payload.response.AdvertisementResponse;
import com.bandhanbook.app.repository.AdvertisementRepository;
import com.bandhanbook.app.repository.EventsRepository;
import com.bandhanbook.app.wrappers.AdvertisementWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.WriteModel;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdvertisementService {

    private final EventService eventService;
    private final EventsRepository eventRepository;
    private final AdvertisementRepository repository;
    private final ImageUploadService fileUploadService;
    private final ReactiveMongoTemplate template;
    private final UsageMetricsService usageMetricsService;
    private final LimitEnforcementComponent limitEnforcementComponent;
    private final MessageUtil messageUtil;


    @Value("${images.base.path}")
    private String basePath;

    @Value("${images.advertisement.path}")
    private String advertisementPath;
    private final ObjectMapper objectMapper;

    public Mono<String> createAdvertisement(String dataJson, Flux<FilePart> files, Users authUser) {

        AdvertisementRequest request;
        try {
            request = objectMapper.readValue(dataJson, AdvertisementRequest.class);
        } catch (Exception e) {
            return Mono.error(new ValidationExceptions(messageUtil.get("invalid.advertisement.data")));
        }

        ObjectId eventId = new ObjectId(request.getEventId());
        List<Frequency> frequencies = request.getFrequency();

        return eventRepository.findById(eventId)
                .switchIfEmpty(Mono.error(new RecordNotFoundException(messageUtil.get("event.not.found"))))
                .flatMap(event -> {

                    String folder = basePath + event.getId().toHexString() + advertisementPath;

                    return files.index()
                            .flatMap(tuple -> {

                                int index = tuple.getT1().intValue();
                                FilePart file = tuple.getT2();

                                if (index >= frequencies.size()) {
                                    return Mono.error(new ValidationExceptions(messageUtil.get("frequencies.count.mismatch")));
                                }
                                return limitEnforcementComponent
                                        .checkAdvertisementLimit(eventId, 1)
                                        .then(fileUploadService.upload(file, event.getId().toHexString(), folder))
                                        .map(img -> Advertisement.builder()
                                                .images(img)
                                                .frequency(frequencies.get(index))
                                                .eventId(eventId)
                                                .active(true)
                                                .organizationId(event.getOrganizationId())
                                                .createdBy(authUser.getId())
                                                .build());
                            }, 4)
                            .collectList()
                            .flatMapMany(repository::saveAll)
                            .then(usageMetricsService.incrementAdvertisement(
                                    event.getOrganizationId(), eventId, frequencies.size()))
                            .thenReturn(messageUtil.get("advertisement.created"));
                });
    }

    public Mono<Tuple3<Long, Long, List<AdvertisementResponse>>> advertisementList(AdvertisementFilterRequest filter, Users authUser) {

        return eventService.getEventIdMono(authUser)
                .flatMap(eventIds -> findWithCounts(filter, eventIds, authUser))
                .map(result -> {
                    List<AdvertisementResponse> ads = result.getData()
                            .stream()
                            .map(ad -> AdvertisementResponse.builder()
                                    .id(ad.getId().toHexString())
                                    .eventId(ad.getEventId().toHexString())
                                    .organizationId(ad.getOrganizationId().toHexString())
                                    .images(ad.getImages())
                                    .frequency(ad.getFrequency())
                                    .durationInDays(ad.getDurationInDays())
                                    .active(ad.isActive())
                                    .createdAt(ad.getCreatedAt())
                                    .updatedAt(ad.getUpdatedAt())
                                    .build())
                            .toList();
                    return Tuples.of(
                            result.getTotalCount(),
                            result.getActiveCount(),
                            ads
                    );
                });
    }

    public Mono<Void> bulkUpdate(List<AdvertisementUpdateRequest> requests) {

        List<WriteModel<Document>> updates = requests.stream()
                .<WriteModel<Document>>map(req ->
                        new UpdateOneModel<>(
                                Filters.eq("_id", new ObjectId(req.getId())),
                                Updates.combine(
                                        Updates.set("is_active", req.isActive()),
                                        Updates.set("updated_at", LocalDateTime.now())
                                )
                        )
                )
                .toList();

        return template
                .getCollection("advertisements")
                .flatMapMany(col -> col.bulkWrite(updates))
                .then();
    }

    public Mono<Void> deleteAdvertisement(List<String> requests) {

        List<ObjectId> ids = requests.stream().map(ObjectId::new).toList();

        return repository.findAllById(ids)
                .collectList()
                .flatMap(ads -> {

                    if (ads.isEmpty()) return Mono.empty();

                    ObjectId orgId = ads.get(0).getOrganizationId();
                    ObjectId eventId = ads.get(0).getEventId();

                    List<String> imageIds = ads.stream()
                            .map(ad -> ad.getImages().getId())
                            .toList();

                    return fileUploadService.bulkDelete(imageIds)
                            .then(repository.deleteAllById(ids))
                            .then(usageMetricsService.decrementAdvertisement(orgId, eventId, ids.size()));
                });
    }

    public Mono<AdvertisementWrapper> findWithCounts(
            AdvertisementFilterRequest filter, List<ObjectId> eventIds, Users authUser) {

        int skip = (filter.getPage() - 1) * filter.getLimit();

        Criteria criteria = Criteria.where("event_id").in(eventIds);

        if (filter.getFrequencies() != null) {
            criteria.and("frequency").in(
                    filter.getFrequencies().stream().map(Frequency::valueOf).toList()
            );
        }
        if (authUser.isCandidate()) {
            criteria.and("is_active").is(true);
        } else if (filter.getIsActive() != null) {
            criteria.and("is_active").is(filter.getIsActive());
        }
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.facet(
                                Aggregation.sort(Sort.Direction.DESC, "created_at"),
                                Aggregation.skip(skip),
                                Aggregation.limit(filter.getLimit())
                        ).as("data")
                        .and(Aggregation.count().as("count")).as("total")
                        .and(
                                Aggregation.match(Criteria.where("is_active").is(true)),
                                Aggregation.count().as("count")
                        ).as("activeCount")
        );

        return template.aggregate(aggregation, "advertisements", AdvertisementWrapper.class)
                .next()
                .defaultIfEmpty(new AdvertisementWrapper());
    }
}