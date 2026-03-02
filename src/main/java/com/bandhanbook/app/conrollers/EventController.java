package com.bandhanbook.app.conrollers;

import com.bandhanbook.app.config.MessageUtil;
import com.bandhanbook.app.config.currentUserConfig.CurrentUser;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.payload.request.EventRequest;
import com.bandhanbook.app.payload.response.EventDbResponse;
import com.bandhanbook.app.payload.response.EventResponse;
import com.bandhanbook.app.payload.response.base.ApiResponse;
import com.bandhanbook.app.service.EventService;
import com.bandhanbook.app.wrappers.EventWrapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "Event API",
        description = "APIs for Event get, add, update and delete"
)
@RequiredArgsConstructor
@RestController
@RequestMapping("/event")
public class EventController {
    private final EventService eventService;
    private final MessageUtil messageUtil;

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<EventResponse>>> show(@PathVariable String id) {
        return eventService.getEventById(new ObjectId(id))
                .map(response -> ResponseEntity.ok(
                        ApiResponse.<EventResponse>builder()
                                .status(HttpStatus.OK.value())
                                .message(messageUtil.get("records.found"))
                                .data(response)
                                .build()
                ));
    }

    @PostMapping()
    public Mono<ResponseEntity<ApiResponse<String>>> createEvent(@Valid @RequestBody EventRequest req, @CurrentUser Users user) {
        return eventService.createEvent(req, user).thenReturn(ResponseEntity.ok(new ApiResponse<>(
                messageUtil.get("event.created"),
                HttpStatus.OK.value()
        )));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<String>>> updateEvent(@Valid @RequestBody EventRequest req, @PathVariable String id) {
        return eventService.updateEvent(req, id).thenReturn(ResponseEntity.ok(new ApiResponse<>(
                messageUtil.get("event.updated"),
                HttpStatus.OK.value()
        )));
    }

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<EventDbResponse>>>> eventsList(@RequestParam Map<String, String> params, @CurrentUser Users authUser) {
        int page = Integer.parseInt(params.getOrDefault("page", "1"));
        int limit = Integer.parseInt(params.getOrDefault("limit", "10"));
        return eventService.eventsList(authUser, params, page, limit)
                .map(res -> {
                    List<EventDbResponse> data = res.getData();
                    List<EventWrapper.RecordCount> recordCount = res.getTotalRecords();
                    long total = recordCount.isEmpty() ? 0 : recordCount.get(0).getTotal();
                    int totalPage = (int) Math.ceil((double) total / limit);
                    return ResponseEntity.ok(
                            ApiResponse.<List<EventDbResponse>>builder()
                                    .status(HttpStatus.OK.value())
                                    .message(messageUtil.get("records.found"))
                                    .data(data)
                                    .meta(ApiResponse.Meta.builder()
                                            .page(page)
                                            .limit(limit)
                                            .totalRecords(total)
                                            .totalPages(totalPage)
                                            .build())
                                    .build()
                    );
                });
    }
}
