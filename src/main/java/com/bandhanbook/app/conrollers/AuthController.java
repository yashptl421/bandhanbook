package com.bandhanbook.app.conrollers;


import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.payload.request.LoginRequest;
import com.bandhanbook.app.payload.request.UserRegisterRequest;
import com.bandhanbook.app.payload.response.LoginResponse;
import com.bandhanbook.app.payload.response.base.ApiResponse;
import com.bandhanbook.app.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import static com.bandhanbook.app.utilities.SuccessResponseMessages.LOGGED_IN;
import static com.bandhanbook.app.utilities.SuccessResponseMessages.USER_REGISTERED;


@Slf4j
@Tag(name = "User Authentication API",
        description = "APIs for user registration, login, and authentication"
)
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private final UserService userService;

    @Operation(summary = "Login from web application")
    @PostMapping("/web-login")
    public Mono<ResponseEntity<ApiResponse<LoginResponse>>> webLogin(@RequestBody @Valid LoginRequest loginRequest) {
        return userService.webLogin(loginRequest)
                .map(res -> ResponseEntity.ok(ApiResponse.<LoginResponse>builder()
                        .status(HttpStatus.OK.value())
                        .message(LOGGED_IN)
                        .data(res)
                        .build()
                ));
    }

    @Operation(summary = "Admin or organization register")
    @PostMapping("/registerUser")
    public Mono<ResponseEntity<ApiResponse<Void>>> registerUser(@RequestBody @Valid UserRegisterRequest userRegisterRequest) {
        return userService.registerUser(userRegisterRequest).thenReturn(ResponseEntity.ok(new ApiResponse<>(
                USER_REGISTERED,
                HttpStatus.OK.value()
        )));
    }

    @Operation(summary = "Get All users")
    @GetMapping
    public Mono<Users> getUsers() {

        return Mono.just(userService.getUsers());
    }
  /*  @Operation(summary = "Register a new user", description = "Registers a new user with the provided details.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User created successfully", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UserRegisterRequest.class)))
            ,
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UserRegisterRequest.class)))})
    @PostMapping({"/signup", "/register"})
    public Mono<UserRegisterResponse> register(@Valid @RequestBody UserRegisterRequest userRegisterRequest,   @RequestAttribute("user") Users authUser){
        return userService.register(userRegisterRequest)
                .map(user -> new UserRegisterResponse("Create user: " + userRegisterRequest.getFullName() + " successfully."))
                .onErrorResume(error -> Mono.just(new UserRegisterResponse(error.getMessage() !=null ? error.getMessage(): "Error occurred while register the user." )))
                .log();


    }*/
}
