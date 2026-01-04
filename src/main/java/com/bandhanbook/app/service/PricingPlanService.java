package com.bandhanbook.app.service;

import com.bandhanbook.app.exception.RecordNotFoundException;
import com.bandhanbook.app.model.PricingPlans;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static com.bandhanbook.app.utilities.ErrorResponseMessages.PLAN_NOT_FOUND;

@Service
@AllArgsConstructor
public class PricingPlanService {
    private List<PricingPlans> cachedPlans = null;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ResourceLoader resourceLoader;

    public PricingPlans getPlanById(String planId) {
        return cachedPlans.stream()
                .filter(p -> p.getId().equals(planId))
                .findFirst()
                .orElseThrow(() -> new RecordNotFoundException(PLAN_NOT_FOUND));
    }

   /* private Mono<PricingPlans> getPlanById(String id) {
        return getPricingPlans()
                .flatMapMany(Flux::fromIterable)
                .filter(plan -> plan.getId().equals(id))
                .next()                               // returns Mono<PricingPlan>
                .switchIfEmpty(Mono.error(new RecordNotFoundException(PLAN_NOT_FOUND)));
    }*/

    public Mono<List<PricingPlans>> getPricingPlans() {
        if (cachedPlans != null) {
            return Mono.just(cachedPlans);
        }
        return Mono.fromCallable(() -> {
                    Resource resource =
                            resourceLoader.getResource("classpath:Json/pricingPlans.json");

                    try (InputStream inputStream = resource.getInputStream()) {
                        return Arrays.asList(objectMapper.readValue(inputStream, PricingPlans[].class));
                    }
                })
                .doOnNext(list -> cachedPlans = list)
                .subscribeOn(Schedulers.boundedElastic());
    }
}
