package com.bandhanbook.app.conrollers;

import com.bandhanbook.app.config.MessageUtil;
import com.bandhanbook.app.config.currentUserConfig.CurrentUser;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.model.constants.AddOnStatus;
import com.bandhanbook.app.payload.request.BuySubscriptionRequest;
import com.bandhanbook.app.payload.request.SubscriptionAddonRequest;
import com.bandhanbook.app.payload.response.SubscriptionAddonResponse;
import com.bandhanbook.app.payload.response.SubscriptionResponse;
import com.bandhanbook.app.payload.response.base.ApiResponse;
import com.bandhanbook.app.service.OrgSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/subscription")
@RequiredArgsConstructor
public class OrgSubscriptionController {

    private final OrgSubscriptionService service;
    private final MessageUtil messageUtil;

    @GetMapping
    public Mono<ApiResponse<List<SubscriptionResponse>>> list(@CurrentUser Users authUser, @RequestParam Map<String, String> params) {
        String orgId = null;
        if (params.containsKey("organization") && null != params.get("organization") && ObjectId.isValid(params.get("organization")))
            orgId = params.get("organization");
        return service.list(authUser, orgId)
                .map(res ->
                        ApiResponse.<List<SubscriptionResponse>>builder()
                                .status(HttpStatus.OK.value())
                                .message(messageUtil.get("records.found"))
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
                                .status(HttpStatus.OK.value())
                                .message(messageUtil.get("records.found"))
                                .data(sub)
                                .build()
                );
    }

    @PostMapping
    public Mono<ApiResponse<String>> buy(@RequestBody BuySubscriptionRequest req) {
        return service.buySubscription(req)
                .map(msg ->
                        ApiResponse.<String>builder()
                                .status(HttpStatus.OK.value())
                                .message(msg)
                                .build()
                );
    }

    @PutMapping("/{id}")
    public Mono<ApiResponse<String>> update(@PathVariable String id, @RequestParam boolean status) {
        return service.updateStatus(id, status)
                .map(msg ->
                        ApiResponse.<String>builder()
                                .status(HttpStatus.OK.value())
                                .message(msg)
                                .build()
                );
    }

    @GetMapping("/addon")
    public Mono<ApiResponse<List<SubscriptionAddonResponse>>> listAddons(@RequestParam Map<String, String> params) {
        String id = params.getOrDefault("subscriptionId", null);
        int page = Integer.parseInt(params.getOrDefault("page", "1"));
        int limit = Integer.parseInt(params.getOrDefault("limit", "10"));
        return service.listAddons(id, page, limit);
    }

    @PostMapping("/addon")
    public Mono<ApiResponse<String>> buyAddon(@RequestBody SubscriptionAddonRequest req) {
        return service.buyAddon(req)
                .map(msg ->
                        ApiResponse.<String>builder()
                                .status(HttpStatus.OK.value())
                                .message(msg)
                                .build()
                );
    }

    @PutMapping("/addon")
    public Mono<ApiResponse<String>> updateAddon(@CurrentUser Users authUser, @RequestParam String id, @RequestParam AddOnStatus status) {
        return service.updateAddonStatus(id, status, authUser)
                .map(msg ->
                        ApiResponse.<String>builder()
                                .status(HttpStatus.OK.value())
                                .message(msg)
                                .build()
                );
    }
}