package com.bandhanbook.app.repository;

import com.bandhanbook.app.model.UserTokens;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserTokensRepository extends MongoRepository<UserTokens, String> {


    UserTokens findByPhoneRoleAndOtp(String phone_number, String role, String otp);

    boolean deleteByPhoneNumber(String phone_number);

}
