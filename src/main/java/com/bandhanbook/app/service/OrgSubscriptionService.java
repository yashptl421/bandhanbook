package com.bandhanbook.app.service;

import com.bandhanbook.app.exception.RecordNotFoundException;
import com.bandhanbook.app.exception.UnAuthorizedException;
import com.bandhanbook.app.exception.ValidationExceptions;
import com.bandhanbook.app.model.*;
import com.bandhanbook.app.model.constants.AddOnStatus;
import com.bandhanbook.app.payload.request.BuySubscriptionRequest;
import com.bandhanbook.app.payload.request.SubscriptionAddonRequest;
import com.bandhanbook.app.payload.response.OrganizationResponse;
import com.bandhanbook.app.payload.response.SubscriptionAddonResponse;
import com.bandhanbook.app.payload.response.SubscriptionResponse;
import com.bandhanbook.app.payload.response.base.ApiResponse;
import com.bandhanbook.app.repository.EventsRepository;
import com.bandhanbook.app.repository.OrgSubscriptionAddonRepository;
import com.bandhanbook.app.repository.OrgSubscriptionsRepository;
import com.bandhanbook.app.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

import static com.bandhanbook.app.utilities.ErrorResponseMessages.*;
import static com.bandhanbook.app.utilities.SuccessResponseMessages.*;

@Service
@RequiredArgsConstructor
public class OrgSubscriptionService {

    private final OrgSubscriptionsRepository repository;
    private final OrganizationRepository organizationRepository;
    private final PricingPlanService pricingPlanService;
    private final OrgSubscriptionAddonRepository addonRepository;
    private final EventsRepository eventsRepository;
    private final ModelMapper modelMapper;

    public Mono<String> buySubscription(BuySubscriptionRequest req) {

        Mono<PricingPlans> planMono = pricingPlanService.getPlanById(req.getPlanId());

        LocalDate start = req.getPlanStartDate().toLocalDate();
        return planMono.flatMap(plan -> {
            OrgSubscriptions subscription = OrgSubscriptions.builder()
                    .orgId(new ObjectId(req.getOrgId()))
                    .planId(req.getPlanId())
                    .maxAgents(plan.getMaxAgents())
                    .maxUsers(plan.getMaxUsers())
                    .active(false)
                    .registrationPeriod(start.minusDays(plan.getRegistrationPeriod()).toString())
                    .startDate(start.toString())
                    .endDate(start.plusYears(1).toString())
                    .build();
            return repository.save(subscription);

        }).thenReturn(SUBSCRIPTION_PURCHASED);
    }

    public Mono<SubscriptionResponse> show(String id) {
        return repository.findById(new ObjectId(id)).flatMap(sub -> {
            SubscriptionResponse res =
                    modelMapper.map(sub, SubscriptionResponse.class);
            return eventsRepository.findById(sub.getEventId()).flatMap( event ->
             organizationRepository.findById(sub.getOrgId())
                    .flatMap(org -> {
                        res.setEventName(event.getName());
                        res.setOrganizationDetails(modelMapper.map(org, OrganizationResponse.class));
                        return pricingPlanService.getPlanById(sub.getPlanId()).map(plan -> {
                            res.setPlanName(plan.getName());
                            res.setPlanPrice(plan.getPrice());
                            return res;
                        });
                    }));
        }).switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND)));
    }

    public Mono<List<SubscriptionResponse>> list(Users authUser, String orgId) {

        Mono<Organization> orgMono =
                (authUser.isOrganization() || orgId != null)
                        ? (orgId != null
                        ? organizationRepository.findById(new ObjectId(orgId))
                        : organizationRepository.findByUserId(authUser.getId()))
                        : Mono.empty();

        Flux<OrgSubscriptions> subscriptionFlux =
                (authUser.isOrganization() || orgId != null)
                        ? orgMono.flatMapMany(org -> repository.findByOrgId(org.getId()))
                        : repository.findAll();

        return subscriptionFlux.flatMap(sub -> {

            Mono<Organization> organizationMono =
                    (authUser.isOrganization() || orgId != null)
                            ? orgMono
                            : organizationRepository.findById(sub.getOrgId());

            Mono<PricingPlans> planMono = pricingPlanService.getPlanById(sub.getPlanId());

            // 🔹 Fetch event name
            Mono<String> eventNameMono =
                    sub.getEventId() != null
                            ? eventsRepository.findById(sub.getEventId())
                            .map(Events::getName)
                            : Mono.just("");

            return Mono.zip(organizationMono, planMono, eventNameMono)
                    .map(tuple -> {

                        Organization org = tuple.getT1();
                        PricingPlans plan = tuple.getT2();
                        String eventName = tuple.getT3();

                        SubscriptionResponse res =
                                modelMapper.map(sub, SubscriptionResponse.class);

                        res.setOrganizationDetails(
                                modelMapper.map(org, OrganizationResponse.class)
                        );
                        res.setPlanName(plan.getName());
                        res.setPlanPrice(plan.getPrice());
                        res.setEventName(eventName);   // ✅ Added

                        return res;
                    });

        }).collectList();
    }

    public Mono<String> updateStatus(String id, boolean status) {
        return repository.findById(new ObjectId(id))
                .switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND)))
                .flatMap(sub -> {
                    sub.setActive(status);
                    return repository.save(sub);
                })
                .thenReturn(SUBSCRIPTION_UPDATED);
    }

    public Mono<OrgSubscriptions> getActiveSubscription(ObjectId orgId) {
        return repository
                .findByOrgIdAndActive(orgId, true)
                .switchIfEmpty(Mono.error(
                        new ValidationExceptions(SUBSCRIPTION_NOT_FOUND)
                ));
    }

    public Mono<String> buyAddon(SubscriptionAddonRequest request) {
        return repository.findById(new ObjectId(request.getSubscriptionId()))
                .switchIfEmpty(Mono.error(new RecordNotFoundException(SUBSCRIPTION_NOT_FOUND)))
                .flatMap(sub -> {
                    OrgSubscriptionAddon addon =
                            modelMapper.map(request, OrgSubscriptionAddon.class);
                    addon.setSubscriptionId(sub.getId());
                    addon.setStatus(AddOnStatus.PENDING);
                    addon.setOrgId(sub.getOrgId());
                    return addonRepository.save(addon);
                })
                .thenReturn(SUBSCRIPTION_ADDON_PURCHASED);
    }

    public Mono<ApiResponse<List<SubscriptionAddonResponse>>> listAddons(Users authUser, String subscriptionId, int page, int limit) {
        if (subscriptionId == null || subscriptionId.isBlank()) {
            return Mono.error(new ValidationExceptions(SUBSCRIPTION_NOT_FOUND));
        }
        Pageable pageable = PageRequest.of(page - 1, limit);
        ObjectId subId = new ObjectId(subscriptionId);

        Flux<SubscriptionAddonResponse> dataFlux =
                addonRepository.findBySubscriptionId(subId, pageable)
                        .map(this::mapToResponse);
        Mono<Long> countMono =
                addonRepository.countBySubscriptionId(subId);

        return Mono.zip(dataFlux.collectList(), countMono)
                .map(tuple -> ApiResponse.<List<SubscriptionAddonResponse>>builder()
                        .data(tuple.getT1())
                        .meta(ApiResponse.Meta.builder()
                                .totalRecords(tuple.getT2())
                                .page(page)
                                .limit(limit)
                                .totalPages((int) Math.ceil((double) tuple.getT2() / limit))
                                .build())
                        .message(DATA_FOUND)
                        .status(HttpStatus.OK.value())
                        .build());
    }

    public Mono<String> updateAddonStatus(String id, AddOnStatus status, Users authUser) {
        if (!authUser.isSuperUser()) {
            return Mono.error(new UnAuthorizedException(UNAUTHORIZED_ACCESS));
        }
        return addonRepository.findById(new ObjectId(id))
                .switchIfEmpty(Mono.error(new RecordNotFoundException(ADDON_NOT_FOUND)))
                .flatMap(addon -> {
                    addon.setStatus(status);
                    return addonRepository.save(addon);
                })
                .thenReturn(SUBSCRIPTION_ADDON_UPDATED);
    }

    public Mono<SubscriptionLimits> getMergedLimits(ObjectId orgId, ObjectId eventId) {
        if (orgId == null && eventId == null) {
            return Mono.error(new ValidationExceptions(SUBSCRIPTION_NOT_FOUND));
        }
        Mono<OrgSubscriptions> subscriptionMono = null;
        if (orgId == null) {
            subscriptionMono = repository.findByEventIdAndActive(eventId, true)
                    .switchIfEmpty(Mono.error(new RecordNotFoundException(SUBSCRIPTION_NOT_FOUND)))
                    .flatMap(Mono::just);
        } else {
            subscriptionMono = repository.findByOrgIdAndActive(orgId, true)
                    .switchIfEmpty(Mono.error(new RecordNotFoundException(SUBSCRIPTION_NOT_FOUND)))
                    .flatMap(Mono::just);
        }
        return subscriptionMono
                .flatMap(sub ->
                        addonRepository
                                .findBySubscriptionIdAndStatus(sub.getId(),AddOnStatus.APPROVED)
                                .collectList()
                                .map(addons -> {

                                    int maxUsers = sub.getMaxUsers();
                                    int maxAgents = sub.getMaxAgents();
                                    int maxBanners = sub.getMaxBanners();
                                    int maxAdvertisements = sub.getMaxAdvertisements();


                                    for (OrgSubscriptionAddon addon : addons) {
                                        maxUsers += addon.getMaxUsers();
                                        maxAgents += addon.getMaxAgents();
                                        maxBanners += addon.getMaxBanners();
                                        maxAdvertisements += addon.getMaxAdvertisements();
                                    }
                                    return SubscriptionLimits.builder()
                                            .maxUsers(maxUsers)
                                            .maxAgents(maxAgents)
                                            .maxBanners(maxBanners)
                                            .maxAdvertisements(maxAdvertisements)
                                            .build();
                                })
                ).switchIfEmpty(Mono.error(new ValidationExceptions(SUBSCRIPTION_INACTIVE)));
    }

    private SubscriptionAddonResponse mapToResponse(OrgSubscriptionAddon addon) {
        return SubscriptionAddonResponse.builder()
                .id(addon.getId().toHexString())
                .orgId(addon.getOrgId().toHexString())
                .subscriptionId(addon.getSubscriptionId().toHexString())
                .maxAgents(addon.getMaxAgents())
                .maxUsers(addon.getMaxUsers())
                .maxBanners(addon.getMaxBanners())
                .maxAdvertisements(addon.getMaxAdvertisements())
                .price(addon.getPrice())
                .status(addon.getStatus())
                .createdAt(addon.getCreatedAt())
                .updatedAt(addon.getUpdatedAt())
                .build();
    }
}
