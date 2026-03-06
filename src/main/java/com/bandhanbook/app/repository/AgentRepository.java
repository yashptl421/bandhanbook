package com.bandhanbook.app.repository;

import com.bandhanbook.app.model.Agents;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.Update;
import reactor.core.publisher.Mono;

import java.util.List;

public interface AgentRepository extends ReactiveMongoRepository<Agents, ObjectId> {
    Mono<Agents> findByUserId(ObjectId user_id);

    Mono<Long> countByOrganizationId(ObjectId organizationId);

    @Query("{ 'user_id': ?0 }")
    @Update("{ '$set': { 'status': 'inactive' } }")
    Mono<Long> deactivateAgentByUserId(ObjectId userId);

    @Query("{ 'organization_id': ?0 }")
    @Update("{ '$set': { 'status': 'inactive' } }")
    Mono<Long> deactivateAgentsByOrganizationId(ObjectId organizationId);

    @Query("{ 'organization_id': ?0 }")
    @Update("{ '$set': { 'status': 'active' } }")
    Mono<Long> activateAgentsByOrganizationId(ObjectId organizationId);

}
