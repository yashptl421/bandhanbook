package com.bandhanbook.app.service;

import com.bandhanbook.app.model.OrgUsageMetrics;
import com.bandhanbook.app.repository.UsageMetricsRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UsageMetricsService {
    // This service can be expanded to include methods for tracking and calculating usage metrics
    // such as total agents, users, banners, and advertisements used by an organization.
    private final ReactiveMongoTemplate mongoTemplate;
    private final UsageMetricsRepository repository;

    public Mono<OrgUsageMetrics> getUsageMetrics(ObjectId orgId, ObjectId eventId) {
        if (orgId != null && eventId != null) {
            return repository.findByOrgIdAndEventIdAndSubscriptionActive(orgId, eventId,true);
        } else if (orgId != null) {
            return repository.findByOrgIdAndSubscriptionActive(orgId, true);
        } else  {
            return repository.findByEventIdAndSubscriptionActive(eventId, true);
        }
    }

    public Mono<Void> createDefault(ObjectId orgId, ObjectId eventId, boolean subscriptionActive) {
        return repository.findByOrgIdAndEventIdAndSubscriptionActive(orgId, eventId, subscriptionActive)
                .flatMap(exists -> {
                    exists.setSubscriptionActive(subscriptionActive);
                    return repository.save(exists).then();

                })
                .switchIfEmpty(Mono.defer(() -> {
                            OrgUsageMetrics metrics = OrgUsageMetrics.builder()
                                    .orgId(orgId)
                                    .eventId(eventId)
                                    .currentUsers(0)
                                    .currentAgents(0)
                                    .currentBanners(0)
                                    .currentAdvertisements(0)
                                    .updatedAt(LocalDateTime.now())
                                    .build();

                            return repository.save(metrics).then();
                        })
                        .then());
    }

    public Mono<Void> incrementUsers(ObjectId orgId) {
        return updateMetric(orgId, "current_users", 1);
    }

    public Mono<Void> decrementUsers(ObjectId orgId) {
        return updateMetric(orgId, "current_users", -1);
    }

    public Mono<Void> incrementAgents(ObjectId orgId) {
        return updateMetric(orgId, "current_agents", 1);
    }

    public Mono<Void> decrementAgents(ObjectId orgId) {
        return updateMetric(orgId, "current_agents", -1);
    }

    public Mono<Void> incrementBanners(ObjectId orgId) {
        return updateMetric(orgId, "current_banners", 1);
    }

    public Mono<Void> decrementBanners(ObjectId orgId) {
        return updateMetric(orgId, "current_banners", -1);
    }

    public Mono<Void> incrementAdvertisement(ObjectId orgId) {
        return updateMetric(orgId, "current_advertisements", 1);
    }

    public Mono<Void> decrementAdvertisement(ObjectId orgId) {
        return updateMetric(orgId, "current_advertisements", -1);
    }


    private Mono<Void> updateMetric(ObjectId orgId, String field, int delta) {

        Query query = Query.query(Criteria.where("org_id").is(orgId));

        Update update = new Update()
                .inc(field, delta)
                .set("updated_at", LocalDateTime.now());

        return mongoTemplate.upsert(query, update, OrgUsageMetrics.class).then();
    }
}
