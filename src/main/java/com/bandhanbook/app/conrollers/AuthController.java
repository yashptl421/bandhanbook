package com.bandhanbook.app.conrollers;


import com.bandhanbook.app.config.MessageUtil;
import com.bandhanbook.app.config.currentUserConfig.CurrentUser;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.payload.request.*;
import com.bandhanbook.app.payload.response.LoginResponse;
import com.bandhanbook.app.payload.response.PhoneLoginResponse;
import com.bandhanbook.app.payload.response.base.ApiResponse;
import com.bandhanbook.app.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@Tag(name = "User Authentication API",
        description = "APIs for user registration, login, and authentication"
)
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final MessageUtil messageUtil;

    @Operation(summary = "Login from mobile application")
    @PostMapping("/login")
    public Mono<ResponseEntity<ApiResponse<Void>>> login(@RequestBody @Valid PhoneLoginRequest request) {
        return authService.login(request)
                .map(res -> ResponseEntity.ok(ApiResponse.<Void>builder()
                        .status(HttpStatus.OK.value())
                        .message(res)
                        .isOtp(res.equalsIgnoreCase(messageUtil.get("otp.sent")))
                        .build()
                ));
    }

    @Operation(summary = "Login from web application")
    @PostMapping("/web-login")
    public Mono<ResponseEntity<ApiResponse<LoginResponse>>> webLogin(@RequestBody @Valid LoginRequest loginRequest) {
        return authService.webLogin(loginRequest)
                .map(res -> ResponseEntity.ok(ApiResponse.<LoginResponse>builder()
                        .status(HttpStatus.OK.value())
                        .message(messageUtil.get("login.successful"))
                        .data(res)
                        .build()
                ));
    }

    @Operation(summary = "Admin or organization register")
    @PostMapping("/registerUser")
    public Mono<ResponseEntity<ApiResponse<Void>>> registerUser(@RequestBody @Valid UserRegisterRequest userRegisterRequest) {
        return authService.registerUser(userRegisterRequest).thenReturn(ResponseEntity.ok(new ApiResponse<>(
                messageUtil.get("candidate.registered"),
                HttpStatus.OK.value()
        )));
    }

    @Operation(summary = "Logout from Application")
    @PostMapping("/logout")
    public Mono<ResponseEntity<ApiResponse<Void>>> logout(@CurrentUser Users users) {
        return authService.logout(users)
                .thenReturn(ResponseEntity.ok(ApiResponse.<Void>builder()
                        .status(HttpStatus.OK.value())
                        .message(messageUtil.get("logout.successful")).build()));
        /*return authService.logout(request.getRefreshToken())
                .thenReturn(ResponseEntity.ok().build());*/
    }

    @Operation(summary = "provide refresh token")
    @PostMapping("/refresh")
    public Mono<ResponseEntity<ApiResponse<LoginResponse>>> refresh(@RequestBody RefreshRequest request) {
        return authService.refreshToken(request.getRefreshToken())
                .map(res -> ResponseEntity.ok(ApiResponse.<LoginResponse>builder()
                        .status(HttpStatus.OK.value())
                        .data(res)
                        .build()
                ));
    }

    @Operation(summary = "Verify Otp")
    @PostMapping("/verify-otp")
    public Mono<ResponseEntity<ApiResponse<PhoneLoginResponse>>> verifyOtp(@RequestBody @Valid PhoneLoginRequest request) {
        return authService.verifyOtp(request)
                .map(res -> ResponseEntity.ok(ApiResponse.<PhoneLoginResponse>builder()
                        .status(HttpStatus.OK.value())
                        .message(messageUtil.get("otp.verified"))
                        .data(res)
                        .build()
                ));
    }

    @Operation(summary = "Resend Otp to user's phone")
    @PostMapping("/resend-otp")
    public Mono<ResponseEntity<ApiResponse<Void>>> resendOtp(@RequestBody PhoneLoginRequest request) {
        return authService.resendOtp(request)
                .map(res -> ResponseEntity.ok(ApiResponse.<Void>builder()
                        .status(HttpStatus.OK.value())
                        .message(res)
                        .build()
                ));
    }


    @Operation(summary = "forgot password - resend Otp to user's phone")
    @PostMapping("/forgot-password")
    public Mono<ResponseEntity<ApiResponse<Void>>> forgotPassword(@RequestBody LoginRequest request) {
        return authService.forgotPassword(request)
                .map(res -> ResponseEntity.ok(ApiResponse.<Void>builder()
                        .status(HttpStatus.OK.value())
                        .message(res)
                        .build()
                ));
    }

    @Operation(summary = "change password using otp")
    @PostMapping("/change-password")
    public Mono<ResponseEntity<ApiResponse<Void>>> changePassword(@CurrentUser Users authUser, @RequestBody ChangePasswordRequest request) {
        return authService.changePassword(authUser, request)
                .map(res -> ResponseEntity.ok(ApiResponse.<Void>builder()
                        .status(HttpStatus.OK.value())
                        .message(res)
                        .build()
                ));
    }

}
