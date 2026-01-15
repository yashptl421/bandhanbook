package com.bandhanbook.app.repository;

import com.bandhanbook.app.model.Advertisement;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
public interface AdvertisementRepository extends ReactiveMongoRepository<Advertisement, ObjectId> {
    Flux<Advertisement> findByEventIdIn(List<ObjectId> eventIds);
    Mono<Long> countByEventIdIn(List<ObjectId> eventId);
    Mono<Long> countByEventIdInAndActiveIsTrue(List<ObjectId> eventId, Boolean active);
}
