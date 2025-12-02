package com.bandhanbook.app.config;

import com.bandhanbook.app.security.jwt.JwtService;
import com.bandhanbook.app.security.userprinciple.UserDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationWebFilter implements WebFilter {

    private final JwtService jwtService;
    private final UserDetailService userService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        String token = authHeader.substring(7);
        if (!jwtService.validateToken(token)) {
            return chain.filter(exchange); // or return unauthorized
        }

        return jwtService.extractUsername(token)
                .flatMap(userService::findByUsername)
                .flatMap(user -> {
                    List<String> roles = jwtService.getRoles(token);

                    var authorities = roles.stream()
                            .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                            .toList();
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    user, null, authorities);

                    SecurityContext context = new SecurityContextImpl(auth);

                    return chain.filter(exchange)
                            .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(context)));
                })
                .onErrorResume(e -> chain.filter(exchange));
    }
}