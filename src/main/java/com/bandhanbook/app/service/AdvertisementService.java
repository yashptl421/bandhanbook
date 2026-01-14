package com.bandhanbook.app.service;

import com.bandhanbook.app.exception.RecordNotFoundException;
import com.bandhanbook.app.model.Advertisement;
import com.bandhanbook.app.model.EventParticipants;
import com.bandhanbook.app.model.Events;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.model.constants.Frequency;
import com.bandhanbook.app.model.constants.Status;
import com.bandhanbook.app.payload.request.AdvertisementRequest;
import com.bandhanbook.app.payload.response.AddvertisementResponse;
import com.bandhanbook.app.repository.*;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

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


    @Value("${images.base.path}")
    private String basePath;

    @Value("${images.advertisement.path}")
    private String advertisementPath;


    public Mono<String> createAdvertisement(AdvertisementRequest request, Flux<FilePart> files, Users authUser) {

        ObjectId eventId = new ObjectId(request.getEventId());
        return eventRepository.findById(eventId)
                .switchIfEmpty(Mono.error(
                        new RecordNotFoundException("Event not found")))
                .flatMap(event -> {

                    List<Frequency> frequencies = request.getFrequency();
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

                                String folder = basePath + event.getId().toHexString() + advertisementPath;

                                return fileUploadService
                                        .upload(file, event.getId().toHexString(), folder)
                                        .map(img -> {
                                                    Advertisement ad = Advertisement.builder()
                                                            .images(img)
                                                            .frequency(frequency)
                                                            .eventId(eventId)
                                                            //  .active(active)
                                                            .build();
                                                    return repository.save(ad);
                                                }
                                        );
                            })
                            .collectList()
                            .thenReturn(ADVERTISEMENT_CREATED);
                });
    }

    public Mono<Tuple2<Long, List<AddvertisementResponse>>> advertisementList(Users authUser, int page, int limit) {
        return getEvetnIdMono(authUser).flatMap(eventId ->
        {
            Mono<Long> totalMono = repository.countByEventIdIn(eventId).flatMap(Mono::just).defaultIfEmpty(0L);
            Mono<List<AddvertisementResponse>> responses = repository.findByEventIdIn(eventId)
                    .skip((long) (page - 1) * limit)
                    .take(limit)
                    .map(ad -> AddvertisementResponse.builder()
                            .id(ad.getId().toHexString())
                            .eventId(ad.getEventId().toHexString())
                            .images(ad.getImages())
                            .frequency(ad.getFrequency())
                            .durationInDays(ad.getDurationInDays())
                            .active(ad.isActive())
                            .createdAt(ad.getCreatedAt())
                            .updatedAt(ad.getUpdatedAt())
                            .build()
                    )
                    .collectList();
            return totalMono.zipWith(responses);
        });
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