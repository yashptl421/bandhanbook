package com.bandhanbook.app.repository;

import com.bandhanbook.app.model.EventParticipants;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventParticipantsRepository extends MongoRepository<EventParticipants, String> {

    boolean existsCandidateByEventId(String candidate_id, String event_id);
}
