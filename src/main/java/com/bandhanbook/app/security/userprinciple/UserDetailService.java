package com.bandhanbook.app.security.userprinciple;

import com.bandhanbook.app.exception.EmailNotFoundException;
import com.bandhanbook.app.exception.PhoneNumberNotFoundException;
import com.bandhanbook.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static com.bandhanbook.app.utilities.ErrorResponseMessages.INVALID_CREDENTIALS;

@Service
public class UserDetailService implements ReactiveUserDetailsService {
    @Autowired
    private UserRepository userRepository;

    @Override
    public Mono<UserDetails> findByUsername(String userName) {
        // Check if input is email
        if (userName.contains("@")) {
            return userRepository.findByEmail(userName)
                    .switchIfEmpty(Mono.error(new EmailNotFoundException(INVALID_CREDENTIALS)))
                    .map(UserPrinciple::new);
        }
        return userRepository.findByPhoneNumber(userName).switchIfEmpty(Mono.error(new PhoneNumberNotFoundException(INVALID_CREDENTIALS)))
                .map(UserPrinciple::new);

    }

    /* @Override
     public Mono<UserDetails> findByUsername(String email) {
         return userRepository.findByEmail(email)
                 .switchIfEmpty(Mono.error(new RuntimeException("User not found")))
                 .map(UserPrinciple::new);
     }
 */
    public Mono<UserPrinciple> findByEmail(String email) {
        return findByUsername(email).cast(UserPrinciple.class);
    }
}
