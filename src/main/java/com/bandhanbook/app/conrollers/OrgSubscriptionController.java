package com.bandhanbook.app.conrollers;

import com.bandhanbook.app.config.currentUserConfig.CurrentUser;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.payload.request.BuySubscriptionRequest;
import com.bandhanbook.app.payload.response.SubscriptionResponse;
import com.bandhanbook.app.payload.response.base.ApiResponse;
import com.bandhanbook.app.service.OrgSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static com.bandhanbook.app.utilities.SuccessResponseMessages.DATA_FOUND;

@RestController
@RequestMapping("/subscription")
@RequiredArgsConstructor
public class OrgSubscriptionController {

    private final OrgSubscriptionService service;

    @GetMapping
    public Mono<ApiResponse<List<SubscriptionResponse>>> list(
            @CurrentUser Users authUser,
            @RequestParam Map<String, String> params
    ) {
        String orgid=null;
        if (params.containsKey("organization") && null != params.get("organization") && ObjectId.isValid(params.get("organization")))
            orgid = params.get("organization");
        return service.list(authUser, orgid)
                .map(res ->
                        ApiResponse.<List<SubscriptionResponse>>builder()
                                .status(200)
                                .message(DATA_FOUND)
                                .data(res)
                                .meta(ApiResponse.Meta.builder()
                                        .totalRecords(res.size())
                                        .build())
                                .build()
                );
    }

    @GetMapping("/{id}")
    public Mono<ApiResponse<SubscriptionResponse>> show(@PathVariable String id) {
        return service.show(id)
                .map(sub ->
                        ApiResponse.<SubscriptionResponse>builder()
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