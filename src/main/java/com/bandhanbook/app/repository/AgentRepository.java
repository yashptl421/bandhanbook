package com.bandhanbook.app.repository;

import com.bandhanbook.app.model.Agents;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

import java.util.List;

public interface AgentRepository extends ReactiveMongoRepository<Agents, String> {
    Mono<Agents> findByUserId(ObjectId user_id);

    Mono<Long> countByOrganizationId(ObjectId organizationId);

    Mono<Agents> findByIdIn(List<ObjectId> id);
}
