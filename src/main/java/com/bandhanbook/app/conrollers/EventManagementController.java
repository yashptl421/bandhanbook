package com.bandhanbook.app.conrollers;

import com.bandhanbook.app.config.currentUserConfig.CurrentUser;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.payload.request.RegistrationSettlementRequest;
import com.bandhanbook.app.payload.request.SettlementUpdateRequest;
import com.bandhanbook.app.payload.response.RegistrationSettlementResponse;
import com.bandhanbook.app.payload.response.SettlementHistoryResponse;
import com.bandhanbook.app.payload.response.SettlementSummaryResponse;
import com.bandhanbook.app.payload.response.base.ApiResponse;
import com.bandhanbook.app.service.EventManagementService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static com.bandhanbook.app.utilities.SuccessResponseMessages.*;

@Slf4j
@Tag(name = "Event Management API",
        description = "APIs for Event collection get, add, update and delete"
)
@RequiredArgsConstructor
@RestController
@RequestMapping("/event-management")
public class EventManagementController {

    private final EventManagementService eventManagementService;

    @PostMapping("/settlement")
    public Mono<ResponseEntity<ApiResponse<String>>> createSettlement(@Valid @RequestBody RegistrationSettlementRequest req, @CurrentUser Users authUser) {

        return eventManagementService.createRegistrationSettlement(req, authUser).thenReturn(ResponseEntity.ok(new ApiResponse<>(
                SETTLEMENT_HISTORY_CREATED,
                HttpStatus.OK.value()
        )));
    }

    @PutMapping("/settlement")
    public Mono<ResponseEntity<ApiResponse<String>>> updateSettlement(@RequestBody SettlementUpdateRequest request, @CurrentUser Users authUser) {

        return eventManagementService.updateRegistrationSettlement(request, authUser).thenReturn(ResponseEntity.ok(new ApiResponse<>(
                SETTLEMENT_HISTORY_UPDATED,
                HttpStatus.OK.value()
        )));
    }

    @GetMapping("/settlement")
    public Mono<ResponseEntity<ApiResponse<List<RegistrationSettlementResponse>>>> getAgentSettlementList(@CurrentUser Users authUser, @RequestParam Map<String, String> params) {
        String agentId = params.getOrDefault("agentId", "");
        String eventId = params.getOrDefault("eventId", "");
        return eventManagementService.getAgentSettlementList(authUser, agentId, eventId).map(res ->
                ResponseEntity.ok(ApiResponse.<List<RegistrationSettlementResponse>>builder()
                        .message(DATA_FOUND)
                        .data(res)
                        .status(HttpStatus.OK.value())
                        .build())
        );
    }

    @GetMapping("/settlement/{id}")
    public Mono<ResponseEntity<ApiResponse<RegistrationSettlementResponse>>> getSettlementById(@PathVariable String id, @CurrentUser Users authUser) {
        return eventManagementService.getSettlementById(id, authUser)
                .map(res -> ResponseEntity.ok(ApiResponse.<RegistrationSettlementResponse>builder()
                        .message(DATA_FOUND)
                        .data(res)
                        .status(HttpStatus.OK.value())
                        .build()));
    }

    @GetMapping("/settlement/history/{id}")
    public Mono<ResponseEntity<ApiResponse<SettlementHistoryResponse>>> getSettlementHistoryById(@PathVariable String id, @CurrentUser Users authUser) {
        return eventManagementService.getSettlementHistoryById(id, authUser)
                .map(res -> ResponseEntity.ok(ApiResponse.<SettlementHistoryResponse>builder()
                        .message(DATA_FOUND)
                        .data(res)
                        .status(HttpStatus.OK.value())
                        .build()));
    }

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<SettlementHistoryResponse>>>> getCloserList(@CurrentUser Users authUser, @RequestParam Map<String, String> params) {
        int page = Integer.parseInt(params.getOrDefault("page", "1"));
        int limit = Integer.parseInt(params.getOrDefault("limit", "10"));
        String agentId = params.getOrDefault("agentId", "");
        return eventManagementService.getCloserList(authUser, params, page, limit)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/settlement/summary")
    public Mono<ResponseEntity<ApiResponse<SettlementSummaryResponse>>> getSettlementSummary(@CurrentUser Users authUser, @RequestParam(required = false) String eventId) {
        return eventManagementService.getSettlementSummary(authUser, eventId)
                .map(res -> ResponseEntity.ok(ApiResponse.<SettlementSummaryResponse>builder()
                        .message(DATA_FOUND)
                        .data(res)
                        .status(HttpStatus.OK.value())
                        .build()));
    }
}
