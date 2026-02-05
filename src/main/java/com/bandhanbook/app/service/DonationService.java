package com.bandhanbook.app.service;

import com.bandhanbook.app.exception.RecordNotFoundException;
import com.bandhanbook.app.model.Agents;
import com.bandhanbook.app.model.Donations;
import com.bandhanbook.app.model.Organization;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.model.constants.DonationStatus;
import com.bandhanbook.app.payload.request.DonationCreateRequest;
import com.bandhanbook.app.payload.request.DonationUpdateRequest;
import com.bandhanbook.app.payload.response.DonationResponse;
import com.bandhanbook.app.repository.AgentRepository;
import com.bandhanbook.app.repository.DonationRepository;
import com.bandhanbook.app.repository.EventsRepository;
import com.bandhanbook.app.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.bandhanbook.app.utilities.ErrorResponseMessages.RECORD_NOT_FOUND;
import static com.bandhanbook.app.utilities.SuccessResponseMessages.DONATION_CREATED;

@Service
@RequiredArgsConstructor
public class DonationService {
    private final DonationRepository donationRepository;
    private final EventsRepository eventsRepository;
    private final AgentRepository agentRepository;
    private final OrganizationRepository organizationRepository;
    private final EventManagementService eventManagementService;
    private final ReactiveMongoTemplate template;

    public Mono<String> createDonation(DonationCreateRequest request, Users authUser) {
        ObjectId eventId = new ObjectId(request.getEventId());
        Mono<Agents> agentsMono = agentRepository.findByUserId(authUser.getId());
        if (agentsMono == null) {
            return Mono.error(new RecordNotFoundException(RECORD_NOT_FOUND));
        }
        return agentsMono.flatMap(agent -> eventsRepository.findById(eventId)
                .switchIfEmpty(Mono.error(new RecordNotFoundException(RECORD_NOT_FOUND)))
                .flatMap(event -> {

                    Donations donation = Donations.builder()
                            .agentId(agent.getId())
                            .organizationId(event.getOrganizationId())
                            .eventId(eventId)
                            .address(request.getAddress())
                            .email(request.getEmail())
                            .phoneNumber(request.getPhoneNumber())
                            .donorType(request.getDonorType())
                            .amount(request.getAmount())
                            .donorName(request.getDonorName())
                            .remark(request.getRemark())
                            .paymentMode(request.getPaymentMode())
                            .status(DonationStatus.RECEIVED)
                            .build();

                    return donationRepository.save(donation)
                            .then(eventManagementService.onDonationCreation(agent.getId(), eventId, event.getOrganizationId(), request.getAmount()))
                            .thenReturn(DONATION_CREATED);
                }));
    }

    public Mono<Tuple2<Long, List<DonationResponse>>> listDonations(
            Users authUser,
            int page,
            int limit,
            Map<String, String> params
    ) {
        int skip = Math.max(page - 1, 0) * limit;

        Criteria criteria = new Criteria()
                .and("deleted_at").is(null);
        Mono<Criteria> criteriaMono;

        if (authUser.isAgent()) {
            criteriaMono = agentRepository.findByUserId(authUser.getId())
                    .map(agent -> criteria.and("agent_id").is(agent.getId()));

        } else if (authUser.isOrganization()) {
            criteriaMono = organizationRepository.findByUserId(authUser.getId())
                    .map(org -> criteria.and("organization_id").is(org.getId()));

        } else {
            criteriaMono = Mono.just(criteria);
        }

        criteriaMono = criteriaMono.map(c -> {

            if (params.containsKey("eventId") && !params.get("eventId").isBlank()) {
                c.and("event_id").is(new ObjectId(params.get("eventId")));
            }

            if (params.containsKey("agentId") && !params.get("agentId").isBlank()
                    && (authUser.isSuperUser() || authUser.isOrganization())) {
                c.and("agent_id").is(new ObjectId(params.get("agentId")));
            }

            return c;
        });
        return criteriaMono.flatMap(c -> {

            Query countQuery = Query.query(c);
            Query dataQuery = Query.query(c)
                    .with(Sort.by(Sort.Direction.DESC, "created_at"))
                    .skip(skip)
                    .limit(limit);

            Mono<Long> totalMono = template.count(countQuery, Donations.class);

            Mono<List<DonationResponse>> dataMono =
                    template.find(dataQuery, Donations.class)
                            .map(this::toResponse)
                            .collectList();

            return totalMono.zipWith(dataMono);
        });
    }

    /* UPDATE */
    public Mono<DonationResponse> updateDonation(
            String id,
            DonationUpdateRequest request
    ) {
        return donationRepository.findById(new ObjectId(id))
                .switchIfEmpty(Mono.error(new RecordNotFoundException(RECORD_NOT_FOUND)))
                .flatMap(d -> {
                    if (request.getAmount() != null) d.setAmount(request.getAmount());
                    if (request.getDonorName() != null) d.setDonorName(request.getDonorName());
                    if (request.getRemark() != null) d.setRemark(request.getRemark());
                    if (request.getStatus() != null) d.setStatus(request.getStatus());
                    return donationRepository.save(d);
                })
                .map(this::toResponse);
    }

    private DonationResponse toResponse(Donations d) {
        return DonationResponse.builder()
                .id(d.getId().toHexString())
                .agentId(d.getAgentId().toHexString())
                .organizationId(d.getOrganizationId().toHexString())
                .eventId(d.getEventId().toHexString())
                .amount(d.getAmount())
                .email(d.getEmail())
                .phoneNumber(d.getPhoneNumber())
                .address(d.getAddress())
                .donorName(d.getDonorName())
                .remark(d.getRemark())
                .paymentMode(d.getPaymentMode())
                .status(d.getStatus())
                .createdAt(d.getCreatedAt())
                .build();
    }

    /* DELETE (SOFT) */
    public Mono<Void> deleteDonation(String id) {
        return donationRepository.findById(new ObjectId(id))
                .switchIfEmpty(Mono.error(new RecordNotFoundException(RECORD_NOT_FOUND)))
                .flatMap(d -> {
                    d.setDeletedAt(LocalDateTime.now());
                    return donationRepository.save(d);
                })
                .then();
    }

}
