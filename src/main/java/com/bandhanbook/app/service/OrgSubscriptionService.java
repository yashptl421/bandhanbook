package com.bandhanbook.app.service;

import com.bandhanbook.app.exception.RecordNotFoundException;
import com.bandhanbook.app.exception.UnAuthorizedException;
import com.bandhanbook.app.exception.ValidationExceptions;
import com.bandhanbook.app.model.*;
import com.bandhanbook.app.payload.request.BuySubscriptionRequest;
import com.bandhanbook.app.payload.request.SubscriptionAddonRequest;
import com.bandhanbook.app.payload.response.OrganizationResponse;
import com.bandhanbook.app.payload.response.SubscriptionAddonResponse;
import com.bandhanbook.app.payload.response.SubscriptionResponse;
import com.bandhanbook.app.payload.response.base.ApiResponse;
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
            return organizationRepository.findById(sub.getOrgId())
                    .flatMap(org -> {
                        res.setOrganizationDetails(modelMapper.map(org, OrganizationResponse.class));
                        return pricingPlanService.getPlanById(sub.getPlanId()).map(plan -> {
                            res.setPlanName(plan.getName());
                            res.setPlanPrice(plan.getPrice());
                            return res;
                        });
                    });
        }).switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND)));
    }

    public Mono<List<SubscriptionResponse>> list(Users authUser, String orgId) {
        if (authUser.isOrganization() || orgId != null) {

            Mono<Organization> orgMono = orgId != null ? organizationRepository.findById(new ObjectId(orgId)) : organizationRepository.findByUserId(authUser.getId());

            return orgMono.flatMapMany(org ->
                    repository.findByOrgId(org.getId()).flatMap(sub -> {
                        SubscriptionResponse res =
                                modelMapper.map(sub, SubscriptionResponse.class);
                        pricingPlanService.getPlanById(sub.getPlanId()).map(plan -> {
                            res.setPlanName(plan.getName());
                            res.setPlanPrice(plan.getPrice());
                            return res;
                        });
                        res.setOrganizationDetails(modelMapper.map(org, OrganizationResponse.class));

                        return pricingPlanService.getPlanById(sub.getPlanId()).map(plan -> {
                            res.setPlanName(plan.getName());
                            res.setPlanPrice(plan.getPrice());
                            return res;
                        });
                    })).collectList();
        } else {
            return repository.findAll().flatMap(sub -> {
                SubscriptionResponse res =
                        modelMapper.map(sub, SubscriptionResponse.class);

                return organizationRepository.findById(sub.getOrgId())
                        .flatMap(org -> {
                            res.setOrganizationDetails(modelMapper.map(org, OrganizationResponse.class));
                            return pricingPlanService.getPlanById(sub.getPlanId()).map(plan -> {
                                res.setPlanName(plan.getName());
                                res.setPlanPrice(plan.getPrice());
                                return res;
                            });
                        });
            }).collectList();
        }
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
                    addon.setOrgId(sub.getOrgId());
                    return addonRepository.save(addon);
                })
                .thenReturn(SUBSCRIPTION_ADDON_PURCHASED);
    }

    public Mono<ApiResponse<List<SubscriptionAddonResponse>>> listAddons(Users authUser, String subscriptionId, int page, int limit) {
        if(subscriptionId==null || subscriptionId.isBlank()){
            return Mono.error(new ValidationExceptions(SUBSCRIPTION_NOT_FOUND));
        }
        Pageable pageable = PageRequest.of(page-1, limit);
        ObjectId subId = new ObjectId(subscriptionId);

        Flux<SubscriptionAddonResponse> dataFlux =
                addonRepository.findBySubscriptionIdAndActiveTrue(subId, pageable)
                        .map(this::mapToResponse);
        Mono<Long> countMono =
                addonRepository.countBySubscriptionIdAndActiveTrue(subId);

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

    public Mono<String> updateAddonStatus(String id, boolean status, Users authUser) {
        if(!authUser.isSuperUser()){
            return Mono.error(new UnAuthorizedException(UNAUTHORIZED_ACCESS));
        }
        return addonRepository.findById(new ObjectId(id))
                .switchIfEmpty(Mono.error(new RecordNotFoundException(ADDON_NOT_FOUND)))
                .flatMap(addon -> {
                    addon.setActive(status);
                    return addonRepository.save(addon);
                })
                .thenReturn(SUBSCRIPTION_ADDON_UPDATED);
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
                .active(addon.isActive())
                .createdAt(addon.getCreatedAt())
                .build();
    }
}
