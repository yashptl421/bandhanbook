package com.bandhanbook.app.conrollers;

import com.bandhanbook.app.config.currentUserConfig.CurrentUser;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.payload.request.AdvertisementFilterRequest;
import com.bandhanbook.app.payload.request.AdvertisementUpdateRequest;
import com.bandhanbook.app.payload.response.AdvertisementResponse;
import com.bandhanbook.app.payload.response.base.ApiResponse;
import com.bandhanbook.app.service.AdvertisementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.bandhanbook.app.utilities.SuccessResponseMessages.*;

@Slf4j
@Tag(name = "Advertisement API",
        description = "APIs for add update and delete Advertisement"
)
@RequiredArgsConstructor
@RestController
@RequestMapping("/advertisement")
public class AdvertisementController {

    private final AdvertisementService advertisementService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<ApiResponse<String>>> createAdvertisement(
            @RequestPart("data") String request,
            @RequestPart("files") Flux<FilePart> files,
            @CurrentUser Users authUser
    ) {

        return advertisementService.createAdvertisement(request, files, authUser).map(res ->
                ResponseEntity.ok(
                        ApiResponse.<String>builder()
                                .status(HttpStatus.OK.value())
                                .message(res)
                                .build()
                )
        );
    }

    @Operation(summary = "List of Advertisement for the organization", description = "Fetch paginated list of Advertisement")
    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<AdvertisementResponse>>>> advertisementList(AdvertisementFilterRequest filter, @CurrentUser Users authUser) {
        return advertisementService.advertisementList(filter, authUser)
                .map(tuple -> ResponseEntity.ok(
                        ApiResponse.<List<AdvertisementResponse>>builder()
                                .status(200)
                                .message(DATA_FOUND)
                                .data(tuple.getT3())
                                .meta(ApiResponse.Meta.builder().page(filter.getPage()).limit(filter.getLimit()).totalRecords(tuple.getT1()).totalPages((int) Math.ceil((double) tuple.getT1() / filter.getLimit())).build())
                                .activeCount(tuple.getT2())
                                .build()));
    }

    @PutMapping
    public Mono<ResponseEntity<ApiResponse<String>>> updateAdvertisement(
            @RequestBody List<AdvertisementUpdateRequest> requests
    ) {
        return advertisementService.bulkUpdate(requests)
                .thenReturn(ResponseEntity.ok(
                                ApiResponse.<String>builder()
                                        .status(HttpStatus.OK.value())
                                        .message(ADVERTISEMENT_UPDATED)
                                        .build()
                        )
                );
    }
    @DeleteMapping
    public Mono<ResponseEntity<ApiResponse<String>>> deleteAdvertisement(
            @RequestBody List<AdvertisementUpdateRequest> requests
    ) {
        return advertisementService.deleteAdvertisement(requests)
                .thenReturn(ResponseEntity.ok(
                                ApiResponse.<String>builder()
                                        .status(HttpStatus.OK.value())
                                        .message(ADVERTISEMENT_DELETED)
                                        .build()
                        )
                );
    }
}
