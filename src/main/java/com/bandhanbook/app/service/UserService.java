package com.bandhanbook.app.service;

import com.bandhanbook.app.exception.EmailNotFoundException;
import com.bandhanbook.app.exception.PhoneNumberNotFoundException;
import com.bandhanbook.app.exception.RecordNotFoundException;
import com.bandhanbook.app.exception.UnAuthorizedException;
import com.bandhanbook.app.model.Agents;
import com.bandhanbook.app.model.EventParticipants;
import com.bandhanbook.app.model.MatrimonyCandidate;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.model.constants.ProfileStatus;
import com.bandhanbook.app.model.constants.RoleNames;
import com.bandhanbook.app.payload.request.CandidateRequest;
import com.bandhanbook.app.payload.request.OrganizationRequest;
import com.bandhanbook.app.payload.request.UserRegisterRequest;
import com.bandhanbook.app.payload.response.CandidateResponse;
import com.bandhanbook.app.payload.response.MatrimonyCandidateResponse;
import com.bandhanbook.app.payload.response.PhoneLoginResponse;
import com.bandhanbook.app.payload.response.base.ApiResponse;
import com.bandhanbook.app.repository.*;
import com.bandhanbook.app.utilities.UtilityHelper;
import com.bandhanbook.app.wrappers.CandidateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.modelmapper.ModelMapper;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.bandhanbook.app.utilities.ErrorResponseMessages.*;
import static com.bandhanbook.app.utilities.SuccessResponseMessages.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final MatrimonyRepository matrimonyRepository;
    private final ModelMapper modelMapper;
    private final EventParticipantsRepository eventParticipantRepo;
    private final OtpService otpService;
    private final AgentRepository agentRepository;
    private final AuthService authService;
    private final OrganizationRepository organizationRepository;
    private final ReactiveMongoTemplate reactiveMongoTemplate;
    private final PasswordEncoder passwordEncoder;
    private final UtilityHelper utilityHelper;
    private final CommonService commonService;


    public Users getUsers() {
        return new Users();
    }


    public Mono<CandidateResponse> showCandidates(String userId, Users authUser) {

        ObjectId targetUserId = new ObjectId(userId);
        Document matrimonyDataFilters = new Document();
        Document eventParticipantFilters = new Document();
        Document agentFilters = new Document();

        if (authUser.isCandidate()) {
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
                                        matrimonyDataFilters.put("status", ProfileStatus.active.name());
                                        matrimonyDataFilters.put("profile_completed", false);
                                    })

                    );

        } else if (authUser.isOrganization()) {
            return organizationRepository.findByUserId(authUser.getId())
                    .flatMap(org -> {
                        agentFilters.put("organization_id", org.getId());
                        return runFullPipeline(targetUserId, matrimonyDataFilters, eventParticipantFilters, agentFilters).map(this::addAddressInResponse);
                    });
        } else if (authUser.isAgent()) {
            return agentRepository.findByUserId(authUser.getId())
                    .flatMap(agent -> {
                        agentFilters.put("organization_id", agent.getOrganizationId());
                        return runFullPipeline(targetUserId, matrimonyDataFilters, eventParticipantFilters, agentFilters).map(this::addAddressInResponse);
                    });
        }
        return getFavouriteIdsMono(authUser).flatMap(favouriteIds -> runFullPipeline(targetUserId, matrimonyDataFilters, eventParticipantFilters, agentFilters)
                .map(candidateResponse -> {
                    if (candidateResponse.getMatrimony_data() != null &&
                            candidateResponse.getMatrimony_data().get_id() != null) {

                        String candidateMatrimonyId =
                                candidateResponse.getMatrimony_data().get_id();

                        candidateResponse.setIsFavorite(
                                favouriteIds.contains(candidateMatrimonyId));
                        addAddressInResponse(candidateResponse);
                    }
                    return candidateResponse;
                }));
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
        if (authUser.isCandidate() && !Objects.equals(authUser.getId(), userObjectId)) {
            return Mono.error(new UnAuthorizedException("You are not authorized to update this profile"));
        }

        Mono<Boolean> emailExists = Mono.justOrEmpty(req.getEmail())
                .flatMap(email ->
                        userRepository.existsByEmailAndRolesContainingAndIdNot(
                                email, RoleNames.Candidate.name(), userObjectId
                        )
                )
                .defaultIfEmpty(false);

        return emailExists.flatMap(exists -> {
            if (exists) {
                return Mono.error(new EmailNotFoundException(EMAIL_EXISTS));
            }
            return userRepository.findById(userObjectId).flatMap(users ->
                    matrimonyRepository.findByUserId(userObjectId).flatMap(candidate -> {
                        if (null != req.getEmail() && !req.getEmail().isBlank() && !req.getEmail().equals(users.getEmail()) && users.getRoles().contains(RoleNames.Candidate.name())) {
                            users.setEmail(req.getEmail());
                        } else {
                            req.setEmail(users.getEmail());
                        }
                        if (null != req.getFullName() && !req.getFullName().isBlank() && !req.getFullName().equals(users.getFullName())) {
                            users.setFullName(req.getFullName());
                        }
                        if (authUser.isCandidate()) {
                            req.getMatrimonyData().setStatus(candidate.getStatus());
                        }
                        if (users.getDeletedAt() != null && (authUser.isCandidate() || authUser.isAgent())) {
                            req.setDeletedAt(users.getDeletedAt());
                        }
                        if ((authUser.isOrganization() || authUser.isAgent()) && req.getMatrimonyData().getStatus().equals(ProfileStatus.active)) {
                            req.getMatrimonyData().setStatus(candidate.getStatus());
                        }

                        modelMapper.map(req.getMatrimonyData(), candidate);
                        return userRepository.save(users).flatMap(user -> matrimonyRepository.save(candidate)
                                .map(updatedCandidate -> {
                                    MatrimonyCandidateResponse res = modelMapper.map(candidate, MatrimonyCandidateResponse.class);
                                    res.setProfileCompletion(utilityHelper.getProfileCompletion(candidate));
                                    return res;
                                }));
                    }).switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND))));
        });
    }

    public Mono<ApiResponse<List<CandidateResponse>>> listCandidates(Users authUser, Map<String, String> params, int page, int limit) {
        Document userFilters = new Document(
                "role",
                new Document("$in", List.of(RoleNames.Candidate.name()))
        );
        Document matrimonyFilters = new Document();
        Document eventFilters = new Document();

        searchFilters(params, userFilters);
        applyMatrimonyFilters(params, matrimonyFilters);
        applyEventFilters(params, eventFilters);

        Document organizationFilters = new Document();

        if (authUser.isSuperUser()
                && params.containsKey("organization") && null != params.get("organization") && !params.get("organization").isBlank()) {
            organizationFilters.put("organization_id", new ObjectId(params.get("organization")));
        } else if (authUser.isOrganization()) {
            return organizationRepository.findByUserId(authUser.getId())
                    .flatMap(org -> {
                        organizationFilters.put("organization_id", org.getId());
                        return runListPipeline(page, limit, userFilters, matrimonyFilters, eventFilters, organizationFilters, authUser);
                    });
        } else if (authUser.isAgent()) {
            return agentRepository.findByUserId(authUser.getId())
                    .flatMap(agent -> {
                        eventFilters.put("added_by", agent.getId());
                        return runListPipeline(page, limit, userFilters, matrimonyFilters, eventFilters, organizationFilters, authUser);
                    });
        } else {
            return matrimonyRepository.findByUserId(authUser.getId())
                    .flatMap(profile -> eventParticipantRepo.findByCandidateId(profile.getId())
                            .map(EventParticipants::getEventId)
                            .collectList()
                            .doOnNext(eventIds -> {
                                eventFilters.put("event_id",
                                        new Document("$in",
                                                eventIds.stream()
                                                        .toList()
                                        )
                                );
                                userFilters.put("_id", new Document("$ne", authUser.getId()));
                                matrimonyFilters.put("status", ProfileStatus.active.name());
                                matrimonyFilters.put("profile_completed", true);
                                matrimonyFilters.put("privacy_settings.hide_profile", false);
                            }).then(
                                    runListPipeline(page, limit, userFilters, matrimonyFilters, eventFilters, organizationFilters, authUser)));
        }
        return runListPipeline(page, limit, userFilters, matrimonyFilters, eventFilters, organizationFilters, authUser);
    }

    private Mono<ApiResponse<List<CandidateResponse>>> runListPipeline(int page, int limit, Document userFilters, Document matrimonyFilters, Document eventFilters, Document organizationFilters, Users authUser) {
        int skip = (page - 1) * limit;
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
                            res.forEach(candidateRes -> {
                                if (candidateRes.getMatrimony_data() != null &&
                                        candidateRes.getMatrimony_data().get_id() != null) {
                                    maskPII(candidateRes, authUser);
                                    String candidateMatrimonyId =
                                            candidateRes.getMatrimony_data().get_id();

                                    candidateRes.setIsFavorite(
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
                                                    return agentRepository.findByUserId(authUser.getId()).flatMap(agent ->
                                                                    saveEventParticipant(candidate, request, agent))
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
                                                                            agentRepository.findByUserId(authUser.getId()).flatMap(agent ->
                                                                                    saveEventParticipant(matrimonyCandidate, request, agent)
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
                                                            agentRepository.findByUserId(authUser.getId()).flatMap(agent ->
                                                                    saveEventParticipant(matrimonyCandidate, request, agent)
                                                            ).thenReturn(USER_REGISTERED)
                                                    )
                                    );
                        })
                ));
    }

    @Transactional
    public Mono<PhoneLoginResponse> myProfile(Users users) {
        if (users.isCandidate()) {
            return authService.getMatrimonyDetails(RoleNames.Candidate.name(), users);
        } else if (users.isAgent()) {
            return authService.getAgentDetails(RoleNames.Agent.name(), users);
        } else if (users.isOrganization()) {
            return authService.getOrganizationDetails(RoleNames.Organization.name(), users);
        } else {
            return Mono.just(modelMapper.map(users, PhoneLoginResponse.class));
        }
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


    @Transactional
    public Mono<FavoriteResponse> addRemoveToFavorites(String profileId, Users authUser) {
        ObjectId candidateId = new ObjectId(profileId);

        return matrimonyRepository.findByUserId(authUser.getId())
                .flatMap(candidateProfile ->
                        matrimonyRepository.findById(candidateId)
                                .flatMap(targetProfile -> {
                                    List<ObjectId> favorites = candidateProfile.getFavorites() != null ? candidateProfile.getFavorites() : new ArrayList<>();
                                    FavoriteResponse res = new FavoriteResponse();
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
        if (authUser.isOrganization()) {
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

    public Mono<Void> deactivateAccount(Users authUser) {
        return userRepository.findById(authUser.getId())
                .switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND)))
                .flatMap(user -> {
                    user.setDeletedAt(LocalDateTime.now());
                    user.setToken(null);
                    return userRepository.save(user);
                })
                .then();
    }

    private Mono<EventParticipants> saveEventParticipant(MatrimonyCandidate candidate, UserRegisterRequest request, Agents agent) {
        return eventParticipantRepo.save(EventParticipants.builder()
                .candidateId(candidate.getId())
                .eventId(new ObjectId(request.getEventId()))
                .addedBy(agent.getId())
                .organizationId(agent.getOrganizationId())
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
                .status(ProfileStatus.pending)
                .bloodDonated(false)
                .profileCompleted(false)
                .build();
    }

    private void searchFilters(Map<String, String> params, Document userFilters) {

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
    }

    private void applyMatrimonyFilters(Map<String, String> params, Document filter) {

        if (params.containsKey("gender") && null != params.get("gender") && !params.get("gender").isBlank())
            filter.put("personal_details.gender", params.get("gender"));

        if (params.containsKey("city") && null != params.get("city") && !params.get("city").isBlank())
            filter.put("address.city", params.get("city"));

        if (params.containsKey("zip") && null != params.get("zip") && !params.get("zip").isBlank())
            filter.put("address.zip", params.get("zip"));

        if (params.containsKey("status") && null != params.get("status") && !params.get("status").isBlank())
            filter.put("status", params.get("status"));
        if (params.containsKey("gotra") && null != params.get("gotra") && !params.get("gotra").isBlank())
            filter.put("personal_details.gotra", params.get("gotra"));

        if (params.containsKey("maternalGotra") && null != params.get("maternalGotra") && !params.get("maternalGotra").isBlank())
            filter.put("personal_details.maternal_gotra", params.get("maternalGotra"));

        if (params.containsKey("manglik") && null != params.get("manglik") && !params.get("manglik").isBlank())
            filter.put("personal_details.manglik", Boolean.parseBoolean(params.get("manglik")));

        if (params.containsKey("maritalStatus") && null != params.get("maritalStatus") && !params.get("maritalStatus").isBlank())
            filter.put("personal_details.marital_status", params.get("maritalStatus"));

        if (params.containsKey("bloodGroup") && null != params.get("bloodGroup") && !params.get("bloodGroup").isBlank())
            filter.put("personal_details.blood_group", params.get("bloodGroup"));

        if (params.containsKey("complexion") && null != params.get("complexion") && !params.get("complexion").isBlank())
            filter.put("personal_details.complexion", params.get("complexion"));

        if (params.containsKey("bloodDonated") && null != params.get("bloodDonated") && !params.get("bloodDonated").isBlank())
            filter.put("is_blood_donated", Boolean.parseBoolean(params.get("bloodDonated")));
        /*if(params.containsKey("myPreference") && null != params.get("myPreference") && !params.get("myPreference").isBlank() && Boolean.parseBoolean(params.get("myPreference"))){
            filter.put("partner_preferences.user_id",new ObjectId(params.get("userId")));
        }*/

        /*if(params.containsKey("height")){
           MatrimonyCandidate.PartnerPreferences.HeightRange range=(MatrimonyCandidate.PartnerPreferences.HeightRange)  params.get("height");
            filter.put("personal_details.height",params.get("height"));
        }*/

    }

    private void applyEventFilters(Map<String, String> params, Document filter) {

        if (params.containsKey("agentId") && null != params.get("agentId") && !params.get("agentId").isBlank())
            filter.put("added_by", new ObjectId(params.get("agentId")));

        if (params.containsKey("eventId") && null != params.get("eventId") && !params.get("eventId").isBlank())
            filter.put("event_id", new ObjectId(params.get("eventId")));
    }

    private Mono<Set<String>> getFavouriteIdsMono(Users authUser) {
        return matrimonyRepository.findByUserId(authUser.getId())
                .map(mp -> (mp.getFavorites() != null ? mp.getFavorites() : List.<ObjectId>of())
                        .stream()
                        .map(ObjectId::toHexString) // normalize
                        .collect(Collectors.toSet())
                )
                .defaultIfEmpty(Set.of());
    }

    private CandidateResponse addAddressInResponse(CandidateResponse response) {
        CandidateResponse.MatrimonyCandidate.Address add = response.getMatrimony_data().getAddress();
        response.getMatrimony_data().setLocalAddress(commonService.getAddressByIds(add.getAddress(), add.getCountry(), add.getState(), add.getCity(), add.getZip()));
        return response;
    }

    public void maskPII(CandidateResponse response, Users authUser) {
        if (authUser.getId().toHexString().equals(response.getId())) {
            return;
        }
        if (response.getMatrimony_data() == null || response.getMatrimony_data().getPrivacy_settings() == null) {
            return;
        }
        CandidateResponse.MatrimonyCandidate.PrivacySettings settings = response.getMatrimony_data().getPrivacy_settings();
        if (settings.isHide_phone()) {
            response.setPhone_number(UtilityHelper.maskPhoneNumber(response.getPhone_number()));
        }
        if (settings.isHide_email()) {
            response.setEmail(UtilityHelper.maskEmail(response.getEmail()));
        }
        if (settings.isHide_profile_image()) {
            response.setProfile_image(null);
        }
    }

    public Mono<String> getCandidateOrgId(Users authUser) {
        return matrimonyRepository.findByUserId(authUser.getId())
                .flatMap(profile ->
                        eventParticipantRepo.findByCandidateId(profile.getId())
                                .map(EventParticipants::getOrganizationId)
                                .collectList()
                                .map(list -> list.stream().findFirst().get().toHexString()));

    }
}
