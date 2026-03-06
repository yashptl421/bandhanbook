package com.bandhanbook.app.repository;

import com.bandhanbook.app.model.Events;
import com.bandhanbook.app.model.constants.EventType;
import com.bandhanbook.app.model.constants.Status;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.Update;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EventsRepository extends ReactiveMongoRepository<Events, ObjectId> {
    Flux<Events> findByOrganizationIdAndStatusAndEventType(ObjectId organizationId, Status status, EventType eventType);
    Flux<Events> findByOrganizationIdAndStatus(ObjectId organizationId, Status status);
    Flux<Events> findByStatus(Status status);

    @Query("{ 'organization_id': ?0 }")
    @Update("{ '$set': { 'status': 'inactive' } }")
    Mono<Long> deactivateEventsByOrganizationId(ObjectId organizationId);

    @Query("{ 'organization_id': ?0 }")
    @Update("{ '$set': { 'status': 'active' } }")
    Mono<Long> activateEventsByOrganizationId(ObjectId organizationId);

}
