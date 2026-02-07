package com.bandhanbook.app.repository;

import com.bandhanbook.app.model.Users;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface UserRepository extends ReactiveMongoRepository<Users, ObjectId> {

   /* @Query(value = "{ 'phone_number': ?0, 'role' : ?=1 }", exists = true)

    Optional<Users> findByPhoneNumber(String phone_number);

    */

    // Mono<Users> getUserByPhoneNumberAndRole(String phone_number, String role);
    Mono<Boolean> existsByEmailAndRolesContainingAndIdNot(String email, String role, ObjectId id);

    Mono<Users> findByEmail(String email);

    Mono<Boolean> existsByPhoneNumber(String phone_number);

    Mono<Boolean> existsByEmail(String email);

    Mono<Users> findByPhoneNumber(String phone_number);

    Mono<Users> findByPhoneNumberOrEmail(String phone_number, String email);

    Mono<Users> findByPhoneNumberAndRolesContaining(String email, String role);

    @Query("{ '_id': ?0, 'deleted_at': null }")
    @Update("{ '$set': { 'deleted_at': ?1, 'token': null } }")
    Mono<Long> deactivateUser(ObjectId userId, LocalDateTime deletedAt);
}
