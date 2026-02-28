package com.bandhanbook.app.service;

import com.bandhanbook.app.exception.UnAuthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static com.bandhanbook.app.utilities.ErrorResponseMessages.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class LimitEnforcementComponent {

    private final OrgSubscriptionService limitService;
    private final UsageMetricsService usageService;

    public Mono<Void> checkUserLimit(ObjectId eventId) {
        return limitService.getMergedLimits(null, eventId)
                .flatMap(limits ->
                        usageService.getUsageMetrics(null, eventId)
                                .flatMap(usage -> {
                                    if (usage.getCurrentUsers() >= limits.getMaxUsers()) {
                                        return Mono.error(new UnAuthorizedException(CANDIDATE_LIMIT_EXCEED));
                                    }
                                    return Mono.empty();
                                })
                );
    }

    public Mono<Void> checkAgentLimit(ObjectId orgId) {
        return limitService.getMergedLimits(orgId, null) // fails if inactive
                .flatMap(limits ->
                        usageService.getUsageMetrics(orgId, null)
                                .flatMap(usage -> {
                                    if (usage.getCurrentAgents() >= limits.getMaxAgents()) {
                                        return Mono.error(new UnAuthorizedException(AGENT_LIMIT_EXCEED));
                                    }
                                    return Mono.empty();
                                })
                );
    }

    public Mono<Void> checkAdvertisementLimit(ObjectId eventId, int newAdsCount) {
        return limitService.getMergedLimits(null, eventId)
                .flatMap(limits ->
                        usageService.getUsageMetrics(null, eventId)
                                .flatMap(usage -> {
                                    int total = usage.getCurrentAdvertisements() + newAdsCount;
                                    if (total > limits.getMaxAdvertisements()) {
                                        return Mono.error(new UnAuthorizedException(ADVERTISEMENT_LIMIT_EXCEED));
                                    }
                                    return Mono.empty();
                                })
                );
    }

    public Mono<Void> checkBannerLimit(ObjectId orgId) {
        return limitService.getMergedLimits(orgId, null) // fails if inactive
                .flatMap(limits ->
                        usageService.getUsageMetrics(orgId, null)
                                .flatMap(usage -> {
                                    if (usage.getCurrentBanners() >= limits.getMaxBanners()) {
                                        return Mono.error(new UnAuthorizedException(BANNER_LIMIT_EXCEED));
                                    }
                                    return Mono.empty();
                                })
                );
    }
}