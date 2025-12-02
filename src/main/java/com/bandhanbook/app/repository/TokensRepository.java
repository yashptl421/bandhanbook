package com.bandhanbook.app.repository;

import com.bandhanbook.app.model.Token;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface TokensRepository extends ReactiveMongoRepository<Token, String> {

    //Mono<Token> findByPhoneRoleAndOtp(String phone_number, String role, String otp);

    Mono<Token> findByPhoneNumberAndRole(String phone_number, String role);
    Mono<Boolean> deleteByPhoneNumber(String phone_number);

}
