package com.bandhanbook.app.conrollers;

import com.bandhanbook.app.config.currentUserConfig.CurrentUser;
import com.bandhanbook.app.model.Banners;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.payload.request.BannerRequest;
import com.bandhanbook.app.payload.response.base.ApiResponse;
import com.bandhanbook.app.service.BannerService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/banners")
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    @Operation(summary = "Add Banner for the organization", description = "Banner image is required")
    @PostMapping(value = "/matrimony-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<ApiResponse<Banners>>> createBanner(@RequestPart("data") BannerRequest request, @RequestPart("file") FilePart file, @CurrentUser Users authUser) {
        return bannerService.createBanner(request, file, authUser)
                .map(banner -> ResponseEntity.ok(
                        ApiResponse.<Banners>builder()
                                .status(200)
                                .message("Banner Created successfully")
                                .data(banner)
                                .build()
                ));
    }
}
