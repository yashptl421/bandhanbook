package com.bandhanbook.app.conrollers;

import com.bandhanbook.app.config.currentUserConfig.CurrentUser;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.payload.request.AgentRequest;
import com.bandhanbook.app.payload.request.OrganizationRequest;
import com.bandhanbook.app.payload.response.AgentResponse;
import com.bandhanbook.app.payload.response.base.ApiResponse;
import com.bandhanbook.app.service.AgentService;
import com.bandhanbook.app.wrappers.AgentWrapper;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static com.bandhanbook.app.utilities.SuccessResponseMessages.*;

@RestController
@RequestMapping("/agent")
public class AgentController {
    @Autowired
    private AgentService agentService;

    @PostMapping()
    public Mono<ResponseEntity<ApiResponse<String>>> createAgent(@Valid @RequestBody AgentRequest request, @CurrentUser Users authUser) {
        return agentService.createAgent(request, authUser).thenReturn(ResponseEntity.ok(new ApiResponse<>(
                AGENT_CREATED,
                HttpStatus.OK.value()
        )));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<AgentResponse>>> showAgent(@PathVariable String id, @CurrentUser Users authUser) {
        return agentService.showAgent(id, authUser).map(response -> ResponseEntity.ok(
                ApiResponse.<AgentResponse>builder()
                        .status(HttpStatus.OK.value())
                        .message(DATA_FOUND)
                        .data(response)
                        .build()
        ));
    }
    @PutMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<String>>> updateAgent(@Valid @RequestBody AgentRequest req, @PathVariable String id) {
        return agentService.updateAgent(req, id).thenReturn(ResponseEntity.ok(new ApiResponse<>(
                ORGANIZATION_UPDATED,
                HttpStatus.OK.value()
        )));
    }

    @GetMapping("")
    public Mono<ResponseEntity<ApiResponse<List<AgentResponse>>>> listOrganization(@RequestParam Map<String, String> params, @CurrentUser Users authUser) {
        int page = Integer.parseInt(params.getOrDefault("page", "1"));
        int limit = Integer.parseInt(params.getOrDefault("limit", "10"));
        return agentService.listAgents(authUser, params, page, limit).map(res ->
        {
            AgentWrapper result = res.get(0);
            List<AgentResponse> data = result.getData();
            List<AgentWrapper.RecordCount> recordCount = result.getTotalRecords();

            long total = recordCount.isEmpty() ? 0 : recordCount.get(0).getTotalRecords();
            int totalRecords = (int) Math.ceil((double) total / limit);

            return ResponseEntity.ok().body(ApiResponse.<List<AgentResponse>>builder()
                    .status(HttpStatus.OK.value())
                    .message(DATA_FOUND)
                    .data(data)
                    .meta(new ApiResponse.Meta(page, limit, totalRecords, total))
                    .build());
        });
    }
}
