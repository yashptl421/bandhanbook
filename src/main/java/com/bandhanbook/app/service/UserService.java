package com.bandhanbook.app.service;

import com.bandhanbook.app.exception.PhoneNumberNotFoundException;
import com.bandhanbook.app.exception.RecordNotFoundException;
import com.bandhanbook.app.exception.UnAuthorizedException;
import com.bandhanbook.app.model.EventParticipants;
import com.bandhanbook.app.model.MatrimonyCandidate;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.model.constants.RoleNames;
import com.bandhanbook.app.payload.request.UserRegisterRequest;
import com.bandhanbook.app.payload.response.CandidateResponse;
import com.bandhanbook.app.payload.response.PhoneLoginResponse;
import com.bandhanbook.app.payload.response.base.ApiResponse;
import com.bandhanbook.app.repository.*;
import com.bandhanbook.app.security.userprinciple.UserDetailService;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.List;

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
    private OrganizationRepository organizationRepository;
    @Autowired
    private ReactiveMongoTemplate reactiveMongoTemplate;

    public Users getUsers() {
        return new Users();
    }


    public Mono<CandidateResponse> showCandidates(String userId, Users authUser) {

        ObjectId targetUserId = new ObjectId(userId);
        Document matrimonyDataFilters = new Document();
        Document eventParticipantFilters = new Document();
        Document agentFilters = new Document();
        if (authUser.getRoles().contains(RoleNames.Candidate.name())) {
            matrimonyDataFilters.put("status", "active");
            matrimonyDataFilters.put("privacy_settings.is_hide_profile", false);

            // candidate event filtering
            matrimonyRepository.findByUserId(authUser.getId())
                    .flatMap(profile ->
                            eventParticipantRepo.findByCandidateId(profile.getId())
                                    .map(EventParticipants::getEventId)
                                    .collectList()
                                    .doOnNext(eventIds -> {
                                        eventParticipantFilters.put("event_id",
                                                new Document("$in",
                                                        eventIds.stream()
                                                                .toList()
                                                )
                                        );
                                        matrimonyDataFilters.put("status", "active");
                                        matrimonyDataFilters.put("profile_completed", false);
                                    })

                    );

        }
        // If Organization → filter agents by orgId
        if (authUser.getRoles().contains(RoleNames.Organization.name())) {
            return organizationRepository.findByUserId(authUser.getId())
                    .flatMap(org -> {
                        agentFilters.put("organization_id", org.getId());
                        return runFullPipeline(targetUserId, matrimonyDataFilters, eventParticipantFilters, agentFilters);
                    });
        }
        if (authUser.getRoles().contains(RoleNames.Agent.name())) {
            return agentRepository.findByUserId(authUser.getId())
                    .flatMap(agent -> {
                        agentFilters.put("organization_id", agent.getOrganizationId());
                        return runFullPipeline(targetUserId, matrimonyDataFilters, eventParticipantFilters, agentFilters);
                    });
        }
        return runFullPipeline(targetUserId, matrimonyDataFilters, eventParticipantFilters, agentFilters);
    }

    private Mono<CandidateResponse> runFullPipeline(
            ObjectId targetUserId,
            Document matrimonyFilters,
            Document eventParticipantFilters,
            Document agentFilters
    ) {

        List<Document> pipeline = List.of(
                new Document("$match", new Document("_id", targetUserId)),

                new Document("$lookup", new Document()
                        .append("from", "matrimonyprofiles")
                        .append("localField", "_id")
                        .append("foreignField", "user_id")
                        .append("as", "matrimony_data")
                        .append("pipeline", List.of(
                                new Document("$match", matrimonyFilters),

                                // lookup event_participants
                                new Document("$lookup", new Document()
                                        .append("from", "eventparticipants")
                                        .append("localField", "_id")
                                        .append("foreignField", "candidate_id")
                                        .append("as", "event_participant")
                                        .append("pipeline", List.of(
                                                new Document("$match", eventParticipantFilters),

                                                // lookup agent_details
                                                new Document("$lookup", new Document()
                                                        .append("from", "agents")
                                                        .append("localField", "added_by")
                                                        .append("foreignField", "user_id")
                                                        .append("as", "agent_details")
                                                        .append("pipeline", List.of(
                                                                new Document("$match", agentFilters),

                                                                // lookup agent → user details
                                                                new Document("$lookup", new Document()
                                                                        .append("from", "users")
                                                                        .append("localField", "user_id")
                                                                        .append("foreignField", "_id")
                                                                        .append("as", "user_details")
                                                                ),
                                                                new Document("$unwind", "$user_details")
                                                        ))
                                                ),
                                                new Document("$addFields",
                                                        new Document("agent_details",
                                                                new Document("$arrayElemAt", List.of("$agent_details", 0))
                                                        )
                                                )
                                                /*new Document("$match", new Document("agent_details.0",
                                                        new Document("$exists", true)))*/
                                        ))
                                ),

                                new Document("$match", new Document("event_participant.0",
                                        new Document("$exists", true)))
                        ))
                ),

                new Document("$match", new Document("matrimony_data",
                        new Document("$exists", true))),

                new Document("$unwind",
                        new Document("path", "$matrimony_data")
                                .append("preserveNullAndEmptyArrays", true))
        );

        List<AggregationOperation> ops = pipeline.stream()
                .map(d -> (AggregationOperation) ctx -> d)
                .toList();

        Aggregation aggregation = Aggregation.newAggregation(ops);

        return reactiveMongoTemplate.aggregate(aggregation, "users", CandidateResponse.class)
                .next()
                .switchIfEmpty(Mono.error(new RuntimeException("Candidate not found")));
    }

    @Transactional
    public Mono<String> register(UserRegisterRequest request, Users authUser) {
        String role = RoleNames.Candidate.name();
        // If no OTP → Send OTP
        if (request.getOtp() == null || request.getOtp().isBlank()) {
            return authService.getValidatedUser(request.getPhoneNumber(), request.getEmail(), role)
                    .then(otpService.requestOtp(request.getPhoneNumber(), role));
        }
        Mono<String> verifiedOtp = otpService.verifyOtp(request.getPhoneNumber(), role, request.getOtp());

        // STEP 3 — Check if user exists by phone
        return verifiedOtp.flatMap(str -> userRepository.findByPhoneNumber(request.getPhoneNumber())

                .flatMap(existingUser ->
                        matrimonyRepository.findByUserId(existingUser.getId())
                                .flatMap(candidate ->
                                        eventParticipantRepo
                                                .existsByCandidateIdAndEventId(candidate.getId(), new ObjectId(request.getEventId()))
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
                .eventId(new ObjectId(request.getEventId()))
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
