package com.bandhanbook.app.conrollers;

import com.bandhanbook.app.config.currentUserConfig.CurrentUser;
import com.bandhanbook.app.model.OrgSubscriptions;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.payload.request.BuySubscriptionRequest;
import com.bandhanbook.app.payload.response.base.ApiResponse;
import com.bandhanbook.app.service.OrgSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.bandhanbook.app.utilities.SuccessResponseMessages.DATA_FOUND;

@RestController
@RequestMapping("/subscription")
@RequiredArgsConstructor
public class OrgSubscriptionController {

    private final OrgSubscriptionService service;

    @GetMapping
    public Mono<ApiResponse<List<OrgSubscriptions>>> list(
            @CurrentUser Users authUser,
            @RequestParam(required = false) String organization
    ) {
        return service.list(authUser, organization)
                .map(tuple ->
                        ApiResponse.<List<OrgSubscriptions>>builder()
                                .status(200)
                                .message(DATA_FOUND)
                                .data(tuple.getT2())
                                .meta(ApiResponse.Meta.builder()
                                        .totalRecords(tuple.getT1())
                                        .build())
                                .build()
                );
    }

    @GetMapping("/{id}")
    public Mono<ApiResponse<OrgSubscriptions>> show(@PathVariable String id) {
        return service.show(id)
                .map(sub ->
                        ApiResponse.<OrgSubscriptions>builder()
                                .status(200)
                                .message(DATA_FOUND)
                                .data(sub)
                                .build()
                );
    }

    @PostMapping
    public Mono<ApiResponse<String>> buy(@RequestBody BuySubscriptionRequest req) {
        return service.buySubscription(req)
                .map(msg ->
                        ApiResponse.<String>builder()
                                .status(200)
                                .message(msg)
                                .build()
                );
    }

    @PutMapping("/{id}")
    public Mono<ApiResponse<String>> update(
            @PathVariable String id,
            @RequestParam boolean status
    ) {
        return service.updateStatus(id, status)
                .map(msg ->
                        ApiResponse.<String>builder()
                                .status(200)
                                .message(msg)
                                .build()
                );
    }
}