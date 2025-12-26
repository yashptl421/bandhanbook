package com.bandhanbook.app.service;

import com.bandhanbook.app.exception.RecordNotFoundException;
import com.bandhanbook.app.model.Events;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.payload.request.EventRequest;
import com.bandhanbook.app.payload.response.EventResponse;
import com.bandhanbook.app.payload.response.OrganizationResponse;
import com.bandhanbook.app.payload.response.UserResponse;
import com.bandhanbook.app.repository.EventsRepository;
import com.bandhanbook.app.repository.OrganizationRepository;
import com.bandhanbook.app.repository.UserRepository;
import org.bson.types.ObjectId;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.List;
import java.util.Map;

import static com.bandhanbook.app.utilities.ErrorResponseMessages.DATA_NOT_FOUND;

@Service
public class EventService {
    private static final Logger logger = LoggerFactory.getLogger(EventService.class);
    @Autowired
    private EventsRepository eventsRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ReactiveMongoTemplate template;

    @Transactional
    public Mono<Void> createEvent(EventRequest eventRequest, Users user) {
        logger.info("Created Event of {}", eventRequest.getName());
        return organizationRepository.findById(new ObjectId(eventRequest.getOrganizationId()))
                .flatMap(organization -> {
                    Events events = modelMapper.map(eventRequest, Events.class);
                    events.setOrganizationId(organization.getId());
                    events.setCreatedBy(user.getId());
                    return eventsRepository.save(events);
                }).switchIfEmpty(Mono.error(new RecordNotFoundException("Organization " + DATA_NOT_FOUND))).then();
    }

    @Transactional
    public Mono<Void> updateEvent(EventRequest eventRequest, String id) {
        logger.info("Updated Event of {}", eventRequest.getName());
        return eventsRepository.findById(id)
                .switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND))).
                flatMap(events -> {
                    modelMapper.map(eventRequest, events);
                    return eventsRepository.save(events);
                }).then();
    }

    public Mono<EventResponse> getEventById(String id) {
        logger.info("get Event By event id {}", id);
        return eventsRepository.findById(id).switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND))).map(events -> {
            return modelMapper.map(events, EventResponse.class);
        });
    }

    public Mono<Tuple2<Long, List<EventResponse>>> eventsList(Map<String, String> params, Users authUser) {
        int page = Integer.parseInt(params.getOrDefault("page", "1"));
        int limit = Integer.parseInt(params.getOrDefault("limit", "10"));
        String search = params.getOrDefault("search", "");
        String organizationId = params.get("organizationId");
        String createdBy = params.get("createdBy");

        // 1. Base Event stream
        Flux<Events> eventsFlux = eventsRepository.findAll()
                .filter(event -> {

                    boolean match = true;

                    if (organizationId != null && !organizationId.isEmpty()) {
                        match = event.getId().toHexString().equals(organizationId);
                    }

                    if (createdBy != null && !createdBy.isEmpty()) {
                        match = match && createdBy.equalsIgnoreCase(event.getCreatedBy().toHexString());
                    }

                    return match;
                });

        // 2. Email search filter
        if (!search.isBlank()) {
            eventsFlux = eventsFlux
                    .flatMap(events ->
                            organizationRepository.findById(events.getOrganizationId())
                                    .filter(org -> org.getOrganizationName() != null &&
                                            org.getOrganizationName().toLowerCase().contains(search.toLowerCase()))
                                    .map(org -> events)
                    );
        }

        // 3. Count total before pagination
        Mono<Long> totalMono = eventsFlux.count();

        // 4. Paginate
        Flux<Events> pagedFlux = eventsFlux
                .skip((long) (page - 1) * limit)
                .take(limit);

        // 5. Convert to response
        Flux<EventResponse> responseFlux = pagedFlux.flatMap(event -> {
            return organizationRepository.findById(event.getOrganizationId()).switchIfEmpty(
                    Mono.error(new RecordNotFoundException(DATA_NOT_FOUND))
            ).flatMap(org -> userRepository.findById(org.getUserId())

                    .map(user -> {
                        EventResponse res = modelMapper.map(event, EventResponse.class);
                        UserResponse userResponse = modelMapper.map(user, UserResponse.class);
                        res.setOrganization_details(modelMapper.map(org, OrganizationResponse.class));
                        res.setCreated_by_details(userResponse);
                        return res;
                    }));
        });
        return totalMono.zipWith(responseFlux.collectList());
    }
}
