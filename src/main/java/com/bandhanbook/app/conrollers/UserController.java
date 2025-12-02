package com.bandhanbook.app.conrollers;

import com.bandhanbook.app.config.currentUserConfig.CurrentUser;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.payload.request.UserRegisterRequest;
import com.bandhanbook.app.payload.response.base.ApiResponse;
import com.bandhanbook.app.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;


@Slf4j
@Tag(name = "User & Candidate",
        description = "APIs for user registration as candidate, update and delete"
)
@RequiredArgsConstructor
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Operation(summary = "Register a new Candidate", description = "Registers a new user with the provided details.")
    @PostMapping({"/signup", "/register"})
    public Mono<ResponseEntity<ApiResponse<String>>> register(@Valid @RequestBody UserRegisterRequest userRegisterRequest, @CurrentUser Users authUser) {
        return userService.register(userRegisterRequest, authUser).map(message -> ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .status(HttpStatus.OK.value())
                        .message(message)
                        .build()
        ));
    }
}
