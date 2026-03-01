package com.bandhanbook.app.service;

import com.bandhanbook.app.exception.RecordNotFoundException;
import com.bandhanbook.app.model.EventParticipants;
import com.bandhanbook.app.model.Events;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.model.constants.EventType;
import com.bandhanbook.app.model.constants.Status;
import com.bandhanbook.app.payload.request.EventRequest;
import com.bandhanbook.app.payload.response.EventResponse;
import com.bandhanbook.app.repository.*;
import com.bandhanbook.app.wrappers.EventWrapper;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.bandhanbook.app.utilities.ErrorResponseMessages.DATA_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class EventService {
    private static final Logger logger = LoggerFactory.getLogger(EventService.class);
    private final EventsRepository eventsRepository;
    private final ModelMapper modelMapper;
    private final OrganizationRepository orgRepository;
    private final AgentRepository agentRepository;
    private final AgentService agentService;
    private final MatrimonyRepository matrimonyRepository;
    private final EventParticipantsRepository eventParticipantRepo;

    @Autowired
    private ReactiveMongoTemplate template;

    @Transactional
    public Mono<Void> createEvent(EventRequest eventRequest, Users user) {
        logger.info("Created Event of {}", eventRequest.getName());
        return orgRepository.findById(new ObjectId(eventRequest.getOrganizationId()))
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
        return eventsRepository.findById(new ObjectId(id))
                .switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND))).
                flatMap(events -> {
                    modelMapper.map(eventRequest, events);
                    return eventsRepository.save(events);
                }).then();
    }

    public Mono<EventResponse> getEventById(ObjectId id) {
        logger.info("get Event By event id {}", id);
        return eventsRepository.findById((id)).switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND)))
                .map(events -> modelMapper.map(events, EventResponse.class));
    }

    public Mono<EventWrapper> eventsList(Users authUser, Map<String, String> params, int page, int limit) {
        int skip = (page - 1) * limit;

        String search = params.get("search");
        String createdBy = params.get("createdBy");
        EventType eventType = params.containsKey("eventType") ? EventType.valueOf(params.get("eventType")) : null;
        Status status = params.containsKey("status") ? Status.valueOf(params.get("status")) : null;


        List<AggregationOperation> pipeline = new ArrayList<>();
        // -------------------------
        // 1. MATCH FILTERS
        // -------------------------
        Criteria criteria = new Criteria();
        return agentService.getOrgIdMono(authUser, params)
                .switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND)))
                .flatMap(id -> {
                    if (!id.isBlank()) {
                        criteria.and("organization_id").is(new ObjectId(id));
                    }
                    if(eventType!=null){
                        criteria.and("event_type").is(eventType);
                    }
                    if(status!=null){
                        criteria.and("status").is(status);
                    }
                    if (createdBy != null && !createdBy.isBlank()) {
                        criteria.and("created_by").is(new ObjectId(createdBy));
                    }

                    pipeline.add(Aggregation.match(criteria));
                    pipeline.add(Aggregation.lookup(
                            "organizations",
                            "organization_id",
                            "_id",
                            "organization_details"
                    ));
                    pipeline.add(Aggregation.unwind("organization_details"));
                    if (search != null && !search.isBlank()) {
                        pipeline.add(Aggregation.match(
                                Criteria.where("organization_details.organizationName")
                                        .regex(search, "i")
                        ));
                    }
                    pipeline.add(Aggregation.lookup(
                            "users",
                            "organization_details.user_id",
                            "_id",
                            "created_by_details"
                    ));
                    pipeline.add(Aggregation.unwind("created_by_details"));
                    pipeline.add(
                            Aggregation.facet(
                                            Aggregation.sort(Sort.Direction.DESC, "created_at"),
                                            Aggregation.skip(skip),
                                            Aggregation.limit(limit)
                                    ).as("data")
                                    .and(Aggregation.count().as("total"))
                                    .as("totalRecords")
                    );

                    Aggregation aggregation = Aggregation.newAggregation(pipeline);

                    return template.aggregate(aggregation, "events", EventWrapper.class)
                            .next()
                            .defaultIfEmpty(new EventWrapper());
                });
    }
    public Mono<List<ObjectId>> getEventIdMono(Users authUser) {
        if (authUser.isOrganization()) {
            return orgRepository.findByUserId(authUser.getId())
                    .flatMap(org -> eventsRepository.findByOrganizationIdAndStatusAndEventType(org.getId(), Status.active, EventType.CANDIDATE_REGISTRATION)
                            .map(Events::getId)
                            .collectList()
                            .switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND))));
        } else if (authUser.isAgent()) {
            return agentRepository.findByUserId(authUser.getId())
                    .flatMap(agents -> eventsRepository.findByOrganizationIdAndStatusAndEventType(agents.getOrganizationId(), Status.active, EventType.CANDIDATE_REGISTRATION)
                            .map(Events::getId)
                            .collectList()
                            .switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND))));
        }
        if (authUser.isCandidate()) {
            return matrimonyRepository.findByUserId(authUser.getId())
                    .switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND)))
                    .flatMapMany(candidate ->
                            eventParticipantRepo.findByCandidateId(candidate.getId())
                    )
                    .map(EventParticipants::getEventId)
                    .distinct()
                    .collectList()
                    .filter(list -> !list.isEmpty())
                    .switchIfEmpty(Mono.error(
                            new RecordNotFoundException(DATA_NOT_FOUND)));
        } else if (authUser.isSuperUser()) {
            return eventsRepository.findByStatus(Status.active)
                    .map(Events::getId)
                    .collectList()
                    .switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND)));
        }
        return Mono.error(new RecordNotFoundException(DATA_NOT_FOUND));
    }
}
