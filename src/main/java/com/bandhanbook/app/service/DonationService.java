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
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

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

    public Flux<DonationResponse> listDonations(Users authUser, int page, int limit) {
        int skip = (page - 1) * limit;
        if (authUser.isAgent()) {
            return donationRepository
                    .findByAgentIdAndDeletedAtIsNull(authUser.getId())
                    .map(this::toResponse);
        }

        if (authUser.isOrganization()) {
            Mono<Organization> orgMono = organizationRepository.findByUserId(authUser.getId());
            return orgMono.flatMapMany(org ->
                    donationRepository.findByOrganizationIdAndDeletedAtIsNull(org.getId())
                            .map(this::toResponse));
        }

        return donationRepository.findAll().map(this::toResponse);
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
