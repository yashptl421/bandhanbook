package com.bandhanbook.app.conrollers;

import com.bandhanbook.app.model.City;
import com.bandhanbook.app.model.Country;
import com.bandhanbook.app.model.States;
import com.bandhanbook.app.payload.response.base.CommonApiResponse;
import com.bandhanbook.app.service.CommonService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;

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
        return commonService.getStates(id).collectList().map(json -> ResponseEntity.ok(
                CommonApiResponse.<List<States>>builder()
                        .status(HttpStatus.OK.value())
                        .message(DATA_FOUND)
                        .data(json)
                        .build()
        ));
    }

    @GetMapping("/cities/{id}")
    public Mono<ResponseEntity<CommonApiResponse<List<City>>>> getCities(@PathVariable String id) throws IOException {
        return commonService.getCities(id).collectList().map(json -> ResponseEntity.ok(
                CommonApiResponse.<List<City>>builder()
                        .status(HttpStatus.OK.value())
                        .message(DATA_FOUND)
                        .data(json)
                        .build()
        ));
    }
}
