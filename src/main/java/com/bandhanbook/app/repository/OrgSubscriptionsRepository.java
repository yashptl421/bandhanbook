package com.bandhanbook.app.repository;

import com.bandhanbook.app.model.OrgSubscriptions;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface OrgSubscriptionsRepository extends ReactiveMongoRepository<OrgSubscriptions, ObjectId> {
    public Mono<OrgSubscriptions> findByOrgId(ObjectId org_id);
}
