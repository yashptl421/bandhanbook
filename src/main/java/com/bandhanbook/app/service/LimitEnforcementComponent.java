package com.bandhanbook.app.service;

import com.bandhanbook.app.exception.UnAuthorizedException;
import com.bandhanbook.app.model.OrgUsageMetrics;
import com.bandhanbook.app.model.SubscriptionLimits;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static com.bandhanbook.app.utilities.ErrorResponseMessages.AGENT_LIMIT_EXCEED;
import static com.bandhanbook.app.utilities.ErrorResponseMessages.CANDIDATE_LIMIT_EXCEED;

@Component
@RequiredArgsConstructor
public class LimitEnforcementComponent {

    private final OrgSubscriptionService limitService;
    private final UsageMetricsService usageService;

    public Mono<Void> checkUserLimit(ObjectId eventId) {
        return Mono.zip(
                limitService.getMergedLimits(null, eventId),
                usageService.getUsageMetrics(null, eventId)
        ).flatMap(tuple -> {
            SubscriptionLimits limits = tuple.getT1();
            OrgUsageMetrics usage = tuple.getT2();
            if (usage.getCurrentUsers() >= limits.getMaxUsers()) {
                return Mono.error(new UnAuthorizedException(CANDIDATE_LIMIT_EXCEED));
            }
            return Mono.empty();
        });
    }

    public Mono<Void> checkAgentLimit(ObjectId orgId) {
        return Mono.zip(
                limitService.getMergedLimits(orgId, null),
                usageService.getUsageMetrics(orgId, null)
        ).flatMap(tuple -> {
            SubscriptionLimits limits = tuple.getT1();
            OrgUsageMetrics usage = tuple.getT2();

            if (usage.getCurrentAgents() >= limits.getMaxAgents()) {
                return Mono.error(new UnAuthorizedException(AGENT_LIMIT_EXCEED));
            }
            return Mono.empty();
        });
    }
}