package com.bandhanbook.app.service;

import com.bandhanbook.app.exception.PhoneNumberNotFoundException;
import com.bandhanbook.app.exception.RecordNotFoundException;
import com.bandhanbook.app.model.OrgSubscriptions;
import com.bandhanbook.app.model.Organization;
import com.bandhanbook.app.model.PricingPlans;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.model.constants.RoleNames;
import com.bandhanbook.app.payload.request.OrganizationRequest;
import com.bandhanbook.app.payload.response.OrganizationResponse;
import com.bandhanbook.app.repository.OrgSubscriptionsRepository;
import com.bandhanbook.app.repository.OrganizationRepository;
import com.bandhanbook.app.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.bandhanbook.app.utilities.ErrorResponseMessages.*;

@Service
public class OrganizationService {

    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ResourceLoader resourceLoader;
    @Autowired
    private OrgSubscriptionsRepository orgSubscriptionsRepository;
    @Autowired
    private ReactiveMongoTemplate template;

    private List<PricingPlans> cachedPlans = null;

    public Mono<Tuple2<Long, List<OrganizationResponse>>> listOrganizations(Map<String, String> params) {

        int page = Integer.parseInt(params.getOrDefault("page", "1"));
        int limit = Integer.parseInt(params.getOrDefault("limit", "10"));
        String search = params.getOrDefault("search", "");
        String organizationId = params.get("organizationId");
        String status = params.get("status");

        // 1. Base organization stream
        Flux<Organization> organizationsFlux = organizationRepository.findAll()
                .filter(org -> {

                    boolean match = true;

                    if (organizationId != null && !organizationId.isEmpty()) {
                        match = org.getId().equals(organizationId);
                    }

                    if (status != null && !status.isEmpty()) {
                        match = match && status.equalsIgnoreCase(org.getStatus());
                    }

                    return match;
                });

        // 2. Email search filter
        if (!search.isBlank()) {
            organizationsFlux = organizationsFlux
                    .flatMap(org ->
                            userRepository.findById(org.getUserId())
                                    .filter(user -> user.getEmail() != null &&
                                            user.getEmail().toLowerCase().contains(search.toLowerCase()))
                                    .map(u -> org)
                    );
        }

        // 3. Count total before pagination
        Mono<Long> totalMono = organizationsFlux.count();

        // 4. Paginate
        Flux<Organization> pagedFlux = organizationsFlux
                .skip((long) (page - 1) * limit)
                .take(limit);

        // 5. Convert to response
        Flux<OrganizationResponse> responseFlux = pagedFlux.flatMap(org ->
                userRepository.findById(org.getUserId()).map(user -> {
                    OrganizationResponse res = modelMapper.map(org, OrganizationResponse.class);
                    res.setUser_details(user);
                    return res;
                })
        );
        return totalMono.zipWith(responseFlux.collectList());
    }

    public Mono<OrganizationResponse> getOrganizationById(String id) {
        return organizationRepository.findById(id)
                .switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND)))
                .flatMap(org ->
                        userRepository.findById(org.getUserId())
                                .switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND)))
                                .zipWith(
                                        orgSubscriptionsRepository.findByOrgId(org.getId())
                                                .switchIfEmpty(Mono.just(new OrgSubscriptions())),  // avoid null
                                        (user, subscription) -> {
                                            OrganizationResponse res = modelMapper.map(org, OrganizationResponse.class);
                                            res.setUser_details(user);
                                            res.setSubscription(subscription);
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
                        return Mono.error(new PhoneNumberNotFoundException(PHONE_EXISTS));
                    }
                    return Mono.empty();
                }).then(userRepository
                        .existsByEmail(organizationRequest.getEmail())
                        .flatMap(exists -> {
                            if (exists) {
                                return Mono.error(new PhoneNumberNotFoundException(EMAIL_EXISTS));
                            }
                            return Mono.empty();
                        }))

                .then(Mono.defer(() -> {
                    Users user = getOrgRequestUser(organizationRequest, role);
                    return userRepository.save(user)
                            .flatMap(savedUser -> {
                                Organization org = modelMapper.map(organizationRequest, Organization.class);
                                org.setUserId(savedUser.getId());
                                return organizationRepository.save(org)
                                        .flatMap(savedOrg ->
                                                getOrgSubscriptions(organizationRequest, savedOrg)
                                                        .flatMap(orgSubscriptionsRepository::save)
                                        );
                            });
                }))
                .then();
    }


    @Transactional
    public Mono<Void> updateOrganization(OrganizationRequest organizationRequest, String id) {
        return organizationRepository.findById(id).switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND))).flatMap(existingOrg -> userRepository.findById(existingOrg.getUserId()).switchIfEmpty(Mono.error(new RecordNotFoundException(DATA_NOT_FOUND))).flatMap(existingUser -> {

            // STEP 1 — validate duplicate phone (but ignore same user)
            Mono<Void> phoneCheck = userRepository.existsByPhoneNumber(organizationRequest.getPhoneNumber()).flatMap(exists -> {
                if (exists && !organizationRequest.getPhoneNumber().equals(existingUser.getPhoneNumber())) {
                    return Mono.error(new PhoneNumberNotFoundException(PHONE_EXISTS));
                }
                return Mono.empty();
            });

            // STEP 2 — validate duplicate email (but ignore same user)
            Mono<Void> emailCheck = userRepository.existsByEmail(organizationRequest.getEmail()).flatMap(exists -> {
                if (exists && !organizationRequest.getEmail().equals(existingUser.getEmail())) {
                    return Mono.error(new PhoneNumberNotFoundException(EMAIL_EXISTS));
                }
                return Mono.empty();
            });

            return Mono.when(phoneCheck, emailCheck) // run validations
                    .then(Mono.defer(() -> {
                        existingUser.setFullName(organizationRequest.getFullName());
                        existingUser.setPhoneNumber(organizationRequest.getPhoneNumber());
                        existingUser.setEmail(organizationRequest.getEmail());
                        return userRepository.save(existingUser);
                    })).flatMap(updatedUser -> {
                        modelMapper.map(organizationRequest, existingOrg);
                        // STEP 4 — update organization
                        return organizationRepository.save(existingOrg);
                    }).flatMap(savedOrg -> {

                        // STEP 5 — update subscription (optional)
                        return getOrgSubscriptions(organizationRequest, savedOrg).flatMap(subscription -> orgSubscriptionsRepository.findByOrgId(savedOrg.getId()).flatMap(existingSubscription -> {
                            //modelMapper.map(subscription, existingSubscription);
                            existingSubscription.setPlanId(subscription.getPlanId());
                            existingSubscription.setMaxAgents(subscription.getMaxAgents());
                            existingSubscription.setMaxUsers(subscription.getMaxUsers());
                            existingSubscription.setStartDate(subscription.getStartDate());
                            existingSubscription.setEndDate(subscription.getEndDate());
                            existingSubscription.setRegistrationPeriod(subscription.getRegistrationPeriod());
                            // Update existing subscription
                            return orgSubscriptionsRepository.save(existingSubscription);
                        }).switchIfEmpty(
                                // Create new subscription
                                orgSubscriptionsRepository.save(subscription)));
                    });
        })).then();
    }

    public Mono<List<PricingPlans>> getPricingPlans() {
        if (cachedPlans != null) {
            return Mono.just(cachedPlans);
        }

        return Mono.fromCallable(() -> {
                    Resource resource =
                            resourceLoader.getResource("classpath:json/pricingPlans.json");

                    try (InputStream inputStream = resource.getInputStream()) {
                        return Arrays.asList(objectMapper.readValue(inputStream, PricingPlans[].class));
                    }
                })
                .doOnNext(list -> cachedPlans = list)        // store in cache
                .subscribeOn(Schedulers.boundedElastic()); // File I/O → must run on elastic thread
    }

    private Users getOrgRequestUser(OrganizationRequest request, String role) {
        return Users.builder()
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .fullName(request.getFullName())
                .role(role)
                .build();
    }

    private Mono<PricingPlans> getPlanById(String id) {
        return getPricingPlans()
                .flatMapMany(Flux::fromIterable)
                .filter(plan -> plan.getId().equals(id))
                .next()                               // returns Mono<PricingPlan>
                .switchIfEmpty(Mono.error(new RecordNotFoundException(PLAN_NOT_FOUND)));
    }

    private Mono<OrgSubscriptions> getOrgSubscriptions(OrganizationRequest organizationRequest, Organization organization) {
        return getPlanById(organizationRequest.getPlanId())
                .map(plan -> OrgSubscriptions.builder()
                        .planId(plan.getId())
                        .orgId(organization.getId())
                        .maxAgents(plan.getMaxAgents())
                        .maxUsers(plan.getMaxUsers())
                        .registrationPeriod(organizationRequest.getPlanStartDate()
                                .minusDays(plan.getRegistrationPeriod())
                                .toString())
                        .startDate(organizationRequest.getPlanStartDate().toString())
                        .endDate(organizationRequest.getPlanStartDate().plusYears(1).toString())
                        .active(false)
                        .build()
                );
    }
}
