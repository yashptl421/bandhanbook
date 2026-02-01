package com.bandhanbook.app.repository;

import com.bandhanbook.app.model.Donations;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface DonationRepository extends ReactiveMongoRepository<Donations, ObjectId> {

    Flux<Donations> findByAgentIdAndDeletedAtIsNull(ObjectId agentId);

    Flux<Donations> findByOrganizationIdAndDeletedAtIsNull(ObjectId organizationId);
    Mono<Long>  countByAgentIdAndDeletedAtIsNull(ObjectId agentId);

    Mono<Long> countByOrganizationIdAndDeletedAtIsNull(ObjectId organizationId);
}
