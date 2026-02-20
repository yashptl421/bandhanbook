package com.bandhanbook.app.repository;

import com.bandhanbook.app.model.OrgSubscriptions;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface OrgSubscriptionsRepository extends ReactiveMongoRepository<OrgSubscriptions, ObjectId> {
    Mono<OrgSubscriptions> findByOrgId(ObjectId org_id);
    Mono<OrgSubscriptions> findByOrgIdAndActive(ObjectId orgId, boolean active);
    Mono<OrgSubscriptions> findByEventIdAndActive(ObjectId eventId, boolean active);
    Mono<Boolean> findByOrgIdAndEventIdAndActive(ObjectId orgId, ObjectId eventId, boolean active);
}
