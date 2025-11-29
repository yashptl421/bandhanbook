package com.bandhanbook.app.repository;

import com.bandhanbook.app.model.OrgSubscriptions;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface OrgSubscriptionsRepository extends ReactiveMongoRepository<OrgSubscriptions, String> {
    public Mono<OrgSubscriptions> findByOrgId(String org_id);
}
