package com.bandhanbook.app.repository;

import com.bandhanbook.app.model.MatrimonyCandidate;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface MatrimonyRepository extends ReactiveMongoRepository<MatrimonyCandidate, String> {
    Mono<MatrimonyCandidate> findByUserId(String user_id);
}
