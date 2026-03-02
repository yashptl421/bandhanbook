package com.bandhanbook.app.repository;

import com.bandhanbook.app.model.MatrimonyCandidate;
import com.bandhanbook.app.model.constants.ProfileStatus;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
public interface MatrimonyRepository extends ReactiveMongoRepository<MatrimonyCandidate, ObjectId> {
    Mono<MatrimonyCandidate> findByUserId(ObjectId user_id);
    @Query("{ 'user_id': ?0 }")
    @Update("{ '$set': { 'status': ?1 } }")
    Mono<Long> updateStatusByUserId(ObjectId userId, ProfileStatus status);
    @Query("{ '_id': { $in: ?0 }, 'privacy_settings.hide_profile': false }")
    Flux<MatrimonyCandidate> findVisibleFavorites(List<ObjectId> ids);
}
