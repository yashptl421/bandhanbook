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
            return repository.findByOrgIdAndEventIdAndSubscriptionActive(orgId, eventId, true);
        } else if (orgId != null) {
            return repository.findByOrgIdAndSubscriptionActive(orgId, true);
        } else {
            return repository.findByEventIdAndSubscriptionActive(eventId, true);
        }
    }

    public Mono<Void> createDefault(ObjectId orgId,
                                    ObjectId eventId,
                                    boolean subscriptionActive) {

        return repository.findByOrgIdAndEventId(orgId, eventId)
                .flatMap(existing -> {
                    existing.setSubscriptionActive(subscriptionActive);
                    existing.setUpdatedAt(LocalDateTime.now());
                    return repository.save(existing);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    OrgUsageMetrics metrics = OrgUsageMetrics.builder()
                            .orgId(orgId)
                            .eventId(eventId)
                            .currentUsers(0)
                            .currentAgents(0)
                            .currentBanners(0)
                            .currentAdvertisements(0)
                            .subscriptionActive(subscriptionActive)
                            .updatedAt(LocalDateTime.now())
                            .build();

                    return repository.save(metrics);
                }))
                .then();
    }

    public Mono<Void> incrementUsers(ObjectId orgId, ObjectId eventId) {
        return updateMetric(orgId, eventId, "current_users", 1);
    }

    public Mono<Void> decrementUsers(ObjectId orgId, ObjectId eventId) {
        return updateMetric(orgId, eventId, "current_users", -1);
    }

    public Mono<Void> incrementAgents(ObjectId orgId) {
        return updateMetric(orgId, null, "current_agents", 1);
    }

    public Mono<Void> decrementAgents(ObjectId orgId, ObjectId eventId) {
        return updateMetric(orgId, null, "current_agents", -1);
    }

    public Mono<Void> incrementBanners(ObjectId orgId) {
        return updateMetric(orgId, null, "current_banners", 1);
    }

    public Mono<Void> decrementBanners(ObjectId orgId) {
        return updateMetric(orgId, null, "current_banners", -1);
    }

    public Mono<Void> incrementAdvertisement(ObjectId orgId, ObjectId eventId) {
        return updateMetric(orgId, eventId, "current_advertisements", 1);
    }

    public Mono<Void> decrementAdvertisement(ObjectId orgId, ObjectId eventId, int count) {
        return updateMetric(orgId, eventId, "current_advertisements", -count);
    }


    private Mono<Void> updateMetric(ObjectId orgId, ObjectId eventId, String field, int delta) {
        Criteria criteria = new Criteria();
        criteria.and("org_id").is(orgId);
        if (eventId != null) {
            criteria.and("event_id").is(eventId);
        }
        criteria.and("subscription_active").is(true);
        Query query = new Query(criteria);
        Update update = new Update()
                .inc(field, delta)
                .set("updated_at", LocalDateTime.now());

        return mongoTemplate.upsert(query, update, OrgUsageMetrics.class).then();
    }
}
