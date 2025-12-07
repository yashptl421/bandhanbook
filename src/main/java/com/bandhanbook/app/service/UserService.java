package com.bandhanbook.app.service;

import com.bandhanbook.app.exception.PhoneNumberNotFoundException;
import com.bandhanbook.app.exception.RecordNotFoundException;
import com.bandhanbook.app.exception.UnAuthorizedException;
import com.bandhanbook.app.model.EventParticipants;
import com.bandhanbook.app.model.MatrimonyCandidate;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.model.constants.RoleNames;
import com.bandhanbook.app.payload.request.UserRegisterRequest;
import com.bandhanbook.app.payload.response.PhoneLoginResponse;
import com.bandhanbook.app.repository.AgentRepository;
import com.bandhanbook.app.repository.EventParticipantsRepository;
import com.bandhanbook.app.repository.MatrimonyRepository;
import com.bandhanbook.app.repository.UserRepository;
import com.bandhanbook.app.security.userprinciple.UserDetailService;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import static com.bandhanbook.app.utilities.ErrorResponseMessages.DATA_NOT_FOUND;
import static com.bandhanbook.app.utilities.ErrorResponseMessages.PHONE_EXISTS;
import static com.bandhanbook.app.utilities.SuccessResponseMessages.USER_REGISTERED;

@Slf4j
@Service
public class UserService {
    @Autowired
    UserRepository userRepository;
    @Autowired
    MatrimonyRepository matrimonyRepository;
    @Autowired
    UserDetailService userDetailService;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private EventParticipantsRepository eventParticipantRepo;
    @Autowired
    private OtpService otpService;
    @Autowired
    private AgentRepository agentRepository;
    @Autowired
    private CommonService commonService;
    @Autowired
    AuthService authService;
    @Autowired
    private ReactiveMongoTemplate mongoTemplate;

    public Users getUsers() {
        return new Users();
    }

   /* public Mono<CandidateResponse> getCandidate(String id, Users authUser) {
        if(authUser.getId().equals(id))
        return userRepository.findById(authUser.getId())
                .switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND)))
                .flatMap(user -> authService.getMatrimonyDetails(RoleNames.Candidate.name(), user));
    }*/

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

    public Mono<PhoneLoginResponse> myProfile(Users users) {
        return userDetailService.findById(users.getId())
                .switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND)))
                .flatMap(user -> {
                    Mono<PhoneLoginResponse> responseMono;
                    if (user.getUsers().getRoles().contains(RoleNames.Candidate.name())) {
                        responseMono = authService.getMatrimonyDetails(RoleNames.Candidate.name(), user.getUsers());
                    } else if (user.getUsers().getRoles().contains(RoleNames.Agent.name())) {
                        responseMono = authService.getAgentDetails(RoleNames.Agent.name(), user.getUsers());
                    } else if (user.getUsers().getRoles().contains(RoleNames.Organization.name())) {
                        responseMono = authService.getOrganizationDetails(RoleNames.Organization.name(), user.getUsers());
                    } else {
                        responseMono = Mono.just(modelMapper.map(user.getUsers(), PhoneLoginResponse.class));
                    }
                    return responseMono.map(res -> {

                        // res.setRole(request.getRole());
                        return res;
                    });
                }).switchIfEmpty(Mono.error(new UnAuthorizedException("Error occurred during login")));
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
}
