package com.bandhanbook.app.conrollers;

import com.bandhanbook.app.config.currentUserConfig.CurrentUser;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.payload.request.AdvertisementRequest;
import com.bandhanbook.app.payload.response.AddvertisementResponse;
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
import java.util.Map;

import static com.bandhanbook.app.utilities.SuccessResponseMessages.DATA_FOUND;

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
    public Mono<ResponseEntity<ApiResponse<String>>> createAdvertisement(@RequestPart("data") AdvertisementRequest request, @RequestPart("files") Flux<FilePart> files, @CurrentUser Users authUser) {

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
    public Mono<ResponseEntity<ApiResponse<List<AddvertisementResponse>>>> advertisementList(@RequestParam Map<String, String> params, @CurrentUser Users authUser) {
        int page = Integer.parseInt(params.getOrDefault("page", "1"));
        int limit = Integer.parseInt(params.getOrDefault("limit", "10"));
        return advertisementService.advertisementList(authUser, page, limit)
                .map(tuple2 -> ResponseEntity.ok(
                        ApiResponse.<List<AddvertisementResponse>>builder()
                                .status(200)
                                .message(DATA_FOUND)
                                .data(tuple2.getT2())
                                .meta(ApiResponse.Meta.builder().page(page).limit(limit).totalRecords(tuple2.getT1()).totalPages((int) Math.ceil((double) tuple2.getT1() / limit)).build())
                                .build()));
    }

}
