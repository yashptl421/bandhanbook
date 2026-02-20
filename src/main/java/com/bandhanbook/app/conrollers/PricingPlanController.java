package com.bandhanbook.app.conrollers;

import com.bandhanbook.app.payload.request.PricingPlanRequest;
import com.bandhanbook.app.payload.response.PricingPlanResponse;
import com.bandhanbook.app.payload.response.base.ApiResponse;
import com.bandhanbook.app.service.PricingPlanService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.bandhanbook.app.utilities.SuccessResponseMessages.DATA_FOUND;

@RestController
@RequestMapping("/pricing-plans")
@RequiredArgsConstructor
public class PricingPlanController {

    private final PricingPlanService service;

    @PostMapping
    public Mono<ApiResponse<PricingPlanResponse>> create(@RequestBody PricingPlanRequest request) {
        return service.create(request).map(response ->
                ApiResponse.<PricingPlanResponse>builder()
                        .status(HttpStatus.OK.value())
                        .message(DATA_FOUND)
                        .data(response)
                        .build()
        );
    }

    @GetMapping("/active")
    public Flux<ApiResponse<PricingPlanResponse>> activePlans() {
        return service.getActivePlans().map(response ->
                ApiResponse.<PricingPlanResponse>builder()
                        .status(HttpStatus.OK.value())
                        .message(DATA_FOUND)
                        .data(response)
                        .build()
        );
    }

    @GetMapping
    public Flux<ApiResponse<PricingPlanResponse>> allPlans() {
        return service.getAllPlans().map(response ->
                ApiResponse.<PricingPlanResponse>builder()
                        .status(HttpStatus.OK.value())
                        .message(DATA_FOUND)
                        .data(response)
                        .build()
        );
    }

    @PutMapping("/{id}")
    public Mono<ApiResponse<PricingPlanResponse>> update(
            @PathVariable String id,
            @RequestBody PricingPlanRequest request) {
        return service.update(new ObjectId(id), request).map(response ->
                ApiResponse.<PricingPlanResponse>builder()
                        .status(HttpStatus.OK.value())
                        .message(DATA_FOUND)
                        .data(response)
                        .build()
        );
    }

    @PatchMapping("/{id}/status")
    public Mono<Void> updateStatus(@PathVariable String id, @RequestParam boolean active) {
        return service.updateStatus(new ObjectId(id), active);
    }
}