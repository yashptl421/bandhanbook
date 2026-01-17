package com.bandhanbook.app.conrollers;

import com.bandhanbook.app.model.City;
import com.bandhanbook.app.model.Country;
import com.bandhanbook.app.model.States;
import com.bandhanbook.app.payload.request.ContactUsRequest;
import com.bandhanbook.app.payload.response.base.ApiResponse;
import com.bandhanbook.app.payload.response.base.CommonApiResponse;
import com.bandhanbook.app.service.CommonService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;

import static com.bandhanbook.app.utilities.SuccessResponseMessages.CONTACT_US_SUCCESS;
import static com.bandhanbook.app.utilities.SuccessResponseMessages.DATA_FOUND;

@Slf4j
@Tag(name = "Common API",
        description = "APIs for get Country, States"
)
@RequiredArgsConstructor
@RestController
@RequestMapping("/common")
public class CommonController {

    @Autowired
    CommonService commonService;

    @GetMapping("/countries")
    public Mono<ResponseEntity<CommonApiResponse<List<Country>>>> getCountries() {
        return commonService.getCountry()
                .map(json -> ResponseEntity.ok(
                        CommonApiResponse.<List<Country>>builder()
                                .status(HttpStatus.OK.value())
                                .message(DATA_FOUND)
                                .data(json)
                                .totalRecords(json.size())
                                .build()
                ));
    }

    @GetMapping("/states/{id}")
    public Mono<ResponseEntity<CommonApiResponse<List<States>>>> getStates(@PathVariable String id) throws IOException {
        return commonService.getStates(Integer.parseInt(id)).collectList().map(json -> ResponseEntity.ok(
                CommonApiResponse.<List<States>>builder()
                        .status(HttpStatus.OK.value())
                        .message(DATA_FOUND)
                        .data(json)
                        .build()
        ));
    }

    @GetMapping("/cities/{id}")
    public Mono<ResponseEntity<CommonApiResponse<List<City>>>> getCities(@PathVariable String id) throws IOException {
        return commonService.getCities(Integer.parseInt(id)).collectList().map(json -> ResponseEntity.ok(
                CommonApiResponse.<List<City>>builder()
                        .status(HttpStatus.OK.value())
                        .message(DATA_FOUND)
                        .data(json)
                        .build()
        ));
    }

    @PostMapping("/contact-us")
    public Mono<ResponseEntity<ApiResponse<String>>> contactUs(@RequestBody ContactUsRequest request) {
        return commonService.contactUs(request).thenReturn(
                ResponseEntity.ok(ApiResponse.<String>builder()
                        .status(HttpStatus.OK.value())
                        .message(CONTACT_US_SUCCESS)
                        .build()));
    }
}
