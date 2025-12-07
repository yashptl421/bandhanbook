package com.bandhanbook.app.service;

import com.bandhanbook.app.exception.*;
import com.bandhanbook.app.model.EventParticipants;
import com.bandhanbook.app.model.MatrimonyCandidate;
import com.bandhanbook.app.model.RefreshToken;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.model.constants.RoleNames;
import com.bandhanbook.app.payload.request.LoginRequest;
import com.bandhanbook.app.payload.request.PhoneLoginRequest;
import com.bandhanbook.app.payload.request.UserRegisterRequest;
import com.bandhanbook.app.payload.response.AgentResponse;
import com.bandhanbook.app.payload.response.LoginResponse;
import com.bandhanbook.app.payload.response.OrganizationResponse;
import com.bandhanbook.app.payload.response.PhoneLoginResponse;
import com.bandhanbook.app.repository.*;
import com.bandhanbook.app.security.jwt.JwtService;
import com.bandhanbook.app.security.userprinciple.UserDetailService;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static com.bandhanbook.app.utilities.ErrorResponseMessages.*;
import static com.bandhanbook.app.utilities.SuccessResponseMessages.USER_REGISTERED;

@Slf4j
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
    private OtpService otpService;
    @Autowired
    private AgentRepository agentRepository;
    @Autowired
    private CommonService commonService;
    @Autowired
    private OrganizationRepository organizationRepository;
   /* @Autowired
    TokensRepository tokensRepository;*/

    public Users getUsers() {
        return new Users();
    }

    @Transactional
    public Mono<String> login(PhoneLoginRequest loginRequest) {

        return userDetailService.findByPhoneNumber(loginRequest.getPhoneNumber())
                .switchIfEmpty(Mono.error(new PhoneNumberNotFoundException(INVALID_CREDENTIALS)))
                .flatMap(user -> {

                    if (!loginRequest.getPassword().isBlank() && !passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                        return Mono.error(new EmailNotFoundException(INVALID_CREDENTIALS));
                    }
                    if (!user.getUsers().getRoles().contains(loginRequest.getRole())) {
                        return Mono.error(new UnAuthorizedException(loginRequest.getRole() + " is not registered with this number"));
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

    @Transactional
    public Mono<PhoneLoginResponse> verifyOtp(PhoneLoginRequest request) {

        return otpService.verifyOtp(request.getPhoneNumber(), request.getRole(), request.getOtp())
                .flatMap(s ->
                        userDetailService.findByPhoneNumber(request.getPhoneNumber()))
                .flatMap(user -> {
                    if (!user.getUsers().getRoles().contains(request.getRole())) {
                        return Mono.error(new UnAuthorizedException(request.getRole() + " is not registered with this number"));
                    }
                    Mono<PhoneLoginResponse> responseMono;
                    if (request.getRole().equals(RoleNames.Candidate.name())) {
                        responseMono = getMatrimonyDetails(request, user.getUsers());
                    } else if (request.getRole().equals(RoleNames.Agent.name())) {
                        responseMono = getAgentDetails(request, user.getUsers());
                    } else {
                        responseMono = getOrganizationDetails(request, user.getUsers());
                    }
                    return responseMono.map(res -> {

                        String accessToken = jwtService.generateToken(user);
                        String refreshToken = jwtService.generateRefreshToken(user.getUsername());
                        RefreshToken refToken = RefreshToken.builder()
                                .userId(user.getUsers().getId())
                                .token(refreshToken)
                                .revoked(false)
                                .expiryDate(LocalDateTime.now().plusDays(30))
                                .build();
                        res.setAccessToken(accessToken);
                        res.setRefreshToken(refreshToken);
                        res.setRole(request.getRole());
                        return res;
                    });
                }).switchIfEmpty(Mono.error(new UnAuthorizedException("Error occurred during login")));
    }

    private Mono<PhoneLoginResponse> getAgentDetails(PhoneLoginRequest request, Users users) {
        return agentRepository.findByUserId(users.getId()).flatMap(agents -> {
                    PhoneLoginResponse res = modelMapper.map(users, PhoneLoginResponse.class);
                    res.setAgent(true);
                    res.setRole(request.getRole());
                    AgentResponse agentResponse = modelMapper.map(agents, AgentResponse.class);
                    agentResponse.setUser_id(agents.getUserId());
                    agentResponse.setOrganization_id(agents.getOrganizationId());
                    agentResponse.setLocalAddress(commonService.getAddressByIds(agents.getAddress(), agents.getCountry(), agents.getState(), agents.getCity(), agents.getZip()));
                    res.setAgent_details(agentResponse);
                    return Mono.just(res);
                })
                .switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND)));
    }

    private Mono<PhoneLoginResponse> getMatrimonyDetails(PhoneLoginRequest request, Users users) {

        return matrimonyRepository.findByUserId(users.getId())
                .flatMap(candidate ->
                        eventParticipantRepo.findByCandidateId(candidate.getId()).map(eventParticipants -> {
                            PhoneLoginResponse res = modelMapper.map(users, PhoneLoginResponse.class);
                            res.setAgent(false);
                            res.setRole(request.getRole());
                            res.setEventParticipants(eventParticipants);
                            res.setMatrimony_data(candidate);
                            return res;
                        }))
                .switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND)));
    }

    private Mono<PhoneLoginResponse> getOrganizationDetails(PhoneLoginRequest request, Users users) {
        return organizationRepository.findByUserId(users.getId()).map(organization -> {
            PhoneLoginResponse res = modelMapper.map(users, PhoneLoginResponse.class);
            res.setAgent(false);
            res.setRole(request.getRole());
            res.setOrganization_details(modelMapper.map(organization, OrganizationResponse.class));
            return res;
        }).switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND)));
    }

    @Transactional
    public Mono<String> register(UserRegisterRequest request, Users authUser) {
        String role = RoleNames.Candidate.name();
        // If no OTP → Send OTP
        if (request.getOtp() == null || request.getOtp().isBlank()) {
            return otpService.requestOtp(request.getPhoneNumber(), role);
        }
        Mono<String> verifiedOtp = otpService.verifyOtp(request.getPhoneNumber(), role, request.getOtp());

        // STEP 3 — Check if user exists by phone
        return verifiedOtp.flatMap(str -> userRepository.findByPhoneNumber(request.getPhoneNumber())

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
                ));
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

    private Mono<EventParticipants> saveEventParticipant(MatrimonyCandidate candidate, UserRegisterRequest request, Users authUser) {
        return eventParticipantRepo.save(EventParticipants.builder()
                .candidateId(candidate.getId())
                .eventId(request.getEventId())
                .addedBy(authUser.getId())
                .build());
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
                                .country(userRegisterRequest.getCountry())
                                .zip(userRegisterRequest.getZip())
                                .city(userRegisterRequest.getCity())
                                .state(userRegisterRequest.getState()).build())

                .build();
    }

    public Mono<Users> getValidatedUser(String phoneNumber, String email, String role) {
        return userRepository
                .findByPhoneNumberOrEmail(phoneNumber, email).flatMap(existingUser -> {
                    if (existingUser.getRoles().contains(role)) {
                        return Mono.error(new PhoneOrEmailNotFoundException(PHONE_EMAIL_EXISTS));
                    }
                    return Mono.just(existingUser);
                });
    }
}
