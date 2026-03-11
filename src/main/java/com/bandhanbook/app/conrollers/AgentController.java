package com.bandhanbook.app.conrollers;

import com.bandhanbook.app.config.MessageUtil;
import com.bandhanbook.app.config.currentUserConfig.CurrentUser;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.payload.request.AgentRequest;
import com.bandhanbook.app.payload.response.AgentResponse;
import com.bandhanbook.app.payload.response.base.ApiResponse;
import com.bandhanbook.app.service.AgentService;
import com.bandhanbook.app.wrappers.AgentWrapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {
    private final AgentService agentService;
    private final MessageUtil messageUtil;

    @PostMapping()
    public Mono<ResponseEntity<ApiResponse<String>>> createAgent(@Valid @RequestBody AgentRequest request, @CurrentUser Users authUser) {
        return agentService.createAgent(request, authUser)
                .map(res -> ResponseEntity.ok(
                        ApiResponse.<String>builder()
                                .status(HttpStatus.OK.value())
                                .message(res)
                                .build()
                ));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<AgentResponse>>> showAgent(@PathVariable String id) {
        return agentService.showAgent(new ObjectId(id)).map(response -> ResponseEntity.ok(
                ApiResponse.<AgentResponse>builder()
                        .status(HttpStatus.OK.value())
                        .message(messageUtil.get("records.found"))
                        .data(response)
                        .build()
        ));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<String>>> updateAgent(@Valid @RequestBody AgentRequest req, @PathVariable String id) {
        return agentService.updateAgent(req, new ObjectId(id)).map(res -> ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .status(HttpStatus.OK.value())
                        .message(res)
                        .build()));
    }

    @GetMapping("")
    public Mono<ResponseEntity<ApiResponse<List<AgentResponse>>>> listAgent(@RequestParam Map<String, String> params, @CurrentUser Users authUser) {
        int page = Integer.parseInt(params.getOrDefault("page", "1"));
        int limit = Integer.parseInt(params.getOrDefault("limit", "10"));
        return agentService.listAgents(authUser, params, page, limit).map(res ->
        {
            List<AgentResponse> data = res.getData();
            List<AgentWrapper.RecordCount> recordCount = res.getTotalRecords();

            long total = recordCount.isEmpty() ? 0 : recordCount.get(0).getTotalRecords();
            int totalRecords = (int) Math.ceil((double) total / limit);

            return ResponseEntity.ok().body(ApiResponse.<List<AgentResponse>>builder()
                    .status(HttpStatus.OK.value())
                    .message(messageUtil.get("records.found"))
                    .data(data)
                    .meta(new ApiResponse.Meta(page, limit, totalRecords, total))
                    .isOtp(null)
                    .isFavorite(null)
                    .build());
        });
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<String>>> updateAgent(@PathVariable String id, @CurrentUser Users authUser) {
        return agentService.deleteAgent(new ObjectId(id), authUser).map(res -> ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .status(HttpStatus.OK.value())
                        .message(res)
                        .build()));
    }
}
