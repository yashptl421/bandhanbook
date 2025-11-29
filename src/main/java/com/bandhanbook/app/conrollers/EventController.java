package com.bandhanbook.app.conrollers;

import com.bandhanbook.app.config.currentUserConfig.CurrentUser;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.payload.request.EventRequest;
import com.bandhanbook.app.payload.response.EventResponse;
import com.bandhanbook.app.payload.response.base.ApiResponse;
import com.bandhanbook.app.service.EventService;
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
@Tag(name = "Event API",
        description = "APIs for Event get, add, update and delete"
)
@RequiredArgsConstructor
@RestController
@RequestMapping("/event")
public class EventController {
    @Autowired
    private final EventService eventService;

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<EventResponse>>> show(@PathVariable String id) {
        return eventService.getEventById(id)
                .map(response -> ResponseEntity.ok(
                        ApiResponse.<EventResponse>builder()
                                .status(HttpStatus.OK.value())
                                .message(DATA_FOUND)
                                .data(response)
                                .build()
                ));
    }

    @PostMapping()
    public Mono<ResponseEntity<ApiResponse<String>>> createEvent(@Valid @RequestBody EventRequest req, @CurrentUser Users user) {
        return eventService.createEvent(req, user).thenReturn(ResponseEntity.ok(new ApiResponse<>(
                EVENT_CREATED,
                HttpStatus.OK.value()
        )));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<String>>> updateEvent(@Valid @RequestBody EventRequest req, @PathVariable String id) {
        return eventService.updateEvent(req, id).thenReturn(ResponseEntity.ok(new ApiResponse<>(
                EVENT_UPDATED,
                HttpStatus.OK.value()
        )));
    }

    @GetMapping("")
    public Mono<ResponseEntity<ApiResponse<List<EventResponse>>>> eventsList(@RequestParam Map<String, String> params) {
        int page = Integer.parseInt(params.getOrDefault("page", "1"));
        int limit = Integer.parseInt(params.getOrDefault("limit", "10"));
        return eventService.eventsList(params).map(tuple -> ResponseEntity.ok(
                ApiResponse.<List<EventResponse>>builder()
                        .status(HttpStatus.OK.value())
                        .message(DATA_FOUND)
                        .data(tuple.getT2())
                        .meta(ApiResponse.Meta.builder().page(page).limit(limit).totalPages((int) Math.ceil((double) tuple.getT1() / limit)).totalRecords(tuple.getT1()).build())
                        .build()
        ));
    }
}
