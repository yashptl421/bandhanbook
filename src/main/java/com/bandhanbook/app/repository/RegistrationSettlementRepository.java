package com.bandhanbook.app.repository;

import com.bandhanbook.app.model.RegistrationSettlement;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface RegistrationSettlementRepository extends ReactiveMongoRepository<RegistrationSettlement, ObjectId> {
    Mono<RegistrationSettlement> findByAgentIdAndEventId(ObjectId agentId, ObjectId eventId);
}
