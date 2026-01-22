package com.bandhanbook.app.repository;

import com.bandhanbook.app.model.Events;
import com.bandhanbook.app.model.constants.EventType;
import com.bandhanbook.app.model.constants.Status;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EventsRepository extends ReactiveMongoRepository<Events, ObjectId> {
    Flux<Events> findByOrganizationIdAndStatusAndEventType(ObjectId organizationId, Status status, EventType eventType);
    Flux<Events> findByStatus(Status status);
}
