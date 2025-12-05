package com.bandhanbook.app.repository;

import com.bandhanbook.app.model.Organization;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface OrganizationRepository extends ReactiveMongoRepository<Organization, String> {
    Mono<Organization> findByUserId(String user_id);
}
