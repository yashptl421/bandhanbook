package com.bandhanbook.app.conrollers;


import com.bandhanbook.app.config.currentUserConfig.CurrentUser;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.payload.request.LoginRequest;
import com.bandhanbook.app.payload.request.PhoneLoginRequest;
import com.bandhanbook.app.payload.request.RefreshRequest;
import com.bandhanbook.app.payload.request.UserRegisterRequest;
import com.bandhanbook.app.payload.response.LoginResponse;
import com.bandhanbook.app.payload.response.PhoneLoginResponse;
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

import static com.bandhanbook.app.utilities.SuccessResponseMessages.*;


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

    @Operation(summary = "Login from mobile application")
    @PostMapping("/login")
    public Mono<ResponseEntity<ApiResponse<Void>>> login(@RequestBody @Valid PhoneLoginRequest request) {
        return userService.login(request)
                .map(res -> ResponseEntity.ok(ApiResponse.<Void>builder()
                        .status(HttpStatus.OK.value())
                        .message(res)
                        .build()
                ));
    }

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

    @Operation(summary = "Logout from Application")
    @PostMapping("/logout")
    public Mono<ResponseEntity<ApiResponse<Void>>> logout(@CurrentUser Users users) {
        return userService.logout(users).thenReturn(ResponseEntity.ok(ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message(LOGGED_OUT).build()));
        /*return userService.logout(request.getRefreshToken())
                .thenReturn(ResponseEntity.ok().build());*/
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<ApiResponse<LoginResponse>>> refresh(@RequestBody RefreshRequest request) {
        return userService.refreshToken(request.getRefreshToken())
                .map(res -> ResponseEntity.ok(ApiResponse.<LoginResponse>builder()
                        .status(HttpStatus.OK.value())
                        .data(res)
                        .build()
                ));
    }

    @Operation(summary = "Get All users")
    @GetMapping
    public Mono<Users> getUsers() {
        return Mono.just(userService.getUsers());
    }


    /*@Operation(summary = "Verify Otp")
    @PostMapping("/verify-otp")
    public Mono<ResponseEntity<ApiResponse<LoginResponse>>> verifyOtp(@RequestBody @Valid PhoneLoginResponse request) {
        return userService.verifyOtp(request)
                .map(res -> ResponseEntity.ok(ApiResponse.<LoginResponse>builder()
                        .status(HttpStatus.OK.value())
                        .message(LOGGED_IN)
                        .data(res)
                        .build()
                ));
    }*/

}
