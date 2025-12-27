package com.bandhanbook.app.service;

import com.bandhanbook.app.exception.RecordNotFoundException;
import com.bandhanbook.app.exception.UnAuthorizedException;
import com.bandhanbook.app.model.Agents;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.model.constants.RoleNames;
import com.bandhanbook.app.payload.request.AgentRequest;
import com.bandhanbook.app.payload.response.AgentResponse;
import com.bandhanbook.app.repository.AgentRepository;
import com.bandhanbook.app.repository.OrganizationRepository;
import com.bandhanbook.app.repository.UserRepository;
import com.bandhanbook.app.wrappers.AgentWrapper;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.bandhanbook.app.utilities.ErrorResponseMessages.DATA_NOT_FOUND;
import static com.bandhanbook.app.utilities.SuccessResponseMessages.AGENT_CREATED;
import static com.bandhanbook.app.utilities.SuccessResponseMessages.AGENT_UPDATED;

@Service
@AllArgsConstructor
@NoArgsConstructor
public class AgentService {
    @Autowired
    UserRepository userRepository;
    @Autowired
    AuthService authService;
    @Autowired
    AgentRepository agentRepository;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private ReactiveMongoTemplate mongoTemplate;
    @Autowired
    private CommonService commonService;

    public Mono<String> createAgent(AgentRequest request, Users authUser) {

        String role = RoleNames.Agent.name();
        Mono<Users> validUser = authService.getValidatedUser(request.getPhoneNumber(), request.getEmail(), role);
        Mono<String> orgId = Mono.just("");
        if (authUser.getRoles().contains(RoleNames.Organization.name())) {
            orgId = organizationRepository.findByUserId(authUser.getId())
                    .switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND)))
                    .map(org -> org.getId().toHexString());
        }
        return orgId
                .flatMap(org -> {
                    if (!org.isEmpty()) {
                        request.setOrganizationId(org);
                    }
                    return validUser.flatMap(existingUser -> {
                        existingUser.getRoles().add(role);
                        return saveAgent(request, existingUser);
                    }).switchIfEmpty(Mono.defer(() -> {
                        Users newUser = modelMapper.map(request, Users.class);
                        newUser.getRoles().add(role);
                        return saveAgent(request, newUser);
                    }));
                });
    }

    public Mono<AgentResponse> showAgent(String agentId, Users authUser) {
        return agentRepository.findById(agentId)
                .switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND)))
                .flatMap(agents ->
                        organizationRepository.findByUserId(authUser.getId())
                                .flatMap(org -> {
                                    if (!org.getId().equals(agents.getOrganizationId())) {
                                        return Mono.error(new UnAuthorizedException("User Not Authorized"));
                                    }
                                    return userRepository.findById(agents.getUserId())
                                            .switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND)))
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
                                            });
                                }).switchIfEmpty(Mono.defer(() -> {
                                    return userRepository.findById(agents.getUserId())
                                            .switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND)))
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
                                            });
                                })));
    }

    private Mono<String> saveAgent(AgentRequest request, Users newUser) {
        return userRepository.save(newUser)
                .flatMap(savedUser -> {
                    // Resolve Organization ID
                    String organizationId = request.getOrganizationId();
                    // 5. Create Agent
                    Agents agent = modelMapper.map(request, Agents.class);
                    agent.setUserId(savedUser.getId());
                    agent.setOrganizationId(new ObjectId(organizationId));
                    return agentRepository.save(agent)
                            .thenReturn(AGENT_CREATED);
                });
    }

    public Mono<String> updateAgent(AgentRequest request, String agentId) {
        return agentRepository.findById(agentId)
                .switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND)))
                .flatMap(existingAgent -> {
                    if (request.getFullName() != null && !request.getFullName().isEmpty()) {
                        userRepository.findById(existingAgent.getUserId())
                                .switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND))).map(users ->
                                {
                                    users.setFullName(request.getFullName());
                                    return userRepository.save(users);
                                });
                    }
                    if (request.getGender() != null && !request.getGender().isEmpty())
                        existingAgent.setGender(request.getGender());
                    if (request.getCaste() != null && !request.getCaste().isEmpty())
                        existingAgent.setCaste(request.getCaste());
                    if (request.getStatus() != null && !request.getStatus().isEmpty())
                        existingAgent.setStatus(request.getStatus());
                    if (request.getAddress() != null && !request.getAddress().isEmpty())
                        existingAgent.setAddress(request.getAddress());
                    if (request.getCity() != 0)
                        existingAgent.setCity(request.getCity());
                    if (request.getState() != 0)
                        existingAgent.setState(request.getState());
                    if (request.getCountry() != 0)
                        existingAgent.setCountry(request.getCountry());
                    if (request.getZip() != null && !request.getZip().isEmpty())
                        existingAgent.setZip(request.getZip());

                    return agentRepository.save(existingAgent)
                            .thenReturn(AGENT_UPDATED);
                });
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
        if (authUser.getRoles().contains(RoleNames.SuperUser.name())) {
            if (filterReq.get("organizationId") != null) {
                return Mono.just(filterReq.get("organizationId"));
            }
            return Mono.just("");
        } else {
            return organizationRepository.findByUserId(authUser.getId())
                    .map(org -> org.getId().toHexString())
                    .switchIfEmpty(Mono.error(new RecordNotFoundException("Organization Not Found")));
        }
    }
}
