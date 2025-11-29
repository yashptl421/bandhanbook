package com.bandhanbook.app.config;

import com.bandhanbook.app.model.constants.RoleNames;
import com.bandhanbook.app.security.jwt.JwtAuthenticationManager;
import com.bandhanbook.app.security.jwt.JwtSecurityContextRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class WebSecurityConfig {
    private final JwtAuthenticationManager authManager;
    private final JwtSecurityContextRepository contextRepository;
    private final JwtAuthenticationWebFilter jwtFilter;

    public WebSecurityConfig(JwtAuthenticationManager authManager,
                             JwtSecurityContextRepository contextRepository, JwtAuthenticationWebFilter jwtFilter) {
        this.authManager = authManager;
        this.contextRepository = contextRepository;
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
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
                        .pathMatchers("/event/**").hasAnyRole(RoleNames.SuperUser.name(),RoleNames.Organization.name())
                        .pathMatchers("/Organization/**").hasRole(RoleNames.SuperUser.name())
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
  /*  private final UserDetailService userDetailService;
    private final JwtEntryPoint jwtEntryPoint;

    @Autowired
    public WebSecurityConfig(UserDetailService userDetailService, JwtEntryPoint jwtEntryPoint) {
        this.userDetailService = userDetailService;
        this.jwtEntryPoint = jwtEntryPoint;
    }

    @Bean
    public JwtTokenFilter jwtTokenFilter() {
        return new JwtTokenFilter();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new UserDetailsService();
    }

    public void configure(AuthenticationManagerBuilder authenticationManagerBuilder) throws Exception {
        authenticationManagerBuilder
                .userDetailsService(userDetailService)
                .passwordEncoder(passwordEncoder());
    }*/
/*
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(userDetailsService());
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return authenticationProvider;
    }*/

    /* @Bean
     protected SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
         httpSecurity
                 .csrf(AbstractHttpConfigure::disable)
                 .authorizeHttpRequests(auth ->
                         auth.requestMatchers("/api/organization/**").permitAll()
                                 .requestMatchers("/api/manager/token").permitAll()
                                 .requestMatchers("/api/manager/change-password").authenticated()
                                 .requestMatchers("/api/manager/delete/**").authenticated()
                                 .requestMatchers("/api/auth/logout").authenticated()
                                 .requestMatchers("/api/manager/user/**").permitAll()
                                 .requestMatchers("/v2/api-docs", "/swagger-ui/**", "/user-service/**", "/user-service/swagger-ui/** ", "/swagger-resources/**", "/webjars/**").permitAll()
                                 .anyRequest().authenticated())
                 .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
                // .exceptionHandling(exception -> exception.authenticationEntryPoint(jwtEntryPoint))
         //   .authenticationProvider(authenticationProvider())
         // .addFilterBefore(jwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

         return httpSecurity.build();
     }
 */
  /*  @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http,
                                                     ReactiveAuthenticationManager authenticationManager,
                                                     ServerAuthenticationConverter authenticationConverter) {
        AuthenticationWebFilter authenticationWebFilter = new AuthenticationWebFilter(authenticationManager);
        authenticationWebFilter.setServerAuthenticationConverter(authenticationConverter);

        return http
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.POST, "/login").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/organization/**").permitAll()
                        .anyExchange().authenticated()
                )
                //  .addFilterAt(authenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(ServerHttpSecurity.CorsSpec::disable)
                .build();
    }*/

 /*   @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.POST, "/**").permitAll()
                        .pathMatchers("/organization/**").permitAll()// Example: Allow public POST requests
                        .anyExchange().authenticated()
                )
                .csrf(csrfSpec -> csrfSpec.disable()) // Disable CSRF for the entire application (not recommended for web forms)
                // OR selectively disable CSRF for specific paths
                // .csrf(csrfSpec -> csrfSpec.csrfTokenRepository(new CookieServerCsrfTokenRepository())
                //     .requireCsrfProtectionMatcher(new NegatedServerWebExchangeMatcher(
                //         ServerWebExchangeMatchers.pathMatchers(HttpMethod.POST, "/api/no-csrf-endpoint", "/other/no-csrf/**")
                //     ))
                // )
                .build();
    }*/
}
