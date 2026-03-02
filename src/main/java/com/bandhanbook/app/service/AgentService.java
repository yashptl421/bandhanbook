package com.bandhanbook.app.service;

import com.bandhanbook.app.config.MessageUtil;
import com.bandhanbook.app.exception.RecordNotFoundException;
import com.bandhanbook.app.exception.UnAuthorizedException;
import com.bandhanbook.app.model.Agents;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.model.constants.ProfileStatus;
import com.bandhanbook.app.model.constants.RoleNames;
import com.bandhanbook.app.payload.request.AgentRequest;
import com.bandhanbook.app.payload.response.AgentResponse;
import com.bandhanbook.app.repository.AgentRepository;
import com.bandhanbook.app.repository.OrganizationRepository;
import com.bandhanbook.app.repository.UserRepository;
import com.bandhanbook.app.wrappers.AgentWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentService {
    private final UserRepository userRepository;
    private final AuthService authService;
    private final AgentRepository agentRepository;
    private final ModelMapper modelMapper;
    private final OrganizationRepository organizationRepository;
    private final ReactiveMongoTemplate mongoTemplate;
    private final CommonService commonService;
    private final UsageMetricsService usageMetricsService;
    private final LimitEnforcementComponent limitEnforcementComponent;
    private final MessageUtil messageUtil;

    public Mono<String> createAgent(AgentRequest request, Users authUser) {

        String role = RoleNames.Agent.name();

        if (!authUser.isOrganization()) {
            return Mono.error(new UnAuthorizedException(messageUtil.get("authorization.error")));
        }

        return organizationRepository.findByUserId(authUser.getId())
                .switchIfEmpty(Mono.error(new RecordNotFoundException(messageUtil.get("record.not.found"))))
                .flatMap(org -> {
                    ObjectId orgId = org.getId();
                    request.setOrganizationId(orgId.toHexString());

                    return limitEnforcementComponent.checkAgentLimit(orgId)
                            .then(authService.getValidatedUser(request.getPhoneNumber(), request.getEmail(), role))
                            .flatMap(existingUser -> {
                                existingUser.getRoles().add(role);
                                return saveAgent(request, existingUser);
                            })
                            .switchIfEmpty(Mono.defer(() -> {
                                Users newUser = modelMapper.map(request, Users.class);
                                newUser.getRoles().add(role);
                                return saveAgent(request, newUser);
                            }));
                });
    }

    public Mono<AgentResponse> showAgent(ObjectId agentId, Users authUser) {
        return agentRepository.findById(agentId)
                .switchIfEmpty(Mono.error(new RecordNotFoundException(messageUtil.get("record.not.found"))))
                .flatMap(agents ->
                        organizationRepository.findByUserId(authUser.getId())
                                .flatMap(org -> {
                                    if (!org.getId().equals(agents.getOrganizationId())) {
                                        return Mono.error(new UnAuthorizedException(messageUtil.get("authorization.error")));
                                    }
                                    return userRepository.findById(agents.getUserId())
                                            .switchIfEmpty(Mono.error(new RecordNotFoundException(messageUtil.get("record.not.found"))))
                                            .map(users -> {
                                                AgentResponse res = modelMapper.map(agents, AgentResponse.class);
                                                res.setUser_id(agents.getUserId().toHexString());
                                                res.setOrganization_id(agents.getOrganizationId().toHexString());
                                                res.setLocalAddress(commonService.getAddressByIds(agents.getAddress(), agents.getCountry(), agents.getState(), agents.getCity(), agents.getZip()));
                                                AgentResponse.UserDetails userDetails = modelMapper.map(users, AgentResponse.UserDetails.class);
                                                userDetails.setFull_name(users.getFullName());
                                                userDetails.setPhone_number(users.getPhoneNumber());
                                                userDetails.setRole(users.getRoles().get(0));
                                                userDetails.setProfile_image(users.getProfileImage());
                                                res.setUser_details(userDetails);
                                                return res;
                                            });
                                }).switchIfEmpty(Mono.defer(() -> userRepository.findById(agents.getUserId())
                                        .switchIfEmpty(Mono.error(new RecordNotFoundException(messageUtil.get("record.not.found"))))
                                        .map(users -> {
                                            AgentResponse res = modelMapper.map(agents, AgentResponse.class);
                                            res.setUser_id(agents.getUserId().toHexString());
                                            res.setOrganization_id(agents.getOrganizationId().toHexString());
                                            res.setLocalAddress(commonService.getAddressByIds(agents.getAddress(), agents.getCountry(), agents.getState(), agents.getCity(), agents.getZip()));
                                            AgentResponse.UserDetails userDetails = modelMapper.map(users, AgentResponse.UserDetails.class);
                                            userDetails.setFull_name(users.getFullName());
                                            userDetails.setPhone_number(users.getPhoneNumber());
                                            userDetails.setRole(users.getRoles().get(0));
                                            res.setUser_details(userDetails);
                                            return res;
                                        }))));
    }

    private Mono<String> saveAgent(AgentRequest request, Users newUser) {
        return userRepository.save(newUser)
                .flatMap(savedUser -> {

                    String organizationId = request.getOrganizationId();

                    Agents agent = modelMapper.map(request, Agents.class);
                    agent.setUserId(savedUser.getId());
                    agent.setOrganizationId(new ObjectId(organizationId));

                    return agentRepository.save(agent)
                            .doOnSuccess(savedAgent ->
                                    triggerAgentMetrics(savedAgent.getOrganizationId())
                            )
                            .thenReturn(messageUtil.get("agent.created"));
                });
    }

    private void triggerAgentMetrics(ObjectId orgId) {
        usageMetricsService.incrementAgents(orgId)
                .doOnError(e -> log.error("Failed to increment agent metrics", e))
                .subscribe(); // controlled fire-and-forget
    }

    @Transactional
    public Mono<String> updateAgent(AgentRequest request, ObjectId agentId) {

        return agentRepository.findById(agentId)
                .switchIfEmpty(Mono.error(new RecordNotFoundException(messageUtil.get("record.not.found"))))
                .flatMap(agent ->

                        updateUserIfRequired(agent.getUserId(), request)

                                .then(updateAgentFields(agent, request))
                                .flatMap(agentRepository::save)
                )
                .thenReturn(messageUtil.get("agent.updated"));
    }

    private Mono<Void> updateUserIfRequired(ObjectId userId, AgentRequest request) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new RecordNotFoundException(messageUtil.get("record.not.found"))))
                .flatMap(user -> {
                    if (request.getFullName() != null && !request.getFullName().isBlank()) {
                        user.setFullName(request.getFullName());
                    }
                    //block or unblock user based on status
                    if (request.getStatus() != null) {
                        user.setLocked(request.getStatus().equals(ProfileStatus.blocked));
                    }
                    // active user if removed_at is not null
                    if (null != request.getStatus() && user.getDeletedAt() != null && request.getStatus().equals(ProfileStatus.active)) {
                        user.setDeletedAt(null);
                    }

                    return userRepository.save(user);
                })
                .then();
    }

    private Mono<Agents> updateAgentFields(Agents agent, AgentRequest request) {

        if (request.getGender() != null)
            agent.setGender(request.getGender());

        if (StringUtils.hasText(request.getCaste()))
            agent.setCaste(request.getCaste());

        if (request.getStatus() != null)
            agent.setStatus(request.getStatus());

        if (StringUtils.hasText(request.getAddress()))
            agent.setAddress(request.getAddress());

        if (request.getCity() != 0)
            agent.setCity(request.getCity());

        if (request.getState() != 0)
            agent.setState(request.getState());

        if (request.getCountry() != 0)
            agent.setCountry(request.getCountry());

        if (StringUtils.hasText(request.getZip()))
            agent.setZip(request.getZip());

        return Mono.just(agent);
    }

    public Mono<List<AgentWrapper>> listAgents(Users authUser, Map<String, String> filterReq, int page, int limit) {
        int skip = (page - 1) * limit;

        return getOrgIdMono(authUser, filterReq).flatMap(orgId -> {
            Criteria criteria = new Criteria();

            if (!orgId.isEmpty())
                criteria.and("organization_id").is(new ObjectId(orgId));
            // Optional filters
            if (filterReq.get("status") != null) criteria.and("status").is(filterReq.get("status"));
            if (filterReq.get("gender") != null) criteria.and("gender").is(filterReq.get("gender"));
            if (filterReq.get("city") != null) criteria.and("city").is(filterReq.get("city"));
            if (filterReq.get("state") != null) criteria.and("state").is(filterReq.get("state"));
            if (filterReq.get("country") != null) criteria.and("country").is(filterReq.get("country"));
            if (filterReq.get("zip") != null) criteria.and("zip").is(filterReq.get("zip"));

            MatchOperation matchStage = Aggregation.match(criteria);
            LookupOperation userLookup = LookupOperation.newLookup()
                    .from("users")
                    .localField("user_id")
                    .foreignField("_id")
                    .as("user_details");


            UnwindOperation unwindUser = Aggregation.unwind("user_details");

            LookupOperation orgLookup = LookupOperation.newLookup()
                    .from("organizations")
                    .localField("organization_id")
                    .foreignField("_id")
                    .as("organization_details");


            UnwindOperation unwindOrg = Aggregation.unwind("organization_details");

            List<Criteria> searchCriteria = new ArrayList<>();
            String search = filterReq.getOrDefault("search", "");
            if (search != null && !search.isEmpty()) {
                searchCriteria.add(new Criteria().orOperator(
                        Criteria.where("user_details.full_name").regex(search, "i"),
                        Criteria.where("user_details.email").regex(search, "i")
                ));
            }

            MatchOperation searchMatch = searchCriteria.isEmpty()
                    ? null
                    : Aggregation.match(new Criteria().andOperator(searchCriteria));

            SortOperation sort = Aggregation.sort(Sort.by(Sort.Direction.DESC, "created_at"));

            FacetOperation facet = Aggregation.facet(
                            Aggregation.skip(skip),
                            Aggregation.limit(limit)
                    ).as("data")
                    .and(Aggregation.count().as("totalRecords")).as("totalRecords");

            Aggregation aggregation = Aggregation.newAggregation(
                    matchStage,
                    userLookup,
                    unwindUser,
                    orgLookup,
                    unwindOrg,
                    searchMatch != null ? searchMatch : Aggregation.match(new Criteria()),
                    sort,
                    facet
            );

            return mongoTemplate.aggregate(aggregation, "agents", AgentWrapper.class)
                    .collectList()
                    .map(Function.identity());
        });
    }

    public Mono<String> getOrgIdMono(Users authUser, Map<String, String> filterReq) {
        // SUPERUSER rule
        if (authUser.isSuperUser()) {
            if (filterReq.get("organizationId") != null) {
                return Mono.just(filterReq.get("organizationId"));
            }
            return Mono.just("");
        } else if (authUser.isAgent()) {
            return agentRepository.findByUserId(authUser.getId()).
                    map(agents -> agents.getOrganizationId().toHexString())
                    .switchIfEmpty(Mono.error(new RecordNotFoundException(messageUtil.get("organization.not.fount"))));
        } else if (authUser.isOrganization()) {
            return organizationRepository.findByUserId(authUser.getId())
                    .map(org -> org.getId().toHexString())
                    .switchIfEmpty(Mono.error(new RecordNotFoundException(messageUtil.get("organization.not.fount"))));
        } else {
            return Mono.just("");
        }
    }
}
