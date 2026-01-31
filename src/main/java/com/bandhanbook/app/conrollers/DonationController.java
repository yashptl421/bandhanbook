package com.bandhanbook.app.conrollers;

import com.bandhanbook.app.config.currentUserConfig.CurrentUser;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.payload.request.DonationCreateRequest;
import com.bandhanbook.app.payload.request.DonationUpdateRequest;
import com.bandhanbook.app.payload.response.DonationResponse;
import com.bandhanbook.app.payload.response.base.ApiResponse;
import com.bandhanbook.app.service.DonationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static com.bandhanbook.app.utilities.SuccessResponseMessages.*;

@RestController
@RequestMapping("/donations")
@RequiredArgsConstructor
public class DonationController {
    private final DonationService donationService;

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<String>>> create(@RequestBody @Valid DonationCreateRequest request, @CurrentUser Users authUser) {
        return donationService.createDonation(request, authUser)
                .map(response -> ResponseEntity.ok(
                        ApiResponse.<String>builder()
                                .status(HttpStatus.OK.value())
                                .message(response)
                                .build()
                ));
    }

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<DonationResponse>>>> list(@RequestParam Map<String, String> params, @CurrentUser Users authUser) {
        int page = Integer.parseInt(params.getOrDefault("page", "1"));
        int limit = Integer.parseInt(params.getOrDefault("limit", "10"));
        return donationService.listDonations(authUser, page, limit)
                .collectList()
                .map(response -> ResponseEntity.ok(
                        ApiResponse.<List<DonationResponse>>builder()
                                .status(HttpStatus.OK.value())
                                .message(DATA_FOUND)
                                .data(response)
                                .build()
                ));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<DonationResponse>>> update(@PathVariable String id, @RequestBody DonationUpdateRequest request) {
        return donationService.updateDonation(id, request)
                .map(response -> ResponseEntity.ok(
                        ApiResponse.<DonationResponse>builder()
                                .status(HttpStatus.OK.value())
                                .message(DONATION_UPDATED)
                                .data(response)
                                .build()
                ));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<String>>> delete(@PathVariable String id) {
        return donationService.deleteDonation(id)
                .thenReturn(ResponseEntity.ok(
                                ApiResponse.<String>builder()
                                        .status(200)
                                        .message(DONATION_DELETED)
                                        .build()
                        )
                );
    }
}
