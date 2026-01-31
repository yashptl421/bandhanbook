package com.bandhanbook.app.service;

import com.bandhanbook.app.exception.RecordNotFoundException;
import com.bandhanbook.app.exception.UnAuthorizedException;
import com.bandhanbook.app.exception.ValidationExceptions;
import com.bandhanbook.app.model.*;
import com.bandhanbook.app.model.constants.SettlementStatus;
import com.bandhanbook.app.model.constants.Status;
import com.bandhanbook.app.payload.request.RegistrationSettlementRequest;
import com.bandhanbook.app.payload.request.SettlementUpdateRequest;
import com.bandhanbook.app.payload.response.EventResponse;
import com.bandhanbook.app.payload.response.RegistrationSettlementResponse;
import com.bandhanbook.app.payload.response.SettlementHistoryResponse;
import com.bandhanbook.app.repository.AgentRepository;
import com.bandhanbook.app.repository.EventsRepository;
import com.bandhanbook.app.repository.OrganizationRepository;
import com.bandhanbook.app.repository.RegistrationSettlementRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.bandhanbook.app.utilities.ErrorResponseMessages.*;

@Service
@RequiredArgsConstructor
public class EventManagementService {
    private final AgentRepository agentRepository;
    private final RegistrationSettlementRepository repository;
    private final EventsRepository eventsRepository;
    private final OrganizationRepository orgRepository;
    private final ModelMapper modelMapper;
    private final ReactiveMongoTemplate template;


    public Mono<RegistrationSettlementResponse> createRegistrationSettlement(RegistrationSettlementRequest request, Users authUser) {
        if (authUser.isAgent()) {
            request.setStatus(SettlementStatus.PENDING);
            return settlementByAgent(request).map(res ->
                    modelMapper.map(res, RegistrationSettlementResponse.class));

        } else if (authUser.isOrganization()) {
            request.setStatus(SettlementStatus.ACCEPTED);
            return settlementByOrganization(request).map(res ->
                    modelMapper.map(res, RegistrationSettlementResponse.class));
        }
        return Mono.empty();
    }

    public Mono<RegistrationSettlementResponse> createDonationSettlement(RegistrationSettlementRequest request, Users authUser) {
        if (authUser.isAgent()) {
            request.setStatus(SettlementStatus.PENDING);
            return settlementByAgent(request).map(res ->
                    modelMapper.map(res, RegistrationSettlementResponse.class));

        } else if (authUser.isOrganization()) {
            request.setStatus(SettlementStatus.ACCEPTED);
            return settlementByOrganization(request).map(res ->
                    modelMapper.map(res, RegistrationSettlementResponse.class));
        }
        return Mono.empty();
    }


    public Mono<RegistrationSettlementResponse> updateRegistrationSettlement(SettlementUpdateRequest request, Users authUser) {
        if (!authUser.isOrganization() && request.getSettlementHistory() != null) {
            Mono.error(new UnAuthorizedException(SETTLEMENT_ACCESS_ERROR));
        }
        SettlementUpdateRequest.History history = request.getSettlementHistory();
        assert history != null;
        if (history.getStatus().equals(SettlementStatus.PENDING)) {
            Mono.error(new UnAuthorizedException(SETTLEMENT_REVERT_ERROR));
        }
        return updateSettlementByOrganization(request)
                .map(res -> modelMapper.map(res, RegistrationSettlementResponse.class));
    }

    public Mono<RegistrationSettlement> onCandidateRegistration(ObjectId agentId, ObjectId eventId, ObjectId orgId, double registrationFee) {
        Query query = new Query(Criteria.where("agent_id").is(agentId)
                .and("event_id").is(eventId)
                .and("organization_id").is(orgId));
        Update update = new Update()
                .inc("registrations", 1)
                .inc("total_amount", registrationFee)
                .inc("total_remaining_amount", registrationFee)
                .setOnInsert("total_settled_amount", 0.0)
                .setOnInsert("registration_fee", registrationFee)
                .setOnInsert("created_at", LocalDateTime.now());

        return template.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().upsert(true).returnNew(true),
                RegistrationSettlement.class
        );
    }

    public Mono<RegistrationSettlement> settlementByAgent(RegistrationSettlementRequest request) {
        Query query = Query.query(Criteria.where("_id").is(new ObjectId(request.getSettlementId())));

        return template.findOne(query, RegistrationSettlement.class)
                .switchIfEmpty(Mono.error(new RecordNotFoundException(RECORD_NOT_FOUND)))
                .flatMap(existing -> {
                    if (existing.getSettlementHistory() != null) {
                        Optional<History> pendingClosure = existing.getSettlementHistory().stream().filter(history -> history.getStatus().equals(SettlementStatus.PENDING)).findFirst();
                        if (pendingClosure.isPresent()) {
                            return Mono.error(new ValidationExceptions(PENDING_CLOSER));
                        }
                    }
                    double newRemaining = existing.getTotalRemainingAmount() - request.getSettlementAmount();

                    if (newRemaining < 0) {
                        return Mono.error(new ValidationExceptions(SETTLEMENT_INSUFFICIENT));
                    }

                    Update update = new Update()
                            .push("settlementHistory",
                                    History.builder()
                                            .id(new ObjectId())
                                            .totalAmount(existing.getTotalRemainingAmount())
                                            .settledAmount(request.getSettlementAmount())
                                            .remainingAmount(newRemaining) // will be recalculated after save if needed
                                            .status(request.getStatus())
                                            .remark(request.getRemark() != null ? request.getRemark() : "Settlement")
                                            .createdAt(LocalDateTime.now())
                                            .build()
                            );

                    return template.findAndModify(
                            query,
                            update,
                            FindAndModifyOptions.options().returnNew(true),
                            RegistrationSettlement.class
                    );
                });
    }

    public Mono<RegistrationSettlement> settlementByOrganization(RegistrationSettlementRequest request) {
        Query query = Query.query(Criteria.where("_id").is(new ObjectId(request.getSettlementId())));
        return template.findOne(query, RegistrationSettlement.class)
                .switchIfEmpty(Mono.error(new RecordNotFoundException(RECORD_NOT_FOUND)))
                .flatMap(existing -> {

                    double newRemaining = existing.getTotalRemainingAmount() - request.getSettlementAmount();

                    if (newRemaining < 0) {
                        return Mono.error(new ValidationExceptions(SETTLEMENT_INSUFFICIENT));
                    }
                    Update update = new Update()
                            .inc("total_settled_amount", request.getSettlementAmount())
                            .inc("total_remaining_amount", -request.getSettlementAmount())
                            .push("settlementHistory",
                                    History.builder()
                                            .id(new ObjectId())
                                            .settledAmount(request.getSettlementAmount())
                                            .remainingAmount(newRemaining) // will be recalculated after save if needed
                                            .status(request.getStatus())
                                            .settlementAt(LocalDateTime.now())
                                            .remark(request.getRemark() != null ? request.getRemark() : "Settlement")
                                            .createdAt(LocalDateTime.now())
                                            .build()
                            );

                    return template.findAndModify(
                            query,
                            update,
                            FindAndModifyOptions.options().returnNew(true),
                            RegistrationSettlement.class
                    );
                });
    }

    public Mono<RegistrationSettlement> updateSettlementByOrganization(SettlementUpdateRequest request) {
        SettlementUpdateRequest.History reqHistory = request.getSettlementHistory();
        Query query = Query.query(
                Criteria.where("_id").is(request.getSettlementId())
                        .and("settlementHistory.id").is(reqHistory.getId())
                        .and("settlementHistory.status").is(SettlementStatus.PENDING)
        );
        return template.findOne(query, RegistrationSettlement.class)
                .switchIfEmpty(Mono.error(new IllegalStateException(SETTLEMENT_INVALID)))
                .flatMap(settlement -> {

                    History history = settlement.getSettlementHistory().stream()
                            .filter(x -> x.getId().equals(new ObjectId(reqHistory.getId())))
                            .findFirst()
                            .orElseThrow();
                    double amount = history.getSettledAmount();
                    if (reqHistory.getStatus().equals(SettlementStatus.REJECTED)) {
                        amount = 0;
                    }

                    double newRemaining = settlement.getTotalRemainingAmount() - amount;

                    if (newRemaining < 0) {
                        return Mono.error(new ValidationExceptions(SETTLEMENT_INSUFFICIENT));
                    }

                    Update u = new Update()
                            // ✅ update specific history entry
                            .set("settlementHistory.$.status", reqHistory.getStatus())
                            .set("settlementHistory.$.settlementAt", LocalDateTime.now())

                            // ✅ update totals atomically
                            .inc("total_settled_amount", amount)
                            .inc("total_remaining_amount", -amount);

                    return template.findAndModify(
                            query, u,
                            FindAndModifyOptions.options().returnNew(true),
                            RegistrationSettlement.class
                    );
                });
    }

    public Mono<List<RegistrationSettlementResponse>> getAgentSettlementList(Users authUser, String reqAgentId) {
        Mono<Agents> agentMono;
        if (authUser.isAgent()) {
            agentMono = agentRepository.findByUserId(authUser.getId());
        } else if ((authUser.isSuperUser() || authUser.isOrganization()) && reqAgentId != null) {
            agentMono = agentRepository.findById(new ObjectId(reqAgentId));
        } else {
            return Mono.error(new RecordNotFoundException(RECORD_NOT_FOUND));
        }
        return agentMono.switchIfEmpty(Mono.error(new RecordNotFoundException(RECORD_NOT_FOUND))
        ).flatMap(agent ->
                eventsRepository.findByOrganizationIdAndStatus(agent.getOrganizationId(), Status.active)
                        .map(Events::getId)
                        .collectList()
                        .flatMap(eventIds -> {

                            if (eventIds.isEmpty()) {
                                return Mono.error(new RecordNotFoundException(RECORD_NOT_FOUND));
                            }
                            Query query = Query.query(
                                    Criteria.where("agent_id").is(agent.getId())
                                            .and("event_id").in(eventIds)
                                            .and("deleted_at").is(null)
                            );

                            return template.find(query, RegistrationSettlement.class)
                                    .switchIfEmpty(Mono.error(new RecordNotFoundException(RECORD_NOT_FOUND)))
                                    .flatMap(settlement ->
                                            eventsRepository.findById(settlement.getEventId())
                                                    .map(event -> {
                                                        RegistrationSettlementResponse response =
                                                                modelMapper.map(settlement, RegistrationSettlementResponse.class);
                                                        response.setEventDetails(
                                                                modelMapper.map(event, EventResponse.class)
                                                        );
                                                        return response;
                                                    })
                                    )
                                    .collectList();
                        })
        );
    }

    public Flux<SettlementHistoryResponse> getCloserList(Users authUser, String reqAgentId) {
        return orgRepository.findByUserId(authUser.getId())
                .flatMapMany(org -> getPendingSettlementsByOrganization(org.getId()));
    }


    public Flux<SettlementHistoryResponse> getPendingSettlementsByOrganization(
            ObjectId organizationId
    ) {

        Aggregation aggregation = Aggregation.newAggregation(

                // 1️⃣ Match organization + not deleted
                Aggregation.match(
                        Criteria.where("organization_id").is(organizationId)
                                .and("deleted_at").is(null)
                ),

                // 2️⃣ Unwind history
                Aggregation.unwind("settlementHistory"),

                // 3️⃣ Only PENDING settlements
                Aggregation.match(
                        Criteria.where("settlementHistory.status")
                                .is(SettlementStatus.PENDING)
                ),

                // 4️⃣ Sort newest first
                Aggregation.sort(
                        Sort.Direction.DESC,
                        "settlementHistory.created_at"
                ),

                // 5️⃣ Project clean response
                Aggregation.project()
                        .and("_id").as("settlementId")
                        .and("agent_id").as("agentId")
                        .and("event_id").as("eventId")
                        .and("settlementHistory._id").as("settlementHistoryId")
                        .and("settlementHistory.total_amount").as("totalAmount")
                        .and("settlementHistory.remaining_amount").as("remainingAmount")
                        .and("settlementHistory.settled_amount").as("settledAmount")
                        .and("settlementHistory.remark").as("remark")
                        .and("settlementHistory.status").as("status")
                        .and("settlementHistory.created_at").as("createdAt")
        );

        return template.aggregate(
                aggregation,
                "registration_settlement",
                SettlementHistoryResponse.class
        );
    }
}
