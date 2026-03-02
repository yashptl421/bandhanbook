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
        if (!authUser.isOrganization()) {
            return Mono.error(new UnAuthorizedException(messageUtil.get("authorization.error")));
        }
        return organizationRepository.findByUserId(authUser.getId())
                .switchIfEmpty(Mono.error(new RecordNotFoundException(messageUtil.get("record.not.found"))))
                .flatMap(org -> {

                    ObjectId orgId = org.getId();
                    request.setOrganizationId(orgId.toHexString());

                    return limitEnforcementComponent.checkAgentLimit(orgId)
                            .then(authService.getValidatedUser(request.getPhoneNumber(), request.getEmail(), RoleNames.Agent.name()))
                            .defaultIfEmpty(modelMapper.map(request, Users.class))
                            .flatMap(user -> {
                                user.getRoles().add(RoleNames.Agent.name());
                                return userRepository.save(user);
                            })
                            .flatMap(savedUser -> saveAgentRecord(request, savedUser));
                });
    }

    public Mono<AgentResponse> showAgent(ObjectId agentId, Users authUser) {

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("_id").is(agentId)),

                Aggregation.lookup("users", "user_id", "_id", "user_details"),
                Aggregation.unwind("user_details", true),

                Aggregation.lookup("organizations", "organization_id", "_id", "organization_details"),
                Aggregation.unwind("organization_details", true)
        );

        return mongoTemplate.aggregate(aggregation, "agents", AgentResponse.class)
                .next()
                .doOnNext(doc -> log.info("Agent aggregation result: {}", doc))
                .switchIfEmpty(Mono.error(new RecordNotFoundException(messageUtil.get("record.not.found"))))
                .map(res -> {
                            res.setLocalAddress(commonService.getAddressByIds(res.getAddress(), res.getCountry(), res.getState(), res.getCity(), res.getZip()));
                            return res;
                        }
                );
    }

    private Mono<String> saveAgentRecord(AgentRequest request, Users savedUser) {

        Agents agent = modelMapper.map(request, Agents.class);
        agent.setUserId(savedUser.getId());
        agent.setOrganizationId(new ObjectId(request.getOrganizationId()));

        return agentRepository.save(agent)
                .then(usageMetricsService.incrementAgents(agent.getOrganizationId()))
                .thenReturn(messageUtil.get("agent.created"));
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

    public Mono<AgentWrapper> listAgents(Users authUser, Map<String, String> filterReq, int page, int limit) {
        int skip = (page - 1) * limit;
        return getOrgIdMono(authUser, filterReq).flatMap(orgId -> {
            Criteria criteria = new Criteria();
            if (!orgId.isEmpty()) {
                criteria.and("organization_id").is(new ObjectId(orgId));
            }
            if (filterReq.get("status") != null)
                criteria.and("status").is(filterReq.get("status"));

            if (filterReq.get("gender") != null)
                criteria.and("gender").is(filterReq.get("gender"));

            if (filterReq.get("city") != null)
                criteria.and("city").is(filterReq.get("city"));

            if (filterReq.get("state") != null)
                criteria.and("state").is(filterReq.get("state"));

            if (filterReq.get("country") != null)
                criteria.and("country").is(filterReq.get("country"));

            if (filterReq.get("zip") != null)
                criteria.and("zip").is(filterReq.get("zip"));
            MatchOperation matchStage = Aggregation.match(criteria);
            LookupOperation userLookup = Aggregation.lookup(
                    "users", "user_id", "_id", "user_details");
            UnwindOperation unwindUser =
                    Aggregation.unwind("user_details", true); // safe unwind
            LookupOperation orgLookup = Aggregation.lookup(
                    "organizations", "organization_id", "_id", "organization_details");
            UnwindOperation unwindOrg =
                    Aggregation.unwind("organization_details", true);
            List<AggregationOperation> operations = new ArrayList<>();
            operations.add(matchStage);
            operations.add(userLookup);
            operations.add(unwindUser);
            operations.add(orgLookup);
            operations.add(unwindOrg);

            String search = filterReq.get("search");
            if (search != null && !search.isBlank()) {
                operations.add(Aggregation.match(
                        new Criteria().orOperator(
                                Criteria.where("user_details.full_name").regex(search, "i"),
                                Criteria.where("user_details.email").regex(search, "i")
                        )
                ));
            }
            operations.add(Aggregation.sort(Sort.Direction.DESC, "created_at"));
            operations.add(Aggregation.facet(
                                    Aggregation.skip(skip),
                                    Aggregation.limit(limit)
                            ).as("data")
                            .and(Aggregation.count().as("totalRecords")).as("totalRecords")
            );
            Aggregation aggregation = Aggregation.newAggregation(operations);
            return mongoTemplate.aggregate(aggregation, "agents", AgentWrapper.class)
                    .next()
                    .defaultIfEmpty(new AgentWrapper());
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
