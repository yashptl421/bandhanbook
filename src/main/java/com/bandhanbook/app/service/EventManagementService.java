package com.bandhanbook.app.service;

import com.bandhanbook.app.exception.RecordNotFoundException;
import com.bandhanbook.app.exception.UnAuthorizedException;
import com.bandhanbook.app.exception.ValidationExceptions;
import com.bandhanbook.app.model.Agents;
import com.bandhanbook.app.model.History;
import com.bandhanbook.app.model.RegistrationSettlement;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.model.constants.SettlementStatus;
import com.bandhanbook.app.payload.request.RegistrationSettlementRequest;
import com.bandhanbook.app.payload.request.SettlementUpdateRequest;
import com.bandhanbook.app.payload.response.RegistrationSettlementResponse;
import com.bandhanbook.app.repository.AgentRepository;
import com.bandhanbook.app.repository.RegistrationSettlementRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.modelmapper.ModelMapper;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.bandhanbook.app.utilities.ErrorResponseMessages.*;

@Service
@RequiredArgsConstructor
public class EventManagementService {
    private final AgentRepository agentRepository;
    private final RegistrationSettlementRepository repository;
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

    public Mono<RegistrationSettlement> approveSettlement(
            ObjectId settlementId,
            ObjectId historyId
    ) {
        Query q = Query.query(
                Criteria.where("_id").is(settlementId)
                        .and("settlementHistory.id").is(historyId)
                        .and("settlementHistory.status").is(SettlementStatus.PENDING)
        );

        return template.findOne(q, RegistrationSettlement.class)
                .switchIfEmpty(Mono.error(new IllegalStateException("Invalid settlement request")))
                .flatMap(s -> {

                    History h = s.getSettlementHistory().stream()
                            .filter(x -> x.getId().equals(historyId))
                            .findFirst()
                            .orElseThrow();

                    double amount = h.getSettledAmount();
                    double newRemaining = s.getTotalRemainingAmount() - amount;

                    if (newRemaining < 0) {
                        return Mono.error(new IllegalStateException("Insufficient remaining amount"));
                    }

                    Update u = new Update()
                            // ✅ update specific history entry
                            .set("settlementHistory.$.status", SettlementStatus.ACCEPTED)
                            .set("settlementHistory.$.remaining_amount", newRemaining)
                            .set("settlementHistory.$.settlementAt", LocalDateTime.now())

                            // ✅ update totals atomically
                            .inc("total_settled_amount", amount)
                            .inc("total_remaining_amount", -amount);

                    return template.findAndModify(
                            q, u,
                            FindAndModifyOptions.options().returnNew(true),
                            RegistrationSettlement.class
                    );
                });
    }

    public Mono<RegistrationSettlementResponse> getAgentCollectionList(Users authUser, String agentId) {
        Mono<ObjectId> agentIdMono = null;
        if (agentId != null && (authUser.isSuperUser() || authUser.isOrganization())) {
            agentIdMono = Mono.just(new ObjectId(agentId));
        } else if (authUser.isAgent()) {
            agentIdMono = agentRepository.findByUserId(authUser.getId()).map(Agents::getId);

        }
        assert agentIdMono != null;
        return agentIdMono.flatMap(id -> repository.findByAgentId(id).map(res ->
                modelMapper.map(res, RegistrationSettlementResponse.class)));
    }
}
