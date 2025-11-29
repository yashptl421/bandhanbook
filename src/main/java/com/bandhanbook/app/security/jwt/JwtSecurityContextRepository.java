package com.bandhanbook.app.security.jwt;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtSecurityContextRepository implements ServerSecurityContextRepository {

    private final JwtAuthenticationManager authManager;

    public JwtSecurityContextRepository(JwtAuthenticationManager authManager) {
        this.authManager = authManager;
    }

    @Override
    public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
        return Mono.empty();
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {

        String header = exchange.getRequest().getHeaders().getFirst("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            Authentication authToken =
                    new UsernamePasswordAuthenticationToken(token, token);

            return authManager.authenticate(authToken).map(authentication -> new SecurityContextImpl());
        }
        return Mono.empty();
    }
}