package com.bandhanbook.app.service;

import com.bandhanbook.app.exception.*;
import com.bandhanbook.app.model.MatrimonyCandidate;
import com.bandhanbook.app.model.RefreshToken;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.model.constants.RoleNames;
import com.bandhanbook.app.payload.request.ChangePasswordRequest;
import com.bandhanbook.app.payload.request.LoginRequest;
import com.bandhanbook.app.payload.request.PhoneLoginRequest;
import com.bandhanbook.app.payload.request.UserRegisterRequest;
import com.bandhanbook.app.payload.response.*;
import com.bandhanbook.app.repository.*;
import com.bandhanbook.app.security.jwt.JwtService;
import com.bandhanbook.app.security.userprinciple.UserDetailService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.bandhanbook.app.utilities.ErrorResponseMessages.*;
import static com.bandhanbook.app.utilities.SuccessResponseMessages.PASSWORD_UPDATED;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final MatrimonyRepository matrimonyRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailService userDetailService;
    private final JwtService jwtService;
    private final ModelMapper modelMapper;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EventParticipantsRepository eventParticipantRepo;
    private final OtpService otpService;
    private final AgentRepository agentRepository;
    private final CommonService commonService;
    private final OrganizationRepository organizationRepository;
    private final OrgSubscriptionsRepository orgSubscriptionsRepository;


    @Transactional
    public Mono<String> login(PhoneLoginRequest loginRequest) {

        return userDetailService.findByPhoneNumber(loginRequest.getPhoneNumber())
                .switchIfEmpty(Mono.error(new PhoneNumberNotFoundException(INVALID_CREDENTIALS)))
                .flatMap(user -> {

                    if (loginRequest.getPassword() != null && !loginRequest.getPassword().isBlank() && !passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                        return Mono.error(new EmailNotFoundException(INVALID_CREDENTIALS));
                    }
                    if (!user.getUsers().getRoles().contains(loginRequest.getRole())) {
                        return Mono.error(new RecordNotFoundException(loginRequest.getRole() + " is not registered with this number"));
                    }
                    return otpService.requestOtp(loginRequest.getPhoneNumber(), loginRequest.getRole());
                });
    }

    @Transactional
    public Mono<Void> registerUser(UserRegisterRequest request) {
        String role = RoleNames.SuperUser.name();
        return getValidatedUser(request.getPhoneNumber(), request.getEmail(), role).then(Mono.defer(() -> {
            Users user = modelMapper.map(request, Users.class);
            user.getRoles().add(role);
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            return userRepository.save(user);
        })).then();
    }

    public Mono<PhoneLoginResponse> verifyOtp(PhoneLoginRequest request) {

        return otpService.verifyOtp(
                        request.getPhoneNumber(),
                        request.getRole(),
                        request.getOtp()
                )
                .then(userDetailService.findByPhoneNumber(request.getPhoneNumber()))
                .flatMap(userPrincipal -> {

                    Users user = userPrincipal.getUsers();

                    // 1️⃣ Role validation
                    if (!user.getRoles().contains(request.getRole())) {
                        return Mono.error(
                                new RecordNotFoundException(
                                        request.getRole() + " is not registered with this number"
                                )
                        );
                    }

                    // Check if user has the requested role
                    if (user.getRoles().size() > 1) {
                        user.setRoles(List.of(request.getRole()));
                    }
                    Mono<PhoneLoginResponse> responseMono;
                    if (request.getRole().equals(RoleNames.Candidate.name())) {
                        responseMono = getMatrimonyDetails(request.getRole(), user);
                    } else if (request.getRole().equals(RoleNames.Agent.name())) {
                        responseMono = getAgentDetails(request.getRole(), user);
                    } else {
                        responseMono = getOrganizationDetails(request.getRole(), user);
                    }
                    return responseMono.map(res -> {
                        String accessToken = jwtService.generateToken(userPrincipal, request.getRole());
                        String refreshToken = jwtService.generateRefreshToken(userPrincipal.getUsername());
                        RefreshToken refToken = RefreshToken.builder()
                                .userId(user.getId())
                                .token(refreshToken)
                                .revoked(false)
                                .expiryDate(LocalDateTime.now().plusDays(30))
                                .build();
                        res.setAccessToken(accessToken);
                        res.setRefreshToken(refreshToken);
                        res.setRole(request.getRole());
                        return res;
                    });
                }).switchIfEmpty(Mono.error(new RuntimeException("Error occurred during login")));
    }

    protected Mono<PhoneLoginResponse> getAgentDetails(String role, Users users) {
        return agentRepository.findByUserId(users.getId()).flatMap(agents -> {
                    PhoneLoginResponse res = modelMapper.map(users, PhoneLoginResponse.class);
                    res.setAgent(true);
                    res.setRole(role);
                    AgentResponse agentResponse = modelMapper.map(agents, AgentResponse.class);
                    agentResponse.setUser_id(agents.getUserId().toHexString());
                    agentResponse.setOrganization_id(agents.getOrganizationId().toHexString());
                    agentResponse.setLocalAddress(commonService.getAddressByIds(agents.getAddress(), agents.getCountry(), agents.getState(), agents.getCity(), agents.getZip()));
                    res.setAgent_details(agentResponse);
                    return Mono.just(res);
                })
                .switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND)));
    }

    public Mono<PhoneLoginResponse> getMatrimonyDetails(String role, Users users) {

        return matrimonyRepository.findByUserId(users.getId())
                .flatMap(candidate ->
                        eventParticipantRepo.findByCandidateId(candidate.getId()).collectList().map(eventParticipants -> {
                            PhoneLoginResponse res = modelMapper.map(users, PhoneLoginResponse.class);
                            res.setAgent(false);
                            res.setRole(role);
                            List<EventParticipantsResponse> eventParticipant = eventParticipants.stream().map(entity -> modelMapper.map(entity, EventParticipantsResponse.class)).collect(Collectors.toList());
                            res.setEventParticipants(eventParticipant);
                            res.setMatrimony_data(modelMapper.map(candidate, MatrimonyCandidateResponse.class));
                            if (candidate.getAddress() != null) {
                                MatrimonyCandidate.Address add = candidate.getAddress();
                                res.getMatrimony_data().setLocalAddress(commonService.getAddressByIds(add.getAddress(), add.getCountry(), add.getState(), add.getCity(), add.getZip()));
                            }
                            return res;
                        }))
                .switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND)));
    }

    protected Mono<PhoneLoginResponse> getOrganizationDetails(String role, Users users) {
        return organizationRepository.findByUserId(users.getId()).flatMap(organization -> {
            PhoneLoginResponse res = modelMapper.map(users, PhoneLoginResponse.class);
            res.setAgent(false);
            res.setRole(role);
            res.setOrganization_details(modelMapper.map(organization, OrganizationResponse.class));
            res.getOrganization_details().setLocalAddress(commonService.getAddressByIds(organization.getAddress(), organization.getCountry(), organization.getState(), organization.getCity(), organization.getZip()));
            return orgSubscriptionsRepository.findByOrgIdAndActive(organization.getId(), true)
                    .map(sub -> {
                        res.setActiveSubscription(true);
                        return res;
                        }).defaultIfEmpty(res);
        }).switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND)));
    }

    public Mono<LoginResponse> webLogin(LoginRequest loginRequest) {

        return userDetailService.findByEmail(loginRequest.getEmail())
                .switchIfEmpty(Mono.error(new EmailNotFoundException(INVALID_CREDENTIALS)))
                .flatMap(user -> {

                    if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                        return Mono.error(new EmailNotFoundException(INVALID_CREDENTIALS));
                    }
                    // Check if user has the requested role
                    String accessToken = null;
                    if (user.getUsers().getRoles().size() > 1) {
                        accessToken = jwtService.generateToken(user, loginRequest.getRole());
                    } else
                        accessToken = jwtService.generateToken(user, user.getUsers().getRoles().get(0));

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
                    loginResponse.setRole(user.getUsers().getRoles().get(0));
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

    public Mono<String> resendOtp(PhoneLoginRequest loginRequest) {

        return userDetailService.findByPhoneNumber(loginRequest.getPhoneNumber())
                .switchIfEmpty(Mono.error(new PhoneNumberNotFoundException(INVALID_CREDENTIALS)))
                .flatMap(user -> {

                    if (!user.getUsers().getRoles().contains(loginRequest.getRole())) {
                        return Mono.error(new RecordNotFoundException(loginRequest.getRole() + " is not registered with this number"));
                    }
                    return otpService.requestOtp(loginRequest.getPhoneNumber(), loginRequest.getRole());
                });
    }

    public Mono<String> forgotPassword(LoginRequest req) {
        return userRepository.findByPhoneNumberOrEmail(null, req.getEmail())
                .flatMap(user -> {
                    String role = req.getRole() != null ? req.getRole() : RoleNames.Organization.name();
                    if (!user.getRoles().contains(role)) {
                        return Mono.error(new RecordNotFoundException(role + " is not registered with this email"));
                    }
                    if (req.getOtp() != null && !req.getOtp().isBlank() && req.getPassword() != null && !req.getPassword().isBlank()) {
                        user.setPassword(passwordEncoder.encode(req.getPassword()));
                        return otpService.verifyOtp(user.getPhoneNumber(), role, req.getOtp()).flatMap(s ->
                                userRepository.save(user)).thenReturn(PASSWORD_UPDATED);
                    } else {
                        return otpService.requestOtp(user.getPhoneNumber(), role);
                    }
                }).switchIfEmpty(Mono.error(new EmailNotFoundException(USER_NOT_FOUND)));
    }

    public Mono<Users> getValidatedUser(String phoneNumber, String email, String role) {
        return userRepository
                .findByPhoneNumberOrEmail(phoneNumber, email).flatMap(existingUser -> {
                    if (!role.equalsIgnoreCase(RoleNames.Candidate.name()) && existingUser.getRoles().contains(role)) {
                        return Mono.error(new PhoneOrEmailNotFoundException(PHONE_EMAIL_EXISTS));
                    }
                    return Mono.just(existingUser);
                }).switchIfEmpty(Mono.empty());
    }

    public Mono<String> changePassword(Users authUser, @Valid ChangePasswordRequest request) {
        if (request.getCurrentPassword().equalsIgnoreCase(request.getNewPassword())) {
            return Mono.error(new ValidationExceptions("current password and new password cannot be same"));
        }
        return userRepository.findById(authUser.getId())
                .flatMap(user -> {
                    if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                        return Mono.error(new RecordNotFoundException(INCORRECT_PASSWORD));
                    }
                    user.setPassword(passwordEncoder.encode(request.getNewPassword()));
                    return userRepository.save(user);
                }).thenReturn(PASSWORD_UPDATED);
    }
}
