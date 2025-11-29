package com.bandhanbook.app.security.jwt;

import com.bandhanbook.app.security.userprinciple.UserPrinciple;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {
    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret}")
    private String jwtSecret;
    @Value("${jwt.expiration}")
    private int jwtExpiration;
    @Value("${jwt.refreshExpiration}")
    private int jwtRefreshExpiration;

    /* public String generateToken(Users user) {
         logger.info("Token created for userId {}", user.getId());
         return Jwts.builder()
                 .subject(user.getId())
                 .claim("role", user.getRole())
                 .issuedAt(new Date())
                 .expiration(new Date(new Date().getTime() + jwtExpiration * 1000L))
                 .signWith(getSigningKey())
                 .compact();
     }*/
    public String generateToken(UserPrinciple user) {
        return Jwts.builder()
                        .subject(user.getUsername())
                        .claim("role", user.getUsers().getRole())
                        .issuedAt(new Date())
                        .expiration(new Date(new Date().getTime() + jwtExpiration * 1000L))
                        .signWith(getSigningKey())
                        .compact();

    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    // Validate & Parse
    public Claims parseToken(String token) {
        return (Claims) Jwts.parser().verifyWith(getSigningKey())              // NEW API
                .build().parse(token).getPayload();
    }

    public String extractUserId(String token) {
        return parseToken(token).getSubject();
    }

    public String extractRole(String token) {
        return parseToken(token).get("role", String.class);
    }

    public Mono<String> extractUsername(String token) {
        return Mono.fromCallable(() ->
                Jwts.parser()
                        .verifyWith(getSigningKey())
                        .build()
                        .parseSignedClaims(token)
                        .getPayload()
                        .getSubject()
        ).subscribeOn(Schedulers.boundedElastic());
    }
}