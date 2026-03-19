package com.bandhanbook.app.service;

import com.bandhanbook.app.config.MessageUtil;
import com.bandhanbook.app.exception.ValidationExceptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class LimitEnforcementComponent {

    private final OrgSubscriptionService limitService;
    private final UsageMetricsService usageService;
    private final MessageUtil messageUtil;

    public Mono<Void> checkUserLimit(ObjectId eventId) {
        return limitService.getMergedLimits(null, eventId)
                .flatMap(limits ->
                        usageService.getUsageMetrics(null, eventId)
                                .flatMap(usage -> {
                                    if (usage.getCurrentUsers() >= limits.getMaxUsers()) {
                                        return Mono.error(new ValidationExceptions(messageUtil.get("candidate.limit.exceed")));
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
                                        return Mono.error(new ValidationExceptions(messageUtil.get("agent.limit.exceed")));
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
                                        return Mono.error(new ValidationExceptions(messageUtil.get("advertisement.limit.exceed")));
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
                                        return Mono.error(new ValidationExceptions(messageUtil.get("banner.limit.exceed")));
                                    }
                                    return Mono.empty();
                                })
                );
    }
}