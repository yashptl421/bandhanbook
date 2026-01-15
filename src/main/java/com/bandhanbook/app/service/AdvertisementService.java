package com.bandhanbook.app.service;

import com.bandhanbook.app.exception.RecordNotFoundException;
import com.bandhanbook.app.model.Advertisement;
import com.bandhanbook.app.model.EventParticipants;
import com.bandhanbook.app.model.Events;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.model.constants.Frequency;
import com.bandhanbook.app.model.constants.Status;
import com.bandhanbook.app.payload.request.AdvertisementFilterRequest;
import com.bandhanbook.app.payload.request.AdvertisementRequest;
import com.bandhanbook.app.payload.request.AdvertisementUpdateRequest;
import com.bandhanbook.app.payload.response.AdvertisementResponse;
import com.bandhanbook.app.repository.*;
import com.bandhanbook.app.wrappers.AdvertisementWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.WriteModel;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.time.LocalDateTime;
import java.util.List;

import static com.bandhanbook.app.utilities.ErrorResponseMessages.DATA_NOT_FOUND;
import static com.bandhanbook.app.utilities.SuccessResponseMessages.ADVERTISEMENT_CREATED;

@Service
@RequiredArgsConstructor
public class AdvertisementService {

    private final EventsRepository eventRepository;
    private final OrganizationRepository organizationRepository;
    private final MatrimonyRepository matrimonyRepository;
    private final AgentRepository agentRepository;
    private final EventParticipantsRepository eventParticipantsRepository;
    private final AdvertisementRepository repository;
    private final ImageUploadService fileUploadService;
    private final ReactiveMongoTemplate template;
    private final ModelMapper modelMapper;


    @Value("${images.base.path}")
    private String basePath;

    @Value("${images.advertisement.path}")
    private String advertisementPath;
    private final ObjectMapper objectMapper;

    public Mono<String> createAdvertisement(
            String dataJson,
            Flux<FilePart> files,
            Users authUser
    ) {
        AdvertisementRequest request;
        try {
            request = objectMapper.readValue(dataJson, AdvertisementRequest.class);
        } catch (Exception e) {
            return Mono.error(new IllegalArgumentException("Invalid advertisement data"));
        }

        ObjectId eventId = new ObjectId(request.getEventId());
        List<Frequency> frequencies = request.getFrequency();

        return eventRepository.findById(eventId)
                .switchIfEmpty(Mono.error(new RecordNotFoundException("Event not found")))
                .flatMap(event -> {

                    String folder = basePath + event.getId().toHexString() + advertisementPath;

                    return files
                            .index()
                            .flatMap(tuple -> {
                                int index = tuple.getT1().intValue();
                                FilePart file = tuple.getT2();

                                if (index >= frequencies.size()) {
                                    return Mono.error(
                                            new IllegalArgumentException(
                                                    "Frequency missing for image index " + index));
                                }
                                Frequency frequency = frequencies.get(index);
                                return fileUploadService
                                        .upload(file, event.getId().toHexString(), folder)
                                        .flatMap(img -> {
                                            Advertisement ad = Advertisement.builder()
                                                    .images(img)
                                                    .frequency(frequency)
                                                    .eventId(eventId)
                                                    .active(true)
                                                    .createdBy(authUser.getId())
                                                    .build();
                                            return repository.save(ad);
                                        });
                            })
                            .then(Mono.just(ADVERTISEMENT_CREATED));
                });
    }

    public Mono<Tuple3<Long, Long, List<AdvertisementResponse>>> advertisementList(AdvertisementFilterRequest filter, Users authUser) {

        return getEvetnIdMono(authUser)
                .flatMap(eventIds ->
                        findWithCounts(filter, eventIds, authUser)
                )
                .map(result -> {

                    List<AdvertisementResponse> ads = result.getData()
                            .stream()
                            .map(ad -> AdvertisementResponse.builder()
                                    .id(ad.getId().toHexString())
                                    .eventId(ad.getEventId().toHexString())
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

    @Transactional
    public Mono<List<AdvertisementResponse>> updateAdvertisement(
            List<AdvertisementUpdateRequest> requests
    ) {
        if (requests.isEmpty()) {
            return Mono.just(List.of());
        }

        List<ObjectId> ids = requests.stream()
                .map(r -> new ObjectId(r.getId()))
                .toList();

        return repository.findAllById(ids)
                .collectMap(ad -> ad.getId().toHexString())
                .flatMapMany(existingMap -> Flux.fromIterable(requests)
                        .map(req -> {
                            Advertisement ad = existingMap.get(req.getId());
                            if (ad == null) {
                                throw new RecordNotFoundException(DATA_NOT_FOUND);
                            }

                            ad.setActive(req.isActive());
                            return ad;
                        })
                )
                .collectList()
                .flatMapMany(repository::saveAll)
                .map(ad -> modelMapper.map(ad, AdvertisementResponse.class))
                .collectList();
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

    public Mono<Void> deleteAdvertisement(List<AdvertisementUpdateRequest> requests) {
        List<ObjectId> ids = requests.stream().map(req -> new ObjectId(req.getId())).toList();
        return repository.deleteByIdIn(ids).then(deleteAdvertisementImages(ids));
    }

    private Mono<Void> deleteAdvertisementImages(List<ObjectId> ids) {
        Mono<List<Advertisement>> MonoList = repository.findAllById(ids)
                .collectList();

        return MonoList.flatMap(ads -> {
            List<String> imageIds = ads.stream()
                    .filter(ad -> ad.getImages() != null && ad.getImages().getId() != null)
                    .map(ad -> ad.getImages().getId())
                    .toList();
            if (imageIds.isEmpty()) {
                return Mono.empty();
            }
            return fileUploadService.bulkDelete(imageIds);
        });
    }

    public Mono<AdvertisementWrapper> findWithCounts(AdvertisementFilterRequest filter, List<ObjectId> eventIds, Users authUser) {
        int skip = (filter.getPage() - 1) * filter.getLimit();
        Criteria criteria = new Criteria();
        criteria.and("event_id").in(eventIds);

        if (filter.getFrequencies() != null) {
            criteria.and("frequency").in(filter.getFrequencies());
        }
        if (filter.getIsActive() != null && !authUser.isCandidate()) {
            criteria.and("is_active").is(filter.getIsActive());
        }
        if (authUser.isCandidate()) {
            criteria.and("is_active").in(true);
        }
        Aggregation aggregation = Aggregation.newAggregation(

                Aggregation.match(criteria),
                Aggregation.facet(
                                Aggregation.sort(Sort.Direction.DESC, "createdAt"),
                                Aggregation.skip(skip),
                                Aggregation.limit(filter.getLimit())
                        ).as("data")

                        .and(Aggregation.count().as("count"))
                        .as("total")

                        .and(
                                Aggregation.match(Criteria.where("is_active").is(true)),
                                Aggregation.count().as("count")
                        ).as("activeCount")
        );

        return template.aggregate(aggregation, "advertisements", AdvertisementWrapper.class)
                .next()
                .defaultIfEmpty(new AdvertisementWrapper());
    }


    private Mono<List<ObjectId>> getEvetnIdMono(Users authUser) {
        if (authUser.isOrganization()) {
            return organizationRepository.findByUserId(authUser.getId())
                    .flatMap(org -> eventRepository.findByOrganizationIdAndStatus(org.getId(), Status.active)
                            .map(Events::getId)
                            .collectList()
                            .switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND))));
        } else if (authUser.isAgent()) {
            return agentRepository.findByUserId(authUser.getId())
                    .flatMap(agents -> eventRepository.findByOrganizationIdAndStatus(agents.getOrganizationId(), Status.active)
                            .map(Events::getId)
                            .collectList()
                            .switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND))));
        }
        if (authUser.isCandidate()) {
            return matrimonyRepository.findByUserId(authUser.getId())
                    .switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND)))
                    .flatMapMany(candidate ->
                            eventParticipantsRepository.findByCandidateId(candidate.getId())
                    )
                    .map(EventParticipants::getEventId)
                    .distinct()
                    .collectList()
                    .filter(list -> !list.isEmpty())
                    .switchIfEmpty(Mono.error(
                            new RecordNotFoundException(DATA_NOT_FOUND)));
        } else if (authUser.isSuperUser()) {
            return eventRepository.findByStatus(Status.active)
                    .map(Events::getId)
                    .collectList()
                    .switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND)));
        }
        return Mono.error(new RecordNotFoundException(DATA_NOT_FOUND));
    }
}