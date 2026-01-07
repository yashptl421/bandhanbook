package com.bandhanbook.app.service;

import com.bandhanbook.app.exception.RecordNotFoundException;
import com.bandhanbook.app.model.OrgSubscriptions;
import com.bandhanbook.app.model.Organization;
import com.bandhanbook.app.model.PricingPlans;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.payload.request.BuySubscriptionRequest;
import com.bandhanbook.app.payload.response.OrganizationResponse;
import com.bandhanbook.app.payload.response.SubscriptionResponse;
import com.bandhanbook.app.repository.OrgSubscriptionsRepository;
import com.bandhanbook.app.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

import static com.bandhanbook.app.utilities.ErrorResponseMessages.DATA_NOT_FOUND;
import static com.bandhanbook.app.utilities.SuccessResponseMessages.SUBSCRIPTION_PURCHASED;
import static com.bandhanbook.app.utilities.SuccessResponseMessages.SUBSCRIPTION_UPDATED;

@Service
@RequiredArgsConstructor
public class OrgSubscriptionService {

    private final OrgSubscriptionsRepository repository;
    private final OrganizationRepository organizationRepository;
    private final PricingPlanService pricingPlanService;
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
                        new IllegalStateException("No active subscription found")
                ));
    }

   /* public Mono<OrgSubscriptions> getActiveSubscription(ObjectId orgId) {

        return repository.findByOrgIdAndActive(orgId, true);
    }*/
}
