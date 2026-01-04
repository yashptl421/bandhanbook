package com.bandhanbook.app.service;

import com.bandhanbook.app.exception.RecordNotFoundException;
import com.bandhanbook.app.model.OrgSubscriptions;
import com.bandhanbook.app.model.PricingPlans;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.payload.request.BuySubscriptionRequest;
import com.bandhanbook.app.payload.response.SubscriptionResponse;
import com.bandhanbook.app.repository.OrgSubscriptionsRepository;
import com.bandhanbook.app.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

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

        PricingPlans plan = pricingPlanService.getPlanById(req.getPlanId());

        LocalDate start = LocalDate.parse(req.getPlanStartDate());
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

        return repository.save(subscription)
                .thenReturn(SUBSCRIPTION_PURCHASED);
    }

    public Mono<OrgSubscriptions> show(String id) {
        return repository.findById(new ObjectId(id))
                .switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND)));
    }

    public Mono<Tuple2<Long, List<SubscriptionResponse>>> list(Users authUser, String organizationId) {
        Flux<OrgSubscriptions> flux;

        if (authUser.isOrganization()) {
            flux = organizationRepository.findByUserId(authUser.getId()).flatMap(org ->
                    repository.findByOrgId(org.getId())).flux();
        } else if (authUser.isSuperUser() && organizationId != null && !organizationId.isEmpty()) {
            flux = repository.findByOrgId(new ObjectId(organizationId)).flux();
        } else {
            flux = repository.findAll();
        }

        return flux.collectList()
                .map(list -> Tuples.of((long) list.size(), list.stream().map(res -> modelMapper.map(res, SubscriptionResponse.class)).toList()));
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
/*    public Mono<OrgSubscriptions> getActiveSubscription(Users authUser) {

        Mono<ObjectId> orgIdMono;

        if (authUser.isOrganization()) {
            orgIdMono = Mono.just(authUser.getOrganizationId());
        } else if (authUser.isAgent()) {
            orgIdMono = Mono.just(authUser.getAgentOrganizationId());
        } else if (authUser.isCandidate()) {
            orgIdMono = Mono.just(authUser.getCandidateOrganizationId());
        } else {
            return Mono.empty();
        }

        return orgIdMono.flatMap(id ->
                repository.findByOrgIdAndActive(id, true)
        );
    }*/
}
