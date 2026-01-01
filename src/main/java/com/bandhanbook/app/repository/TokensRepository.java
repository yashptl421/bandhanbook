package com.bandhanbook.app.repository;

import com.bandhanbook.app.model.Token;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface TokensRepository extends ReactiveMongoRepository<Token, String> {

    Mono<Token> findByPhoneNumberAndRoleAndOtp(String phone_number, String role, String otp);

    Mono<Token> findByPhoneNumberAndRole(String phone_number, String role);

    Mono<Boolean> deleteByPhoneNumber(String phone_number);

    @Query("{ '_id': ?0 }")
    @Update("{ '$inc': { 'failed_attempts': 1 } }")
    Mono<Void> incrementFailedAttempts(ObjectId id);

}
