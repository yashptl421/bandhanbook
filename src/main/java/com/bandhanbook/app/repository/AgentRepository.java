package com.bandhanbook.app.repository;

import com.bandhanbook.app.model.Agents;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface AgentRepository extends ReactiveMongoRepository<Agents, String> {
    Mono<Agents> findByUserId(String user_id);
}
