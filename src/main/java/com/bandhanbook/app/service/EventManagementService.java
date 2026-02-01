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
import com.bandhanbook.app.payload.response.SettlementSummaryResponse;
import com.bandhanbook.app.payload.response.base.ApiResponse;
import com.bandhanbook.app.repository.AgentRepository;
import com.bandhanbook.app.repository.EventsRepository;
import com.bandhanbook.app.repository.OrganizationRepository;
import com.bandhanbook.app.repository.RegistrationSettlementRepository;
import com.bandhanbook.app.utilities.UtilityHelper;
import com.bandhanbook.app.wrappers.SettlementHistoryWrapper;
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
import java.util.Map;
import java.util.Optional;

import static com.bandhanbook.app.utilities.ErrorResponseMessages.*;
import static com.bandhanbook.app.utilities.SuccessResponseMessages.DATA_FOUND;

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
            return settlementByAgent(request, authUser).map(res ->
                    modelMapper.map(res, RegistrationSettlementResponse.class));

        } /*else if (authUser.isOrganization()) {
            request.setStatus(SettlementStatus.ACCEPTED);
            return settlementByOrganization(request).map(res ->
                    modelMapper.map(res, RegistrationSettlementResponse.class));
        }*/
        return Mono.empty();
    }

    public Mono<RegistrationSettlementResponse> updateRegistrationSettlement(SettlementUpdateRequest request, Users authUser) {
        if (!authUser.isOrganization() || request.getSettlementHistory() == null) {
            return Mono.error(new UnAuthorizedException(SETTLEMENT_ACCESS_ERROR));
        }

        SettlementUpdateRequest.History history = request.getSettlementHistory();

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

    public Mono<RegistrationSettlement> onDonationCreation(ObjectId agentId, ObjectId eventId, ObjectId orgId, double amount) {
        Query query = new Query(Criteria.where("agent_id").is(agentId)
                .and("event_id").is(eventId)
                .and("organization_id").is(orgId));
        Update update = new Update()
                .inc("donationCount", 1)
                .inc("total_amount", amount)
                .inc("total_remaining_amount", amount)
                .setOnInsert("total_settled_amount", 0.0)
                .setOnInsert("recentAmount", amount)
                .setOnInsert("created_at", LocalDateTime.now());

        return template.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().upsert(true).returnNew(true),
                RegistrationSettlement.class
        );
    }

    public Mono<RegistrationSettlement> settlementByAgent(RegistrationSettlementRequest request, Users authUser) {
        Mono<ObjectId> agentIdMono = agentRepository.findByUserId(authUser.getId()).map(Agents::getId);

        return agentIdMono.flatMap(agentId ->
                repository.findByAgentIdAndEventId(agentId, new ObjectId(request.getEventId()))
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
                            Query query = Query.query(Criteria.where("event_id").is(new ObjectId(request.getEventId()))
                                    .and("agent_id").is(agentId)
                            );
                            Update update = new Update()
                                    .push("settlementHistory",
                                            History.builder()
                                                    .id(new ObjectId())
                                                    .totalAmount(existing.getTotalRemainingAmount())
                                                    .batchId(UtilityHelper.getRegistrationBatchId())
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
                        }).switchIfEmpty(Mono.error(new RecordNotFoundException(SETTLEMENT_NOT_FOUND))));
    }

   /* public Mono<RegistrationSettlement> settlementByOrganization(RegistrationSettlementRequest request) {
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
    }*/

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

    public Mono<List<RegistrationSettlementResponse>> getAgentSettlementList(Users authUser, String reqAgentId, String eventId) {
        Mono<Agents> agentMono = resolveAgent(authUser, reqAgentId);
        return agentMono.flatMap(agent -> {
            Mono<List<ObjectId>> eventIdsMono = !eventId.isBlank() ? Mono.just(List.of(new ObjectId(eventId))) : eventsRepository
                    .findByOrganizationIdAndStatus(agent.getOrganizationId(), Status.active)
                    .map(Events::getId)
                    .collectList()
                    .filter(list -> !list.isEmpty())
                    .switchIfEmpty(
                            Mono.error(new RecordNotFoundException(RECORD_NOT_FOUND))
                    );
            return eventIdsMono.flatMap(eventIds -> {
                Query query = Query.query(
                        Criteria.where("agent_id").is(agent.getId())
                                .and("event_id").in(eventIds)
                                .and("deleted_at").is(null)
                );

                Flux<RegistrationSettlement> settlements =
                        template.find(query, RegistrationSettlement.class);

                Mono<Map<ObjectId, Events>> eventMapMono = eventsRepository.findAllById(eventIds).collectMap(Events::getId);

                return settlements
                        .collectList()
                        .filter(list -> !list.isEmpty())
                        .switchIfEmpty(Mono.error(new RecordNotFoundException(RECORD_NOT_FOUND)))
                        .zipWith(eventMapMono)
                        .map(tuple -> {
                            List<RegistrationSettlement> settlementList = tuple.getT1();
                            Map<ObjectId, Events> eventMap = tuple.getT2();
                            return settlementList.stream()
                                    .map(settlement -> {
                                        RegistrationSettlementResponse response = modelMapper.map(settlement, RegistrationSettlementResponse.class);
                                        Events event = eventMap.get(settlement.getEventId());
                                        if (event != null) {
                                            response.setEventDetails(modelMapper.map(event, EventResponse.class));
                                        }
                                        return response;
                                    })
                                    .toList();
                        });
            });
        });
    }

    public Mono<ApiResponse<List<SettlementHistoryResponse>>> getCloserList(Users authUser, String reqAgentId, int page, int limit) {
        if (authUser.isOrganization()) {
            return orgRepository.findByUserId(authUser.getId())
                    .flatMap(org -> getPendingSettlements(org.getId(), reqAgentId.isBlank() ? null : new ObjectId(reqAgentId), page, limit));
        } else if (authUser.isAgent()) {
            return agentRepository.findByUserId(authUser.getId()).flatMap(agents -> getPendingSettlements(null, agents.getId(), page, limit));
        } else
            return getPendingSettlements(null, reqAgentId.isBlank() ? null : new ObjectId(reqAgentId), page, limit);
    }

    public Mono<SettlementSummaryResponse> getSettlementSummary(Users authUser, String eventId) {
        Criteria criteria = Criteria.where("deleted_at").is(null);
        if (eventId != null && !eventId.isBlank())
            criteria.and("event_id").is(new ObjectId(eventId));

        if (authUser.isOrganization()) {
            return orgRepository.findByUserId(authUser.getId())
                    .flatMap(org -> {
                        criteria.and("organization_id").is(org.getId());
                        return settlementSummaryAggregation(template, criteria);
                    });
        } else if (authUser.isAgent()) {
            return agentRepository.findByUserId(authUser.getId())
                    .flatMap(agent -> {
                        criteria.and("agent_id").is(agent.getId());
                        return settlementSummaryAggregation(template, criteria);
                    });
        } else {
            return settlementSummaryAggregation(template, criteria);
        }
    }

    private Mono<SettlementSummaryResponse> settlementSummaryAggregation(ReactiveMongoTemplate template, Criteria criteria) {
        Aggregation aggregation = Aggregation.newAggregation(Aggregation.match(criteria),
                Aggregation.group()
                        .sum("total_settled_amount").as("totalSettledAmount")
                        .sum("total_remaining_amount").as("totalRemainingAmount")
                        .sum("total_amount").as("totalAmount")
                        .count().as("totalSettlements")
        );

        return template.aggregate(aggregation, "registration_settlement", SettlementSummaryResponse.class)
                .next()
                .defaultIfEmpty(new SettlementSummaryResponse());
    }

    public Mono<RegistrationSettlementResponse> getSettlementById(String id, Users authUser) {
        Mono<RegistrationSettlement> settlementMono = repository.findById(new ObjectId(id))
                .switchIfEmpty(Mono.error(new RecordNotFoundException(SETTLEMENT_NOT_FOUND)));

        if (authUser.isOrganization()) {
            return orgRepository.findByUserId(authUser.getId())
                    .flatMap(org -> settlementMono.filter(s -> s.getOrganizationId().equals(org.getId()))
                            .switchIfEmpty(Mono.error(new UnAuthorizedException(SETTLEMENT_ACCESS_ERROR))))
                    .map(s -> modelMapper.map(s, RegistrationSettlementResponse.class));
        } else if (authUser.isAgent()) {
            return agentRepository.findByUserId(authUser.getId())
                    .flatMap(agent -> settlementMono.filter(s -> s.getAgentId().equals(agent.getId()))
                            .switchIfEmpty(Mono.error(new UnAuthorizedException(SETTLEMENT_ACCESS_ERROR))))
                    .map(s -> modelMapper.map(s, RegistrationSettlementResponse.class));
        } else {
            return settlementMono.map(s -> modelMapper.map(s, RegistrationSettlementResponse.class));
        }
    }

    public Mono<SettlementHistoryResponse> getSettlementHistoryById(String id, Users authUser) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(
                        Criteria.where("settlementHistory._id").is(new ObjectId(id))
                                .and("deleted_at").is(null)
                ),
                Aggregation.unwind("settlementHistory"),
                Aggregation.match(
                        Criteria.where("settlementHistory._id").is(new ObjectId(id))
                ),
                Aggregation.project()
                        .and("_id").as("settlementId")
                        .and("agent_id").as("agentId")
                        .and("event_id").as("eventId")
                        .and("organization_id").as("organizationId")
                        .and("settlementHistory._id").as("settlementHistoryId")
                        .and("settlementHistory.total_amount").as("totalAmount")
                        .and("settlementHistory.batch_id").as("batchId")
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
        ).next().switchIfEmpty(Mono.error(new RecordNotFoundException(SETTLEMENT_NOT_FOUND)));
    }

    public Mono<ApiResponse<List<SettlementHistoryResponse>>> getPendingSettlements(ObjectId organizationId, ObjectId agentId, int page, int limit) {
        int skip = (page - 1) * limit;
        Criteria baseCriteria = Criteria.where("deleted_at").is(null);
        if (organizationId != null) {
            baseCriteria = baseCriteria.and("organization_id").is(organizationId);
        }
        if (agentId != null) {
            baseCriteria = baseCriteria.and("agent_id").is(agentId);
        }
        Aggregation aggregation = Aggregation.newAggregation(

                Aggregation.match(baseCriteria),

                Aggregation.unwind("settlementHistory"),

                Aggregation.match(
                        Criteria.where("settlementHistory.status")
                                .is(SettlementStatus.PENDING)
                ),
                Aggregation.lookup(
                        "agents",
                        "agent_id",
                        "_id",
                        "agent"
                ),
                Aggregation.unwind("agent"),

                Aggregation.lookup(
                        "users",
                        "agent.user_id",
                        "_id",
                        "agent_user"
                ),
                Aggregation.unwind("agent_user"),

                Aggregation.sort(
                        Sort.Direction.DESC,
                        "settlementHistory.created_at"
                ),
                Aggregation.facet(
                                Aggregation.skip(skip),
                                Aggregation.limit(limit),
                                Aggregation.project()
                                        .and("_id").as("settlementId")
                                        .and("agent_id").as("agentId")
                                        .and("event_id").as("eventId")
                                        .and("agent_user.full_name").as("agentName")
                                        .and("organization_id").as("organizationId")
                                        .and("settlementHistory._id").as("settlementHistoryId")
                                        .and("settlementHistory.total_amount").as("totalAmount")
                                        .and("settlementHistory.batch_id").as("batchId")
                                        .and("settlementHistory.remaining_amount").as("remainingAmount")
                                        .and("settlementHistory.settled_amount").as("settledAmount")
                                        .and("settlementHistory.remark").as("remark")
                                        .and("settlementHistory.status").as("status")
                                        .and("settlementHistory.created_at").as("createdAt")

                        ).as("data")
                        .and(Aggregation.count().as("total"))
                        .as("metadata")
        );

        return template.aggregate(
                        aggregation,
                        "registration_settlement",
                        SettlementHistoryWrapper.class
                )
                .next()
                .defaultIfEmpty(new SettlementHistoryWrapper())
                .map(wrapper -> {

                    List<SettlementHistoryResponse> data =
                            wrapper.getData() == null ? List.of() : wrapper.getData();
                    long total =
                            (wrapper.getMetadata() != null && !wrapper.getMetadata().isEmpty())
                                    ? wrapper.getMetadata().get(0).getTotal()
                                    : 0;

                    int totalPages = (int) Math.ceil((double) total / limit);

                    return ApiResponse.<List<SettlementHistoryResponse>>builder()
                            .status(200)
                            .message(data.isEmpty() ? DATA_NOT_FOUND : DATA_FOUND)
                            .data(data)
                            .meta(ApiResponse.Meta.builder()
                                    .page(page)
                                    .limit(limit)
                                    .totalRecords(total)
                                    .totalPages(totalPages)
                                    .build())
                            .build();
                });
    }

    private Mono<Agents> resolveAgent(Users authUser, String reqAgentId) {

        if (authUser.isAgent()) {
            return agentRepository.findByUserId(authUser.getId())
                    .switchIfEmpty(Mono.error(new RecordNotFoundException(RECORD_NOT_FOUND)));
        }

        if ((authUser.isSuperUser() || authUser.isOrganization())
                && reqAgentId != null && !reqAgentId.isBlank()) {

            return agentRepository.findById(new ObjectId(reqAgentId))
                    .switchIfEmpty(Mono.error(new RecordNotFoundException(RECORD_NOT_FOUND)));
        }

        return Mono.error(new RecordNotFoundException(RECORD_NOT_FOUND));
    }

}
