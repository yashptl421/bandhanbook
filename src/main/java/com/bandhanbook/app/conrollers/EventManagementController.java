package com.bandhanbook.app.conrollers;

import com.bandhanbook.app.config.currentUserConfig.CurrentUser;
import com.bandhanbook.app.exception.UnAuthorizedException;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.payload.request.RegistrationSettlementRequest;
import com.bandhanbook.app.payload.request.SettlementUpdateRequest;
import com.bandhanbook.app.payload.response.RegistrationSettlementResponse;
import com.bandhanbook.app.payload.response.SettlementHistoryResponse;
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

import static com.bandhanbook.app.utilities.ErrorResponseMessages.SETTLEMENT_ACCESS_ERROR;
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
    public Mono<ResponseEntity<ApiResponse<String>>> createRegistrationSettlement(@Valid @RequestBody RegistrationSettlementRequest req, @CurrentUser Users authUser) {

        return eventManagementService.createRegistrationSettlement(req, authUser).thenReturn(ResponseEntity.ok(new ApiResponse<>(
                SETTLEMENT_HISTORY_CREATED,
                HttpStatus.OK.value()
        )));
    }
    @PostMapping
    public Mono<ResponseEntity<ApiResponse<String>>> createDonationSettlement(@Valid @RequestBody RegistrationSettlementRequest req, @CurrentUser Users authUser) {

        return eventManagementService.createDonationSettlement(req, authUser).thenReturn(ResponseEntity.ok(new ApiResponse<>(
                SETTLEMENT_HISTORY_CREATED,
                HttpStatus.OK.value()
        )));
    }

    @PutMapping("/settlement")
    public Mono<ResponseEntity<ApiResponse<String>>> updateRegistrationSettlement(@RequestBody SettlementUpdateRequest request, @CurrentUser Users authUser) {

        return eventManagementService.updateRegistrationSettlement(request, authUser).thenReturn(ResponseEntity.ok(new ApiResponse<>(
                SETTLEMENT_HISTORY_UPDATED,
                HttpStatus.OK.value()
        )));
    }

    @GetMapping("")
    public Mono<ResponseEntity<ApiResponse<List<RegistrationSettlementResponse>>>> getAgentSettlementList(@CurrentUser Users authUser, @RequestParam(required = false) String agentId) {
        return eventManagementService.getAgentSettlementList(authUser, agentId).map(res ->
                ResponseEntity.ok(ApiResponse.<List<RegistrationSettlementResponse>>builder()
                        .message(DATA_FOUND)
                        .data(res)
                        .status(HttpStatus.OK.value())
                        .build())
        );
    }

    @GetMapping("/settlements")
    public Mono<ResponseEntity<ApiResponse<List<SettlementHistoryResponse>>>> getCloserList(@CurrentUser Users authUser, @RequestParam(required = false) String agentId) {
        if (!authUser.isOrganization()) {
            return Mono.error(new UnAuthorizedException(SETTLEMENT_ACCESS_ERROR));
        }
        return eventManagementService.getCloserList(authUser, agentId)
                .collectList()
                .map(res ->
                        ResponseEntity.ok(ApiResponse.<List<SettlementHistoryResponse>>builder()
                                .message(DATA_FOUND)
                                .data(res)
                                .status(HttpStatus.OK.value())
                                .build())
                );
    }
}
