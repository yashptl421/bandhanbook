package com.bandhanbook.app.repository;

import com.bandhanbook.app.model.Organization;
import lombok.AllArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface OrganizationRepository extends ReactiveMongoRepository<Organization, ObjectId> {
    Mono<Organization> findByUserId(ObjectId user_id);
}
