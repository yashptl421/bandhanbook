package com.bandhanbook.app.security.userprinciple;

import com.bandhanbook.app.config.MessageUtil;
import com.bandhanbook.app.exception.CommontException;
import com.bandhanbook.app.exception.EmailNotFoundException;
import com.bandhanbook.app.exception.PhoneNumberNotFoundException;
import com.bandhanbook.app.model.constants.RoleNames;
import com.bandhanbook.app.repository.UserRepository;
import com.bandhanbook.app.utilities.UtilityHelper;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserDetailService implements ReactiveUserDetailsService {
    private final UserRepository userRepository;
    private final MessageUtil messageUtil;

    @Override
    public Mono<UserDetails> findByUsername(String userName) {
        // Check if input is email
        return findUser(userName).map(users -> {
            if (!users.isAccountNonLocked()) {
                throw new CommontException(messageUtil.get("account.blocked"));
            }

            if (!users.isEnabled()) {
                throw new CommontException("Account has been removed, Please contact to agent or administrator to activate it.");
            }
            return users;
        });
    }

    private Mono<UserDetails> findUser(String userName) {

        if (userName.contains("@")) {
            return userRepository.findByEmail(userName)
                    .switchIfEmpty(Mono.error(new EmailNotFoundException(messageUtil.get("user.not.found"))))
                    .map(users -> new UserPrinciple(users, RoleNames.NA));
        }
        if (UtilityHelper.validPhoneNumber(userName)) {
            return userRepository.findByPhoneNumber(userName).switchIfEmpty(Mono.error(new PhoneNumberNotFoundException(messageUtil.get("user.not.found"))))
                    .map(users -> new UserPrinciple(users, RoleNames.NA));
        }
        return userRepository.findById(new ObjectId(userName)).switchIfEmpty(Mono.error(new UsernameNotFoundException(messageUtil.get("user.not.found"))))
                .map(users -> new UserPrinciple(users, RoleNames.NA));

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

    public Mono<UserPrinciple> findByPhoneNumber(String phoneNumber) {
        return findByUsername(phoneNumber).cast(UserPrinciple.class);
    }

    public Mono<UserPrinciple> findById(ObjectId id) {
        return findByUsername(id.toHexString()).cast(UserPrinciple.class);
    }
}
