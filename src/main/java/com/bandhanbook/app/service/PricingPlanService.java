package com.bandhanbook.app.service;

import com.bandhanbook.app.config.MessageUtil;
import com.bandhanbook.app.exception.RecordNotFoundException;
import com.bandhanbook.app.exception.ValidationExceptions;
import com.bandhanbook.app.model.AddonCharges;
import com.bandhanbook.app.model.PricingPlans;
import com.bandhanbook.app.model.SubscriptionLimits;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.payload.request.PricingPlanRequest;
import com.bandhanbook.app.payload.response.PricingPlanResponse;
import com.bandhanbook.app.repository.PricingPlanRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PricingPlanService {
    private final PricingPlanRepository repository;
    private final MessageUtil messageUtil;

    @Value("${max.candidate}")
    private double maxCandidateCharges;
    @Value("${max.agent}")
    private double maxAgentCharges;
    @Value("${max.banner}")
    private double maxBannerCharges;
    @Value("${max.advertisement}")
    private double maxAdvertisementCharges;

    public Mono<PricingPlanResponse> create(PricingPlanRequest request) {

        PricingPlans plan = PricingPlans.builder()
                .name(request.getName())
                .price(request.getPrice())
                .billingCycle(request.getBillingCycle())
                .limits(buildLimits(request))
                .features(request.getFeatures())
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        return repository.save(plan).map(this::toResponse);
    }

    public Flux<PricingPlanResponse> getActivePlans() {
        return repository.findByIsActiveTrue()
                .map(this::toResponse);
    }

    public Flux<PricingPlanResponse> getAllPlans() {
        return repository.findAll()
                .map(this::toResponse);
    }

    public Mono<PricingPlans> getPlanById(ObjectId id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new RecordNotFoundException(messageUtil.get("record.not.found"))));
    }

    public Mono<PricingPlanResponse> update(ObjectId id, PricingPlanRequest request) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new RecordNotFoundException(messageUtil.get("record.not.found"))))
                .flatMap(plan -> {
                    plan.setName(request.getName());
                    plan.setPrice(request.getPrice());
                    plan.setBillingCycle(request.getBillingCycle());
                    plan.setLimits(buildLimits(request));
                    plan.setFeatures(request.getFeatures());
                    return repository.save(plan);
                })
                .map(this::toResponse);
    }

    public Mono<Void> updateStatus(ObjectId id, boolean active) {
        return repository.findById(id)
                .flatMap(plan -> {
                    plan.setActive(active);
                    return repository.save(plan);
                })
                .then();
    }

    public Mono<AddonCharges> getAddonPricing(Users authUser) {
        if (!authUser.isSuperUser() && !authUser.isOrganization()) {
            return Mono.error(new ValidationExceptions(messageUtil.get("record.not.found")));
        }
        return Mono.just(AddonCharges.builder()
                .agentCharges(maxAgentCharges)
                .bannerCharges(maxBannerCharges)
                .candidateCharges(maxCandidateCharges)
                .advertisementsCharges(maxAdvertisementCharges).build());
    }

    private PricingPlans.Limits buildLimits(PricingPlanRequest request) {
        PricingPlans.Limits limits = new PricingPlans.Limits();
        limits.setMaxUsers(request.getMaxUsers());
        limits.setMaxAgents(request.getMaxAgents());
        limits.setMaxBanners(request.getMaxBanners());
        limits.setMaxAdvertisements(request.getMaxAdvertisements());
        return limits;
    }

    private PricingPlanResponse toResponse(PricingPlans plan) {
        return PricingPlanResponse.builder()
                .id(plan.getId().toHexString())
                .name(plan.getName())
                .price(plan.getPrice())
                .billingCycle(plan.getBillingCycle())
                .maxUsers(plan.getLimits().getMaxUsers())
                .maxAgents(plan.getLimits().getMaxAgents())
                .maxBanners(plan.getLimits().getMaxBanners())
                .maxAdvertisements(plan.getLimits().getMaxAdvertisements())
                .features(plan.getFeatures())
                .active(plan.isActive())
                .build();
    }
    public double getAddonTotalChanges(SubscriptionLimits reqLimits) {
        return reqLimits.getMaxUsers() * maxCandidateCharges +
                reqLimits.getMaxAgents() * maxAgentCharges +
                reqLimits.getMaxAgents() * maxAgentCharges +
                reqLimits.getMaxAdvertisements() * maxAdvertisementCharges;
    }
}
