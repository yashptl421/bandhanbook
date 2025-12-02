package com.bandhanbook.app.service;

import com.bandhanbook.app.exception.EmailNotFoundException;
import com.bandhanbook.app.exception.InvalidOtpException;
import com.bandhanbook.app.exception.PhoneNumberNotFoundException;
import com.bandhanbook.app.exception.RecordNotFoundException;
import com.bandhanbook.app.model.*;
import com.bandhanbook.app.model.constants.RoleNames;
import com.bandhanbook.app.payload.request.LoginRequest;
import com.bandhanbook.app.payload.request.UserRegisterRequest;
import com.bandhanbook.app.payload.response.LoginResponse;
import com.bandhanbook.app.repository.*;
import com.bandhanbook.app.security.jwt.JwtService;
import com.bandhanbook.app.security.userprinciple.UserDetailService;
import com.bandhanbook.app.utilities.UtilityHelper;
import io.jsonwebtoken.Claims;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;

import static com.bandhanbook.app.utilities.ErrorResponseMessages.*;
import static com.bandhanbook.app.utilities.SuccessResponseMessages.OTP_SENT;
import static com.bandhanbook.app.utilities.SuccessResponseMessages.USER_REGISTERED;

@Service
public class UserService {
    @Autowired
    UserRepository userRepository;
    @Autowired
    MatrimonyRepository matrimonyRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    UserDetailService userDetailService;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private EventParticipantsRepository eventParticipantRepo;
    @Autowired
    TokensRepository tokensRepository;

    public Users getUsers() {
        return new Users();
    }

    public Mono<Void> registerUser(UserRegisterRequest request) {
        String role = RoleNames.SuperUser.name();
        return userRepository
                .existsByPhoneNumber(request.getPhoneNumber())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new PhoneNumberNotFoundException(PHONE_EXISTS));
                    }
                    return Mono.empty();
                }).then(Mono.defer(() -> {
                    Users user = modelMapper.map(request, Users.class);
                    user.getRoles().add(role);
                    user.setPassword(passwordEncoder.encode(user.getPassword()));
                    return userRepository.save(user);
                })).then();
    }

    @Transactional
    public Mono<String> register(UserRegisterRequest request, Users authUser) {
        String role = RoleNames.Candidate.name();
        // If no OTP → Send OTP
        if (request.getOtp() == null || request.getOtp().isBlank()) {
            return sendOtp(request, role);
        }
        Mono<Token> tokenMono = tokensRepository.findByPhoneNumberAndRole(request.getPhoneNumber(), role)
                .switchIfEmpty(Mono.error(new InvalidOtpException(INVALID_OTP)));
                /*findByPhoneRoleAndOtp(request.getPhoneNumber(), request.getOtp(), role)
                .switchIfEmpty(Mono.error(new InvalidOtpException()));
                */

        return tokenMono.flatMap(token -> {

            // If OTP not 123456 → return || invalid must be remove in prod
            if (!"123456".equals(request.getOtp())) {
                return Mono.error(new InvalidOtpException(INVALID_OTP));
            }

            // STEP 3 — Check if user exists by phone
            return userRepository.findByPhoneNumber(request.getPhoneNumber())

                    .flatMap(existingUser ->
                            matrimonyRepository.findByUserId(existingUser.getId())
                                    .flatMap(candidate ->
                                            eventParticipantRepo
                                                    .existsByCandidateIdAndEventId(candidate.getId(), request.getEventId())
                                                    .flatMap(exists -> {
                                                        if (exists) {
                                                            return Mono.error(new PhoneNumberNotFoundException(PHONE_EXISTS));
                                                        }
                                                        // Add candidate to new event
                                                        return saveEventParticipant(candidate, request, authUser)
                                                                .thenReturn(USER_REGISTERED);
                                                    })
                                    )
                                    .switchIfEmpty(
                                            Mono.defer(() -> {
                                                existingUser.getRoles().add(role);
                                                return userRepository.save(existingUser)
                                                        .flatMap(savedUser ->
                                                                matrimonyRepository
                                                                        .save(registerReqToCandidate(request, savedUser))
                                                                        .flatMap(matrimonyCandidate ->
                                                                                saveEventParticipant(matrimonyCandidate, request, authUser)
                                                                                        .thenReturn(USER_REGISTERED)
                                                                        )
                                                        );
                                            })
                                    )
                    )
                    .switchIfEmpty(
                            Mono.defer(() -> {
                                Users newUser = modelMapper.map(request, Users.class);
                                newUser.getRoles().add(role);

                                return userRepository.save(newUser)
                                        .flatMap(savedUser ->
                                                matrimonyRepository.save(registerReqToCandidate(request, savedUser))
                                                        .flatMap(matrimonyCandidate ->
                                                                saveEventParticipant(matrimonyCandidate, request, authUser)
                                                                        .thenReturn(USER_REGISTERED)
                                                        )
                                        );
                            })
                    );
        });
    }

    public Mono<LoginResponse> webLogin(LoginRequest loginRequest) {

        return userDetailService.findByEmail(loginRequest.getEmail())
                .switchIfEmpty(Mono.error(new EmailNotFoundException(INVALID_CREDENTIALS)))
                .flatMap(user -> {

                    if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                        return Mono.error(new EmailNotFoundException(INVALID_CREDENTIALS));
                    }
                    String accessToken = jwtService.generateToken(user);
                    String refreshToken = jwtService.generateRefreshToken(user.getUsername());
                    RefreshToken refToken = RefreshToken.builder()
                            .userId(user.getUsers().getId())
                            .token(refreshToken)
                            .revoked(false)
                            .expiryDate(LocalDateTime.now().plusDays(30))
                            .build();
                    LoginResponse loginResponse = modelMapper.map(user.getUsers(), LoginResponse.class);
                    loginResponse.setAccessToken(accessToken);
                    loginResponse.setRefreshToken(refreshToken);
                    return refreshTokenRepository.save(refToken).thenReturn(loginResponse);
                });
    }

    public Mono<LoginResponse> refreshToken(String oldRefreshToken) {

        return refreshTokenRepository.findByToken(oldRefreshToken)
                .switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND)))
                .flatMap(savedToken -> {
                    if (savedToken.isRevoked() || savedToken.getExpiryDate().isBefore(LocalDateTime.now())) {
                        return Mono.error(new RecordNotFoundException(DATA_NOT_FOUND));
                    }

                    Claims claims = jwtService.validateRefreshToken(oldRefreshToken);
                    String userName = claims.getSubject();

                    String newRefreshToken = jwtService.generateRefreshToken(userName);
                    savedToken.setToken(newRefreshToken);

                    return refreshTokenRepository.save(savedToken)
                            .flatMap(t ->
                                    userDetailService.findByEmail(userName)
                                            .map(user -> {
                                                LoginResponse loginResponse = modelMapper.map(user, LoginResponse.class);
                                                loginResponse.setAccessToken(jwtService.generateToken(user));
                                                loginResponse.setRefreshToken(newRefreshToken);
                                                return loginResponse;
                                            }));
                });
    }

    public Mono<Void> logout(Users users) {
        return userRepository.findById(users.getId())
                .flatMap(user -> {
                    user.setToken(null);
                    return userRepository.save(user);
                })
                .then();
    }

    public Mono<Void> logout(String refreshToken) {
        return refreshTokenRepository.findByToken(refreshToken)
                .flatMap(token -> {
                    token.setRevoked(true);
                    return refreshTokenRepository.save(token);
                })
                .then();
    }

    private Mono<String> sendOtp(UserRegisterRequest request, String role) {
        String otp = generateOtp();
        return tokensRepository.save(Token.builder()
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .role(role)
                .otp(otp)
                .createdAt(Instant.now())
                .build()).thenReturn((OTP_SENT));
    }

    private String generateOtp() {
        return UtilityHelper.generateOtp();
    }

    private Mono<EventParticipants> saveEventParticipant(MatrimonyCandidate candidate, UserRegisterRequest request, Users authUser) {
        return tokensRepository.deleteByPhoneNumber(request.getPhoneNumber()).then(eventParticipantRepo.save(EventParticipants.builder()
                .candidateId(candidate.getId())
                .eventId(request.getEventId())
                .addedBy(authUser.getId())
                .build()));
    }

    private MatrimonyCandidate registerReqToCandidate(UserRegisterRequest userRegisterRequest, Users user) {
        return MatrimonyCandidate.builder().userId(user.getId())
                .personalDetails(MatrimonyCandidate.PersonalDetails.builder()
                        .dob(userRegisterRequest.getDob())
                        .gender(userRegisterRequest.getGender())
                        .build())
                .address(
                        MatrimonyCandidate.Address.builder()
                                .address(userRegisterRequest.getAddress())
                                .zip(userRegisterRequest.getZip())
                                .city(userRegisterRequest.getCity())
                                .state(userRegisterRequest.getState()).build())

                .build();
    }
}
