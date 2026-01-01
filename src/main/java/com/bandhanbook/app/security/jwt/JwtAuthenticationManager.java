package com.bandhanbook.app.security.jwt;

import com.bandhanbook.app.model.constants.RoleNames;
import com.bandhanbook.app.security.userprinciple.UserDetailService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {

    private final JwtService jwtService;
    @Autowired
    private UserDetailService userDetailService;

    public JwtAuthenticationManager(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {

        String token = authentication.getCredentials().toString();

        String userId = jwtService.extractUserId(token);
        RoleNames activeRole = jwtService.getActiveRole(token);

        return userDetailService.findById(new ObjectId(userId))
                .map(user -> {
                    user.setActiveRole(activeRole);

                    return new UsernamePasswordAuthenticationToken(
                            user,
                            token,
                            user.getAuthorities()
                    );
                });
    }
}