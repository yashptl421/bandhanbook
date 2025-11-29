package com.bandhanbook.app.repository;

import com.bandhanbook.app.model.Events;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface EventsRepository extends ReactiveMongoRepository<Events, String> {
}
