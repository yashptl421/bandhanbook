package com.bandhanbook.app.service;

import com.bandhanbook.app.exception.RecordNotFoundException;
import com.bandhanbook.app.model.OrgSubscriptions;
import com.bandhanbook.app.model.PricingPlans;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.payload.request.BuySubscriptionRequest;
import com.bandhanbook.app.repository.OrgSubscriptionsRepository;
import com.bandhanbook.app.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.LocalDate;
import java.util.List;

import static com.bandhanbook.app.utilities.ErrorResponseMessages.DATA_NOT_FOUND;
import static com.bandhanbook.app.utilities.SuccessResponseMessages.SUBSCRIPTION_PURCHASED;

@Service
@RequiredArgsConstructor
public class OrgSubscriptionService {

    private final OrgSubscriptionsRepository  repository;
    private final OrganizationRepository organizationRepository;
    private final PricingPlanService pricingPlanService;

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
   /* public Mono<Tuple2<Long, List<OrgSubscriptions>>> list(
            Users authUser,
            String organizationId
    ) {
        Flux<OrgSubscriptions> flux;

        if (authUser.isOrganization()) {

            return organizationRepository.findByUserId(authUser.getId()).flatMap(org-> org.getId())
        } else if (authUser.isSuperUser() && organizationId != null) {
            flux = repository.findByOrgId(new ObjectId(organizationId));
        } else {
            flux = repository.findAll();
        }

        return flux.collectList()
                .map(list -> Tuples.of((long) list.size(), list));
    }*/
}
