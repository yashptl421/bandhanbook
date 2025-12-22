package com.bandhanbook.app.repository;

import com.bandhanbook.app.model.Organization;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface OrganizationRepository extends ReactiveMongoRepository<Organization, ObjectId> {
    Mono<Organization> findByUserId(ObjectId user_id);
}
