package com.bandhanbook.app.repository;

import com.bandhanbook.app.model.Organization;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface OrganizationRepository extends ReactiveMongoRepository<Organization, String> {
}
