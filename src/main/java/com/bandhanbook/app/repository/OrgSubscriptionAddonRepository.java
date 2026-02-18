package com.bandhanbook.app.repository;

import com.bandhanbook.app.model.OrgSubscriptionAddon;
import com.bandhanbook.app.model.constants.AddOnStatus;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface OrgSubscriptionAddonRepository extends ReactiveMongoRepository<OrgSubscriptionAddon, ObjectId> {
    Flux<OrgSubscriptionAddon> findBySubscriptionId(ObjectId subscriptionId, Pageable pageable);
    Flux<OrgSubscriptionAddon> findBySubscriptionIdAndStatus(ObjectId subscriptionId, AddOnStatus status);

    Mono<Long> countBySubscriptionId(ObjectId subscriptionId);
}
