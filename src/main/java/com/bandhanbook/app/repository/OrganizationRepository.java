package com.bandhanbook.app.repository;

import com.bandhanbook.app.model.Organization;
import lombok.AllArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface OrganizationRepository extends ReactiveMongoRepository<Organization, ObjectId> {
    Mono<Organization> findByUserId(ObjectId user_id);

    @Query("{ 'user_id': ?0 }")
    @Update("{ '$set': { 'status': 'inactive' } }")
    Mono<Long> deactivateOrganizationByUserId(ObjectId userId);
}
