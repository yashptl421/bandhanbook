package com.bandhanbook.app.service;

import com.bandhanbook.app.model.Token;
import com.bandhanbook.app.repository.TokensRepository;
import com.bandhanbook.app.security.userprinciple.UserDetailService;
import com.bandhanbook.app.utilities.UtilityHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

import static com.bandhanbook.app.utilities.ErrorResponseMessages.INVALID_OTP;
import static com.bandhanbook.app.utilities.SuccessResponseMessages.OTP_SENT;
import static com.bandhanbook.app.utilities.SuccessResponseMessages.OTP_VERIFIED;

@Service
public class OtpService {
    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    UserDetailService userDetailService;
    @Autowired
    TokensRepository tokensRepository;
    /* @Autowired
     private final SmsSender smsSender;
 */
    @Value("${otp.expiration}")
    private int otpExpiration;
    @Value("${otp.duration}")
    private int duration;
    @Value("${otp.windowDuration}")
    private int windowDurations;

    @Value("${otp.maxFailedAttempts}")
    private int maxFailedAttempts;
    @Value("${otp.maxRequestsPerWindow}")
    private int maxRequestsPerWindow;

    private final Duration otpCooldown = Duration.ofSeconds(duration); // min seconds between sends
    private final Duration windowDuration = Duration.ofHours(windowDurations); // window for maxRequests

    /**
     * Request (send) OTP.
     */
    public Mono<String> requestOtp(String phoneNumber, String role) {
        Instant now = Instant.now();
        log.info("otp request for phone {}", phoneNumber);
        return tokensRepository.findByPhoneNumberAndRole(phoneNumber, role)
                .flatMap(existing -> {
                    // cooldown check
                    if (existing.getLastSentAt() != null && existing.getLastSentAt().plus(otpCooldown).isAfter(now)) {
                        long wait = Duration.between(now, existing.getLastSentAt().plus(otpCooldown)).getSeconds();
                        return Mono.error(new IllegalStateException("Please wait " + wait + " seconds before requesting a new OTP"));
                    }

                    // sliding window check
                    if (existing.getWindowStart() == null || existing.getWindowStart().plus(windowDuration).isBefore(now)) {
                        existing.setWindowStart(now);
                        existing.setRequestCountInWindow(0);
                    }

                    if (existing.getRequestCountInWindow() >= maxRequestsPerWindow) {
                        return Mono.error(new IllegalStateException("Too many OTP requests. Try later."));
                    }

                    String otp = generateOtp();
                    //String otpHash = passwordEncoder.encode(otp);

                    //existing.setOtpHash(otpHash);
                    existing.setOtp(otp);
                    existing.setLastSentAt(now);
                    existing.setCreatedAt(now);
                    existing.setRequestCountInWindow(existing.getRequestCountInWindow() + 1);
                    existing.setFailedAttempts(0);

                    // save then send
                    return tokensRepository.save(existing).thenReturn(OTP_SENT);
                    /* .flatMap(saved -> smsSender.sendSms(phoneNumber, "Your OTP: " + otp).thenReturn(saved))*/

                })
                .switchIfEmpty(Mono.defer(() -> {
                    // new record
                    String otp = generateOtp();
                    //String otpHash = passwordEncoder.encode(otp);

                    Token token = Token.builder()
                            .phoneNumber(phoneNumber)
                            .role(role)
                            .otp(otp)
                            .lastSentAt(now)
                            .windowStart(now)
                            .requestCountInWindow(1)
                            .failedAttempts(0)
                            .createdAt(now)
                            .build();

                    return tokensRepository.save(token).thenReturn(OTP_SENT);
                    /* .flatMap(saved -> smsSender.sendSms(phoneNumber, "Your OTP: " + otp).thenReturn(saved))*/

                }));
    }

    /*public Mono<String> generateTokens(String phoneNumber, String role, String otpInput) {
        Instant now = Instant.now();
        log.info("Verify otp for phone {}", phoneNumber);
        verifyOtp(phoneNumber, role, otpInput)
                .then(userRepository.findByPhoneNumberAndRole(phoneNumber, role)
                        .flatMap(user -> {
                            // existing user -> build principal and issue JWT
                            UserPrincipal principal = UserPrincipal.builder()
                                    .id(user.getId())
                                    .email(user.getEmail())
                                    .phoneNumber(user.getPhoneNumber())
                                    .roles(user.getRoles())
                                    .build();

                            String accessToken = jwtService.generateAccessToken(principal);
                            String refreshToken = jwtService.generateRefreshToken(principal);

                            Map<String, String> data = Map.of(
                                    "accessToken", accessToken,
                                    "refreshToken", refreshToken
                            );

                            return Mono.just(ApiResponse.<Map<String, String>>builder()
                                    .status(200)
                                    .message("OTP verified")
                                    .data(data)
                                    .build());
                        })
                        .switchIfEmpty(Mono.defer(() -> {
                            // user not found -> optionally create user or return 'not registered'
                            return Mono.error(new UsernameNotFoundException("User not registered"));
                        }))
                );
    }*/

    public Mono<String> verifyOtp(String phoneNumber, String role, String otpInput) {
        Instant now = Instant.now();
        log.info("Verify otp for phone {}", phoneNumber);
        return tokensRepository.findByPhoneNumberAndRole(phoneNumber, role)
                .switchIfEmpty(Mono.error(new BadCredentialsException(INVALID_OTP)))
                .flatMap(token -> {
                    // check expiry via createdAt + TTL window (optional)
                    if (token.getCreatedAt() == null || token.getCreatedAt().plus(Duration.ofSeconds(otpExpiration)).isBefore(now)) {
                        // remove token and error
                        return tokensRepository.delete(token).then(Mono.error(new BadCredentialsException("OTP expired")));
                    }

                    // check failed attempts
                    if (token.getFailedAttempts() >= maxFailedAttempts) {
                        return Mono.error(new LockedException("Too many failed attempts. Contact support."));
                    }

                    // compare hashed OTP
                    //   boolean matches = passwordEncoder.matches(otpInput, token.getOtpHash());
                    if (!otpInput.equals(token.getOtp())) {
                        token.setFailedAttempts(token.getFailedAttempts() + 1);
                        return tokensRepository.save(token).then(Mono.error(new BadCredentialsException("Invalid OTP")));
                    }

                    // OTP matches -> remove token or mark consumed
                    return tokensRepository.delete(token).thenReturn(OTP_VERIFIED);
                });
    }

    private String generateOtp() {
        return UtilityHelper.generateOtp();
    }
}
