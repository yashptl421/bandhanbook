package com.bandhanbook.app.repository;

import com.bandhanbook.app.model.OrgUsageMetrics;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface UsageMetricsRepository
        extends ReactiveMongoRepository<OrgUsageMetrics, ObjectId> {

    Mono<OrgUsageMetrics> findByOrgIdAndSubscriptionActive(ObjectId orgId, boolean subscriptionActive);
    Mono<OrgUsageMetrics> findByOrgIdAndEventIdAndSubscriptionActive(ObjectId orgId, ObjectId eventId, boolean subscriptionActive);
    Mono<OrgUsageMetrics> findByEventIdAndSubscriptionActive(ObjectId eventId, boolean subscriptionActive);


}