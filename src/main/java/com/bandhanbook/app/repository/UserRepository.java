package com.bandhanbook.app.repository;

import com.bandhanbook.app.model.Users;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
}
