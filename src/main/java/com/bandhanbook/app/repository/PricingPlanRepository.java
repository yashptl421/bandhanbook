package com.bandhanbook.app.repository;

import com.bandhanbook.app.model.PricingPlans;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface PricingPlanRepository
        extends ReactiveMongoRepository<PricingPlans, ObjectId> {

    Flux<PricingPlans> findByIsActiveTrue();

}