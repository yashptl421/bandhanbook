package com.bandhanbook.app.service;

import com.bandhanbook.app.config.MessageUtil;
import com.bandhanbook.app.exception.PhoneNumberNotFoundException;
import com.bandhanbook.app.exception.RecordNotFoundException;
import com.bandhanbook.app.model.OrgSubscriptions;
import com.bandhanbook.app.model.Organization;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.model.constants.RoleNames;
import com.bandhanbook.app.model.constants.Status;
import com.bandhanbook.app.payload.request.OrganizationRequest;
import com.bandhanbook.app.payload.response.OrgSubscriptionsResponse;
import com.bandhanbook.app.payload.response.OrganizationResponse;
import com.bandhanbook.app.payload.response.UserResponse;
import com.bandhanbook.app.repository.*;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final OrgSubscriptionsRepository orgSubscriptionsRepository;
    private final EventsRepository eventsRepository;
    private final CommonService commonService;
    private final MessageUtil messageUtil;
    private final AgentRepository agentRepository;

    public Mono<Tuple2<Long, List<OrganizationResponse>>> listOrganizations(Map<String, String> params) {

        int page = Integer.parseInt(params.getOrDefault("page", "1"));
        int limit = Integer.parseInt(params.getOrDefault("limit", "10"));
        String search = params.getOrDefault("search", "");
        String organizationId = params.get("organizationId");
        String status = params.get("status");
        Flux<Organization> organizationsFlux = organizationRepository.findAll()
                .filter(org -> {
                    boolean match = true;
                    if (organizationId != null && !organizationId.isEmpty()) {
                        match = org.getId().toHexString().equals(organizationId);
                    }
                    if (status != null && !status.isEmpty()) {
                        match = match && status.equalsIgnoreCase(org.getStatus());
                    }
                    return match;
                });
        if (!search.isBlank()) {
            organizationsFlux = organizationsFlux
                    .flatMap(org ->
                            userRepository.findById(org.getUserId())
                                    .filter(user -> user.getEmail() != null &&
                                            user.getEmail().toLowerCase().contains(search.toLowerCase()))
                                    .map(u -> org)
                    );
        }
        Mono<Long> totalMono = organizationsFlux.count();
        Flux<Organization> pagedFlux = organizationsFlux
                .skip((long) (page - 1) * limit)
                .take(limit);

        Flux<OrganizationResponse> responseFlux = pagedFlux.flatMap(org ->
                userRepository.findById(org.getUserId()).map(user -> {
                    OrganizationResponse res = modelMapper.map(org, OrganizationResponse.class);
                    res.setUser_details(modelMapper.map(user, UserResponse.class));
                    return res;
                })
        );
        return totalMono.zipWith(responseFlux.collectList());
    }

    public Mono<OrganizationResponse> getOrganizationById(ObjectId id) {
        return organizationRepository.findById(id)
                .switchIfEmpty(Mono.error(new RecordNotFoundException(messageUtil.get("record.not.found"))))
                .flatMap(org ->
                        userRepository.findById(org.getUserId())
                                .switchIfEmpty(Mono.error(new RecordNotFoundException(messageUtil.get("record.not.found"))))
                                .zipWith(
                                        orgSubscriptionsRepository.findByOrgIdAndActive(org.getId(), true)
                                                .switchIfEmpty(Mono.just(new OrgSubscriptions())),
                                        (user, subscription) -> {
                                            OrganizationResponse res = modelMapper.map(org, OrganizationResponse.class);
                                            res.setUser_details(modelMapper.map(user, UserResponse.class));
                                            res.setSubscription(modelMapper.map(subscription, OrgSubscriptionsResponse.class));
                                            res.setLocalAddress(commonService.getAddressByIds(res.getAddress(), res.getCountry(), res.getState(), res.getCity(), res.getZip()));
                                            return res;
                                        })
                );
    }

    @Transactional
    public Mono<Void> createOrganization(OrganizationRequest organizationRequest) {
        String role = RoleNames.Organization.name();
        return userRepository
                .existsByPhoneNumber(organizationRequest.getPhoneNumber())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new PhoneNumberNotFoundException(messageUtil.get("phone.exist")));
                    }
                    return Mono.empty();
                }).then(userRepository
                        .existsByEmail(organizationRequest.getEmail())
                        .flatMap(exists -> {
                            if (exists) {
                                return Mono.error(new PhoneNumberNotFoundException(messageUtil.get("email.exist")));
                            }
                            return Mono.empty();
                        }))

                .then(Mono.defer(() -> {
                    Users user = getOrgRequestUser(organizationRequest, role);
                    return userRepository.save(user)
                            .flatMap(savedUser -> {
                                Organization org = modelMapper.map(organizationRequest, Organization.class);
                                org.setUserId(savedUser.getId());
                                return organizationRepository.save(org);
                            });
                }))
                .then();
    }

    public Mono<Void> updateOrganization(OrganizationRequest request, ObjectId id, Users authUser) {

        return organizationRepository.findById(id)
                .switchIfEmpty(Mono.error(new RecordNotFoundException(messageUtil.get("record.not.found"))))

                .flatMap(org ->
                        userRepository.findById(org.getUserId())
                                .switchIfEmpty(Mono.error(new RecordNotFoundException(messageUtil.get("record.not.found"))))
                                .flatMap(user -> processUpdate(user, org, request, authUser))
                )
                .then();
    }

    private Mono<Void> processUpdate(Users user, Organization org, OrganizationRequest request, Users authUser) {
        Mono<Void> statusOperation = Mono.empty();
        if (authUser.isSuperUser() && request.getStatus() != null) {
            if (Status.inactive.name().equals(request.getStatus())) {
                statusOperation = Mono.when(
                        agentRepository.deactivateAgentsByOrganizationId(org.getId()),
                        eventsRepository.deactivateEventsByOrganizationId(org.getId()),
                        organizationRepository.deactivateOrganizationById(org.getId())
                );
            } else if (Status.active.name().equals(request.getStatus())) {
                statusOperation = Mono.when(
                        agentRepository.activateAgentsByOrganizationId(org.getId()),
                        eventsRepository.activateEventsByOrganizationId(org.getId()),
                        organizationRepository.activateOrganizationById(org.getId())
                );
            }

            user.setPhoneNumber(request.getPhoneNumber());
            user.setEmail(request.getEmail());
        }

        user.setFullName(request.getFullName());
        modelMapper.map(request, org);
        Mono<Users> saveUser = userRepository.save(user);
        Mono<Organization> saveOrg = organizationRepository.save(org);
        return statusOperation
                .then(Mono.when(saveUser, saveOrg))
                .then();
    }

    private Users getOrgRequestUser(OrganizationRequest request, String role) {
        return Users.builder()
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .fullName(request.getFullName())
                .role(role)
                .build();
    }

}
