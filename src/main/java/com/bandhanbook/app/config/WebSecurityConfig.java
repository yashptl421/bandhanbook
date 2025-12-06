package com.bandhanbook.app.config;

import com.bandhanbook.app.model.constants.RoleNames;
import com.bandhanbook.app.security.jwt.JwtAuthenticationManager;
import com.bandhanbook.app.security.jwt.JwtSecurityContextRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

@Configuration
@EnableWebFluxSecurity
public class WebSecurityConfig {
    private final JwtAuthenticationManager authManager;
    private final JwtSecurityContextRepository contextRepository;
    private final JwtAuthenticationWebFilter jwtFilter;

    public WebSecurityConfig(JwtAuthenticationManager authManager,
                             JwtSecurityContextRepository contextRepository, JwtAuthenticationWebFilter jwtFilter,CorsConfigurationSource corsConfigurationSource) {
        this.authManager = authManager;
        this.contextRepository = contextRepository;
        this.jwtFilter = jwtFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }
    private final CorsConfigurationSource corsConfigurationSource;


    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authenticationManager(authManager)
                .securityContextRepository(contextRepository)

                .authorizeExchange(auth -> auth
                        .pathMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/webjars/**",
                                "/swagger-resources/**",
                                "/auth/**").permitAll()
                        .pathMatchers("/swagger-ui/index.html/*", " /swagger-ui/**").permitAll()
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers("/event/**").hasAnyRole(RoleNames.SuperUser.name(),RoleNames.Organization.name())
                        .pathMatchers("/organization/**").hasRole(RoleNames.SuperUser.name())
                        .pathMatchers("/agent/**").hasAnyRole(RoleNames.SuperUser.name(),RoleNames.Organization.name())
                        .pathMatchers("/user/**").hasAnyRole(RoleNames.Organization.name(), RoleNames.Candidate.name(), RoleNames.SuperUser.name())
                        .anyExchange().authenticated()
                ).addFilterAt(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
