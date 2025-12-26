package com.bandhanbook.app.service;

import com.bandhanbook.app.exception.PhoneNumberNotFoundException;
import com.bandhanbook.app.exception.RecordNotFoundException;
import com.bandhanbook.app.exception.UnAuthorizedException;
import com.bandhanbook.app.model.EventParticipants;
import com.bandhanbook.app.model.MatrimonyCandidate;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.model.constants.RoleNames;
import com.bandhanbook.app.payload.request.CandidateRequest;
import com.bandhanbook.app.payload.request.OrganizationRequest;
import com.bandhanbook.app.payload.request.UserRegisterRequest;
import com.bandhanbook.app.payload.response.CandidateResponse;
import com.bandhanbook.app.payload.response.MatrimonyCandidateResponse;
import com.bandhanbook.app.payload.response.PhoneLoginResponse;
import com.bandhanbook.app.payload.response.base.ApiResponse;
import com.bandhanbook.app.repository.*;
import com.bandhanbook.app.security.userprinciple.UserDetailService;
import com.bandhanbook.app.utilities.UtilityHelper;
import com.bandhanbook.app.wrappers.CandidateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

import static com.bandhanbook.app.utilities.ErrorResponseMessages.DATA_NOT_FOUND;
import static com.bandhanbook.app.utilities.ErrorResponseMessages.PHONE_EXISTS;
import static com.bandhanbook.app.utilities.SuccessResponseMessages.*;

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
    AuthService authService;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private ReactiveMongoTemplate reactiveMongoTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UtilityHelper utilityHelper;


    public Users getUsers() {
        return new Users();
    }


    public Mono<CandidateResponse> showCandidates(String userId, Users authUser) {

        ObjectId targetUserId = new ObjectId(userId);
        Document matrimonyDataFilters = new Document();
        Document eventParticipantFilters = new Document();
        Document agentFilters = new Document();
        if (authUser.getRoles().contains(RoleNames.Candidate.name())) {
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
        if (authUser.getRoles().contains(RoleNames.Agent.name()) || authUser.getRoles().contains(RoleNames.Organization.name())) {
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
                                                        .append("foreignField", "_id")
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
    public Mono<MatrimonyCandidateResponse> updateCandidate(String userId, CandidateRequest req, Users authUser) {

        ObjectId userObjectId = new ObjectId(userId);
        if (authUser.getRoles().contains(RoleNames.Candidate.name()) && !Objects.equals(authUser.getId(), userObjectId)) {
            return Mono.error(new UnAuthorizedException("You are not authorized to update this profile"));
        }

      /*  Mono<Boolean> emailExists = Mono.justOrEmpty(req.getEmail())
                .flatMap(email ->
                        userRepository.existsByEmailAndRolesContainingAndIdNot(
                                email, RoleNames.Candidate.name(), userObjectId
                        )
                )
                .defaultIfEmpty(false);*/

      /*  return emailExists.flatMap(exists -> {
            if (exists) {
                return Mono.error(new EmailNotFoundException(EMAIL_EXISTS));
            }*/
        return userRepository.findById(userObjectId).flatMap(users ->
                matrimonyRepository.findByUserId(userObjectId).flatMap(candidate -> {
                        /*if (!req.getEmail().isBlank() && !req.getEmail().equals(users.getEmail())) {
                            users.setEmail(req.getEmail());
                        }
                        if (!req.getFullName().isBlank() && !req.getFullName().equals(users.getFullName())) {
                            users.setFullName(req.getFullName());
                        }
                        if (authUser.getRoles().contains(RoleNames.Candidate.name())) {
                            req.getMatrimonyData().setStatus(candidate.getStatus());
                        }*/
                    modelMapper.map(req.getMatrimonyData(), candidate);
                    return matrimonyRepository.save(candidate)
                            .map(updatedCandidate -> {
                                MatrimonyCandidateResponse res = modelMapper.map(candidate, MatrimonyCandidateResponse.class);
                                res.setProfileCompletion(utilityHelper.getProfileCompletion(candidate));
                                return res;
                            });
                }).switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND))));
        /*  });*/
    }

    public Mono<ApiResponse<List<CandidateResponse>>> listCandidates(Users authUser, Map<String, String> params, int page, int limit) {
        int skip = (page - 1) * limit;

        Document userFilters = new Document(
                "role",
                new Document("$in", List.of(RoleNames.Candidate.name()))
        );

        if (params.containsKey("search")) {
            userFilters.put("$or", List.of(
                    new Document("full_name",
                            new Document("$regex", params.get("search")).append("$options", "i")),
                    new Document("email",
                            new Document("$regex", params.get("search")).append("$options", "i"))
            ));
        }

        if (params.get("phoneNumber") != null && !params.get("phoneNumber").isBlank() && !params.get("phoneNumber").equalsIgnoreCase("string")) {
            System.out.println(params.get("phoneNumber"));
            userFilters.put("phone_number",
                    new Document("$regex", params.get("phoneNumber")).append("$options", "i"));
        }

        Document matrimonyFilters = new Document();
        Document eventFilters = new Document();

        applyMatrimonyFilters(params, matrimonyFilters);
        applyEventFilters(params, eventFilters);

        if (authUser.getRoles().contains("Candidate")) {
            matrimonyFilters.put("status", "active");
            matrimonyFilters.put("profile_completed", true);
            matrimonyFilters.put("privacy_settings.is_hide_profile", false);
            userFilters.put("_id", new Document("$ne", authUser.getId()));
        }

        return resolveOrgAndAgentId(authUser, params)
                .switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND)))
                .flatMap(id -> {

                    Document organizationFilters = new Document();
                    if (!id.isBlank() && authUser.getRoles().contains(RoleNames.Organization.name())) {
                        organizationFilters.put("organization_id", new ObjectId(id));
                    }
                    if (!id.isBlank() && authUser.getRoles().contains(RoleNames.Agent.name())) {
                        eventFilters.put("added_by", new ObjectId(id));
                    }

                    List<Document> pipeline = List.of(

                            new Document("$match", userFilters),

                            new Document("$lookup", new Document()
                                    .append("from", "matrimonyprofiles")
                                    .append("localField", "_id")
                                    .append("foreignField", "user_id")
                                    .append("as", "matrimony_data")
                                    .append("pipeline", List.of(

                                            new Document("$match", matrimonyFilters),

                                            new Document("$lookup", new Document()
                                                    .append("from", "eventparticipants")
                                                    .append("localField", "_id")
                                                    .append("foreignField", "candidate_id")
                                                    .append("as", "event_participant")
                                                    .append("pipeline", List.of(

                                                            new Document("$match", eventFilters),

                                                            new Document("$lookup", new Document()
                                                                    .append("from", "events")
                                                                    .append("localField", "event_id")
                                                                    .append("foreignField", "_id")
                                                                    .append("as", "event")
                                                                    .append("pipeline", List.of(
                                                                            new Document("$match", organizationFilters)
                                                                    ))
                                                            ),
                                                            new Document("$match",
                                                                    new Document("event.0",
                                                                            new Document("$exists", true)))
                                                    ))
                                            ),
                                            new Document("$match",
                                                    new Document("event_participant.0",
                                                            new Document("$exists", true)))
                                    ))
                            ),

                            new Document("$match",
                                    new Document("matrimony_data.0",
                                            new Document("$exists", true))),

                            new Document("$facet", new Document()
                                    .append("metadata", List.of(
                                            new Document("$count", "total")
                                    ))
                                    .append("data", List.of(
                                            new Document("$sort", new Document("createdAt", -1)),
                                            new Document("$skip", skip),
                                            new Document("$limit", limit),
                                            new Document("$project", new Document()
                                                    .append("full_name", "$full_name")
                                                    .append("phone_number", "$phone_number")
                                                    .append("email", "$email")
                                                    .append("matrimony_data",
                                                            new Document("$arrayElemAt",
                                                                    List.of("$matrimony_data", 0)))
                                            )
                                    ))
                            )
                    );

                    List<AggregationOperation> ops = pipeline.stream()
                            .map(d -> (AggregationOperation) ctx -> d)
                            .toList();

                    Aggregation aggregation = Aggregation.newAggregation(ops);

                    return getFavouriteIdsMono(authUser).flatMap(favouriteIds ->
                            reactiveMongoTemplate.aggregate(aggregation, "users", CandidateWrapper.class)
                                    .next()
                                    .defaultIfEmpty(new CandidateWrapper())
                                    .map(result -> {
                                        List<CandidateResponse> res = result.getData();
                                        List<CandidateWrapper.RecordCount> metadata = result.getMetadata();
                                        System.out.print(favouriteIds);
                                        res.forEach(candidate -> {
                                            if (candidate.getMatrimony_data() != null &&
                                                    candidate.getMatrimony_data().get_id() != null) {

                                                String candidateMatrimonyId =
                                                        candidate.getMatrimony_data().get_id();

                                                candidate.setIsFavorite(
                                                        favouriteIds.contains(candidateMatrimonyId)
                                                );
                                            }
                                        });
                                        long total = metadata.isEmpty()
                                                ? 0
                                                : metadata.get(0).getTotal();

                                        int totalPages = (int) Math.ceil((double) total / limit);

                                        return ApiResponse.<List<CandidateResponse>>builder()
                                                .status(200)
                                                .message(res.isEmpty() ? DATA_NOT_FOUND : DATA_FOUND)
                                                .meta(ApiResponse.Meta.builder()
                                                        .page(page)
                                                        .limit(limit)
                                                        .totalRecords(total)
                                                        .totalPages(totalPages)
                                                        .build())
                                                .data(res)
                                                .build();
                                    }));
                });

    }

    private Mono<String> resolveOrgAndAgentId(Users authUser, Map<String, String> params) {
        // SUPER USER → from request
        if (authUser.getRoles().contains(RoleNames.SuperUser.name())
                && params.containsKey("organization")) {
            return Mono.just(params.get("organization"));
        }

        // ORGANIZATION → find by user_id
        if (authUser.getRoles().contains(RoleNames.Organization.name())) {
            return organizationRepository.findByUserId(authUser.getId())
                    .map(org -> String.valueOf(org.getId()));
        }

        // AGENT → find agent → org
        if (authUser.getRoles().contains(RoleNames.Agent.name())) {
            return agentRepository.findByUserId(authUser.getId())
                    .map(agent -> String.valueOf(agent.getId()));
        }

        return Mono.just("");
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
                                                    return agentRepository.findByUserId(authUser.getId()).map(agent ->
                                                                    saveEventParticipant(candidate, request, agent.getId()))
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
                                                                            agentRepository.findByUserId(authUser.getId()).map(agent ->
                                                                                    saveEventParticipant(matrimonyCandidate, request, agent.getId())
                                                                            )).thenReturn(USER_REGISTERED)
                                                    );
                                        })
                                )
                )
                .switchIfEmpty(
                        Mono.defer(() -> {
                            Users newUser = modelMapper.map(request, Users.class);
                            newUser.getRoles().add(role);
                            //newUser.setPassword(passwordEncoder.encode(request.getPassword()));
                            return userRepository.save(newUser)
                                    .flatMap(savedUser ->
                                            matrimonyRepository.save(registerReqToCandidate(request, savedUser))
                                                    .flatMap(matrimonyCandidate ->
                                                            agentRepository.findByUserId(authUser.getId()).map(agent ->
                                                                    saveEventParticipant(matrimonyCandidate, request, agent.getId())
                                                            ).thenReturn(USER_REGISTERED)
                                                    )
                                    );
                        })
                ));
    }

    @Transactional
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

    public Mono<List<PhoneLoginResponse>> getFavorites(Users authUser) {
        return matrimonyRepository.findByUserId(authUser.getId())
                .flatMapMany(candidateProfile -> {
                    List<ObjectId> favoriteIds = candidateProfile.getFavorites() != null ? candidateProfile.getFavorites() : new ArrayList<>();
                    return matrimonyRepository.findAllById(favoriteIds);
                })
                .flatMap(favoriteCandidate ->
                        userRepository.findById(favoriteCandidate.getUserId())
                                .map(user -> {
                                    PhoneLoginResponse res = modelMapper.map(user, PhoneLoginResponse.class);
                                    res.setMatrimony_data(modelMapper.map(favoriteCandidate, MatrimonyCandidateResponse.class));
                                    return res;
                                })
                )
                .collectList();
    }

    public Mono<FavoriteResponse> addRemoveToFavorites(String profileId, Users authUser) {
        ObjectId candidateId = new ObjectId(profileId);

        return matrimonyRepository.findByUserId(authUser.getId())
                .flatMap(candidateProfile ->
                        matrimonyRepository.findById(candidateId)
                                .flatMap(targetProfile -> {
                                    List<ObjectId> favorites = candidateProfile.getFavorites() != null ? candidateProfile.getFavorites() : new ArrayList<>();
                                    FavoriteResponse res =new FavoriteResponse();
                                    if (favorites.contains(targetProfile.getId())) {
                                        favorites.remove(targetProfile.getId());
                                    } else {
                                        favorites.add(targetProfile.getId());
                                        res.setFavorite(true);
                                    }
                                    candidateProfile.setFavorites(favorites);
                                    res.setMessage(FAVORITES_UPDATED);
                                    return matrimonyRepository.save(candidateProfile)
                                            .thenReturn(res);
                                })
                                .switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND))));
    }

    public Mono<String> updateProfile(OrganizationRequest request, Users authUser) {
        if (authUser.getRoles().contains(RoleNames.Organization.name())) {
            return organizationRepository.findByUserId(authUser.getId())
                    .flatMap(organization -> {
                        modelMapper.map(request, organization);
                        return organizationRepository.save(organization)
                                .thenReturn(ORGANIZATION_UPDATED);
                    })
                    .switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND)));
        } else {
            return Mono.error(new UnAuthorizedException("You are not authorized to update organization profile"));
        }
    }

    private Mono<EventParticipants> saveEventParticipant(MatrimonyCandidate candidate, UserRegisterRequest request, ObjectId agentId) {
        return eventParticipantRepo.save(EventParticipants.builder()
                .candidateId(candidate.getId())
                .eventId(new ObjectId(request.getEventId()))
                .addedBy(agentId)
                .build());
    }

    private MatrimonyCandidate registerReqToCandidate(UserRegisterRequest req, Users user) {
        return MatrimonyCandidate.builder().userId(user.getId())
                .personalDetails(MatrimonyCandidate.PersonalDetails.builder()
                        .dob(req.getDob())
                        .gender(req.getGender())
                        .build())
                .address(
                        MatrimonyCandidate.Address.builder()
                                .address(req.getAddress())
                                .country(req.getCountry())
                                .zip(req.getZip())
                                .city(req.getCity())
                                .state(req.getState()).build())
                .privacySettings(MatrimonyCandidate.PrivacySettings.builder().build())
                .contactDetails(MatrimonyCandidate.ContactDetails.builder().build())
                .familyDetails(MatrimonyCandidate.FamilyDetails.builder().build())
                .educationDetails(MatrimonyCandidate.EducationDetails.builder().build())
                .lifestyleInterests(MatrimonyCandidate.LifestyleInterests.builder().build())
                .partnerPreferences(MatrimonyCandidate.PartnerPreferences.builder().build())
                .occupationDetails(MatrimonyCandidate.OccupationDetails.builder().build())
                .favorites(new ArrayList<>())
                .bloodDonated(false)
                .profileCompleted(false)
                .build();
    }

    private void applyMatrimonyFilters(Map<String, String> params, Document filter) {

        if (params.containsKey("gender"))
            filter.put("personal_details.gender", params.get("gender"));

        if (params.containsKey("city"))
            filter.put("address.city", params.get("city"));

        if (params.containsKey("zip"))
            filter.put("address.zip", params.get("zip"));

        if (params.containsKey("status"))
            filter.put("status", params.get("status"));
    }

    private void applyEventFilters(Map<String, String> params, Document filter) {

        if (params.containsKey("agentId"))
            filter.put("added_by", new ObjectId(params.get("agentId")));

        if (params.containsKey("eventId"))
            filter.put("event_id", new ObjectId(params.get("eventId")));
    }

    private Mono<Set<String>> getFavouriteIdsMono(Users authUser) {
        return matrimonyRepository.findByUserId(authUser.getId())
                .map(mp -> {
                            return (mp.getFavorites() != null ? mp.getFavorites() : List.<ObjectId>of())
                                    .stream()
                                    .map(ObjectId::toHexString) // normalize
                                    .collect(Collectors.toSet());
                        }
                )
                .defaultIfEmpty(Set.of());
    }
}
