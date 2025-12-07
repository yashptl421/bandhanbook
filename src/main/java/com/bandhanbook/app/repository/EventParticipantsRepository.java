package com.bandhanbook.app.repository;

import com.bandhanbook.app.model.EventParticipants;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface EventParticipantsRepository extends ReactiveMongoRepository<EventParticipants, String> {

    Mono<Boolean> existsByCandidateIdAndEventId(String candidate_id, String event_id);
    Mono<EventParticipants> findByCandidateId(String candidate_id);
}
