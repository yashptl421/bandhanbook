package com.bandhanbook.app.conrollers;

import com.bandhanbook.app.config.currentUserConfig.CurrentUser;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.payload.request.BannerRequest;
import com.bandhanbook.app.payload.response.BannerResponse;
import com.bandhanbook.app.payload.response.base.ApiResponse;
import com.bandhanbook.app.service.BannerService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static com.bandhanbook.app.utilities.SuccessResponseMessages.BANNER_CREATED;
import static com.bandhanbook.app.utilities.SuccessResponseMessages.BANNER_UPDATED;

@RestController
@RequestMapping("/banner")
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<ApiResponse<BannerResponse>>> createBanner(@RequestPart("data") BannerRequest request, @RequestPart("file") FilePart file, @CurrentUser Users authUser) {
        return bannerService.createBanner(request, file, authUser)
                .map(banner -> ResponseEntity.ok(
                        ApiResponse.<BannerResponse>builder()
                                .status(200)
                                .message(BANNER_CREATED)
                                .data(banner)
                                .build()
                ));
    }

    @Operation(summary = "List of Banner for the organization", description = "Fetch paginated list of banners")
    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<BannerResponse>>>> listBanners(
            @RequestParam Map<String, String> params,
            @CurrentUser Users authUser
    ) {
        int page = Integer.parseInt(params.getOrDefault("page", "1"));
        int limit = Integer.parseInt(params.getOrDefault("limit", "10"));
        return bannerService.listBanners(authUser, page, limit)
                .map(res ->
                        ResponseEntity.ok(
                                ApiResponse.<List<BannerResponse>>builder()
                                        .status(200)
                                        .message("DATA_FOUND")
                                        .data(res.getData())
                                        .meta(res.getMeta())
                                        .activeCount(res.getActiveCount())
                                        .inactiveCount(res.getInactiveCount()).build()
                        ));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<BannerResponse>>> updateBannerStatus(
            @PathVariable String id,
            @RequestBody BannerRequest request
    ) {
        return bannerService.updateBanner(id, request.getIsActive())
                .map(updated ->
                        ResponseEntity.ok(
                                ApiResponse.<BannerResponse>builder()
                                        .status(200)
                                        .message(BANNER_UPDATED)
                                        .data(updated)
                                        .build()
                        )
                );
    }
}
