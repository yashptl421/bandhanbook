package com.bandhanbook.app.conrollers;

import com.bandhanbook.app.config.currentUserConfig.CurrentUser;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.payload.request.RegistrationSettlementRequest;
import com.bandhanbook.app.payload.request.SettlementUpdateRequest;
import com.bandhanbook.app.payload.response.RegistrationSettlementResponse;
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

/*    @PostMapping()
    public Mono<ResponseEntity<ApiResponse<String>>> createCollection(@Valid @RequestBody RegistrationSettlementRequest req) {

        return eventManagementService.createCollection(req).thenReturn(ResponseEntity.ok(new ApiResponse<>(
                AGENT_HISTORY_CREATED,
                HttpStatus.OK.value()
        )));
    }*/

    @PostMapping("/settlement")
    public Mono<ResponseEntity<ApiResponse<String>>> createRegistrationSettlement(@Valid @RequestBody RegistrationSettlementRequest req, @CurrentUser Users authUser) {

        return eventManagementService.createRegistrationSettlement(req, authUser).thenReturn(ResponseEntity.ok(new ApiResponse<>(
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
    public Mono<ResponseEntity<ApiResponse<RegistrationSettlementResponse>>> getCollectionList(@CurrentUser Users authUser, @RequestParam(required = false) String agentId) {
        return eventManagementService.getAgentCollectionList(authUser, agentId).map(res ->
                ResponseEntity.ok(ApiResponse.<RegistrationSettlementResponse>builder()
                        .message(DATA_FOUND)
                        .data(res)
                        .status(HttpStatus.OK.value())
                        .build())
        );
    }
}
