package com.bandhanbook.app.conrollers;

import com.bandhanbook.app.model.PricingPlans;
import com.bandhanbook.app.payload.request.OrganizationRequest;
import com.bandhanbook.app.payload.response.OrganizationResponse;
import com.bandhanbook.app.payload.response.base.ApiResponse;
import com.bandhanbook.app.payload.response.base.CommonApiResponse;
import com.bandhanbook.app.service.OrganizationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static com.bandhanbook.app.utilities.SuccessResponseMessages.*;


@Slf4j
@Tag(name = "Organization API",
        description = "APIs for Organization get, add, update and delete"
)
@RequiredArgsConstructor
@RestController
@RequestMapping("/organization")
public class OrganizationController {

    @Autowired
    private final OrganizationService organizationService;

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<OrganizationResponse>>> show(@PathVariable String id) {
        return organizationService.getOrganizationById(id)
                .map(response -> ResponseEntity.ok(
                        ApiResponse.<OrganizationResponse>builder()
                                .status(HttpStatus.OK.value())
                                .message(DATA_FOUND)
                                .data(response)
                                .build()
                ));
    }

    @GetMapping("")
    public Mono<ResponseEntity<ApiResponse<List<OrganizationResponse>>>> listOrganization(@RequestParam Map<String, String> params) {
        int page = Integer.parseInt(params.getOrDefault("page", "1"));
        int limit = Integer.parseInt(params.getOrDefault("limit", "10"));
        return organizationService.listOrganizations(params).map(tuple -> ResponseEntity.ok(
                ApiResponse.<List<OrganizationResponse>>builder()
                        .status(HttpStatus.OK.value())
                        .message(DATA_FOUND)
                        .data(tuple.getT2())
                        .meta(ApiResponse.Meta.builder().page(page).limit(limit).totalPages((int) Math.ceil((double) tuple.getT1() / limit)).totalRecords(tuple.getT1()).build())
                        .build()
        ));
    }

    @PostMapping()
    public Mono<ResponseEntity<ApiResponse<String>>> createOrganization(@Valid @RequestBody OrganizationRequest req) {
        return organizationService.createOrganization(req).thenReturn(ResponseEntity.ok(new ApiResponse<>(
                ORGANIZATION_CREATED,
                HttpStatus.OK.value()
        )));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<String>>> updateOrganization(@Valid @RequestBody OrganizationRequest req, @PathVariable String id) {
        return organizationService.updateOrganization(req, id).thenReturn(ResponseEntity.ok(new ApiResponse<>(
                ORGANIZATION_UPDATED,
                HttpStatus.OK.value()
        )));
    }

    @GetMapping("/pricing-plan")
    public Mono<ResponseEntity<CommonApiResponse<List<PricingPlans>>>> getPricingPlans() {
        return organizationService.getPricingPlans()
                .map(json -> ResponseEntity.ok(
                        CommonApiResponse.<List<PricingPlans>>builder()
                                .status(HttpStatus.OK.value())
                                .message(DATA_FOUND)
                                .data(json)
                                .totalRecords(json.size())
                                .build()
                ));
    }
}
