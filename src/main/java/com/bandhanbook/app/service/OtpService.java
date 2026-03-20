package com.bandhanbook.app.service;

import com.bandhanbook.app.config.MessageUtil;
import com.bandhanbook.app.exception.ValidationExceptions;
import com.bandhanbook.app.model.Token;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.repository.TokensRepository;
import com.bandhanbook.app.utilities.EmailUtilities;
import com.bandhanbook.app.utilities.UtilityHelper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OtpService {
    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private final PasswordEncoder passwordEncoder;
    private final TokensRepository tokensRepository;
    private final MessageUtil messageUtil;
    private final EmailService emailService;
    private final EmailUtilities emailUtilities;

    /* @Autowired
     private final SmsSender smsSender;
 */
    @Value("${otp.expiration}")
    private int otpExpiration;
    @Value("${otp.register_expiration}")
    private int registerOtpExpiration;
    @Value("${otp.duration}")
    private Duration otpCooldown;
    @Value("${otp.windowDuration}")
    private Duration windowDuration;

    @Value("${otp.maxFailedAttempts}")
    private int maxFailedAttempts;
    @Value("${otp.maxRequestsPerWindow}")
    private int maxRequestsPerWindow;

   /* private final Duration otpCooldown = Duration.ofSeconds(duration); // min seconds between sends
    private final Duration windowDuration = Duration.ofHours(windowDurations); // window for maxRequests*/

    public Mono<String> sendForgotPasswordOtp(Users user, String role) {

        return requestOtp(user.getPhoneNumber(), role, true)
                .flatMap(token -> {
                    String otp = token.getOtp();
                    Map<String, String> mailContent = emailUtilities.getForgotPasswordContent(user.getFullName(), otp);
                    //emailService.sendNoReplyEmail(user.getEmail(), mailContent.get("subject"), mailContent.get("message"));
                    //.flatMap(saved -> smsSender.sendSms(phoneNumber, "Your OTP: " + otp).thenReturn(saved))
                    return Mono.empty();

                }).thenReturn(messageUtil.get("otp.sent"));
    }

    public Mono<String> sentRegistrationOtp(Users user, String role) {
        return requestOtp(user.getPhoneNumber(), role, true)
                .flatMap(token -> {
                    Map<String, String> mailContent = emailUtilities.getRegistrationMailContent(user.getFullName(), token.getOtp());
                    //emailService.sendNoReplyEmail(user.getEmail(), mailContent.get("subject"), mailContent.get("message"));
                    log.info("otp request for phone {}", user.getEmail());
                    //.flatMap(smsSender.sendSms(user.getPhoneNumber(), "Your OTP: " + token.getOtp())
                    return Mono.empty();
                })
                .thenReturn(messageUtil.get("otp.sent"));
    }

    public Mono<String> sendLoginOtp(String phoneNumber, String role) {
        return requestOtp(phoneNumber, role, false)
                .thenReturn(messageUtil.get("otp.sent"))
                /* .flatMap(saved -> smsSender.sendSms(phoneNumber, "Your OTP: " + otp).thenReturn(saved))*/;
    }

    public Mono<Token> requestOtp(String phoneNumber, String role, boolean isRegisterOrResetOtp) {
        Instant now = Instant.now();
        return tokensRepository.findByPhoneNumberAndRole(phoneNumber, role)
                .flatMap(existing -> {
                    if (existing.getLastSentAt() != null && existing.getLastSentAt().plus(otpCooldown).isAfter(now)) {
                        long wait = Duration.between(now, existing.getLastSentAt().plus(otpCooldown)).getSeconds();
                        return Mono.error(new ValidationExceptions("Please wait " + wait + " seconds before requesting a new OTP"));
                    }

                    if (existing.getWindowStart() == null || existing.getWindowStart().plus(windowDuration).isBefore(now)) {
                        existing.setWindowStart(now);
                        existing.setRequestCountInWindow(0);
                    }

                    if (existing.getRequestCountInWindow() >= maxRequestsPerWindow) {
                        return Mono.error(new ValidationExceptions(messageUtil.get("otp.request.limit.exceeded")));
                    }

                    String otp = generateOtp();
                    //String otpHash = passwordEncoder.encode(otp);

                    //existing.setOtpHash(otpHash);
                    existing.setOtp(otp);
                    existing.setLastSentAt(now);
                    existing.setRequestCountInWindow(existing.getRequestCountInWindow() + 1);
                    if (isRegisterOrResetOtp) {
                        existing.setExpiresAt(now.plusSeconds(registerOtpExpiration));
                    } else {
                        existing.setExpiresAt(now.plusSeconds(otpExpiration));
                    }
                    // save otp
                    return tokensRepository.save(existing);
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
                            .expiresAt(Instant.now().plusSeconds(otpExpiration))
                            .build();

                    return tokensRepository.save(token);
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

    @Transactional
    public Mono<String> verifyOtp(String phoneNumber, String role, String otpInput) {
        Instant now = Instant.now();
        log.info("Verify otp for phone {}", phoneNumber);
        return tokensRepository.findByPhoneNumberAndRole(phoneNumber, role)
                .switchIfEmpty(Mono.error(new ValidationExceptions(messageUtil.get("invalid.otp"))))
                .flatMap(token -> {
                    if (token.getExpiresAt() == null || token.getExpiresAt().isBefore(now)) {
                        return tokensRepository.delete(token)
                                .then(Mono.error(new ValidationExceptions(messageUtil.get("invalid.otp"))));
                    }
                    if (token.getFailedAttempts() >= maxFailedAttempts) {
                        return Mono.error(new LockedException(messageUtil.get("too.many.otp")));
                    }

                    // compare hashed OTP
                    //   boolean matches = passwordEncoder.matches(otpInput, token.getOtpHash());
                    if (!otpInput.equals(token.getOtp())) {
                        token.setFailedAttempts(token.getFailedAttempts() + 1);
                        return tokensRepository.save(token)
                                .doOnSuccess(t -> log.info("Saved failedAttempts = {}", t.getFailedAttempts()))
                                .then(Mono.error(new BadCredentialsException(messageUtil.get("invalid.otp"))));
                    }

                    // OTP matches -> remove token or mark consumed
                    return tokensRepository.delete(token).thenReturn(messageUtil.get("otp.verified"));
                });
    }

    private String generateOtp() {
        return UtilityHelper.generateOtp();
    }
}
